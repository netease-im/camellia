package com.netease.nim.camellia.redis.proxy.plugin.rewrite;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyRequest;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/10/7
 */
public class DefaultRouteRewriteChecker implements RouteRewriteChecker {

    private final ConcurrentHashMap<String, RewriteConfig> configCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RouteRewriteResult> commandCache = new ConcurrentHashMap<>();

    public DefaultRouteRewriteChecker() {
        ProxyDynamicConf.registerCallback(() -> {
            configCache.clear();
            commandCache.clear();
        });
    }

    @Override
    public RouteRewriteResult checkRewrite(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            RedisCommand redisCommand = command.getRedisCommand();
            CommandContext commandContext = command.getCommandContext();

            //cache检查
            String commandCacheKey = commandContext.getBid() + "|" + commandContext.getBgroup() + "|" + redisCommand.strRaw();
            RouteRewriteResult rewriterResult = commandCache.get(commandCacheKey);
            if (rewriterResult != null) {
                return rewriterResult;
            }

            String configStr = ProxyDynamicConf.getString("hotkey.route.rewrite.config", commandContext.getBid(), commandContext.getBgroup(), null);
            if (configStr == null) {
                configStr = ProxyDynamicConf.getString("hotkey.route.rewrite.default.config", null);
            }
            if (configStr == null) {
                return null;
            }
            RewriteConfig rewriteConfig = configCache.get(configStr);
            if (rewriteConfig == null) {
                rewriteConfig = init(configStr);
                configCache.put(configStr, rewriteConfig);
            }
            //先检查精确的command
            for (RewriteConfig.Config config : rewriteConfig.configList) {
                if (config.command.equals(redisCommand.strRaw())) {
                    commandCache.put(commandCacheKey, config.result);
                    return config.result;
                }
            }
            //再检查read/write的
            if (redisCommand.getType() == RedisCommand.Type.READ && rewriteConfig.readCommands != null) {
                commandCache.put(commandCacheKey, rewriteConfig.readCommands.result);
                return rewriteConfig.readCommands.result;
            }
            if (redisCommand.getType() == RedisCommand.Type.WRITE && rewriteConfig.writeCommands != null) {
                commandCache.put(commandCacheKey, rewriteConfig.writeCommands.result);
                return rewriteConfig.writeCommands.result;
            }
            //再检查兜底的
            if (rewriteConfig.allCommands != null) {
                commandCache.put(commandCacheKey, rewriteConfig.allCommands.result);
                return rewriteConfig.allCommands.result;
            }
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(DefaultRouteRewriteChecker.class, "check rewrite error", e);
            return null;
        }
    }

    private RewriteConfig init(String configStr) {
        try {
            if (configStr == null || configStr.length() == 0) {
                return new RewriteConfig();
            }
            List<Config> configs = JSONArray.parseArray(configStr, Config.class);
            if (configs == null || configs.isEmpty()) {
                return new RewriteConfig();
            }
            RewriteConfig rewriteConfig = new RewriteConfig();
            for (Config config : configs) {
                RouteRewriteResult rewriterResult = new RouteRewriteResult(config.bid, config.bgroup);
                RewriteConfig.Config configure = new RewriteConfig.Config(config.command.toLowerCase(), rewriterResult);
                if (config.command.equalsIgnoreCase("all_commands")) {
                    rewriteConfig.allCommands = configure;
                    continue;
                }
                if (config.command.equalsIgnoreCase("read_commands")) {
                    rewriteConfig.readCommands = configure;
                    continue;
                }
                if (config.command.equalsIgnoreCase("write_commands")) {
                    rewriteConfig.writeCommands = configure;
                    continue;
                }
                rewriteConfig.configList.add(configure);
            }
            return rewriteConfig;
        } catch (Exception e) {
            ErrorLogCollector.collect(DefaultRouteRewriteChecker.class, "init rewrite config error, config = " + configStr, e);
            return new RewriteConfig();
        }
    }

    private static class RewriteConfig {
        private final List<Config> configList = new ArrayList<>();
        private Config allCommands;
        private Config readCommands;
        private Config writeCommands;

        private static class Config {
            private final String command;
            private final RouteRewriteResult result;

            public Config(String command, RouteRewriteResult result) {
                this.command = command;
                this.result = result;
            }
        }
    }

    private static class Config {
        private String command;
        private long bid;
        private String bgroup;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public long getBid() {
            return bid;
        }

        public void setBid(long bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }
    }

}

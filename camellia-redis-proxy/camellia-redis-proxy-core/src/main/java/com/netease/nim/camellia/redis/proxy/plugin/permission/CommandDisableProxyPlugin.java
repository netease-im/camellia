package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2022/10/24
 */
public class CommandDisableProxyPlugin implements ProxyPlugin {

    private static final Logger logger = LoggerFactory.getLogger(CommandDisableProxyPlugin.class);
    private Set<RedisCommand> disabledCommands = new HashSet<>();
    private Map<RedisCommand, ProxyPluginResponse> errorReplyMap = new HashMap<>();

    @Override
    public void init(ProxyBeanFactory factory) {
        reload();
        ProxyDynamicConf.registerCallback(this::reload);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.COMMAND_DISABLE_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.COMMAND_DISABLE_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        Command command = request.getCommand();
        if (command != null) {
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand != null) {
                if (disabledCommands.contains(redisCommand)) {
                    ProxyPluginResponse response = errorReplyMap.get(redisCommand);
                    if (response == null) {
                        response = new ProxyPluginResponse(false, new ErrorReply("ERR command '" + redisCommand.strRaw() + "' is disabled in proxy"));
                    }
                    return response;
                }
            }
        }
        return ProxyPluginResponse.SUCCESS;
    }

    private void reload() {
        try {
            String string = ProxyDynamicConf.getString("disabled.commands", "");
            if (string == null || string.trim().length() == 0) {
                this.disabledCommands = new HashSet<>();
            } else {
                String[] split = string.split(",");
                Set<RedisCommand> disabledCommands = new HashSet<>();
                for (String str : split) {
                    if (str == null || str.trim().length() == 0) continue;
                    RedisCommand command = RedisCommand.getSupportRedisCommandByName(str.trim().toLowerCase());
                    if (command == null) {
                        logger.warn("command = {} not support by proxy, skip disable", str);
                    } else {
                        disabledCommands.add(command);
                    }
                }
                this.disabledCommands = disabledCommands;
            }
            logger.info("disabled.commands update, commands = {}", disabledCommands);
            Map<RedisCommand, ProxyPluginResponse> errorReplyMap = new HashMap<>();
            for (RedisCommand command : this.disabledCommands) {
                ProxyPluginResponse response = new ProxyPluginResponse(false, new ErrorReply("ERR command '" + command.strRaw() + "' is disabled in proxy"));
                errorReplyMap.put(command, response);
            }
            this.errorReplyMap = errorReplyMap;
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }
}

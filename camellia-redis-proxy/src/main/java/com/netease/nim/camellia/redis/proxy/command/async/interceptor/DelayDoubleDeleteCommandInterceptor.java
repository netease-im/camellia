package com.netease.nim.camellia.redis.proxy.command.async.interceptor;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/6/8
 */
public class DelayDoubleDeleteCommandInterceptor implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DelayDoubleDeleteCommandInterceptor.class);

    private final ConcurrentHashMap<String, Set<String>> keyPrefixCacheMap = new ConcurrentHashMap<>();

    public DelayDoubleDeleteCommandInterceptor() {
        ProxyDynamicConf.registerCallback(() -> {
            try {
                keyPrefixCacheMap.clear();
            } catch (Exception e) {
                logger.error("cache clear error", e);
            }
        });
    }

    @Override
    public CommandInterceptResponse check(Command command) {
        try {
            if (command == null) return CommandInterceptResponse.SUCCESS;
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand != RedisCommand.DEL) {
                return CommandInterceptResponse.SUCCESS;
            }
            CommandContext commandContext = command.getCommandContext();
            Long bid = commandContext.getBid();
            String bgroup = commandContext.getBgroup();
            boolean enable = ProxyDynamicConf.getBoolean("delay.double.del.enable", bid, bgroup, false);
            if (!enable) {
                return CommandInterceptResponse.SUCCESS;
            }
            int delaySeconds = ProxyDynamicConf.getInt("double.del.delay.seconds", bid, bgroup, -1);
            if (delaySeconds <= 0) {
                return CommandInterceptResponse.SUCCESS;
            }
            Set<String> keyPrefix = getKeyPrefix(bid, bgroup);
            if (keyPrefix.isEmpty()) {
                return CommandInterceptResponse.SUCCESS;
            }
            if (keyPrefix.contains("")) {
                //所有key都要双删
                ExecutorUtils.newTimeout(timeout -> {
                    try {
                        AsyncCamelliaRedisTemplate template = ProxyInfoUtils.getAsyncCamelliaRedisTemplateChooser().choose(bid, bgroup);
                        template.sendCommand(Collections.singletonList(command));
                    } catch (Exception e) {
                        ErrorLogCollector.collect(DelayDoubleDeleteCommandInterceptor.class, "delay double del invoke error", e);
                    }
                }, delaySeconds, TimeUnit.SECONDS);
            } else {
                //只有部分key需要双删
                for (int i = 1; i < command.getObjects().length; i++) {
                    byte[] key = command.getObjects()[i];
                    boolean needDoubleDel = false;
                    for (String prefix : keyPrefix) {
                        if (Utils.bytesToString(key).startsWith(prefix)) {
                            needDoubleDel = true;
                            break;
                        }
                    }
                    if (needDoubleDel) {
                        ExecutorUtils.newTimeout(timeout -> {
                            try {
                                AsyncCamelliaRedisTemplate template = ProxyInfoUtils.getAsyncCamelliaRedisTemplateChooser().choose(bid, bgroup);
                                template.sendCommand(Collections.singletonList(new Command(new byte[][]{RedisCommand.DEL.raw(), key})));
                            } catch (Exception e) {
                                ErrorLogCollector.collect(DelayDoubleDeleteCommandInterceptor.class, "delay double del invoke error", e);
                            }
                        }, delaySeconds, TimeUnit.SECONDS);
                    }
                }
            }
            return CommandInterceptResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(DelayDoubleDeleteCommandInterceptor.class, "delay double del error", e);
            return CommandInterceptResponse.SUCCESS;
        }
    }

    public Set<String> getKeyPrefix(Long bid, String bgroup) {
        String cacheKey = bid + "|" + bgroup;
        Set<String> set = keyPrefixCacheMap.get(cacheKey);
        if (set != null) return set;
        try {
            String string = ProxyDynamicConf.getString("double.del.key.prefix", bid, bgroup, null);
            set = new HashSet<>();
            if (string != null) {
                JSONArray array = JSONArray.parseArray(string);
                if (array != null) {
                    for (Object o : array) {
                        set.add(String.valueOf(o));
                    }
                }
            }
            keyPrefixCacheMap.put(cacheKey, set);
            return set;
        } catch (Exception e) {
            set = new HashSet<>();
            keyPrefixCacheMap.put(cacheKey, set);
            return set;
        }
    }
}

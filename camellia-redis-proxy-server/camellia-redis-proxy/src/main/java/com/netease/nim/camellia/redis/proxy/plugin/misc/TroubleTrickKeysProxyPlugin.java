package com.netease.nim.camellia.redis.proxy.plugin.misc;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个用于临时屏蔽某些key的Plugin，可以通过ProxyDynamicConf动态修改相关配置
 * 使用场景：
 * 由于业务侧bug或者其他一些原因，某些key成为热点key（比如死循环调用）或者大key，为了避免持续的请求导致后端redis服务异常（比如cpu持续高负荷运转或者被打挂）
 * 可以使用TroubleTrickKeysCommandInterceptor临时配置某些key的某些command为TroubleTrick，则针对这些key的这些方法会直接返回异常（快速失败），从而保护后端redis服务
 * Created by caojiajun on 2022/9/14
 */
public class TroubleTrickKeysProxyPlugin implements ProxyPlugin {

    private static final ProxyPluginResponse TROUBLE_TRICK_ERROR = new ProxyPluginResponse(false, "ERR trouble trick key fast fail");
    private final ConcurrentHashMap<CacheKey, Map<RedisCommand, TroubleTrickKeys>> cache = new ConcurrentHashMap<>();

    @Override
    public void init(ProxyBeanFactory factory) {
        ProxyDynamicConf.registerCallback(cache::clear);
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.TROUBLE_TRICK_KEYS_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.TROUBLE_TRICK_KEYS_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            if (command == null) {
                return ProxyPluginResponse.SUCCESS;
            }
            Long bid = command.getChannelInfo().getBid();
            String bgroup = command.getChannelInfo().getBgroup();

            Map<RedisCommand, TroubleTrickKeys> map = get(new CacheKey(bid, bgroup));
            if (map == null || map.isEmpty()) {
                return ProxyPluginResponse.SUCCESS;
            }
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == null) {
                return ProxyPluginResponse.SUCCESS;
            }
            TroubleTrickKeys troubleTrickKeys = map.get(redisCommand);
            if (troubleTrickKeys == null || troubleTrickKeys.keys.isEmpty()) {
                return ProxyPluginResponse.SUCCESS;
            }
            for (byte[] key : command.getKeys()) {
                String keyStr = Utils.bytesToString(key);
                if (troubleTrickKeys.keys.contains(keyStr)) {
                    ErrorLogCollector.collect(TroubleTrickKeysProxyPlugin.class,
                            "trouble trick key fast fail, bid = " + bid + ", bgroup = " + bgroup + ", command = " + troubleTrickKeys.redisCommand + ", keys = " + command.getKeysStr());
                    return TROUBLE_TRICK_ERROR;
                }
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(TroubleTrickKeysProxyPlugin.class, "check error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }

    private Map<RedisCommand, TroubleTrickKeys> get(CacheKey cacheKey) {
        Map<RedisCommand, TroubleTrickKeys> value = cache.get(cacheKey);
        if (value != null) return value;
        Map<RedisCommand, TroubleTrickKeys> map = new ConcurrentHashMap<>();
        try {
            //可以根据bid/bgroup进行细粒度配置
            //配置示例一：
            //trouble.trick.keys=ZREVRANGEBYSCORE:["key1","key2"];GET:["key3","key4"]
            //含义：针对key1和key2的ZREVRANGEBYSCORE方法，针对key3和key4的GET方法，直接返回异常
            //配置示例二：
            //2.default.trouble.trick.keys=ZRANGE:["key1","key2"];SMEMBERS:["key3","key4"]
            //含义：bid=2/bgroup=default路由配置下，针对key1和key2的ZRANGE方法，针对key3和key4的SMEMBERS方法，直接返回异常
            String string = ProxyDynamicConf.getString("trouble.trick.keys", cacheKey.bid, cacheKey.bgroup, null);
            if (string == null || string.trim().length() == 0) {
                cache.put(cacheKey, map);
                return map;
            }
            String[] split = string.split(";");
            for (String str : split) {
                try {
                    String[] split1 = str.split(":");
                    if (split1.length == 2) {
                        RedisCommand redisCommand = RedisCommand.getRedisCommandByName(split1[0].toLowerCase());
                        if (redisCommand == null) continue;
                        TroubleTrickKeys troubleTrickKeys = new TroubleTrickKeys();
                        troubleTrickKeys.redisCommand = redisCommand;
                        JSONArray array = JSONArray.parseArray(split1[1]);
                        for (Object o : array) {
                            String key = String.valueOf(o);
                            troubleTrickKeys.keys.add(key);
                        }
                        if (!troubleTrickKeys.keys.isEmpty()) {
                            map.put(redisCommand, troubleTrickKeys);
                        }
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(TroubleTrickKeysProxyPlugin.class, "parse trouble.trick.keys error", e);
                }
            }
            cache.put(cacheKey, map);
            return map;
        } catch (Exception e) {
            cache.put(cacheKey, map);
            return map;
        }
    }

    private static class TroubleTrickKeys {
        private RedisCommand redisCommand;
        private final Set<String> keys = new HashSet<>();
    }

    private static class CacheKey {
        private final Long bid;
        private final String bgroup;

        public CacheKey(Long bid, String bgroup) {
            this.bid = bid;
            this.bgroup = bgroup;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(bid, cacheKey.bid) &&
                    Objects.equals(bgroup, cacheKey.bgroup);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bid, bgroup);
        }
    }
}

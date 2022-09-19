package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * 可以用于控制客户端的请求速率，支持全局级别的速率控制，也支持bid/bgroup级别
 * 相关速率控制的配置托管在camellia-redis-proxy.properties，支持动态配置变更
 * Created by caojiajun on 2022/9/13
 */
public class RateLimitProxyPlugin implements ProxyPlugin {

    private static final ProxyPluginResponse TOO_FREQUENCY = new ProxyPluginResponse(false, "ERR request too frequent");

    private RateLimitConf rateLimitConf;
    private final Counter counter = new Counter();
    private final ConcurrentHashMap<String, RateLimitConf> rateLimitConfMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();

    @Override
    public void init(ProxyBeanFactory factory) {
        ProxyDynamicConf.registerCallback(() -> {
            rateLimitConf = null;
            rateLimitConfMap.clear();
        });
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.RATE_LIMIT_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.RATE_LIMIT_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            CommandContext commandContext = command.getCommandContext();
            RateLimitConf rateLimitConf = getRateLimitConf();
            if (rateLimitConf.maxCount == 0) {
                return TOO_FREQUENCY;
            } else if (rateLimitConf.maxCount > 0) {
                long current = getCounter().incrementAndGet(rateLimitConf.checkMillis);
                if (current > rateLimitConf.maxCount) {
                    return TOO_FREQUENCY;
                }
            }
            Long bid = commandContext.getBid();
            String bgroup = commandContext.getBgroup();
            if (bid != null && bgroup != null) {
                RateLimitConf rateLimitConf1 = getRateLimitConf(bid, bgroup);
                if (rateLimitConf1.maxCount == 0) {
                    return TOO_FREQUENCY;
                } else if (rateLimitConf1.maxCount > 0) {
                    long current = getCounter(bid, bgroup).incrementAndGet(rateLimitConf1.checkMillis);
                    if (current > rateLimitConf1.maxCount) {
                        return TOO_FREQUENCY;
                    }
                }
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(RateLimitProxyPlugin.class, "rate limit error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }

    private Counter getCounter() {
        return counter;
    }

    private RateLimitConf getRateLimitConf() {
        if (this.rateLimitConf != null) {
            return this.rateLimitConf;
        }
        long checkMillis = ProxyDynamicConf.getLong("rate.limit.check.millis", 1000L);
        long maxCount = ProxyDynamicConf.getLong("rate.limit.max.count", -1L);
        this.rateLimitConf = new RateLimitConf(checkMillis, maxCount);
        return this.rateLimitConf;
    }

    private Counter getCounter(long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        return CamelliaMapUtils.computeIfAbsent(counterMap, key, s -> new Counter());
    }

    private RateLimitConf getRateLimitConf(long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        return CamelliaMapUtils.computeIfAbsent(rateLimitConfMap, key, str -> {
            long checkMillis = ProxyDynamicConf.getLong(bid + "." + bgroup + ".rate.limit.check.millis", 1000L);
            long maxCount = ProxyDynamicConf.getLong(bid + "." + bgroup + ".rate.limit.max.count", -1L);
            return new RateLimitConf(checkMillis, maxCount);
        });
    }

    private static class RateLimitConf {
        long checkMillis;
        long maxCount;

        public RateLimitConf(long checkMillis, long maxCount) {
            this.checkMillis = checkMillis;
            this.maxCount = maxCount;
        }
    }

    private static class Counter {
        private volatile long timestamp = TimeCache.currentMillis;
        private final LongAdder count = new LongAdder();
        private final AtomicBoolean lock = new AtomicBoolean();

        long incrementAndGet(long expireMillis) {
            if (TimeCache.currentMillis - timestamp > expireMillis) {
                if (lock.compareAndSet(false, true)) {
                    try {
                        if (TimeCache.currentMillis - timestamp > expireMillis) {
                            timestamp = TimeCache.currentMillis;
                            count.reset();
                        }
                    } finally {
                        lock.compareAndSet(true, false);
                    }
                }
            }
            count.increment();
            return count.sum();
        }
    }
}

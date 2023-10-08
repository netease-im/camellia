package com.netease.nim.camellia.redis.proxy.plugin.permission;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.CamelliaMiscApi;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.constant.RateLimitConstant;
import com.netease.nim.camellia.core.constant.TenantConstant;
import com.netease.nim.camellia.core.model.RateLimitDto;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.permission.model.Counter;
import com.netease.nim.camellia.redis.proxy.plugin.permission.model.RateLimitConf;
import com.netease.nim.camellia.redis.proxy.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * It can be used to control the request rate of the client, supports global level rate control, and also supports bid/bgroup level
 * The configuration related to rate control is hosted by camellia-dashboard, which supports dynamic configuration changes
 * Created by @tasszz2k on 2022/12/19
 */
public class DynamicRateLimitProxyPlugin implements ProxyPlugin {

    private static final Logger logger = LoggerFactory.getLogger(DynamicRateLimitProxyPlugin.class);
    private static final ProxyPluginResponse TOO_FREQUENCY = new ProxyPluginResponse(false, "ERR request too frequent");

    // Global level rate limit control (for all bid and bgroup)
    private RateLimitConf globalRateLimitConf;
    private final Counter globalCounter = new Counter();
    // rateLimitConfMap is used to store the rate limit configuration of each bid/bgroup
    private final ConcurrentHashMap<String, RateLimitConf> rateLimitConfMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counterMap = new ConcurrentHashMap<>();
    private String md5;
    private CamelliaMiscApi api;

    @Override
    public void init(ProxyBeanFactory factory) {
        try {
            api = CamelliaMiscApiUtil.initFromDynamicConfig();
            int seconds = ProxyDynamicConf.getInt("dynamic.plugin.config.update.interval.seconds", 5);
            ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
            reload();
        } catch (Exception e) {
            logger.error("init Dynamic Rate Limit Proxy Plugin error", e);
        }
    }



    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.DYNAMIC_RATE_LIMIT_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.DYNAMIC_RATE_LIMIT_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            CommandContext commandContext = command.getCommandContext();
            RateLimitConf rateLimitConf0 = getGlobalRateLimitConf();
            if (rateLimitConf0.getMaxCount() == 0) {
                return TOO_FREQUENCY;
            } else if (rateLimitConf0.getMaxCount() > 0) {
                long current = getGlobalCounter().incrementAndGet(rateLimitConf0.getCheckMillis());
                if (current > rateLimitConf0.getMaxCount()) {
                    return TOO_FREQUENCY;
                }
            }
            Long bid = commandContext.getBid();
            String bgroup = commandContext.getBgroup();
            if (bid != null && bgroup != null) {
                RateLimitConf rateLimitConf1 = getRateLimitConf(bid, bgroup);
                if (rateLimitConf1.getMaxCount() == 0) {
                    return TOO_FREQUENCY;
                } else if (rateLimitConf1.getMaxCount() > 0) {
                    long current = getCounter(bid, bgroup).incrementAndGet(rateLimitConf1.getCheckMillis());
                    if (current > rateLimitConf1.getMaxCount()) {
                        return TOO_FREQUENCY;
                    }
                }
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(DynamicRateLimitProxyPlugin.class, "rate limit error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }


    private void reload() {
        List<RateLimitDto> dtoList = fetchData();
        if (dtoList == null) {
            return;
        }
        Map<String, RateLimitConf> newData = dtoList.stream()
                .collect(
                        Collectors.toMap(
                                conf -> TenantUtils.buildKey(conf.getBid(), conf.getBgroup()),
                                conf -> new RateLimitConf(conf.getCheckMillis(), conf.getMaxCount())));
        // update rate limit config map
        // 1. remove old data (rate limit config map) that has been deleted in the new data
        // 2. insert/update new data into rate limit config map
        // this way can avoid the problem of data inconsistency between the old rate limit config map and the new rate limit config map
        rateLimitConfMap.keySet().stream().filter(key -> !newData.containsKey(key)).forEach(rateLimitConfMap::remove);

        rateLimitConfMap.putAll(newData);
        globalRateLimitConf = null;
    }


    /**
     * Fetch rate limit config from camellia-dashboard via http request
     *
     * @return rate limit config list
     */
    private List<RateLimitDto> fetchData() {
        try {
            DataWithMd5Response<List<RateLimitDto>> response = api.getRateLimitConfigurationList(md5);
            if (response != null && CamelliaApiCode.SUCCESS.getCode() == response.getCode() && response.getData() != null) {
                String newMd5 = response.getMd5();
                if (Utils.hasChange(this.md5, newMd5)) {
                    this.md5 = newMd5;
                    return response.getData();
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(DynamicRateLimitProxyPlugin.class, "cannot fetch rate limit configurations from camellia-dashboard", e);
        }
        return null;
    }

    private Counter getGlobalCounter() {
        return globalCounter;
    }

    private RateLimitConf getGlobalRateLimitConf() {
        if (this.globalRateLimitConf != null) {
            return this.globalRateLimitConf;
        }
        // read rate limit config from rate limit config map
        this.globalRateLimitConf = rateLimitConfMap.get(TenantUtils.buildKey(TenantConstant.GLOBAL_BID, TenantConstant.GLOBAL_BGROUP));
        if (this.globalRateLimitConf != null) {
            return this.globalRateLimitConf;
        }
        // if rate limit config is not found in rate limit config map, use the default rate limit config
        this.globalRateLimitConf = getDefaultLimitConf();
        return this.globalRateLimitConf;
    }

    private Counter getCounter(long bid, String bgroup) {
        String key = TenantUtils.buildKey(bid, bgroup);
        return CamelliaMapUtils.computeIfAbsent(counterMap, key, s -> new Counter());
    }

    private RateLimitConf getRateLimitConf(long bid, String bgroup) {
        String key = TenantUtils.buildKey(bid, bgroup);
        return CamelliaMapUtils.computeIfAbsent(rateLimitConfMap, key, str -> {
            RateLimitConf rateLimitConfEachTenant = rateLimitConfMap.get(TenantUtils.buildKey(TenantConstant.DEFAULT_BID, TenantConstant.DEFAULT_BGROUP));
            if (rateLimitConfEachTenant != null) {
                return rateLimitConfEachTenant;
            }
            // if rate limit config is not found in rate limit config map, use the default rate limit config
            return getDefaultLimitConf();
        });
    }

    private RateLimitConf getDefaultLimitConf() {
        return new RateLimitConf(RateLimitConstant.DEFAULT_CHECK_MILLIS, RateLimitConstant.DEFAULT_MAX_COUNT);
    }
}

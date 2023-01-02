package com.netease.nim.camellia.feign.client;

import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.RandomCamelliaServerSelector;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.feign.conf.DynamicContractTypeGetter;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/8
 */
public class DynamicOption {

    //连接超时配置
    private DynamicValueGetter<Long> connectTimeout;
    private DynamicValueGetter<TimeUnit> connectTimeoutUnit;

    //读超时配置
    private DynamicValueGetter<Long> readTimeout;
    private DynamicValueGetter<TimeUnit> readTimeoutUnit;

    //是否支持重定向
    private DynamicValueGetter<Boolean> followRedirects;

    //是否开启监控
    private DynamicValueGetter<Boolean> monitorEnable;

    //是否开启熔断，若circuitBreakerConfig为null，则不开启熔断
    private CircuitBreakerConfig circuitBreakerConfig;

    //动态检查feign的注解类型，当前支持默认注解和spring-mvc注解，默认支持动态监测
    private DynamicContractTypeGetter dynamicContractTypeGetter = new DynamicContractTypeGetter.Default();

    //当基于注册中心时，如何选择服务节点，默认随机，支持哈希以及其他自定义规则，功能类似于一个简单的ribbon
    private CamelliaServerSelector<FeignResource> serverSelector = new RandomCamelliaServerSelector<>();

    private DynamicOption() {
    }

    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    public Long getConnectTimeout() {
        if (connectTimeout == null) return null;
        return connectTimeout.get();
    }

    public TimeUnit getConnectTimeoutUnit() {
        if (connectTimeoutUnit == null) return null;
        return connectTimeoutUnit.get();
    }

    public Long getReadTimeout() {
        if (readTimeout == null) return null;
        return readTimeout.get();
    }

    public TimeUnit getReadTimeoutUnit() {
        if (readTimeoutUnit == null) return null;
        return readTimeoutUnit.get();
    }

    public Boolean isFollowRedirects() {
        if (followRedirects == null) return null;
        return followRedirects.get();
    }

    public Boolean isMonitorEnable() {
        if (monitorEnable == null) return false;
        return monitorEnable.get();
    }

    public CamelliaServerSelector<FeignResource> getServerSelector() {
        if (serverSelector == null) {
            return new RandomCamelliaServerSelector<>();
        }
        return serverSelector;
    }

    public DynamicContractTypeGetter getDynamicContractTypeGetter() {
        return dynamicContractTypeGetter;
    }

    public static class Builder {
        private final DynamicOption dynamicOption = new DynamicOption();
        public Builder() {
        }

        public Builder connectTimeout(DynamicValueGetter<Long> connectTimeout, DynamicValueGetter<TimeUnit> connectTimeoutUnit) {
            dynamicOption.connectTimeout = connectTimeout;
            dynamicOption.connectTimeoutUnit = connectTimeoutUnit;
            return this;
        }

        public Builder readTimeout(DynamicValueGetter<Long> readTimeout, DynamicValueGetter<TimeUnit> readTimeoutUnit) {
            dynamicOption.readTimeout = readTimeout;
            dynamicOption.readTimeoutUnit = readTimeoutUnit;
            return this;
        }

        public Builder followRedirects(DynamicValueGetter<Boolean> followRedirects) {
            dynamicOption.followRedirects = followRedirects;
            return this;
        }

        public Builder monitorEnable(DynamicValueGetter<Boolean> monitorEnable) {
            dynamicOption.monitorEnable = monitorEnable;
            return this;
        }

        public Builder circuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
            dynamicOption.circuitBreakerConfig = circuitBreakerConfig;
            return this;
        }

        public Builder dynamicContractTypeGetter(DynamicContractTypeGetter dynamicContractTypeGetter) {
            dynamicOption.dynamicContractTypeGetter = dynamicContractTypeGetter;
            return this;
        }

        public Builder serverSelector(CamelliaServerSelector<FeignResource> serverSelector) {
            dynamicOption.serverSelector = serverSelector;
            return this;
        }

        public DynamicOption build() {
            return dynamicOption;
        }
    }
}

package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.client.annotation.RetryPolicy;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.route.FeignResourceTableUpdater;

/**
 * Created by caojiajun on 2022/3/28
 */
public class CamelliaFeignBuildParam<T> {

    private long bid;
    private String bgroup;
    private Class<T> apiType;
    private CamelliaFeignFallbackFactory<T> fallbackFactory;
    private CamelliaFeignProps feignProps;
    private FeignResourceTableUpdater updater;
    private CamelliaFeignEnv feignEnv;
    private DynamicOption dynamicOption;
    private Monitor monitor;
    private CamelliaFeignFailureListener failureListener;
    private int retry;
    private RetryPolicy retryPolicy;

    public CamelliaFeignBuildParam() {
    }

    public CamelliaFeignBuildParam(long bid, String bgroup, Class<T> apiType, CamelliaFeignFallbackFactory<T> fallbackFactory, CamelliaFeignProps feignProps, FeignResourceTableUpdater updater,
                                   CamelliaFeignEnv feignEnv, DynamicOption dynamicOption, Monitor monitor, CamelliaFeignFailureListener failureListener) {
        this.bid = bid;
        this.bgroup = bgroup;
        this.apiType = apiType;
        this.fallbackFactory = fallbackFactory;
        this.feignProps = feignProps;
        this.updater = updater;
        this.feignEnv = feignEnv;
        this.dynamicOption = dynamicOption;
        this.monitor = monitor;
        this.failureListener = failureListener;
    }

    public CamelliaFeignBuildParam<T> duplicate() {
        CamelliaFeignBuildParam<T> param = new CamelliaFeignBuildParam<>(bid, bgroup, apiType, fallbackFactory, feignProps, updater, feignEnv, dynamicOption, monitor, failureListener);
        param.setRetry(retry);
        param.setRetryPolicy(retryPolicy);
        return param;
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

    public Class<T> getApiType() {
        return apiType;
    }

    public void setApiType(Class<T> apiType) {
        this.apiType = apiType;
    }

    public CamelliaFeignFallbackFactory<T> getFallbackFactory() {
        return fallbackFactory;
    }

    public void setFallbackFactory(CamelliaFeignFallbackFactory<T> fallbackFactory) {
        this.fallbackFactory = fallbackFactory;
    }

    public FeignResourceTableUpdater getUpdater() {
        return updater;
    }

    public void setUpdater(FeignResourceTableUpdater updater) {
        this.updater = updater;
    }

    public CamelliaFeignProps getFeignProps() {
        return feignProps;
    }

    public void setFeignProps(CamelliaFeignProps feignProps) {
        this.feignProps = feignProps;
    }

    public CamelliaFeignEnv getFeignEnv() {
        return feignEnv;
    }

    public void setFeignEnv(CamelliaFeignEnv feignEnv) {
        this.feignEnv = feignEnv;
    }

    public DynamicOption getDynamicOption() {
        return dynamicOption;
    }

    public void setDynamicOption(DynamicOption dynamicOption) {
        this.dynamicOption = dynamicOption;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public CamelliaFeignFailureListener getFailureListener() {
        return failureListener;
    }

    public void setFailureListener(CamelliaFeignFailureListener failureListener) {
        this.failureListener = failureListener;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
}

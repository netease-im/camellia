package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.route.FeignResourceTableUpdater;

/**
 * Created by caojiajun on 2022/3/28
 */
public class CamelliaFeignBuildParam<T> {

    private Class<T> apiType;
    private T fallback;
    private CamelliaFeignProps feignProps;
    private FeignResourceTableUpdater updater;
    private CamelliaFeignEnv feignEnv;
    private DynamicOption dynamicOption;

    public CamelliaFeignBuildParam() {
    }

    public CamelliaFeignBuildParam(Class<T> apiType, T fallback, CamelliaFeignProps feignProps, FeignResourceTableUpdater updater,
                                   CamelliaFeignEnv feignEnv, DynamicOption dynamicOption) {
        this.apiType = apiType;
        this.fallback = fallback;
        this.feignProps = feignProps;
        this.updater = updater;
        this.feignEnv = feignEnv;
        this.dynamicOption = dynamicOption;
    }

    public CamelliaFeignBuildParam<T> duplicate() {
        return new CamelliaFeignBuildParam<>(apiType, fallback, feignProps, updater, feignEnv, dynamicOption);
    }

    public Class<T> getApiType() {
        return apiType;
    }

    public void setApiType(Class<T> apiType) {
        this.apiType = apiType;
    }

    public T getFallback() {
        return fallback;
    }

    public void setFallback(T fallback) {
        this.fallback = fallback;
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
}

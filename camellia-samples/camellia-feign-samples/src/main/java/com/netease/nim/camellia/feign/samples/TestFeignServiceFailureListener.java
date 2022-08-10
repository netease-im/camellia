package com.netease.nim.camellia.feign.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.feign.CamelliaFeignFailureContext;
import com.netease.nim.camellia.feign.CamelliaFeignFailureListener;
import com.netease.nim.camellia.feign.CamelliaFeignRetryClient;
import com.netease.nim.camellia.feign.exception.CamelliaFeignException;

/**
 * Created by caojiajun on 2022/8/10
 */
public class TestFeignServiceFailureListener implements CamelliaFeignFailureListener {

    private final CamelliaFeignRetryClient<ITestFeignService> retryClient;

    public TestFeignServiceFailureListener(CamelliaFeignRetryClient<ITestFeignService> retryClient) {
        this.retryClient = retryClient;
    }

    @Override
    public void onFailure(CamelliaFeignFailureContext context) {
        System.out.println("onFailure=" + JSONObject.toJSON(context));
        try {
            Object o = retryClient.sendRetry(context, null, this);
            System.out.println("retry-response=" + JSONObject.toJSON(o));
        } catch (CamelliaFeignException e) {
            System.out.println("sendRetry error");
            e.printStackTrace();
        }
    }
}

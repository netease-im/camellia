package com.netease.nim.camellia.feign.samples.test;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.feign.CamelliaFeignClientFactory;
import com.netease.nim.camellia.feign.CamelliaFeignRetryClient;
import com.netease.nim.camellia.feign.samples.*;

/**
 * Created by caojiajun on 2022/3/30
 */
public class TestMain {

    public static void main(String[] args) {
        CamelliaFeignClientFactory factory = new CamelliaFeignClientFactory();
        CamelliaFeignRetryClient<ITestFeignService> retryClient = new CamelliaFeignRetryClient<>(ITestFeignService.class, factory);
        ITestFeignService service = factory.getService(ITestFeignService.class, new TestFeignServiceFallbackFactory(),
                new TestFeignServiceFailureListener(retryClient));
        User user = new User();
        user.setUid(123);
        UserResponse response = service.getUser(user);
        System.out.println(JSONObject.toJSON(response));
    }
}

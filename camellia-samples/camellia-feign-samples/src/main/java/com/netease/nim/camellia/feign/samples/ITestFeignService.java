package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.core.client.annotation.Retry;
import com.netease.nim.camellia.feign.CamelliaFeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by caojiajun on 2022/3/30
 */
@CamelliaFeignClient(route = "feign#http://127.0.0.1:8080", fallback = TestFeignServiceFallback.class, retry = 1, retryPolicy = CustomRetryPolicy2.class)
public interface ITestFeignService {

    //重试2次
    @Retry(retry = 2, retryPolicy = CustomRetryPolicy.class)
    @RequestMapping(value = "/getUser", method = RequestMethod.POST)
    UserResponse getUser(User user);

    //覆盖service的配置，不重试
    @Retry(retry = 0)
    @RequestMapping(value = "/getUser", method = RequestMethod.POST)
    UserResponse getUserNoRetry(User user);

    //继承service的重试策略，也就是重试1次
    @RequestMapping(value = "/getUser", method = RequestMethod.POST)
    UserResponse getUserByServiceRetry(User user);
}

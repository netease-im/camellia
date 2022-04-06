package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.feign.CamelliaFeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by caojiajun on 2022/3/30
 */
@CamelliaFeignClient(route = "feign#http://127.0.0.1:8080", fallback = TestFeignServiceFallback.class)
public interface ITestFeignService {

    @RequestMapping(value = "/getUser", method = RequestMethod.POST)
    UserResponse getUser(User user);
}

package com.netease.nim.camellia.feign.samples;

import org.springframework.stereotype.Component;

/**
 * Created by caojiajun on 2022/4/6
 */
@Component
public class TestFeignServiceFallback implements ITestFeignService {
    @Override
    public UserResponse getUser(User user) {
        UserResponse response = new UserResponse();
        user.setName("fallback");
        response.setUser(user);
        return response;
    }
}

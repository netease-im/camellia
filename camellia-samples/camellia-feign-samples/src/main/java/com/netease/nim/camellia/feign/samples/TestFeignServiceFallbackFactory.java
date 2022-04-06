package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.feign.CamelliaFeignFallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Created by caojiajun on 2022/4/6
 */
@Component
public class TestFeignServiceFallbackFactory implements CamelliaFeignFallbackFactory<ITestFeignService> {

    @Override
    public ITestFeignService getFallback(Throwable t) {
        return user -> {
            UserResponse response = new UserResponse();
            user.setName("fallback-factory");
            response.setUser(user);
            return response;
        };
    }
}

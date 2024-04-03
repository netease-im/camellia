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
        return new ITestFeignService() {
            @Override
            public UserResponse getUser(User user) {
                UserResponse response = new UserResponse();
                user.setName("fallback");
                response.setUser(user);
                return response;
            }

            @Override
            public UserResponse getUserNoRetry(User user) {
                UserResponse response = new UserResponse();
                user.setName("fallback-no-retry");
                response.setUser(user);
                return response;
            }

            @Override
            public UserResponse getUserByServiceRetry(User user) {
                UserResponse response = new UserResponse();
                user.setName("fallback-by-service-retry");
                response.setUser(user);
                return response;
            }
        };
    }
}

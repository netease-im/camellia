package com.netease.nim.camellia.dashboard.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * Created by caojiajun on 2018/2/26.
 */
@Configuration
public class WebInterceptorConfiguration implements WebMvcConfigurer {

    @Bean
    public LogInterceptor LogInterceptor() {
        return new LogInterceptor();
    }

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(LogInterceptor());
    }
}

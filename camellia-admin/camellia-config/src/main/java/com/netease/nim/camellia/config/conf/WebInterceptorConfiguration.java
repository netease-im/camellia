package com.netease.nim.camellia.config.conf;

import com.netease.nim.camellia.config.auth.UserAuthorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * Created by caojiajun on 2018/2/26.
 */
@Configuration
public class WebInterceptorConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    private UserAuthorityService userAuthorityService;

    @Bean
    public LogInterceptor LogInterceptor() {
        return new LogInterceptor();
    }

    public void addInterceptors(InterceptorRegistry registry) {
        LogInterceptor logInterceptor = LogInterceptor();
        logInterceptor.setUserAuthorityService(userAuthorityService);
        registry.addInterceptor(logInterceptor);
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setPathMatcher(new AntPathMatcher());
    }
}

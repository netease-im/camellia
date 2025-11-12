package com.netease.nim.camellia.console.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Configuration
public class WebInterceptorConfiguration implements WebMvcConfigurer {

    @Bean
    public LogInterceptor LogInterceptor() {
        return new LogInterceptor();
    }


   @Autowired
   UserAccessInterceptor userAccessInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(LogInterceptor());
        registry.addInterceptor(userAccessInterceptor).addPathPatterns("/camellia/**").excludePathPatterns("/camellia/console/login","/camellia/system/**");
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setPathMatcher(new AntPathMatcher());
    }
}

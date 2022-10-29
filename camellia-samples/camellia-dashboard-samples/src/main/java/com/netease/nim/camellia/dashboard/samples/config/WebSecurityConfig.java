package com.netease.nim.camellia.dashboard.samples.config;

import com.netease.nim.camellia.dashboard.samples.security.RestAuthenticationEntryPoint;
import com.netease.nim.camellia.dashboard.samples.security.filter.ApiKeyAuthenticationFilter;
import com.netease.nim.camellia.dashboard.samples.security.provider.ApiKeyAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.util.Collections;

/*
   - Configuration: Adds a new Spring configuration
   - Enable web security: Overrides the default web security configuration
*/
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.cors()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable()
                .formLogin()
                .disable()
                .httpBasic()
                .disable()
                .exceptionHandling()
                .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                .and()
                .authorizeRequests()

                // White list endpoints
                .antMatchers("/", "/error", "/api/all", "/api/auth/**", "/oauth2/**")
                .permitAll()
                //
                .anyRequest()
                .authenticated();

        // Add our custom Token based authentication filter
        http.addFilterBefore(
                new ApiKeyAuthenticationFilter(authenticationManager()),
                AnonymousAuthenticationFilter.class);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(apiKeyAuthenticationProvider));
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().mvcMatchers(HttpMethod.OPTIONS, "/**");
        web.ignoring()
                .mvcMatchers(
                        "/swagger-ui.html/**",
                        "/configuration/**",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/webjars/**",
                        "/swagger**");
    }
}

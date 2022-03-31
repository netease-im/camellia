package com.netease.nim.camellia.feign;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by caojiajun on 2022/3/1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CamelliaFeignClient {

    long bid() default -1;

    String bgroup() default "default";

    String route() default "";

    String[] qualifiers() default {};//仅给camellia-feign-spring-boot-starter使用

    boolean primary() default true;//仅给camellia-feign-spring-boot-starter使用
}

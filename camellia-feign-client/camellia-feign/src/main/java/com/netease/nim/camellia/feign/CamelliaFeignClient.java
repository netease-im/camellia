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

    long bid() default -1;//业务bid，默认-1，若路由规则委托给camellia-dashboard，则bid需要大于0

    String bgroup() default "default";//业务bgroup

    String route() default "";//如果路由没有委托给camellia-dashboard，则本字段必填

    String[] qualifiers() default {};//仅给camellia-feign-spring-boot-starter使用

    boolean primary() default true;//仅给camellia-feign-spring-boot-starter使用

    //如果使用camellia-feign-spring-boot-starter，则会优先去spring工厂取，如果取不到，则尝试使用无参构造方法建一个
    //正常使用CamelliaFeignClientFactory的话，则会优先使用传入的fallback，否则尝试使用无参构造方法建一个
    Class<?> fallback() default void.class;
    //fallbackFactory的优先级高于fallback
    Class<?> fallbackFactory() default void.class;
    //请求失败的回调
    Class<?> failureListener() default void.class;

    int retry() default 0;//最大重试次数

    Class<? extends RetryPolicy> retryPolicy() default RetryPolicy.NeverRetryPolicy.class;//重试策略，主要是看
}

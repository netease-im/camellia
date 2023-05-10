package com.netease.nim.camellia.hot.key.server.bean;

/**
 * Created by caojiajun on 2023/5/9
 */
public interface BeanFactory {

    <T> T getBean(Class<T> requiredType);
}

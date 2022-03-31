package com.netease.nim.camellia.feign.conf;

import feign.RequestLine;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

/**
 * Created by caojiajun on 2022/3/30
 */
public enum ContractType {
    DEFAULT,
    SPRING_MVC,
    ;

    public static ContractType checkContractType(Class<?> apiType) {
        for (Method method : apiType.getMethods()) {
            RequestLine requestLine = method.getAnnotation(RequestLine.class);
            if (requestLine != null) {
                return ContractType.DEFAULT;
            }
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                return ContractType.SPRING_MVC;
            }
        }
        return ContractType.DEFAULT;
    }
}

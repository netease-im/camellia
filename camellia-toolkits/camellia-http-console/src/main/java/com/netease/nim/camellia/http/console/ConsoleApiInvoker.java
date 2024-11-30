package com.netease.nim.camellia.http.console;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2023/6/30
 */
public class ConsoleApiInvoker {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleApiInvoker.class);

    private final String uri;
    private final boolean withParams;
    private final IConsoleService service;
    private final Method method;

    public ConsoleApiInvoker(IConsoleService service, Method method, String uri, boolean withParams) {
        this.service = service;
        this.method = method;
        this.uri = uri;
        this.withParams = withParams;
    }

    public ConsoleResult invoke(QueryStringDecoder queryStringDecoder) throws Exception {
        try {
            if (withParams) {
                Map<String, List<String>> params = queryStringDecoder.parameters();
                return (ConsoleResult) method.invoke(service, params);
            } else {
                return (ConsoleResult) method.invoke(service);
            }
        } catch (Exception e) {
            logger.error("invoke error, uri = {}", uri, e);
            throw e;
        }
    }
}

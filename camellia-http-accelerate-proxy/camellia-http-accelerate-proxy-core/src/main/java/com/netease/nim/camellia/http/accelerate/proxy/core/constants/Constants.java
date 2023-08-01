package com.netease.nim.camellia.http.accelerate.proxy.core.constants;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Created by caojiajun on 2023/7/7
 */
public class Constants {

    public static final DefaultFullHttpResponse BAD_GATEWAY = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);

    public static final DefaultFullHttpResponse INTERNAL_SERVER_ERROR = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
}

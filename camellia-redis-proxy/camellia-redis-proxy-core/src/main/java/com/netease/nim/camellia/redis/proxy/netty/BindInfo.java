package com.netease.nim.camellia.redis.proxy.netty;

import io.netty.bootstrap.ServerBootstrap;

/**
 * Created by caojiajun on 2024/1/19
 */
public class BindInfo {

    public Type type;
    public ServerBootstrap bootstrap;
    public int port;
    public String udsPath;

    public BindInfo(Type type, ServerBootstrap bootstrap, int port) {
        this.type = type;
        this.bootstrap = bootstrap;
        this.port = port;
    }

    public BindInfo(Type type, ServerBootstrap bootstrap, String udsPath) {
        this.type = type;
        this.bootstrap = bootstrap;
        this.udsPath = udsPath;
    }

    public static enum Type {
        PORT,
        TLS_PORT,
        CPORT,
        UDS,
        HTTP,
        ;
    }

}

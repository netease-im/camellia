package com.netease.nim.camellia.core.client.env;


/**
 *
 * Created by caojiajun on 2019/8/14.
 */
public interface Monitor {

    void incrWrite(String resource, String className, String methodName);

    void incrRead(String resource, String className, String methodName);
}

package com.netease.nim.camellia.tests.redis.proxy;

import com.netease.nim.camellia.redis.proxy.util.CamelliaRedisProxyStarter;

/**
 * Created by caojiajun on 2026/2/12
 */
public class TestStartNoSpringBoot {

    public static void main(String[] args) {
        //设置相关参数
        CamelliaRedisProxyStarter.updatePort(6380);//设置proxy的端口
        CamelliaRedisProxyStarter.updatePassword("pass123");//设置proxy的密码
        CamelliaRedisProxyStarter.updateRouteConf("redis://@127.0.0.1:6379");//可以设置单个地址，也可以设置一个json去配置双写/分片等
        //启动
        CamelliaRedisProxyStarter.start();
    }
}

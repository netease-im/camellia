package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.console.ConsoleResult;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 *
 * Created by caojiajun on 2019/11/28.
 */
@Component
public class MyConsoleService extends ConsoleServiceAdaptor implements InitializingBean {

    @Value("${server.port:6379}")
    private int port;

    @Value("${spring.application.name:camellia-redis-proxy}")
    private String applicationName;

    @Override
    public ConsoleResult online() {
        //TODO register to 注册中心，或者挂载到负载均衡服务器
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        //TODO deregister from 注册中心，或者从负载均衡服务器上摘掉
        return super.offline();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setServerPort(port);
    }
}

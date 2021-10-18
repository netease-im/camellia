package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.console.ConsoleResult;
import com.netease.nim.camellia.redis.proxy.console.ConsoleServiceAdaptor;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyBoot;
import com.netease.nim.camellia.redis.proxy.springboot.ProxyDynamicConfSupport;
import com.netease.nim.camellia.redis.zk.registry.springboot.CamelliaRedisProxyZkRegisterBoot;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 *
 * Created by caojiajun on 2019/11/28.
 */
@Component
public class MyConsoleService extends ConsoleServiceAdaptor implements InitializingBean {

    @Autowired
    private CamelliaRedisProxyBoot redisProxyBoot;

    @Autowired
    private CamelliaRedisProxyZkRegisterBoot zkRegisterBoot;

    @Autowired
    private ProxyDynamicConfSupport support;

    @Override
    public ConsoleResult online() {
        //TODO register to 注册中心，或者挂载到负载均衡服务器
        zkRegisterBoot.register();
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        //TODO deregister from 注册中心，或者从负载均衡服务器上摘掉
        zkRegisterBoot.deregister();
        return super.offline();
    }

    @Override
    public ConsoleResult reload() {
        support.reload();
        return super.reload();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setServerPort(redisProxyBoot.getPort());
    }
}

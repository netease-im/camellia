package com.netease.nim.camellia.redis.proxy.upstream.proxies;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisProxiesDiscoveryResource;
import com.netease.nim.camellia.redis.base.proxy.IProxyDiscovery;
import com.netease.nim.camellia.redis.base.proxy.Proxy;
import com.netease.nim.camellia.redis.base.resource.RedissProxiesDiscoveryResource;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * 基于服务发现的proxy client
 * 对于每个proxy，都会当做一个普通的redis去访问
 */
public class RedisProxiesDiscoveryClient extends AbstractRedisProxiesClient {

    private static final Logger logger = LoggerFactory.getLogger(RedisProxiesDiscoveryClient.class);

    private final Resource resource;
    private final String userName;
    private final String password;
    private final int db;
    private final IProxyDiscovery proxyDiscovery;

    public RedisProxiesDiscoveryClient(RedisProxiesDiscoveryResource resource) {
        this.resource = resource;
        if (GlobalRedisProxyEnv.getDiscoveryFactory() == null) {
            throw new CamelliaRedisException("RedisProxiesClient init fail, proxy discovery not init");
        }
        this.proxyDiscovery = GlobalRedisProxyEnv.getDiscoveryFactory().getProxyDiscovery(resource.getProxyName());
        if (proxyDiscovery == null) {
            throw new CamelliaRedisException("RedisProxiesClient init fail, proxy discovery init fail, resource = " + resource.getUrl());
        }
        this.userName = resource.getUserName();
        this.password = resource.getPassword();
        this.db = resource.getDb();
        init();
    }

    public RedisProxiesDiscoveryClient(RedissProxiesDiscoveryResource resource) {
        this.resource = resource;
        if (GlobalRedisProxyEnv.getDiscoveryFactory() == null) {
            throw new CamelliaRedisException("RedisProxiesClient init fail, proxy discovery not init");
        }
        this.proxyDiscovery = GlobalRedisProxyEnv.getDiscoveryFactory().getProxyDiscovery(resource.getProxyName());
        if (proxyDiscovery == null) {
            throw new CamelliaRedisException("RedisProxiesClient init fail, proxy discovery init fail, resource = " + resource.getUrl());
        }
        this.userName = resource.getUserName();
        this.password = resource.getPassword();
        this.db = resource.getDb();
        init();
    }

    protected void init() {
        super.init();
        proxyDiscovery.setCallback(new CamelliaDiscovery.Callback<Proxy>() {
            @Override
            public void add(Proxy proxy) {
                logger.info("proxy add, proxy = {}, resource = {}", proxy, resource);
                RedisProxiesDiscoveryClient.this.add(toAddr(proxy));
            }
            @Override
            public void remove(Proxy proxy) {
                logger.info("proxy remove, proxy = {}, resource = {}", proxy, resource);
                RedisProxiesDiscoveryClient.this.remove(toAddr(proxy));
            }
        });
        logger.info("RedisProxiesDiscoveryClient init success, resource = {}", resource.getUrl());
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    private RedisConnectionAddr toAddr(Proxy proxy) {
        return new RedisConnectionAddr(proxy.getHost(), proxy.getPort(), userName, password, db);
    }

    @Override
    public List<RedisConnectionAddr> getAll() {
        try {
            List<Proxy> proxies = proxyDiscovery.findAll();
            if (proxies == null || proxies.isEmpty()) {
                logger.warn("proxies findAll empty, resource = {}", resource);
                return null;
            }
            List<RedisConnectionAddr> list = new ArrayList<>();
            for (Proxy proxy : proxies) {
                RedisConnectionAddr addr = toAddr(proxy);
                list.add(addr);
            }
            return list;
        } catch (Exception e) {
            logger.error("getAll error, resource = {}", resource, e);
            return null;
        }
    }
}

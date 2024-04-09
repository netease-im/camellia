package com.netease.nim.camellia.redis.proxy.kv.core;

import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.kv.core.command.CommanderConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.CacheConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.kv.core.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.kv.core.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.DefaultKeyMetaServer;
import com.netease.nim.camellia.redis.proxy.kv.core.meta.KeyMetaServer;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/4/9
 */
public class UpstreamKVClientTemplateFactory implements IUpstreamClientTemplateFactory {

    private final UpstreamKVClientTemplate template;

    public UpstreamKVClientTemplateFactory() {
        String namespace = ProxyDynamicConf.getString("kv.namespace", "d");

        int threads = ProxyDynamicConf.getInt("kv.command.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize = ProxyDynamicConf.getInt("kv.command.executor.queue.size", 100000);

        CamelliaHashedExecutor executor = new CamelliaHashedExecutor("kv-command-executor", threads, queueSize, new CamelliaHashedExecutor.AbortPolicy());

        KVClient kvClient = initKVClient();

        this.template = initUpstreamKVClientTemplate(namespace, kvClient, executor);

        RedisConnectionHub.getInstance().init(new CamelliaTranspondProperties(), GlobalRedisProxyEnv.getProxyBeanFactory());
    }

    @Override
    public IUpstreamClientTemplate getOrInitialize(Long bid, String bgroup) {
        return template;
    }

    @Override
    public CompletableFuture<IUpstreamClientTemplate> getOrInitializeAsync(Long bid, String bgroup) {
        return CompletableFuture.completedFuture(template);
    }

    @Override
    public IUpstreamClientTemplate tryGet(Long bid, String bgroup) {
        return template;
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return false;
    }

    @Override
    public int shutdown() {
        return 0;
    }

    private KVClient initKVClient() {
        String className = ProxyDynamicConf.getString("kv.client.class.name", null);
        return (KVClient) GlobalRedisProxyEnv.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(className));
    }

    private UpstreamKVClientTemplate initUpstreamKVClientTemplate(String namespace, KVClient kvClient, CamelliaHashedExecutor executor) {
        KeyStruct keyStruct = new KeyStruct(namespace.getBytes(StandardCharsets.UTF_8));
        CacheConfig cacheConfig = new CacheConfig(namespace);
        KvConfig kvConfig = new KvConfig(namespace);

        String metaRedisUrl = ProxyDynamicConf.getString("kv.meta.redis.url", null);
        RedisTemplate metaRedisTemplate = new RedisTemplate(new UpstreamRedisClientTemplate(ReadableResourceTableUtil.parseTable(metaRedisUrl)));
        KeyMetaServer keyMetaServer = new DefaultKeyMetaServer(metaRedisTemplate, keyStruct, cacheConfig);

        String cacheRedisUrl = ProxyDynamicConf.getString("kv.cache.redis.url", null);
        RedisTemplate cacheRedisTemplate = new RedisTemplate(new UpstreamRedisClientTemplate(ReadableResourceTableUtil.parseTable(cacheRedisUrl)));

        CommanderConfig commanderConfig = new CommanderConfig(kvClient, keyStruct, cacheConfig, kvConfig, keyMetaServer, cacheRedisTemplate);

        return new UpstreamKVClientTemplate(executor, commanderConfig);
    }
}

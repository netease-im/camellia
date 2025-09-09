package com.netease.nim.camellia.redis.pipeline;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.*;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterWrapper;
import redis.clients.jedis.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2025/7/30
 */
public interface PipelineFactory {

    AbstractPipeline getPipeline(Resource resource, int slot);

    void sync();

    public static class DefaultPipelineFactory implements PipelineFactory {

        private final Map<String, AbstractPipeline> map = new HashMap<>();
        private final Map<String, Jedis> jedisMap = new HashMap<>();

        private final JedisPoolFactory jedisPoolFactory;
        private final JedisClusterFactory jedisClusterFactory;

        public DefaultPipelineFactory(CamelliaRedisEnv env) {
            this.jedisPoolFactory = env.getJedisPoolFactory();
            this.jedisClusterFactory = env.getJedisClusterFactory();
        }

        @Override
        public AbstractPipeline getPipeline(Resource resource, int slot) {
            if (resource instanceof RedisClusterSlavesResource) {
                JedisClusterWrapper jedisCluster = jedisClusterFactory.getJedisCluster((RedisClusterSlavesResource) resource);
                HostAndPort hostAndPort = jedisCluster.getSlaveHostAndPort(slot);
                String key = resource.getUrl() + "|" + hostAndPort.toString();
                AbstractPipeline pipeline = map.get(key);
                if (pipeline != null) {
                    return pipeline;
                }
                Jedis jedis = jedisMap.get(key);
                if (jedis == null) {
                    jedis = jedisCluster.getJedis(hostAndPort);
                    jedisMap.put(key, jedis);
                }
                pipeline = jedis.pipelined();
                map.put(key, pipeline);
                return pipeline;
            }
            String key = resource.getUrl();

            AbstractPipeline pipeline = map.get(key);
            if (pipeline != null) {
                return pipeline;
            }
            Jedis jedis = jedisMap.get(resource.getUrl());
            if (jedis != null) {
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            }
            if (resource instanceof RedisResource) {
                JedisPool jedisPool = jedisPoolFactory.getJedisPool((RedisResource) resource);
                jedis = jedisPool.getResource();
                jedisMap.put(resource.getUrl(), jedis);
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            } else if (resource instanceof RedisSentinelResource) {
                JedisSentinelPool jedisSentinelPool = jedisPoolFactory.getJedisSentinelPool((RedisSentinelResource) resource);
                jedis = jedisSentinelPool.getResource();
                jedisMap.put(resource.getUrl(), jedis);
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            } else if (resource instanceof CamelliaRedisProxyResource) {
                JedisPool jedisPool = jedisPoolFactory.getCamelliaJedisPool((CamelliaRedisProxyResource) resource);
                jedis = jedisPool.getResource();
                jedisMap.put(resource.getUrl(), jedis);
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            } else if (resource instanceof RedisSentinelSlavesResource) {
                JedisPool jedisPool = jedisPoolFactory.getJedisSentinelSlavesPool((RedisSentinelSlavesResource) resource);
                jedis = jedisPool.getResource();
                jedisMap.put(resource.getUrl(), jedis);
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            } else if (resource instanceof RedisProxiesResource) {
                JedisPool jedisPool = jedisPoolFactory.getRedisProxiesJedisPool((RedisProxiesResource) resource);
                jedis = jedisPool.getResource();
                jedisMap.put(resource.getUrl(), jedis);
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            } else if (resource instanceof RedisProxiesDiscoveryResource) {
                JedisPool jedisPool = jedisPoolFactory.getRedisProxiesDiscoveryJedisPool((RedisProxiesDiscoveryResource) resource);
                jedis = jedisPool.getResource();
                jedisMap.put(resource.getUrl(), jedis);
                Pipeline pipelined = jedis.pipelined();
                map.put(resource.getUrl(), pipelined);
                return pipelined;
            } else if (resource instanceof RedisClusterResource) {
                JedisClusterWrapper jedisCluster = jedisClusterFactory.getJedisCluster((RedisClusterResource) resource);
                ClusterPipeline clusterPipeline = jedisCluster.pipelined();
                map.put(resource.getUrl(), clusterPipeline);
                return clusterPipeline;
            } else {
                throw new CamelliaRedisException("not support pipeline");
            }
        }

        @Override
        public void sync() {
            for (Map.Entry<String, AbstractPipeline> entry : map.entrySet()) {
                AbstractPipeline pipeline = entry.getValue();
                pipeline.sync();
                pipeline.close();
            }
            for (Map.Entry<String, Jedis> entry : jedisMap.entrySet()) {
                Jedis jedis = entry.getValue();
                jedis.close();
            }
            map.clear();
            jedisMap.clear();
        }
    }
}

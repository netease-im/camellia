package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.lang.reflect.Field;
import java.util.*;

/**
 *
 * Created by caojiajun on 2019/8/5.
 */
public class JedisClusterWrapper extends JedisCluster {

    private JedisClusterInfoCache cache;
    private Field slots;

    private void init() {
        try {
            Field cacheField = JedisClusterConnectionHandler.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            cache = (JedisClusterInfoCache)cacheField.get(this.connectionHandler);
            slots = JedisClusterInfoCache.class.getDeclaredField("slots");
            slots.setAccessible(true);
            JedisPool pool = cache.getSlotPool(0);
            if (pool == null) {
                //jedis-2.9.3 will not throw exception if JedisCluster init fail, so throw exception here
                throw new CamelliaRedisException("init jedis cluster info cache error");
            }
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    public List<JedisPool> getJedisPoolList() {
        try {
            Map<Integer, JedisPool> map = (Map<Integer, JedisPool>) slots.get(this.cache);
            return new ArrayList<>(new HashSet<>(map.values()));
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    public JedisPool getJedisPool(String key) {
        return cache.getSlotPool(JedisClusterCRC16.getSlot(key));
    }

    public JedisPool getJedisPool(byte[] key) {
        return cache.getSlotPool(JedisClusterCRC16.getSlot(key));
    }

    public JedisPool getJedisPool(String host, int port) {
        return cache.getNode(host + ":" + port);
    }

    public void renewSlotCache() {
        cache.renewClusterSlots(null);
    }

    public JedisClusterWrapper(HostAndPort node) {
        super(node);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, int timeout) {
        super(node, timeout);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, int timeout, int maxAttempts) {
        super(node, timeout, maxAttempts);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, GenericObjectPoolConfig poolConfig) {
        super(node, poolConfig);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, int timeout, GenericObjectPoolConfig poolConfig) {
        super(node, timeout, poolConfig);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, int timeout, int maxAttempts, GenericObjectPoolConfig poolConfig) {
        super(node, timeout, maxAttempts, poolConfig);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts, GenericObjectPoolConfig poolConfig) {
        super(node, connectionTimeout, soTimeout, maxAttempts, poolConfig);
        init();
    }

    public JedisClusterWrapper(HostAndPort node, int connectionTimeout, int soTimeout, int maxAttempts, String password, GenericObjectPoolConfig poolConfig) {
        super(node, connectionTimeout, soTimeout, maxAttempts, password, poolConfig);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> nodes) {
        super(nodes);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> nodes, int timeout) {
        super(nodes, timeout);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> nodes, int timeout, int maxAttempts) {
        super(nodes, timeout, maxAttempts);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> nodes, GenericObjectPoolConfig poolConfig) {
        super(nodes, poolConfig);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> nodes, int timeout, GenericObjectPoolConfig poolConfig) {
        super(nodes, timeout, poolConfig);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> jedisClusterNode, int timeout, int maxAttempts, GenericObjectPoolConfig poolConfig) {
        super(jedisClusterNode, timeout, maxAttempts, poolConfig);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, GenericObjectPoolConfig poolConfig) {
        super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, poolConfig);
        init();
    }

    public JedisClusterWrapper(Set<HostAndPort> jedisClusterNode, int connectionTimeout, int soTimeout, int maxAttempts, String password, GenericObjectPoolConfig poolConfig) {
        super(jedisClusterNode, connectionTimeout, soTimeout, maxAttempts, password, poolConfig);
        init();
    }

    public JedisClusterConnectionHandler getConnectionHandler() {
        return connectionHandler;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}

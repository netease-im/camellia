package com.netease.nim.camellia.redis.adaptor;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.LinkedBlockingQueue;


/**
 *
 * Created by caojiajun on 2021/7/28
 */
public class JedisPoolCamelliaAdaptor extends JedisPool {

    private final CamelliaRedisTemplate template;
    private final LinkedBlockingQueue<JedisCamelliaAdaptor> queue;

    public JedisPoolCamelliaAdaptor(CamelliaRedisTemplate template) {
        this(template, 128);
    }

    public JedisPoolCamelliaAdaptor(CamelliaRedisTemplate template, int poolSize) {
        this.template = template;
        this.queue = new LinkedBlockingQueue<>(poolSize);
    }

    void returnJedisCamelliaAdaptor(JedisCamelliaAdaptor adaptor) {
        if (adaptor != null) {
            queue.offer(adaptor);
        }
    }

    @Override
    public Jedis getResource() {
        JedisCamelliaAdaptor adaptor = queue.poll();
        if (adaptor != null) {
            adaptor.open();
            return adaptor;
        }
        adaptor = new JedisCamelliaAdaptor(template, this);
        adaptor.open();
        return adaptor;
    }

    @Override
    public void returnBrokenResource(Jedis resource) {
        if (resource != null) {
            resource.close();
        }
    }

    @Override
    public void returnResource(Jedis resource) {
        if (resource != null) {
            resource.close();
        }
    }

    @Override
    public void returnResourceObject(Jedis resource) {
        if (resource != null) {
            resource.close();
        }
    }

    @Override
    protected void returnBrokenResourceObject(Jedis resource) {
        if (resource != null) {
            resource.close();
        }
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void destroy() {
        //do nothing
    }

    @Override
    public void initPool(GenericObjectPoolConfig poolConfig, PooledObjectFactory<Jedis> factory) {
        //do nothing
    }

    @Override
    protected void closeInternalPool() {
        //do nothing
    }

    @Override
    public int getNumActive() {
        return 0;
    }

    @Override
    public int getNumIdle() {
        return 0;
    }

    @Override
    public int getNumWaiters() {
        return 0;
    }

    @Override
    public long getMeanBorrowWaitTimeMillis() {
        return 0L;
    }

    @Override
    public long getMaxBorrowWaitTimeMillis() {
        return 0L;
    }

    @Override
    public void addObjects(int count) {
        //do nothing
    }
}

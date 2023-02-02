package com.netease.nim.camellia.redis.proxy.upstream.proxies;

import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.standalone.AbstractSimpleRedisClient;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/2/2
 */
public abstract class AbstractRedisProxiesClient extends AbstractSimpleRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRedisProxiesClient.class);

    private final Object lock = new Object();
    private List<RedisConnectionAddr> originalList = new ArrayList<>();
    private List<RedisConnectionAddr> dynamicList = new ArrayList<>();

    protected void init() {
        refresh();
        if (originalList.isEmpty()) {
            throw new CamelliaRedisException("init fail, no reachable proxy, resource = " + getResource().getUrl());
        }
        int seconds = ProxyDynamicConf.getInt("redis.proxies.reload.interval.seconds", 60);
        ExecutorUtils.scheduleAtFixedRate(this::refresh, seconds, seconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isValid() {
        List<RedisConnectionAddr> list = new ArrayList<>(originalList);
        for (RedisConnectionAddr addr : list) {
            if (checkValid(addr)) {
                return true;
            }
        }
        return false;
    }

    public abstract List<RedisConnectionAddr> getAll();

    private void refresh() {
        List<RedisConnectionAddr> list = getAll();
        if (list == null || list.isEmpty()) {
            logger.warn("addr list is empty, skip refresh, resource = {}", getResource());
            return;
        }
        synchronized (lock) {
            List<RedisConnectionAddr> validList = new ArrayList<>();
            for (RedisConnectionAddr addr : list) {
                if (checkValid(addr)) {
                    validList.add(addr);
                }
            }
            if (validList.isEmpty()) {
                logger.warn("no reachable addr list {}, skip refresh, resource = {}", list, getResource());
                return;
            }
            this.originalList = new ArrayList<>(list);
            this.dynamicList = new ArrayList<>(originalList);
        }
    }

    public void add(RedisConnectionAddr addr) {
        synchronized (lock) {
            if (!originalList.contains(addr)) {
                originalList.add(addr);
                dynamicList = new ArrayList<>(originalList);
            }
        }
    }

    public void remove(RedisConnectionAddr addr) {
        synchronized (lock) {
            if (originalList.contains(addr)) {
                originalList.remove(addr);
                dynamicList = new ArrayList<>(originalList);
            }
        }
    }

    @Override
    public RedisConnectionAddr getAddr() {
        try {
            if (originalList.isEmpty()) return null;
            if (originalList.size() == 1) {
                return originalList.get(0);
            }
            int retry = originalList.size();
            while (retry-- > 0) {
                if (dynamicList.isEmpty()) {
                    dynamicList = new ArrayList<>(originalList);
                }
                int i = ThreadLocalRandom.current().nextInt(dynamicList.size());
                RedisConnectionAddr addr = dynamicList.get(i);
                if (checkValid(addr)) {
                    return addr;
                } else {
                    dynamicList.remove(addr);
                }
            }
            int i = ThreadLocalRandom.current().nextInt(originalList.size());
            return originalList.get(i);
        } catch (Exception e) {
            try {
                if (originalList.isEmpty()) return null;
                int i = ThreadLocalRandom.current().nextInt(originalList.size());
                return originalList.get(i);
            } catch (Exception ex) {
                try {
                    return originalList.get(0);
                } catch (Exception exc) {
                    return null;
                }
            }
        }
    }

}

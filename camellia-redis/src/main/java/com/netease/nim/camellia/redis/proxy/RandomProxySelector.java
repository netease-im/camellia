package com.netease.nim.camellia.redis.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * pick proxy in random
 * Created by caojiajun on 2020/12/15
 */
public class RandomProxySelector implements IProxySelector {

    private static final Logger logger = LoggerFactory.getLogger(RandomProxySelector.class);

    private final Object lock = new Object();

    private final Set<Proxy> proxySet = new HashSet<>();
    private List<Proxy> dynamicProxyList = new ArrayList<>();

    @Override
    public Proxy next() {
        try {
            if (dynamicProxyList.isEmpty()) {
                dynamicProxyList = new ArrayList<>(proxySet);
            }
            int index = ThreadLocalRandom.current().nextInt(dynamicProxyList.size());
            return dynamicProxyList.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void ban(Proxy proxy) {
        try {
            synchronized (lock) {
                dynamicProxyList.remove(proxy);
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void add(Proxy proxy) {
        try {
            synchronized (lock) {
                proxySet.add(proxy);
                dynamicProxyList = new ArrayList<>(proxySet);
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public void remove(Proxy proxy) {
        try {
            synchronized (lock) {
                if (proxySet.size() == 1) {
                    logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy.toString());
                } else {
                    proxySet.remove(proxy);
                    dynamicProxyList = new ArrayList<>(proxySet);
                }
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public Set<Proxy> getAll() {
        return new HashSet<>(proxySet);
    }
}

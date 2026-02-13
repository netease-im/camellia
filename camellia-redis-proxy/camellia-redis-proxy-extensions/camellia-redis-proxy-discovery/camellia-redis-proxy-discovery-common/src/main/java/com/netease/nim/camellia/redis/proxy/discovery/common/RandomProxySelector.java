package com.netease.nim.camellia.redis.proxy.discovery.common;

import com.netease.nim.camellia.core.discovery.ServerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * pick proxy in random
 * Created by caojiajun on 2020/12/15
 */
public class RandomProxySelector implements IProxySelector {

    private static final Logger logger = LoggerFactory.getLogger(RandomProxySelector.class);

    private final Object lock = new Object();

    private final Set<ServerNode> proxySet = new HashSet<>();
    private List<ServerNode> dynamicProxyList = new ArrayList<>();

    @Override
    public ServerNode next() {
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
    public boolean ban(ServerNode proxy) {
        try {
            synchronized (lock) {
                dynamicProxyList.remove(proxy);
            }
            return true;
        } catch (Exception e) {
            logger.error("ban error, proxy = {}", proxy, e);
            return false;
        }
    }

    @Override
    public boolean add(ServerNode proxy) {
        try {
            synchronized (lock) {
                proxySet.add(proxy);
                dynamicProxyList = new ArrayList<>(proxySet);
            }
            return true;
        } catch (Exception e) {
            logger.error("add error, proxy = {}", proxy, e);
            return false;
        }
    }

    @Override
    public boolean remove(ServerNode proxy) {
        try {
            synchronized (lock) {
                if (proxySet.size() == 1) {
                    logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy.toString());
                    return false;
                } else {
                    proxySet.remove(proxy);
                    dynamicProxyList = new ArrayList<>(proxySet);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("remove error, proxy = {}", proxy, e);
            return false;
        }
    }

    @Override
    public Set<ServerNode> getAll() {
        return new HashSet<>(proxySet);
    }

    @Override
    public List<ServerNode> sort(List<ServerNode> list) {
        List<ServerNode> ret = new ArrayList<>(list);
        Collections.shuffle(ret);
        return ret;
    }
}

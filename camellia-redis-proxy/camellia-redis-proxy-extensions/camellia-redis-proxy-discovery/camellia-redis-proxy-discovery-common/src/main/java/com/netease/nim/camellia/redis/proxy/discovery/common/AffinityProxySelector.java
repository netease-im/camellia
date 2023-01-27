package com.netease.nim.camellia.redis.proxy.discovery.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class AffinityProxySelector implements IProxySelector {

    private static final Logger logger = LoggerFactory.getLogger(AffinityProxySelector.class);

    private final ConcurrentLinkedQueue<Proxy> dynamicProxyQueue = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<Proxy> proxyList = new CopyOnWriteArrayList<>();

    @Override
    public Proxy next() {
        int retry = 2;
        while (retry -- > 0) {
            //双向循环队列，从头上拿出来，再塞回到队尾
            Proxy ret = dynamicProxyQueue.poll();
            if (ret == null) continue;
            dynamicProxyQueue.offer(ret);
            return ret;
        }
        //兜底到随机策略
        try {
            int index = ThreadLocalRandom.current().nextInt(proxyList.size());
            return proxyList.get(index);
        } catch (Exception e) {
            try {
                return proxyList.get(0);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Override
    public Proxy next(Boolean affinity) {
        //如果保持亲和性，则一直使用同一个proxy
        if (Boolean.TRUE.equals(affinity)) {
            Proxy head = dynamicProxyQueue.peek();
            if (head != null) {
                return head;
            }
        }
        return next();
    }

    @Override
    public void ban(Proxy proxy) {
        logger.warn("proxy {}:{} was baned", proxy.getHost(),proxy.getPort());
        dynamicProxyQueue.remove(proxy);
    }

    @Override
    public void add(Proxy proxy) {
        if (!dynamicProxyQueue.contains(proxy)) {
            dynamicProxyQueue.add(proxy);
        }
        if (!proxyList.contains(proxy)) {
            proxyList.add(proxy);
        }
    }

    @Override
    public void remove(Proxy proxy) {
        if (dynamicProxyQueue.size() == 1) {
            if (logger.isWarnEnabled()) {
                logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy);
            }
        } else {
            dynamicProxyQueue.remove(proxy);
        }
        if (proxyList.size() > 1) {
            proxyList.remove(proxy);
        }
    }

    @Override
    public Set<Proxy> getAll() {
        return new HashSet<>(dynamicProxyQueue);
    }

    @Override
    public List<Proxy> sort(List<Proxy> list) {
        List<Proxy> ret = new ArrayList<>(list);
        Collections.shuffle(ret);
        return ret;
    }
}

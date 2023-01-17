package com.neteasy.nim.camellia.redis.discovery.jedis.affinity;

import com.netease.nim.camellia.redis.proxy.discovery.common.IProxySelector;
import com.netease.nim.camellia.redis.proxy.discovery.common.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicStampedReference;

public class AffinityProxySelector implements IProxySelector {
    private static final Logger logger = LoggerFactory.getLogger(AffinityProxySelector.class);

    private final Object lock = new Object();

    private final ConcurrentSkipListSet<Proxy> proxySet = new ConcurrentSkipListSet<>();
    private ConcurrentLinkedQueue<Proxy> dynamicProxyList= new ConcurrentLinkedQueue<Proxy>();
    private AtomicStampedReference<Integer> currentActive = new AtomicStampedReference<Integer>(10,0);
    @Override
    public Proxy next() {
        //双向循环队列，从头上拿出来，再塞回到队尾
        Proxy ret = dynamicProxyList.poll();
        dynamicProxyList.offer(ret);
        return ret;
    }

    @Override
    public Proxy next(Boolean affinity) {
        //如果保持亲和性，则一直使用同一个proxy
        if(affinity){
            return dynamicProxyList.peek();
        }
        return next();
        
    }

    @Override
    public void ban(Proxy proxy) {
            dynamicProxyList.remove(proxy);
    }

    @Override
    public void add(Proxy proxy) {
        if(!dynamicProxyList.contains(proxy)) {
            dynamicProxyList.add(proxy);
        }
    }

    @Override
    public void remove(Proxy proxy) {
        if (dynamicProxyList.size() == 1) {
            logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy.toString());
        } else {
            dynamicProxyList.remove(proxy);
        }
    }

    @Override
    public Set<Proxy> getAll() {
        return new HashSet<>(dynamicProxyList);
    }

    @Override
    public List<Proxy> sort(List<Proxy> list) {
        List<Proxy> ret = new ArrayList<>(list);
        Collections.shuffle(ret);
        return ret;
    }
}

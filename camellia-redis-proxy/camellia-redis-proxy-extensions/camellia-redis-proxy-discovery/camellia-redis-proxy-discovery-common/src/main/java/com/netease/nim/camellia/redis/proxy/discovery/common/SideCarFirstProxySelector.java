package com.netease.nim.camellia.redis.proxy.discovery.common;


import com.netease.nim.camellia.core.discovery.ServerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * pick side-car proxy first, other wise, will return the nearest proxy
 * Created by caojiajun on 2020/12/16
 */
public class SideCarFirstProxySelector implements IProxySelector {

    private static final Logger logger = LoggerFactory.getLogger(SideCarFirstProxySelector.class);

    private final Object lock = new Object();

    private final String localhost;
    private ServerNode sideCarProxy;
    private ServerNode dynamicSideCarProxy;
    private final Set<ServerNode> proxySetSameRegion = new HashSet<>();
    private List<ServerNode> dynamicProxyListSameRegion = new ArrayList<>();
    private final Set<ServerNode> proxySetOtherRegion = new HashSet<>();
    private List<ServerNode> dynamicProxyListOtherRegion = new ArrayList<>();

    private final RegionResolver regionResolver;
    private final String localRegion;

    public SideCarFirstProxySelector(String localhost, RegionResolver regionResolver) {
        this.localhost = localhost;
        this.regionResolver = regionResolver;
        this.localRegion = regionResolver.resolve(localhost);
    }

    @Override
    public ServerNode next() {
        try {
            if (dynamicSideCarProxy != null) {
                return dynamicSideCarProxy;
            }
            int retry = 0;
            while (retry < 2) {
                ServerNode proxy = tryPick(dynamicProxyListSameRegion);
                if (proxy != null) return proxy;
                proxy = tryPick(dynamicProxyListOtherRegion);
                if (proxy != null) return proxy;
                reset();
                retry ++;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void reset() {
        dynamicSideCarProxy = sideCarProxy;
        dynamicProxyListSameRegion = new ArrayList<>(proxySetSameRegion);
        dynamicProxyListOtherRegion = new ArrayList<>(proxySetOtherRegion);
    }

    private ServerNode tryPick(List<ServerNode> dynamicProxyList) {
        try {
            if (!dynamicProxyList.isEmpty()) {
                int index = ThreadLocalRandom.current().nextInt(dynamicProxyList.size());
                return dynamicProxyList.get(index);
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean ban(ServerNode proxy) {
        try {
            synchronized (lock) {
                if (proxy == null) {
                    return false;
                }
                if (proxy.getHost().equals(localhost)) {
                    dynamicSideCarProxy = null;
                    return true;
                }
                String region = regionResolver.resolve(proxy.getHost());
                if (Objects.equals(region, localRegion)) {
                    dynamicProxyListSameRegion.remove(proxy);
                } else {
                    dynamicProxyListOtherRegion.remove(proxy);
                }
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
                if (proxy == null) {
                    return false;
                }
                if (proxy.getHost().equals(localhost)) {
                    sideCarProxy = proxy;
                    dynamicSideCarProxy = proxy;
                    return true;
                }
                String region = regionResolver.resolve(proxy.getHost());
                if (Objects.equals(region, localRegion)) {
                    proxySetSameRegion.add(proxy);
                    dynamicProxyListSameRegion = new ArrayList<>(proxySetSameRegion);
                } else {
                    proxySetOtherRegion.add(proxy);
                    dynamicProxyListOtherRegion = new ArrayList<>(proxySetOtherRegion);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("add error, proxy = {}", proxy, e);
            return false;
        }
    }

    @Override
    public boolean remove(ServerNode proxy) {
        try {
            synchronized (lock) {
                if (proxy == null) return false;
                if (isOnlyOneProxy()) {
                    logger.warn("proxySet.size <= 1, skip remove proxy! proxy = {}", proxy);
                    return false;
                }
                if (proxy.getHost().equals(localhost)) {
                    sideCarProxy = null;
                    dynamicSideCarProxy = null;
                    return true;
                }
                String region = regionResolver.resolve(proxy.getHost());
                if (Objects.equals(region, localRegion)) {
                    proxySetSameRegion.remove(proxy);
                    dynamicProxyListSameRegion = new ArrayList<>(proxySetSameRegion);
                } else {
                    proxySetOtherRegion.remove(proxy);
                    dynamicProxyListOtherRegion = new ArrayList<>(proxySetOtherRegion);
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("remove error, proxy = {}", proxy, e);
            return false;
        }
    }

    private boolean isOnlyOneProxy() {
        int count = 0;
        if (sideCarProxy != null) {
            count ++;
        }
        count += proxySetOtherRegion.size();
        count += proxySetSameRegion.size();
        return count <= 1;
    }

    @Override
    public Set<ServerNode> getAll() {
        Set<ServerNode> proxySet = new HashSet<>();
        if (sideCarProxy != null) {
            proxySet.add(sideCarProxy);
        }
        proxySet.addAll(proxySetSameRegion);
        proxySet.addAll(proxySetOtherRegion);
        return proxySet;
    }

    @Override
    public List<ServerNode> sort(List<ServerNode> list) {
        List<ServerNode> shuffleList = new ArrayList<>(list);
        SideCarFirstProxySelector selector = new SideCarFirstProxySelector(localhost, regionResolver);
        for (ServerNode proxy : shuffleList) {
            selector.add(proxy);
        }
        List<ServerNode> ret = new ArrayList<>(list.size());
        if (selector.sideCarProxy != null) {
            ret.add(selector.sideCarProxy);
        }
        List<ServerNode> proxySetSameRegion = new ArrayList<>(selector.proxySetSameRegion);
        Collections.shuffle(proxySetSameRegion);
        List<ServerNode> proxySetOtherRegion = new ArrayList<>(selector.proxySetOtherRegion);
        Collections.shuffle(proxySetOtherRegion);
        ret.addAll(proxySetSameRegion);
        ret.addAll(proxySetOtherRegion);
        return ret;
    }


}

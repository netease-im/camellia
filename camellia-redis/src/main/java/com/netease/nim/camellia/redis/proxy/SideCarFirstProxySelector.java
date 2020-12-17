package com.netease.nim.camellia.redis.proxy;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * pick sid-car proxy first, other wise, will return the nearest proxy
 * Created by caojiajun on 2020/12/16
 */
public class SideCarFirstProxySelector implements IProxySelector {

    private static final Logger logger = LoggerFactory.getLogger(SideCarFirstProxySelector.class);

    private final Object lock = new Object();

    private final String localhost;
    private Proxy sideCarProxy;
    private Proxy dynamicSideCarProxy;
    private final Set<Proxy> proxySetSameRegion = new HashSet<>();
    private List<Proxy> dynamicProxyListSameRegion = new ArrayList<>();
    private final Set<Proxy> proxySetOtherRegion = new HashSet<>();
    private List<Proxy> dynamicProxyListOtherRegion = new ArrayList<>();

    private final RegionResolver regionResolver;
    private final String localRegion;

    public SideCarFirstProxySelector(String localhost, RegionResolver regionResolver) {
        this.localhost = localhost;
        this.regionResolver = regionResolver;
        this.localRegion = regionResolver.resolve(localhost);
    }

    @Override
    public Proxy next() {
        try {
            if (dynamicSideCarProxy != null) {
                return dynamicSideCarProxy;
            }
            int retry = 0;
            while (retry < 2) {
                Proxy proxy = tryPick(dynamicProxyListSameRegion);
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

    private Proxy tryPick(List<Proxy> dynamicProxyList) {
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
    public void ban(Proxy proxy) {
        try {
            synchronized (lock) {
                if (proxy == null) return;
                if (proxy.getHost().equals(localhost)) {
                    dynamicSideCarProxy = null;
                    return;
                }
                String region = regionResolver.resolve(proxy.getHost());
                if (Objects.equals(region, localRegion)) {
                    dynamicProxyListSameRegion.remove(proxy);
                } else {
                    dynamicProxyListOtherRegion.remove(proxy);
                }
            }
        } catch (Exception e) {
            logger.error("ban error", e);
        }
    }

    @Override
    public void add(Proxy proxy) {
        try {
            synchronized (lock) {
                if (proxy == null) return;
                if (proxy.getHost().equals(localhost)) {
                    sideCarProxy = proxy;
                    dynamicSideCarProxy = proxy;
                    return;
                }
                String region = regionResolver.resolve(proxy.getHost());
                if (Objects.equals(region, localRegion)) {
                    proxySetSameRegion.add(proxy);
                    dynamicProxyListSameRegion = new ArrayList<>(proxySetSameRegion);
                } else {
                    proxySetOtherRegion.add(proxy);
                    dynamicProxyListOtherRegion = new ArrayList<>(proxySetOtherRegion);
                }
            }
        } catch (Exception e) {
            logger.error("add error", e);
        }
    }

    @Override
    public void remove(Proxy proxy) {
        try {
            synchronized (lock) {
                if (proxy == null) return;
                if (proxy.getHost().equals(localhost)) {
                    if (proxySetSameRegion.isEmpty() && proxySetOtherRegion.isEmpty()) {
                        logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy.toString());
                    } else {
                        sideCarProxy = null;
                        dynamicSideCarProxy = null;
                    }
                    return;
                }
                String region = regionResolver.resolve(proxy.getHost());
                if (Objects.equals(region, localRegion)) {
                    if (sideCarProxy == null && proxySetOtherRegion.isEmpty()) {
                        logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy.toString());
                    } else {
                        proxySetSameRegion.remove(proxy);
                        dynamicProxyListSameRegion = new ArrayList<>(proxySetSameRegion);
                    }
                } else {
                    if (sideCarProxy == null && proxySetSameRegion.isEmpty()) {
                        logger.warn("proxySet.size = 1, skip remove proxy! proxy = {}", proxy.toString());
                    } else {
                        proxySetOtherRegion.remove(proxy);
                        dynamicProxyListOtherRegion = new ArrayList<>(proxySetOtherRegion);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("remove error", e);
        }
    }

    @Override
    public Set<Proxy> getAll() {
        Set<Proxy> proxySet = new HashSet<>();
        if (sideCarProxy != null) {
            proxySet.add(sideCarProxy);
        }
        proxySet.addAll(proxySetSameRegion);
        proxySet.addAll(proxySetOtherRegion);
        return proxySet;
    }
}

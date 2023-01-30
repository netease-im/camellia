package com.netease.nim.camellia.redis.proxy.discovery.common;

import com.netease.nim.camellia.core.discovery.AbstractCamelliaDiscovery;
import com.netease.nim.camellia.redis.base.proxy.IProxyDiscovery;
import com.netease.nim.camellia.redis.base.proxy.Proxy;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/10/17
 */
public class DetectedLocalConfProxyDiscovery extends AbstractCamelliaDiscovery<Proxy> implements IProxyDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(DetectedLocalConfProxyDiscovery.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    private List<Proxy> originalList;
    private List<Proxy> aliveList = new ArrayList<>();

    /**
     *
     * @param config 10.1.1.1:6380,10.1.1.2:6380
     */
    public DetectedLocalConfProxyDiscovery(String config, int detectIntervalSeconds) {
        init(parseConfig(config), detectIntervalSeconds);
    }

    public DetectedLocalConfProxyDiscovery(String config) {
        init(parseConfig(config), 10);
    }

    public DetectedLocalConfProxyDiscovery(List<Proxy> list) {
        init(list, 10);
    }

    public DetectedLocalConfProxyDiscovery(List<Proxy> list, int detectIntervalSeconds) {
        init(list, detectIntervalSeconds);
    }

    private void init(List<Proxy> list, int detectIntervalSeconds) {
        this.originalList = list;
        checkAndUpdate();
        if (aliveList.isEmpty()) {
            throw new IllegalArgumentException("all proxy node is not alive");
        }
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("detected-local-proxy-discovery"))
                .scheduleAtFixedRate(this::checkAndUpdate, detectIntervalSeconds, detectIntervalSeconds, TimeUnit.SECONDS);
    }

    private List<Proxy> parseConfig(String config) {
        List<Proxy> originalList = new ArrayList<>();
        String[] split = config.split(",");
        for (String str : split) {
            String[] split1 = str.split(":");
            originalList.add(new Proxy(split1[0], Integer.parseInt(split1[1])));
        }
        return originalList;
    }

    private void checkAndUpdate() {
        if (running.compareAndSet(false, true)) {
            try {
                List<Proxy> aliveList = new ArrayList<>();
                for (Proxy proxy : originalList) {
                    if (checkAlive(proxy)) {
                        aliveList.add(proxy);
                    }
                }

                if (aliveList.isEmpty()) {
                    logger.warn("all proxy node is not alive, proxy.list = {}", originalList);
                    return;
                }

                Set<Proxy> newSet = new HashSet<>(aliveList);
                Set<Proxy> oldSet = new HashSet<>(this.aliveList);

                //new - old = add
                Set<Proxy> addSet = new HashSet<>(newSet);
                addSet.removeAll(oldSet);
                if (!addSet.isEmpty()) {
                    //callback add
                    for (Proxy proxy : addSet) {
                        try {
                            invokeAddCallback(proxy);
                        } catch (Exception e) {
                            logger.error("callback add error", e);
                        }
                    }
                }

                //old - new = remove
                Set<Proxy> removeSet = new HashSet<>(oldSet);
                removeSet.removeAll(newSet);
                if (!removeSet.isEmpty()) {
                    //callback remove
                    for (Proxy proxy : removeSet) {
                        try {
                            invokeRemoveCallback(proxy);
                        } catch (Exception e) {
                            logger.error("callback remove error", e);
                        }
                    }
                }

                this.aliveList = new ArrayList<>(aliveList);
            } catch (Exception e) {
                logger.error("schedule error", e);
            } finally {
                running.compareAndSet(true, false);
            }
        }
    }

    private boolean checkAlive(Proxy proxy) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(proxy.getHost(), proxy.getPort()), 200);
            return true;
        } catch (Exception e) {
            logger.warn("check {} error, ex = {}", proxy, e.toString());
            return false;
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                logger.error("close error", e);
            }
        }
    }


    @Override
    public List<Proxy> findAll() {
        return new ArrayList<>(aliveList);
    }

}

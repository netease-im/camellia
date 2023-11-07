package com.netease.nim.camellia.feign.discovery;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.CamelliaServerSelector;
import com.netease.nim.camellia.core.discovery.CamelliaServerHealthChecker;
import com.netease.nim.camellia.core.discovery.GlobalDiscoveryEnv;
import com.netease.nim.camellia.feign.GlobalCamelliaFeignEnv;
import com.netease.nim.camellia.feign.resource.FeignDiscoveryResource;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/3/1
 */
public class DiscoveryResourcePool implements FeignResourcePool {

    private static final Logger logger = LoggerFactory.getLogger(DiscoveryResourcePool.class);

    private final CamelliaDiscovery<FeignServerInfo> discovery;
    private final CamelliaServerSelector<FeignResource> serverSelector;
    private final CamelliaServerHealthChecker<FeignServerInfo> healthChecker;
    private final FeignDiscoveryResource discoveryResource;

    private List<FeignResource> originalList = new ArrayList<>();
    private List<FeignResource> dynamicList = new ArrayList<>();

    private final Object lock = new Object();

    public DiscoveryResourcePool(FeignDiscoveryResource discoveryResource,
                                 CamelliaDiscovery<FeignServerInfo> discovery,
                                 CamelliaServerSelector<FeignResource> serverSelector,
                                 CamelliaServerHealthChecker<FeignServerInfo> healthChecker,
                                 ScheduledExecutorService scheduledExecutor) {
        this.discovery = discovery;
        this.serverSelector = serverSelector;
        this.discoveryResource = discoveryResource;
        this.healthChecker = healthChecker;
        if (discovery == null) {
            throw new IllegalArgumentException("discovery is empty");
        }
        if (serverSelector == null) {
            throw new IllegalArgumentException("serverSelector is empty");
        }
        reload();
        if (originalList.isEmpty()) {
            logger.warn("server list is empty, camellia-feign-service = {}", discoveryResource.getUrl());
        }
        discovery.setCallback(new CamelliaDiscovery.Callback<FeignServerInfo>() {
            @Override
            public void add(FeignServerInfo server) {
                DiscoveryResourcePool.this.add(toFeignResource(server));
            }

            @Override
            public void remove(FeignServerInfo server) {
                DiscoveryResourcePool.this.remove(toFeignResource(server));
            }
        });
        //兜底1分钟reload一次
        scheduledExecutor.scheduleAtFixedRate(this::reload, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public FeignResource getResource(Object loadBalanceKey) {
        try {
            Exception cause = null;
            int retry = 10;
            FeignResource feignResource = null;
            while (retry > 0) {
                retry --;
                try {
                    if (dynamicList.isEmpty()) {
                        feignResource = serverSelector.pick(originalList, loadBalanceKey);
                    } else {
                        feignResource = serverSelector.pick(dynamicList, loadBalanceKey);
                    }
                } catch (Exception e) {
                    cause = e;
                    continue;
                }
                if (feignResource != null) {
                    if (healthCheck(feignResource)) {
                        return feignResource;
                    }
                }
            }
            if (feignResource != null) {
                return feignResource;
            }
            throw new IllegalStateException("no reachable server", cause);
        } catch (Exception e) {
            throw new IllegalStateException("no reachable server", e);
        }
    }

    @Override
    public void onError(FeignResource feignResource) {
        try {
            synchronized (lock) {
                Set<FeignResource> set = new HashSet<>(dynamicList);
                set.remove(feignResource);
                List<FeignResource> list = new ArrayList<>(set);
                Collections.sort(list);
                dynamicList = new ArrayList<>(list);
                if (dynamicList.isEmpty()) {
                    GlobalCamelliaFeignEnv.register(discoveryResource, serverSelector, new ArrayList<>(originalList));
                } else {
                    GlobalCamelliaFeignEnv.register(discoveryResource, serverSelector, new ArrayList<>(dynamicList));
                }
                if (GlobalDiscoveryEnv.logInfoEnable) {
                    logger.info("onError feignResource = {}, dynamicList = {}, originalList = {}", feignResource, dynamicList, originalList);
                }
            }
        } catch (Exception e) {
            logger.error("onError error", e);
        }
    }

    private void add(FeignResource feignResource) {
        try {
            synchronized (lock) {
                Set<FeignResource> set = new HashSet<>(originalList);
                set.add(feignResource);
                List<FeignResource> list = new ArrayList<>(set);
                Collections.sort(list);
                originalList = new ArrayList<>(list);
                dynamicList = new ArrayList<>(list);
                GlobalCamelliaFeignEnv.register(discoveryResource, serverSelector, new ArrayList<>(list));
                if (GlobalDiscoveryEnv.logInfoEnable) {
                    logger.info("add feignResource = {}, dynamicList = {}, originalList = {}", feignResource, dynamicList, originalList);
                }
            }
        } catch (Exception e) {
            logger.error("add error", e);
        }
    }

    private void remove(FeignResource feignResource) {
        try {
            synchronized (lock) {
                Set<FeignResource> set = new HashSet<>(originalList);
                set.remove(feignResource);
                if (set.isEmpty()) {
                    logger.warn("last server, skip remove");
                    return;
                }
                List<FeignResource> list = new ArrayList<>(set);
                Collections.sort(list);
                originalList = new ArrayList<>(list);
                dynamicList = new ArrayList<>(list);
                GlobalCamelliaFeignEnv.register(discoveryResource, serverSelector, new ArrayList<>(list));
                if (GlobalDiscoveryEnv.logInfoEnable) {
                    logger.info("remove feignResource = {}, dynamicList = {}, originalList = {}", feignResource, dynamicList, originalList);
                }
            }
        } catch (Exception e) {
            logger.error("remove error", e);
        }
    }

    private final CamelliaLocalCache cache = new CamelliaLocalCache();
    private static final String HEALTH_CHECK = "healthCheck";

    private boolean healthCheck(FeignResource feignResource) {
        String url = feignResource.getUrl();
        Boolean result = cache.get(HEALTH_CHECK, url, Boolean.class);
        if (result == null) {
            result = healthChecker.healthCheck(toServerInfo(feignResource));
            //缓存个1s
            cache.put(HEALTH_CHECK, url, result, 1);
            return result;
        }
        return result;
    }

    private void reload() {
        List<FeignServerInfo> all = discovery.findAll();
        List<FeignResource> list = new ArrayList<>();
        for (FeignServerInfo feignServerInfo : all) {
            list.add(toFeignResource(feignServerInfo));
        }
        Collections.sort(list);
        this.originalList = new ArrayList<>(list);
        this.dynamicList = new ArrayList<>(list);

        GlobalCamelliaFeignEnv.register(discoveryResource, serverSelector, new ArrayList<>(list));
        if (GlobalDiscoveryEnv.logInfoEnable) {
            logger.info("reload, dynamicList = {}, originalList = {}", dynamicList, originalList);
        }
    }

    private FeignResource toFeignResource(FeignServerInfo feignServerInfo) {
        return new FeignResource(discoveryResource.getProtocol() + feignServerInfo.getHost() + ":" + feignServerInfo.getPort());
    }

    private FeignServerInfo toServerInfo(FeignResource feignResource) {
        String feignUrl = feignResource.getFeignUrl();
        String[] split = feignUrl.substring(discoveryResource.getProtocol().length()).split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        FeignServerInfo feignServerInfo = new FeignServerInfo();
        feignServerInfo.setHost(host);
        feignServerInfo.setPort(port);
        return feignServerInfo;
    }
}

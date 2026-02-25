package com.netease.nim.camellia.core.discovery;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2026/2/25
 */
public class CamelliaDiscoverySwitcher extends AbstractCamelliaDiscovery {

    private static final ScheduledExecutorService defaultSchedule = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new CamelliaThreadFactory(CamelliaDiscoverySwitcher.class));
    private static final Logger logger = LoggerFactory.getLogger(CamelliaDiscoverySwitcher.class);

    private final DiscoveryIndexSwitcher switcher;
    private final List<CamelliaDiscovery> discoveryList;
    private int index;

    public CamelliaDiscoverySwitcher(List<CamelliaDiscovery> discoveryList, DiscoveryIndexSwitcher switcher) {
        this(discoveryList, switcher, defaultSchedule, 10);
    }

    public CamelliaDiscoverySwitcher(List<CamelliaDiscovery> discoveryList, DiscoveryIndexSwitcher switcher,
                                     ScheduledExecutorService scheduler, int scheduleIntervalSeconds) {
        this.discoveryList = discoveryList;
        this.switcher = switcher;
        for (int i=0; i<discoveryList.size(); i++) {
            CamelliaDiscovery discovery = discoveryList.get(i);
            discovery.setCallback(new CallbackBridge(this, i));
        }
        schedule();
        scheduler.scheduleAtFixedRate(this::schedule, scheduleIntervalSeconds, scheduleIntervalSeconds, TimeUnit.SECONDS);
    }

    private void schedule() {
        try {
            int newIndex = switcher.selectIndex();
            if (newIndex < 0 || newIndex >= discoveryList.size()) {
                logger.error("select discovery invalid index = {}, size = {}", newIndex, discoveryList.size());
                return;
            }
            if (index != newIndex) {
                CamelliaDiscovery discovery = discoveryList.get(newIndex);
                List<ServerNode> all = discovery.findAll();
                for (ServerNode serverNode : all) {
                    invokeAddCallback(serverNode);
                }
                this.index = newIndex;
            }
        } catch (Exception e) {
            logger.error("discovery switch error", e);
        }
    }

    @Override
    public List<ServerNode> findAll() {
        return discoveryList.get(index).findAll();
    }

    private static class CallbackBridge implements Callback {

        private final CamelliaDiscoverySwitcher discoverySwitcher;
        private final int index;

        public CallbackBridge(CamelliaDiscoverySwitcher discoverySwitcher, int index) {
            this.discoverySwitcher = discoverySwitcher;
            this.index = index;
        }

        @Override
        public void add(ServerNode server) {
            if (discoverySwitcher.index == index) {
                discoverySwitcher.invokeAddCallback(server);
            }
        }

        @Override
        public void remove(ServerNode server) {
            if (discoverySwitcher.index == index) {
                discoverySwitcher.invokeRemoveCallback(server);
            }
        }
    }
}

package com.netease.nim.camellia.core.discovery;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2026/4/20
 */
public class CamelliaDiscoveryFactorySwitcherTest {

    @Test
    public void shouldReuseDiscoveryForSameServiceName() {
        CountingFactory factory1 = new CountingFactory("factory1");
        CountingFactory factory2 = new CountingFactory("factory2");
        ScheduledExecutorService scheduler = newScheduler();
        try {
            CamelliaDiscoveryFactorySwitcher switcher = newFactorySwitcher(scheduler, factory1, factory2);

            CamelliaDiscovery discovery1 = switcher.getDiscovery("serviceA");
            CamelliaDiscovery discovery2 = switcher.getDiscovery("serviceA");

            Assert.assertSame(discovery1, discovery2);
            Assert.assertEquals(1, factory1.getCallCount("serviceA"));
            Assert.assertEquals(1, factory2.getCallCount("serviceA"));
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void shouldIsolateDifferentServiceNames() {
        CountingFactory factory1 = new CountingFactory("factory1");
        CountingFactory factory2 = new CountingFactory("factory2");
        ScheduledExecutorService scheduler = newScheduler();
        try {
            CamelliaDiscoveryFactorySwitcher switcher = newFactorySwitcher(scheduler, factory1, factory2);

            CamelliaDiscovery discovery1 = switcher.getDiscovery("serviceA");
            CamelliaDiscovery discovery2 = switcher.getDiscovery("serviceB");

            Assert.assertNotSame(discovery1, discovery2);
            Assert.assertEquals(1, factory1.getCallCount("serviceA"));
            Assert.assertEquals(1, factory1.getCallCount("serviceB"));
            Assert.assertEquals(1, factory2.getCallCount("serviceA"));
            Assert.assertEquals(1, factory2.getCallCount("serviceB"));
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void shouldCreateDiscoveryOnlyOnceUnderConcurrentAccess() throws Exception {
        CountingFactory factory1 = new CountingFactory("factory1");
        CountingFactory factory2 = new CountingFactory("factory2");
        ScheduledExecutorService scheduler = newScheduler();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CamelliaDiscovery>> futures = new ArrayList<>();
        try {
            CamelliaDiscoveryFactorySwitcher switcher = newFactorySwitcher(scheduler, factory1, factory2);
            for (int i = 0; i < 8; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    Assert.assertTrue(ready.await(5, TimeUnit.SECONDS));
                    Assert.assertTrue(start.await(5, TimeUnit.SECONDS));
                    return switcher.getDiscovery("serviceA");
                }));
            }
            start.countDown();
            Set<CamelliaDiscovery> result = new HashSet<>();
            for (Future<CamelliaDiscovery> future : futures) {
                result.add(future.get(5, TimeUnit.SECONDS));
            }
            Assert.assertEquals(1, result.size());
            Assert.assertEquals(1, factory1.getCallCount("serviceA"));
            Assert.assertEquals(1, factory2.getCallCount("serviceA"));
        } finally {
            executorService.shutdownNow();
            scheduler.shutdownNow();
        }
    }

    private CamelliaDiscoveryFactorySwitcher newFactorySwitcher(ScheduledExecutorService scheduler, CountingFactory... factories) {
        return new CamelliaDiscoveryFactorySwitcher(Arrays.asList(factories), () -> 0, scheduler, 10);
    }

    private ScheduledExecutorService newScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "camellia-discovery-switcher-test");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static class CountingFactory implements CamelliaDiscoveryFactory {
        private final String name;
        private final Map<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();

        private CountingFactory(String name) {
            this.name = name;
        }

        @Override
        public CamelliaDiscovery getDiscovery(String serviceName) {
            counterMap.computeIfAbsent(serviceName, k -> new AtomicInteger()).incrementAndGet();
            return new StaticDiscovery(name + "-" + serviceName);
        }

        private int getCallCount(String serviceName) {
            AtomicInteger counter = counterMap.get(serviceName);
            if (counter == null) {
                return 0;
            }
            return counter.get();
        }
    }

    private static class StaticDiscovery extends AbstractCamelliaDiscovery {

        private final List<ServerNode> serverNodes;

        private StaticDiscovery(String name) {
            this.serverNodes = Collections.singletonList(new ServerNode(name, 6379));
        }

        @Override
        public List<ServerNode> findAll() {
            return serverNodes;
        }
    }
}

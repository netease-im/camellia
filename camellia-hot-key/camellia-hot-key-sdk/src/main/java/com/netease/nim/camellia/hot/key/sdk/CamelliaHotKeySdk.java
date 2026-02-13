package com.netease.nim.camellia.hot.key.sdk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CollectionSplitUtil;
import com.netease.nim.camellia.hot.key.common.exception.CamelliaHotKeyException;
import com.netease.nim.camellia.hot.key.common.model.*;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyConstants;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import com.netease.nim.camellia.hot.key.sdk.collect.*;
import com.netease.nim.camellia.hot.key.sdk.conf.CamelliaHotKeySdkConfig;
import com.netease.nim.camellia.hot.key.sdk.listener.CamelliaHotKeyConfigListener;
import com.netease.nim.camellia.hot.key.sdk.listener.CamelliaHotKeyListener;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyClient;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyClientHub;
import com.netease.nim.camellia.hot.key.sdk.netty.HotKeyClientListener;
import com.netease.nim.camellia.hot.key.sdk.util.HotKeySdkUtils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by caojiajun on 2023/5/6
 */
public class CamelliaHotKeySdk implements ICamelliaHotKeySdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHotKeySdk.class);

    private static final AtomicLong idGen = new AtomicLong(0);

    private static final LongAdder dropCount = new LongAdder();

    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-hot-key-sdk-error"))
                .scheduleAtFixedRate(() -> {
                    long c = dropCount.sumThenReset();
                    if (c > 0) {
                        logger.debug("drop {} key collect for queue full", c);
                    }
                }, 30, 30, TimeUnit.SECONDS);
    }

    private final long id;
    private final CamelliaHotKeySdkConfig config;
    private final ConcurrentHashMap<String, List<CamelliaHotKeyListener>> hotKeyListenerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<CamelliaHotKeyConfigListener>> hotKeyConfigListenerMap = new ConcurrentHashMap<>();

    private final IHotKeyCounterCollector collector;

    private final boolean async;
    private LinkedBlockingQueue<QueueItem> queue;

    private int collectListInitSize = HotKeySdkUtils.update(0);

    public CamelliaHotKeySdk(CamelliaHotKeySdkConfig config) {
        this.config = config;
        if (config.getDiscoveryFactory() == null || config.getServiceName() == null) {
            throw new CamelliaHotKeyException("discovery is missing");
        }
        CollectorType collectorType = config.getCollectorType();
        if (collectorType == CollectorType.Caffeine) {
            this.collector = new CaffeineCollector(config.getCapacity());
        } else if (collectorType == CollectorType.ConcurrentLinkedHashMap) {
            this.collector = new ConcurrentLinkedHashMapCollector(config.getCapacity());
        } else if (collectorType == CollectorType.ConcurrentHashMap) {
            this.collector = new ConcurrentHashMapCollector(config.getCapacity());
        } else {
            throw new IllegalArgumentException("unknown collectorType");
        }

        HotKeyClientHub.getInstance().registerDiscovery(config.getDiscoveryFactory(), config.getServiceName());
        HotKeyClientHub.getInstance().registerListener(new DefaultHotKeyClientListener(this));
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-hot-key-sdk-schedule")).scheduleAtFixedRate(this::schedulePush,
                config.getPushIntervalMillis(), config.getPushIntervalMillis(), TimeUnit.MILLISECONDS);

        this.id = idGen.incrementAndGet();
        this.async = config.isAsync();
        if (this.async) {
            this.queue = new LinkedBlockingQueue<>(config.getAsyncQueueCapacity());
            startPolling();
        }

        logger.info("CamelliaHotKeySdk init success, pushIntervalMillis = {}, pushBatch = {}, capacity = {}, " +
                        "collector = {}, async = {}, asyncQueueCapacity = {}",
                config.getPushIntervalMillis(), config.getPushBatch(), config.getCapacity(),
                config.getCollectorType(), async, config.getAsyncQueueCapacity());
    }

    public CamelliaHotKeySdkConfig getConfig() {
        return config;
    }

    @Override
    public void push(String namespace, String key, KeyAction keyAction, long count) {
        if (async) {
            if (!queue.offer(new QueueItem(namespace, key, keyAction, count))) {
                dropCount.increment();
            }
        } else {
            collector.push(namespace, key, keyAction, count);
        }
    }

    private static class QueueItem {
        String namespace;
        String key;
        KeyAction keyAction;
        long count;
        public QueueItem(String namespace, String key, KeyAction keyAction, long count) {
            this.namespace = namespace;
            this.key = key;
            this.keyAction = keyAction;
            this.count = count;
        }
    }

    private void startPolling() {
        new Thread(() -> {
            while (true) {
                try {
                    QueueItem item = queue.take();
                    collector.push(item.namespace, item.key, item.keyAction, item.count);
                } catch (Exception e) {
                    logger.error("collector push error", e);
                }
            }
        }, "camellia-hot-key-sdk-polling-" + id).start();
    }

    @Override
    public HotKeyConfig getHotKeyConfig(String namespace) {
        try {
            HotKeyClient client = HotKeyClientHub.getInstance().selectClient(config.getServiceName(), UUID.randomUUID().toString());
            if (client == null) {
                logger.warn("not found valid HotKeyClient, return null HotKeyConfig");
                throw new CamelliaHotKeyException("can not found valid hot key server");
            }
            CompletableFuture<HotKeyPack> future = client.sendPack(HotKeyPack.newPack(HotKeyCommand.GET_CONFIG,
                    new GetConfigPack(namespace, HotKeyConstants.Client.source)));
            HotKeyPack hotKeyPack = future.get(HotKeyConstants.Client.getConfigTimeoutMillis, TimeUnit.MILLISECONDS);
            GetConfigRepPack repPack = (GetConfigRepPack) hotKeyPack.getBody();
            return repPack.getConfig();
        } catch (CamelliaHotKeyException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaHotKeyException(e);
        }
    }

    @Override
    public void sendHotkeyCacheStats(List<HotKeyCacheStats> statsList) {
        try {
            HotKeyClient client = HotKeyClientHub.getInstance().selectClient(config.getServiceName(), UUID.randomUUID().toString());
            if (client == null) {
                logger.warn("not found valid HotKeyClient, sendHotkeyCacheStats fail");
                throw new CamelliaHotKeyException("can not found valid hot key server");
            }
            CompletableFuture<HotKeyPack> future = client.sendPack(HotKeyPack.newPack(HotKeyCommand.HOT_KEY_CACHE_STATS, new HotKeyCacheStatsPack(statsList)));
            HotKeyPack hotKeyPack = future.get(HotKeyConstants.Client.pushCacheHitStatsTimeoutMillis, TimeUnit.MILLISECONDS);
            if (hotKeyPack != null) {
                HotKeyCacheStatsRepPack repPack = (HotKeyCacheStatsRepPack) hotKeyPack.getBody();
                if (repPack != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("sendHotkeyCacheStats success, size = {}", statsList.size());
                    }
                }
            }
        } catch (CamelliaHotKeyException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaHotKeyException(e);
        }
    }

    @Override
    public synchronized void addListener(String namespace, CamelliaHotKeyConfigListener listener) {
        List<CamelliaHotKeyConfigListener> listeners = CamelliaMapUtils.computeIfAbsent(hotKeyConfigListenerMap, namespace, k -> new ArrayList<>());
        listeners.add(listener);
    }

    @Override
    public synchronized void addListener(String namespace, CamelliaHotKeyListener listener) {
        List<CamelliaHotKeyListener> listeners = CamelliaMapUtils.computeIfAbsent(hotKeyListenerMap, namespace, k -> new ArrayList<>());
        listeners.add(listener);
    }

    private void schedulePush() {
        try {
            List<KeyCounter> collect = collector.collect();
            Map<HotKeyClient, List<KeyCounter>> map = new HashMap<>();
            for (KeyCounter counter : collect) {
                HotKeyClient client = HotKeyClientHub.getInstance().selectClient(config.getServiceName(), counter.getKey());
                List<KeyCounter> counters = CamelliaMapUtils.computeIfAbsent(map, client, k -> new ArrayList<>(collectListInitSize));
                counters.add(counter);
            }
            int maxSize = 0;
            for (Map.Entry<HotKeyClient, List<KeyCounter>> entry : map.entrySet()) {
                HotKeyClient client = entry.getKey();
                if (client == null) {
                    logger.error("selectClient return null, skip push");
                    continue;
                }
                List<KeyCounter> counters = entry.getValue();
                if (counters.size() > maxSize) {
                    maxSize = counters.size();
                }
                List<List<KeyCounter>> split = CollectionSplitUtil.split(counters, config.getPushBatch());
                for (List<KeyCounter> list : split) {
                    CompletableFuture<HotKeyPack> future = client.sendPack(HotKeyPack.newPack(HotKeyCommand.PUSH, new PushPack(list)));
                    future.thenAccept(pack -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("send push pack to server success, size = {}", list.size());
                        }
                    });
                    future.exceptionally(throwable -> {
                        logger.error("send push pack to server error, size = {}", list.size(), throwable);
                        return null;
                    });
                }
            }
            this.collectListInitSize = HotKeySdkUtils.update(maxSize);
        } catch (Exception e) {
            logger.error("schedulePush error", e);
        }
    }

    private static class DefaultHotKeyClientListener implements HotKeyClientListener {

        private final CamelliaHotKeySdk sdk;

        public DefaultHotKeyClientListener(CamelliaHotKeySdk sdk) {
            this.sdk = sdk;
        }

        @Override
        public void onHotKey(HotKey hotKey) {
            String namespace = hotKey.getNamespace();
            List<CamelliaHotKeyListener> listeners = sdk.hotKeyListenerMap.get(namespace);
            if (listeners != null && !listeners.isEmpty()) {
                for (CamelliaHotKeyListener listener : listeners) {
                    try {
                        HotKeyEvent event = new HotKeyEvent(hotKey.getNamespace(), hotKey.getAction(), hotKey.getKey(), hotKey.getExpireMillis());
                        listener.onHotKeyEvent(event);
                    } catch (Exception e) {
                        logger.error("onHotKeyEvent error, hotKey = {}", JSONObject.toJSONString(hotKey), e);
                    }
                }
            }
        }

        @Override
        public void onHotKeyConfig(HotKeyConfig hotKeyConfig) {
            String namespace = hotKeyConfig.getNamespace();
            List<CamelliaHotKeyConfigListener> listeners = sdk.hotKeyConfigListenerMap.get(namespace);
            if (listeners != null && !listeners.isEmpty()) {
                for (CamelliaHotKeyConfigListener listener : listeners) {
                    try {
                        listener.onHotKeyConfigChange(hotKeyConfig);
                    } catch (Exception e) {
                        logger.error("onHotKeyConfigChange error, hotKey = {}", JSONObject.toJSONString(hotKeyConfig), e);
                    }
                }
            }
        }
    }
}

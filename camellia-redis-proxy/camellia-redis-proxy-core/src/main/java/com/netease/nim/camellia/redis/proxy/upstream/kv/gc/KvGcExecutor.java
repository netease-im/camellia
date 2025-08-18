package com.netease.nim.camellia.redis.proxy.upstream.kv.gc;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.KvExecutorMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.KvGcMonitor;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.RedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.RedisTemplate;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyDesign;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.EncodeVersion;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.redis.proxy.upstream.utils.CompletableFutureUtils;
import com.netease.nim.camellia.redis.proxy.util.*;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/4/22
 */
public class KvGcExecutor {
    private static final Logger logger = LoggerFactory.getLogger(KvGcExecutor.class);

    private final String namespace;
    private final KVClient kvClient;
    private final KeyDesign keyDesign;
    private final KvConfig kvConfig;
    private final MpscSlotHashExecutor deleteExecutor;
    private final ThreadPoolExecutor submitExecutor;
    private final ScheduledThreadPoolExecutor scheduler;
    private final ThreadPoolExecutor metaKeyScanExecutor;
    private final ThreadPoolExecutor subKeyScanExecutor;
    private RedisTemplate redisTemplate;

    public KvGcExecutor(KVClient kvClient, KeyDesign keyDesign, KvConfig kvConfig) {
        this.namespace = Utils.bytesToString(keyDesign.getNamespace());
        this.kvClient = kvClient;
        this.keyDesign = keyDesign;
        this.kvConfig = kvConfig;
        this.deleteExecutor = new MpscSlotHashExecutor("camellia-kv-gc", kvConfig.gcExecutorPoolSize(),
                kvConfig.gcExecutorQueueSize(), new MpscSlotHashExecutor.CallerRunsPolicy());
        this.submitExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024*128), new CamelliaThreadFactory("camellia-kv-gc-submit"), new ThreadPoolExecutor.AbortPolicy());
        this.scheduler = new ScheduledThreadPoolExecutor(1, new CamelliaThreadFactory("camellia-kv-gc-scheduler"));
        this.metaKeyScanExecutor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(32), new CamelliaThreadFactory("camellia-kv-gc-meta-key-executor"));
        this.subKeyScanExecutor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(32), new CamelliaThreadFactory("camellia-kv-gc-sub-key-executor"));

        boolean gcScheduleEnable = ProxyDynamicConf.getBoolean("kv.gc.schedule.enable", false);
        if (gcScheduleEnable) {
            if (!kvClient.supportCheckAndDelete() && !kvClient.supportTTL()) {
                String url = ProxyDynamicConf.getString("kv.gc.clean.expired.key.meta.target.proxy.cluster.url", null);
                if (url == null) {
                    throw new KvException("kv client do not support checkAndDelete api, should config 'kv.gc.clean.expired.key.meta.target.proxy.cluster.url'");
                }
                ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(url);
                RedisProxyEnv env = GlobalRedisProxyEnv.getClientTemplateFactory().getEnv();
                this.redisTemplate = new RedisTemplate(new UpstreamRedisClientTemplate(env, resourceTable));
            }
        }
        KvExecutorMonitor.register("gc-" + kvConfig.getNamespace(), deleteExecutor);
        logger.info("kv gc executor init success, namespace = {}", namespace);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                boolean gcScheduleEnable = ProxyDynamicConf.getBoolean("kv.gc.schedule.enable", false);
                if (!gcScheduleEnable) {
                    return;
                }

                if (!checkInScheduleGcTime()) {
                    return;
                }

                int gcScheduleIntervalMinute = ProxyDynamicConf.getInt("kv.gc.schedule.interval.minute", 24 * 60);
                long lastGcTime = KvGcEnv.getLastGcTime(namespace);
                if (System.currentTimeMillis() - lastGcTime < gcScheduleIntervalMinute*60*1000L) {
                    return;
                }

                if (!KvGcEnv.acquireGcLock(namespace)) {
                    return;
                }

                //meta-key scan
                CompletableFuture<Boolean> metaKeyScanFuture;
                int metaKeyScanThreads = ProxyDynamicConf.getInt("kv.gc.meta.key.max.scan.threads", 4);
                if (!kvClient.supportTTL()) {
                    Integer startSlot = KvGcEnv.getMetaKeyScanStartSlot(namespace);
                    ScanTask scanTask = new ScanTask("meta-key", metaKeyScanExecutor, metaKeyScanThreads, startSlot) {
                        @Override
                        public void scan(int targetSlot, AtomicLong scanKeys, AtomicLong deleteKeys) {
                            scanMetaKeys(targetSlot, scanKeys, deleteKeys);
                        }

                        @Override
                        public void updateCompleteSlot(int completeSlot) {
                            KvGcEnv.updateMetaKeyScanStartSlot(namespace, completeSlot);
                        }
                    };
                    metaKeyScanFuture = scanTask.invoke();
                } else {
                    metaKeyScanFuture = CompletableFuture.completedFuture(true);
                }

                //sub-key scan
                int subKeyScanThreads = ProxyDynamicConf.getInt("kv.gc.sub.key.max.scan.threads", 4);
                Integer startSlot = KvGcEnv.getSubKeyScanStartSlot(namespace);
                ScanTask scanTask = new ScanTask("sub-key", subKeyScanExecutor, subKeyScanThreads, startSlot) {
                    @Override
                    public void scan(int targetSlot, AtomicLong scanKeys, AtomicLong deleteKeys) {
                        scanSubKeys(targetSlot, scanKeys, deleteKeys);
                    }
                    @Override
                    public void updateCompleteSlot(int completeSlot) {
                        KvGcEnv.updateSubKeyScanStartSlot(namespace, completeSlot);
                    }
                };
                CompletableFuture<Boolean> subKeyScanFuture = scanTask.invoke();

                //
                if (metaKeyScanFuture.get() && subKeyScanFuture.get()) {
                    KvGcEnv.updateGcTime(namespace, System.currentTimeMillis());
                }
            } catch (Throwable e) {
                logger.error("gc schedule error", e);
            } finally {
                KvGcEnv.release(namespace);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    private abstract class ScanTask {

        private final AtomicLong scanKeys = new AtomicLong();
        private final AtomicLong deleteKeys = new AtomicLong();

        private final String name;
        private final int threads;
        private final Integer startSlot;
        private final ThreadPoolExecutor executor;
        private final long startTime = System.currentTimeMillis();

        public ScanTask(String name, ThreadPoolExecutor executor, int threads, Integer startSlot) {
            this.name = name;
            this.executor = executor;
            this.threads = threads;
            Integer slot = startSlot;
            if (slot == null || slot < 0) {
                slot = 0;
            }
            this.startSlot = slot;
            logger.info("scan task init, name = {}, threads = {}, start slot = {}", name, threads, startSlot);
        }

        public CompletableFuture<Boolean> invoke() {
            if (executor.getCorePoolSize() < threads) {
                executor.setMaximumPoolSize(threads);
                executor.setCorePoolSize(threads);
            } else if (executor.getCorePoolSize() > threads) {
                executor.setCorePoolSize(threads);
                executor.setMaximumPoolSize(threads);
            }
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

            LinkedBlockingQueue<Integer> slots = new LinkedBlockingQueue<>();
            for (int i=startSlot; i<RedisClusterCRC16Utils.SLOT_SIZE; i++) {
                slots.add(i);
            }
            List<CompletableFuture<String>> futureList = new ArrayList<>();
            final Set<Integer> completeSlots = new HashSet<>();
            for (int i=0; i<threads; i++) {
                CompletableFuture<String> future = new CompletableFuture<>();
                futureList.add(future);
                executor.submit(() -> {
                    String reason = "unknown";
                    try {
                        while (true) {
                            Integer targetSlot = slots.poll();
                            if (targetSlot == null) {
                                reason = "complete";
                                updateCompleteSlot(-1);
                                break;
                            }
                            scan(targetSlot, scanKeys, deleteKeys);
                            synchronized (completeSlots) {
                                completeSlots.add(targetSlot);
                                int completeSlot = getCompleteSlot(startSlot, completeSlots);
                                if (completeSlot >= 0) {
                                    updateCompleteSlot(completeSlot);
                                }
                            }

                            if (!checkInScheduleGcTime()) {
                                reason = "not in gc time";
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("meta key scan error", e);
                    } finally {
                        future.complete(reason);
                    }
                });
            }
            CompletableFuture<List<String>> all = CompletableFutureUtils.allOf(futureList);
            all.thenAccept(results -> {
                logger.info("scan task = {} stop for {}, scan slots = {}, scan keys = {}, delete keys = {}, spendMs = {}",
                        name, results.getFirst(), completeSlots.size(), scanKeys.get(), deleteKeys.get(), System.currentTimeMillis() - startTime);
                completableFuture.complete(true);
            });
            return completableFuture;
        }

        private int getCompleteSlot(int startSlot, Set<Integer> completeSlots) {
            int result = -1;
            while (true) {
                if (completeSlots.contains(startSlot)) {
                    result = startSlot;
                    startSlot ++;
                } else {
                    break;
                }
            }
            return result;
        }

        public abstract void scan(int targetSlot, AtomicLong scanKeys, AtomicLong deleteKeys);

        public abstract void updateCompleteSlot(int completeSlot);
    }



    public void submitSubKeyDeleteTask(int slot, byte[] key, KeyMeta keyMeta) {
        try {
            if (keyMeta.getKeyType() == KeyType.string) {
                return;
            }
            submitExecutor.submit(() -> {
                try {
                    deleteExecutor.submit(slot, new SubKeyDeleteTask(slot, key, namespace, keyMeta, kvClient, keyDesign, kvConfig));
                } catch (Exception e) {
                    logger.warn("execute sub key delete task error, ex = {}", e.toString());
                }
            });
        } catch (Exception e) {
            ErrorLogCollector.collect(KvGcExecutor.class, "submit sub key delete task error", e);
        }
    }

    private void scanMetaKeys(int slot, AtomicLong scanKeys, AtomicLong deleteMetaKeys) {
        long startTime = System.currentTimeMillis();
        logger.info("scan meta keys start, namespace = {}, slot = {}", namespace, slot);
        long time = TimeCache.currentMillis;
        try {
            byte[] metaPrefix = keyDesign.getMetaPrefix();
            //
            byte[] startKey = metaPrefix;
            //
            int limit = kvConfig.scanBatch();
            while (true) {
                List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, metaPrefix, limit, Sort.ASC, false);
                if (scan.isEmpty()) {
                    break;
                }
                KvGcMonitor.scanMetaKeys(namespace, scan.size());
                for (KeyValue keyValue : scan) {
                    startKey = keyValue.getKey();
                    scanKeys.incrementAndGet();
                    KeyMeta keyMeta;
                    try {
                        keyMeta = KeyMeta.fromBytes(keyValue.getValue());
                    } catch (Exception e) {
                        logger.error("gc decode key meta value error, namespace = {}, will delete kv = {}", namespace, keyValue, e);
                        kvClient.delete(slot, keyValue.getKey());
                        TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                        continue;
                    }
                    if (keyMeta == null) {
                        continue;
                    }
                    if (keyMeta.isExpire()) {
                        byte[] key;
                        try {
                            key = keyDesign.decodeKeyByMetaKey(startKey);
                        } catch (Exception e) {
                            logger.error("gc decode key meta error, namespace = {}, will delete kv = {}", namespace, keyValue, e);
                            kvClient.delete(slot, keyValue.getKey());
                            TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                            continue;
                        }
                        if (key != null && keyMeta.getKeyType() != KeyType.string) {
                            deleteExecutor.submit(slot, new SubKeyDeleteTask(slot, key, namespace, keyMeta, kvClient, keyDesign, kvConfig));
                        }
                        if (kvClient.supportCheckAndDelete()) {//kv本身已经支持cas的删除，则可以直接删
                            kvClient.checkAndDelete(slot, startKey, keyValue.getValue());
                            deleteMetaKeys.incrementAndGet();
                            KvGcMonitor.deleteMetaKeys(Utils.bytesToString(keyDesign.getNamespace()), 1);
                        } else {
                            //如果kv不支持cas的删除，则hash到特定的proxy节点处理，避免并发问题
                            try {
                                Reply reply = redisTemplate.sendCleanExpiredKeyMetaInKv(keyDesign.getNamespace(), key, kvConfig.gcCleanExpiredKeyMetaTimeoutMillis());
                                if (reply instanceof ErrorReply) {
                                    logger.error("send clean expired key meta in kv failed, namespace = {}, reply = {}", namespace, ((ErrorReply) reply).getError());
                                } else {
                                    deleteMetaKeys.incrementAndGet();
                                    KvGcMonitor.deleteMetaKeys(Utils.bytesToString(keyDesign.getNamespace()), 1);
                                }
                            } catch (Exception e) {
                                logger.error("send clean expired key meta in kv error", e);
                            }
                        }
                    }
                }
                if (scan.size() < limit) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                if (TimeCache.currentMillis - time > 60*1000L) {
                    logger.info("scan meta keys doing, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                            namespace, slot, System.currentTimeMillis() - startTime, scanKeys.get(), deleteMetaKeys.get());
                    time = TimeCache.currentMillis;
                }
            }
            logger.info("scan meta keys end, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                    namespace, slot, System.currentTimeMillis() - startTime, scanKeys.get(), deleteMetaKeys.get());
        } catch (Throwable e) {
            logger.error("scan meta keys error, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                    namespace, slot, System.currentTimeMillis() - startTime, scanKeys.get(), deleteMetaKeys.get(), e);
        }
    }

    private void scanSubKeys(int slot, AtomicLong scanKeys, AtomicLong deleteSubKeys) {
        long startTime = System.currentTimeMillis();
        logger.info("scan sub keys start, namespace = {}, slot = {}", namespace, slot);
        long time = TimeCache.currentMillis;
        try {
            ConcurrentLinkedHashMap<CacheKey, KeyStatus> cacheMap = new ConcurrentLinkedHashMap.Builder<CacheKey, KeyStatus>()
                    .initialCapacity(10000)
                    .maximumWeightedCapacity(10000)
                    .build();
            List<byte[]> prefixList = new ArrayList<>();
            prefixList.add(keyDesign.getSubKeyPrefix());
            prefixList.add(keyDesign.getSubKeyPrefix2());
            prefixList.add(keyDesign.getSubIndexKeyPrefix());
            for (byte[] prefix : prefixList) {
                //
                byte[] startKey = prefix;
                //
                int limit = kvConfig.scanBatch();
                while (true) {
                    List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
                    if (scan.isEmpty()) {
                        break;
                    }
                    KvGcMonitor.scanSubKeys(namespace, scan.size());
                    List<byte[]> toDeleteKeys = new ArrayList<>();
                    for (KeyValue keyValue : scan) {
                        startKey = keyValue.getKey();
                        scanKeys.incrementAndGet();
                        byte[] key;
                        long keyVersion;
                        try {
                            key = keyDesign.decodeKeyBySubKey(startKey);
                            keyVersion = keyDesign.decodeKeyVersionBySubKey(startKey, key.length);
                        } catch (Exception e) {
                            logger.error("gc decode sub key error, namespace = {}, will delete kv = {}", namespace, keyValue, e);
                            kvClient.delete(slot, keyValue.getKey());
                            TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                            continue;
                        }
                        if (keyVersion > System.currentTimeMillis() - 600 * 1000L) {
                            continue;
                        }
                        KeyStatus keyStatus = checkExpireOrNotExists(slot, cacheMap, key, keyVersion);
                        if (keyStatus == KeyStatus.NOT_EXISTS || keyStatus == KeyStatus.EXPIRE) {
                            toDeleteKeys.add(keyValue.getKey());
                        }
                    }
                    if (!toDeleteKeys.isEmpty()) {
                        kvClient.batchDelete(slot, toDeleteKeys.toArray(new byte[0][0]));
                        deleteSubKeys.addAndGet(toDeleteKeys.size());
                        KvGcMonitor.deleteSubKeys(Utils.bytesToString(keyDesign.getNamespace()), toDeleteKeys.size());
                    }
                    if (scan.size() < limit) {
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                    if (TimeCache.currentMillis - time > 60 * 1000L) {
                        logger.info("scan sub keys doing, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteSubKeys = {}",
                                namespace, slot, System.currentTimeMillis() - startTime, scanKeys.get(), deleteSubKeys.get());
                        time = TimeCache.currentMillis;
                    }
                }
            }
            cacheMap.clear();
            logger.info("scan sub keys end, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteSubKeys = {}",
                    namespace, slot, System.currentTimeMillis() - startTime, scanKeys.get(), deleteSubKeys.get());
        } catch (Throwable e) {
            logger.error("scan sub keys error, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteSubKeys = {}",
                    namespace, slot, System.currentTimeMillis() - startTime, scanKeys.get(), deleteSubKeys.get(), e);
        }
    }

    private KeyStatus checkExpireOrNotExists(int slot, Map<CacheKey, KeyStatus> cacheMap, byte[] key, long keyVersion) {
        CacheKey cacheKey = new CacheKey(key, keyVersion);
        KeyStatus keyStatus = cacheMap.get(cacheKey);
        if (keyStatus != null) {
            return keyStatus;
        }
        byte[] metaKey = keyDesign.metaKey(key);
        KeyValue metaValue = kvClient.get(slot, metaKey);
        if (metaValue == null) {
            keyStatus = KeyStatus.NOT_EXISTS;
        } else {
            KeyMeta keyMeta = KeyMeta.fromBytes(metaValue.getValue());
            if (keyMeta == null) {
                keyStatus = KeyStatus.NOT_EXISTS;
            } else {
                if (keyMeta.isExpire() || keyMeta.getKeyVersion() != keyVersion) {
                    keyStatus = KeyStatus.EXPIRE;
                } else {
                    keyStatus = KeyStatus.NORMAL;
                }
            }
        }
        cacheMap.put(cacheKey, keyStatus);
        return keyStatus;
    }

    private static final ThreadLocal<SimpleDateFormat> hourFormatThreadLocal = ThreadLocal.withInitial(() -> new SimpleDateFormat("HH"));
    private static final ThreadLocal<SimpleDateFormat> minuteFormatThreadLocal = ThreadLocal.withInitial(() -> new SimpleDateFormat("mm"));

    private boolean checkInScheduleGcTime() {
        int startHour = 0;
        int startMinute = 1;

        int endHour = 5;
        int endMinute = 0;

        try {
            String string = ProxyDynamicConf.getString("kv.gc.schedule.time.range", "01:00-05:00");
            String[] strings = string.split("-");
            String[] split1 = strings[0].split(":");
            startHour = Integer.parseInt(split1[0]);
            startMinute = Integer.parseInt(split1[1]);
            String[] split2 = strings[1].split(":");
            endHour = Integer.parseInt(split2[0]);
            endMinute = Integer.parseInt(split2[1]);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        Date now = new Date();

        int hour = Integer.parseInt(hourFormatThreadLocal.get().format(now));
        int minute = Integer.parseInt(minuteFormatThreadLocal.get().format(now));

        if (hour < startHour) {
            return false;
        }
        if (hour == startHour && minute < startMinute) {
            return false;
        }
        if (hour > endHour) {
            return false;
        }
        if (hour == endHour && minute > endMinute) {
            return false;
        }
        return true;
    }

    private record CacheKey(byte[] key, long keyVersion) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return keyVersion == cacheKey.keyVersion && Arrays.equals(key, cacheKey.key);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(keyVersion);
            result = 31 * result + Arrays.hashCode(key);
            return result;
        }
    }

    private static enum KeyStatus {
        NORMAL,
        EXPIRE,
        NOT_EXISTS,
        ;
    }

    private record SubKeyDeleteTask(int slot, byte[] key, String namespace, KeyMeta keyMeta, KVClient kvClient,
                                    KeyDesign keyDesign, KvConfig kvConfig) implements Runnable {

        @Override
        public void run() {
            try {
                int count = 0;
                KeyType keyType = keyMeta.getKeyType();
                if (keyType == KeyType.hash) {
                    count = clearHashSubKeys(slot);
                } else if (keyType == KeyType.zset) {
                    count = clearZSetSubKeys(slot);
                } else if (keyType == KeyType.set) {
                    count = clearSetSubKeys(slot);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("sub key delete, namespace = {}, count = {}", namespace, count);
                }
                KvGcMonitor.deleteSubKeys(Utils.bytesToString(keyDesign.getNamespace()), count);
            } catch (Exception e) {
                logger.warn("add delete task error, namespace = {}, ex = {}", namespace, e.toString());
            }
        }

        private int clearHashSubKeys(int slot) {
            return clearByPrefix(slot, keyDesign.subKeyPrefix(keyMeta, key));
        }

        private int clearSetSubKeys(int slot) {
            return clearByPrefix(slot, keyDesign.subKeyPrefix(keyMeta, key));
        }

        private int clearZSetSubKeys(int slot) {
            int count = 0;
            EncodeVersion encodeVersion = keyMeta.getEncodeVersion();
            if (encodeVersion == EncodeVersion.version_0) {
                count += clearByPrefix(slot, keyDesign.subKeyPrefix(keyMeta, key));
            }
            if (encodeVersion == EncodeVersion.version_0) {
                count += clearByPrefix(slot, keyDesign.subKeyPrefix2(keyMeta, key));
            }
            if (encodeVersion == EncodeVersion.version_1) {
                count += clearByPrefix(slot, keyDesign.subIndexKeyPrefix(keyMeta, key));
            }
            return count;
        }

        private int clearByPrefix(int slot, byte[] prefix) {
            int count = 0;
            byte[] startKey = prefix;
            int limit = kvConfig.scanBatch();
            while (true) {
                List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
                if (scan.isEmpty()) {
                    break;
                }
                byte[][] keys = new byte[scan.size()][];
                int i = 0;
                for (KeyValue keyValue : scan) {
                    keys[i] = keyValue.getKey();
                    startKey = keyValue.getKey();
                    i++;
                }
                kvClient.batchDelete(slot, keys);
                count += keys.length;
                if (scan.size() < limit) {
                    break;
                }
            }
            return count;
        }
    }
}

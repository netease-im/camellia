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
import com.netease.nim.camellia.redis.proxy.util.*;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

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
    private final ThreadPoolExecutor scheduleExecutor;
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
        this.scheduleExecutor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(32), new CamelliaThreadFactory("camellia-kv-gc-scheduler-executor"));

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
                Future<Boolean> metaKeyScanFuture;
                if (!kvClient.supportTTL()) {
                    metaKeyScanFuture = scheduleExecutor.submit(this::scanMetaKeys);
                } else {
                    metaKeyScanFuture = CompletableFuture.completedFuture(true);
                }
                //sub-key scan
                Future<Boolean> subKeyScanFuture = scheduleExecutor.submit(this::scanSubKeys);
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

    private boolean scanMetaKeys() {
        long startTime = System.currentTimeMillis();
        logger.info("scan meta keys start, namespace = {}", namespace);
        long time = TimeCache.currentMillis;
        long deleteMetaKeys = 0;
        int scanKeys = 0;
        try {
            byte[] metaPrefix = keyDesign.getMetaPrefix();
            //
            byte[] startKey;
            int slot;
            Pair<byte[], Integer> pair = KvGcEnv.getMetaKeyScanStartKey(namespace);
            if (pair == null) {
                startKey = metaPrefix;
                slot = 0;
            } else {
                startKey = pair.getFirst();
                slot = pair.getSecond();
            }
            //
            int limit = kvConfig.scanBatch();
            while (true) {
                if (slot >= RedisClusterCRC16Utils.SLOT_SIZE) {
                    KvGcEnv.updateMetaKeyScanStartKey(namespace, null, -1);
                    break;
                }
                List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, metaPrefix, limit, Sort.ASC, false);
                if (scan.isEmpty()) {
                    slot ++;
                    startKey = metaPrefix;
                    continue;
                }
                KvGcMonitor.scanMetaKeys(namespace, scan.size());
                for (KeyValue keyValue : scan) {
                    startKey = keyValue.getKey();
                    scanKeys ++;
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
                            deleteMetaKeys ++;
                            KvGcMonitor.deleteMetaKeys(Utils.bytesToString(keyDesign.getNamespace()), 1);
                        } else {
                            //如果kv不支持cas的删除，则hash到特定的proxy节点处理，避免并发问题
                            try {
                                Reply reply = redisTemplate.sendCleanExpiredKeyMetaInKv(keyDesign.getNamespace(), key, kvConfig.gcCleanExpiredKeyMetaTimeoutMillis());
                                if (reply instanceof ErrorReply) {
                                    logger.error("send clean expired key meta in kv failed, namespace = {}, reply = {}", namespace, ((ErrorReply) reply).getError());
                                } else {
                                    deleteMetaKeys++;
                                    KvGcMonitor.deleteMetaKeys(Utils.bytesToString(keyDesign.getNamespace()), 1);
                                }
                            } catch (Exception e) {
                                logger.error("send clean expired key meta in kv error", e);
                            }
                        }
                    }
                }
                if (scan.size() < limit) {
                    slot ++;
                    startKey = metaPrefix;
                    continue;
                }
                if (!checkInScheduleGcTime()) {
                    KvGcEnv.updateMetaKeyScanStartKey(namespace, startKey, slot);
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                if (TimeCache.currentMillis - time > 60*1000L) {
                    logger.info("scan meta keys doing, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                            namespace, slot, System.currentTimeMillis() - startTime, scanKeys, deleteMetaKeys);
                    time = TimeCache.currentMillis;
                }
            }
            if (checkInScheduleGcTime()) {
                logger.info("scan meta keys end, namespace = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                        namespace, System.currentTimeMillis() - startTime, scanKeys, deleteMetaKeys);
            } else {
                logger.info("scan meta keys end for not in schedule gc time, namespace = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                        namespace, System.currentTimeMillis() - startTime, scanKeys, deleteMetaKeys);
            }
            return true;
        } catch (Throwable e) {
            logger.error("scan meta keys error, namespace = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                    namespace, System.currentTimeMillis() - startTime, scanKeys, deleteMetaKeys, e);
            return false;
        }
    }

    private boolean scanSubKeys() {
        long startTime = System.currentTimeMillis();
        logger.info("scan sub keys start, namespace = {}", namespace);
        long time = TimeCache.currentMillis;
        long deleteSubKeys = 0;
        int scanKeys = 0;
        try {
            ConcurrentLinkedHashMap<CacheKey, KeyStatus> cacheMap = new ConcurrentLinkedHashMap.Builder<CacheKey, KeyStatus>()
                    .initialCapacity(10000)
                    .maximumWeightedCapacity(10000)
                    .build();
            List<byte[]> prefixList = new ArrayList<>();
            prefixList.add(keyDesign.getSubKeyPrefix());
            prefixList.add(keyDesign.getSubKeyPrefix2());
            prefixList.add(keyDesign.getSubIndexKeyPrefix());
            for (int i=0; i<prefixList.size(); i++) {
                //
                byte[] prefix = prefixList.get(i);
                //
                Pair<byte[], Integer> pair = KvGcEnv.getSubKeyScanStartKey(namespace, i);
                byte[] startKey;
                int slot;
                if (pair == null) {
                    startKey = prefixList.get(i);
                    slot = 0;
                } else {
                    startKey = pair.getFirst();
                    slot = pair.getSecond();
                }
                //
                int limit = kvConfig.scanBatch();
                while (true) {
                    if (slot >= RedisClusterCRC16Utils.SLOT_SIZE) {
                        KvGcEnv.setSubKeyScanStartKey(namespace, i, null, -1);
                        break;
                    }
                    List<KeyValue> scan = kvClient.scanByPrefix(slot, startKey, prefix, limit, Sort.ASC, false);
                    if (scan.isEmpty()) {
                        slot ++;
                        startKey = prefixList.get(i);
                        continue;
                    }
                    KvGcMonitor.scanSubKeys(namespace, scan.size());
                    List<byte[]> toDeleteKeys = new ArrayList<>();
                    for (KeyValue keyValue : scan) {
                        startKey = keyValue.getKey();
                        scanKeys ++;
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
                        if (keyVersion > System.currentTimeMillis() - 600*1000L) {
                            continue;
                        }
                        KeyStatus keyStatus = checkExpireOrNotExists(slot, cacheMap, key, keyVersion);
                        if (keyStatus == KeyStatus.NOT_EXISTS || keyStatus == KeyStatus.EXPIRE) {
                            toDeleteKeys.add(keyValue.getKey());
                        }
                    }
                    if (!toDeleteKeys.isEmpty()) {
                        kvClient.batchDelete(slot, toDeleteKeys.toArray(new byte[0][0]));
                        deleteSubKeys += toDeleteKeys.size();
                        KvGcMonitor.deleteSubKeys(Utils.bytesToString(keyDesign.getNamespace()), toDeleteKeys.size());
                    }
                    if (scan.size() < limit) {
                        slot ++;
                        startKey = prefixList.get(i);
                        continue;
                    }
                    if (!checkInScheduleGcTime()) {
                        KvGcEnv.setSubKeyScanStartKey(namespace, i, startKey, slot);
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
                    if (TimeCache.currentMillis - time > 60*1000L) {
                        logger.info("scan sub keys doing, namespace = {}, slot = {}, spendMs = {}, scanKeys = {}, deleteMetaKeys = {}",
                                namespace, slot, System.currentTimeMillis() - startTime, scanKeys, deleteSubKeys);
                        time = TimeCache.currentMillis;
                    }
                }
            }
            cacheMap.clear();
            if (checkInScheduleGcTime()) {
                logger.info("scan sub keys end, namespace = {}, spendMs = {}, scanKeys = {}, deleteSubKeys = {}",
                        namespace, System.currentTimeMillis() - startTime, scanKeys, deleteSubKeys);
            } else {
                logger.info("scan sub keys end for not in schedule gc time, namespace = {}, spendMs = {}, scanKeys = {}, deleteSubKeys = {}",
                        namespace, System.currentTimeMillis() - startTime, scanKeys, deleteSubKeys);
            }
            return true;
        } catch (Throwable e) {
            logger.error("scan sub keys error, namespace = {}, spendMs = {}, scanKeys = {}, deleteSubKeys = {}",
                    namespace, System.currentTimeMillis() - startTime, scanKeys, deleteSubKeys, e);
            return false;
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

    private static class CacheKey {
        private final byte[] key;
        private final long keyVersion;

        public CacheKey(byte[] key, long keyVersion) {
            this.key = key;
            this.keyVersion = keyVersion;
        }

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

    private static class SubKeyDeleteTask implements Runnable {
        private final int slot;
        private final byte[] key;
        private final String namespace;
        private final KeyMeta keyMeta;
        private final KVClient kvClient;
        private final KeyDesign keyDesign;
        private final KvConfig kvConfig;

        public SubKeyDeleteTask(int slot, byte[] key, String namespace, KeyMeta keyMeta, KVClient kvClient, KeyDesign keyDesign, KvConfig kvConfig) {
            this.slot = slot;
            this.key = key;
            this.namespace = namespace;
            this.keyMeta = keyMeta;
            this.kvClient = kvClient;
            this.keyDesign = keyDesign;
            this.kvConfig = kvConfig;
        }

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

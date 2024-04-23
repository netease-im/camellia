package com.netease.nim.camellia.redis.proxy.upstream.kv.gc;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KeyStruct;
import com.netease.nim.camellia.redis.proxy.upstream.kv.domain.KvConfig;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyMeta;
import com.netease.nim.camellia.redis.proxy.upstream.kv.meta.KeyType;
import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/4/22
 */
public class KvGcExecutor {
    private static final Logger logger = LoggerFactory.getLogger(KvGcExecutor.class);

    private final KVClient kvClient;
    private final KeyStruct keyStruct;
    private final KvConfig kvConfig;
    private final CamelliaHashedExecutor deleteExecutor;
    private final ThreadPoolExecutor submitExecutor;
    private final ScheduledThreadPoolExecutor scheduleExecutor;

    public KvGcExecutor(KVClient kvClient, KeyStruct keyStruct, KvConfig kvConfig) {
        this.kvClient = kvClient;
        this.keyStruct = keyStruct;
        this.kvConfig = kvConfig;
        this.deleteExecutor = new CamelliaHashedExecutor("camellia-kv-gc", kvConfig.gcExecutorPoolSize(),
                kvConfig.gcExecutorQueueSize(), new CamelliaHashedExecutor.CallerRunsPolicy());
        this.submitExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000), new CamelliaThreadFactory("camellia-kv-gc-submit"), new ThreadPoolExecutor.AbortPolicy());
        this.scheduleExecutor = new ScheduledThreadPoolExecutor(1, new CamelliaThreadFactory("camellia-kv-gc-scheduler"));
    }

    public void start() {
        scheduleExecutor.scheduleAtFixedRate(() -> {
            scanMetaKeys();
            scanSubKeys();
        }, 1, 1, TimeUnit.DAYS);
    }

    public void submitSubKeyDeleteTask(byte[] key, KeyMeta keyMeta) {
        try {
            submitExecutor.submit(() -> {
                try {
                    deleteExecutor.submit(key, new SubKeyDeleteTask(key, keyMeta, kvClient, keyStruct, kvConfig));
                } catch (Exception e) {
                    logger.warn("execute delete task error, ex = {}", e.toString());
                }
            });
        } catch (Exception e) {
            logger.error("submit key delete task error");
        }
    }

    private void scanMetaKeys() {
        long startTime = System.currentTimeMillis();
        logger.info("scan meta keys start");
        try {
            byte[] metaPrefix = keyStruct.getMetaPrefix();
            byte[] startKey = metaPrefix;
            int limit = kvConfig.scanBatch();
            while (true) {
                List<KeyValue> scan = kvClient.scan(startKey, metaPrefix, limit, Sort.ASC, false);
                if (scan.isEmpty()) {
                    break;
                }
                for (KeyValue keyValue : scan) {
                    startKey = keyValue.getKey();
                    KeyMeta keyMeta = KeyMeta.fromBytes(keyValue.getValue());
                    if (keyMeta == null) {
                        continue;
                    }
                    if (keyMeta.isExpire()) {
                        byte[] key = keyStruct.decodeKeyByMetaKey(startKey);
                        if (key != null && keyMeta.getKeyType() != KeyType.string) {
                            deleteExecutor.submit(key, new SubKeyDeleteTask(key, keyMeta, kvClient, keyStruct, kvConfig));
                        }
                        kvClient.checkAndDelete(startKey, keyValue.getValue());
                    }
                }
                if (scan.size() < limit) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
            }
            logger.info("scan meta keys end, spendMs = {}", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("scan meta keys error, spendMs = {}", System.currentTimeMillis() - startTime, e);
        }
    }

    private void scanSubKeys() {
        long startTime = System.currentTimeMillis();
        logger.info("scan sub keys start");
        try {
            ConcurrentLinkedHashMap<CacheKey, KeyStatus> cacheMap = new ConcurrentLinkedHashMap.Builder<CacheKey, KeyStatus>()
                    .initialCapacity(10000)
                    .maximumWeightedCapacity(10000)
                    .build();
            byte[] storePrefix = keyStruct.getSubKeyPrefix();
            byte[] startKey = storePrefix;
            int limit = kvConfig.scanBatch();
            while (true) {
                List<KeyValue> scan = kvClient.scan(startKey, storePrefix, limit, Sort.ASC, false);
                if (scan.isEmpty()) {
                    break;
                }
                List<byte[]> toDeleteKeys = new ArrayList<>();
                for (KeyValue keyValue : scan) {
                    startKey = keyValue.getKey();
                    byte[] key = keyStruct.decodeKeyBySubKey(startKey);
                    long keyVersion = keyStruct.decodeKeyVersionBySubKey(startKey, key.length);
                    KeyStatus keyStatus = checkExpireOrNotExists(cacheMap, key, keyVersion);
                    if (keyStatus == KeyStatus.NOT_EXISTS || keyStatus == KeyStatus.EXPIRE) {
                        toDeleteKeys.add(keyValue.getKey());
                    }
                }
                if (!toDeleteKeys.isEmpty()) {
                    kvClient.batchDelete(toDeleteKeys.toArray(new byte[0][0]));
                }
                if (scan.size() < limit) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(kvConfig.gcBatchSleepMs());
            }
            cacheMap.clear();
            logger.info("scan sub keys end, spendMs = {}", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            logger.error("scan sub keys error, spendMs = {}", System.currentTimeMillis() - startTime);
        }
    }

    private KeyStatus checkExpireOrNotExists(Map<CacheKey, KeyStatus> cacheMap, byte[] key, long keyVersion) {
        CacheKey cacheKey = new CacheKey(key, keyVersion);
        KeyStatus keyStatus = cacheMap.get(cacheKey);
        if (keyStatus != null) {
            return keyStatus;
        }
        byte[] metaKey = keyStruct.metaKey(key);
        KeyValue metaValue = kvClient.get(metaKey);
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
        private final byte[] key;
        private final KeyMeta keyMeta;
        private final KVClient kvClient;
        private final KeyStruct keyStruct;
        private final KvConfig kvConfig;

        public SubKeyDeleteTask(byte[] key, KeyMeta keyMeta, KVClient kvClient, KeyStruct keyStruct, KvConfig kvConfig) {
            this.key = key;
            this.keyMeta = keyMeta;
            this.kvClient = kvClient;
            this.keyStruct = keyStruct;
            this.kvConfig = kvConfig;
        }

        @Override
        public void run() {
            try {
                KeyType keyType = keyMeta.getKeyType();
                if (keyType == KeyType.hash) {
                    byte[] startKey = keyStruct.hashFieldSubKey(keyMeta, key, new byte[0]);
                    byte[] prefix = startKey;
                    int limit = kvConfig.scanBatch();
                    while (true) {
                        List<KeyValue> scan = kvClient.scan(startKey, prefix, limit, Sort.ASC, false);
                        if (scan.isEmpty()) {
                            break;
                        }
                        for (KeyValue keyValue : scan) {
                            kvClient.delete(keyValue.getKey());
                            startKey = keyValue.getKey();
                        }
                        if (scan.size() < limit) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("add delete task error, ex = {}", e.toString());
            }
        }

    }
}

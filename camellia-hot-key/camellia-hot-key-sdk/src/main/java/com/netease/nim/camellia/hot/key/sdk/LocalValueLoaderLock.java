package com.netease.nim.camellia.hot.key.sdk;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import java.util.Objects;
import java.util.UUID;

/**
 * Created by caojiajun on 2023/5/8
 */
public class LocalValueLoaderLock implements IValueLoaderLock {

    private static final ConcurrentLinkedHashMap<String, LockInfo> lockMap = new ConcurrentLinkedHashMap.Builder<String, LockInfo>()
            .initialCapacity(10000)
            .maximumWeightedCapacity(10000)
            .build();

    private final String prefix;
    private final long expireMillis;
    private LockInfo lockInfo;

    private LocalValueLoaderLock(String prefix, long expireMillis) {
        this.prefix = prefix;
        this.expireMillis = expireMillis;
    }

    /**
     * 初始化一个锁对象
     * @param prefix 前缀
     * @param expireMillis 过期时间
     * @return 锁对象
     */
    public static LocalValueLoaderLock newLock(String prefix, long expireMillis) {
        return new LocalValueLoaderLock(prefix, expireMillis);
    }

    @Override
    public boolean tryLock(String key) {
        String lockKey = buildLockKey(key);
        LockInfo lockInfo = lockMap.get(lockKey);
        if (lockInfo == null) {
            this.lockInfo = new LockInfo(expireMillis);
            LockInfo old = lockMap.putIfAbsent(lockKey, new LockInfo(expireMillis));
            return old == null;
        }
        if (lockInfo.isExpire()) {
            lockMap.remove(lockKey, lockInfo);
        } else {
            return false;
        }
        this.lockInfo = new LockInfo(expireMillis);
        LockInfo old = lockMap.putIfAbsent(lockKey, new LockInfo(expireMillis));
        return old == null;
    }

    @Override
    public boolean release(String key) {
        String lockKey = buildLockKey(key);
        if (lockInfo == null) {
            return false;
        }
        return lockMap.remove(lockKey, this.lockInfo);
    }

    private String buildLockKey(String key) {
        return prefix + "|" + key + "~lock";
    }

    private static class LockInfo {
        private final String lockId;
        private final long expireTimestamp;

        public LockInfo(long expireMillis) {
            this.lockId = UUID.randomUUID().toString();
            this.expireTimestamp = System.currentTimeMillis() + expireMillis;
        }

        public String getLockId() {
            return lockId;
        }

        public long getExpireTimestamp() {
            return expireTimestamp;
        }

        public boolean isExpire() {
            return System.currentTimeMillis() > expireTimestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LockInfo lockInfo = (LockInfo) o;
            return expireTimestamp == lockInfo.expireTimestamp && Objects.equals(lockId, lockInfo.lockId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lockId, expireTimestamp);
        }
    }
}

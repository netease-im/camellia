package com.netease.nim.camellia.redis.toolkit.lock;


import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 一个会自动续约redis锁的管理器，避免锁在使用中途过期被自动释放掉
 * 适用于锁持有时间不能精确估计的业务场景
 * Created by caojiajun on 2022/2/15
 */
public class CamelliaRedisLockManager {

    private static final int defaultPoolSize = Runtime.getRuntime().availableProcessors() * 4;
    private static final long defaultAcquireTimeoutMillis = 5000;
    private static final long defaultExpireTimeoutMillis = 5000;

    private final ScheduledExecutorService scheduledExec;

    private final CamelliaRedisTemplate template;

    private final long acquireTimeoutMillis;
    private final long expireTimeoutMillis;

    private final ConcurrentHashMap<LockKey, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LockKey, CamelliaRedisLock> lockMap = new ConcurrentHashMap<>();

    private static final CamelliaRedisLockManager INSTANCE = new CamelliaRedisLockManager();

    public CamelliaRedisLockManager() {
        this(null, defaultPoolSize, defaultAcquireTimeoutMillis, defaultExpireTimeoutMillis);
    }

    public CamelliaRedisLockManager(CamelliaRedisTemplate template) {
        this(template, defaultPoolSize, defaultAcquireTimeoutMillis, defaultExpireTimeoutMillis);
    }

    public CamelliaRedisLockManager(int poolSize) {
        this(null, poolSize, defaultAcquireTimeoutMillis, defaultExpireTimeoutMillis);
    }

    public CamelliaRedisLockManager(CamelliaRedisTemplate template, int poolSize) {
        this(template, poolSize, defaultAcquireTimeoutMillis, defaultExpireTimeoutMillis);
    }

    public CamelliaRedisLockManager(CamelliaRedisTemplate template, int poolSize, long acquireTimeoutMillis, long expireTimeoutMillis) {
        this.template = template;
        this.scheduledExec = new ScheduledThreadPoolExecutor(poolSize, new CamelliaThreadFactory("camellia-redis-lock-manager"));
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        this.expireTimeoutMillis = expireTimeoutMillis;
    }

    public static CamelliaRedisLockManager getInstance() {
        return INSTANCE;
    }

    public boolean lock(CamelliaRedisTemplate template, String lockKey) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(CamelliaRedisTemplate template, byte[] lockKey) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(CamelliaRedisTemplate template, String lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(CamelliaRedisTemplate template, byte[] lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(String lockKey) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(byte[] lockKey) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(String lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lock(byte[] lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(CamelliaRedisTemplate template, String lockKey) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(CamelliaRedisTemplate template, byte[] lockKey) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(CamelliaRedisTemplate template, String lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(CamelliaRedisTemplate template, byte[] lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(String lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(byte[] lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(String lockKey) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLock(byte[] lockKey) {
        return tryLock(template, new LockKey(lockKey), acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Runnable runnable) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(CamelliaRedisTemplate template, String lockKey, Runnable runnable) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(CamelliaRedisTemplate template, String lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(byte[] lockKey, Runnable runnable) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(String lockKey, Runnable runnable) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(byte[] lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean tryLockAndRun(String lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return tryLockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Runnable runnable) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(CamelliaRedisTemplate template, String lockKey, Runnable runnable) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(CamelliaRedisTemplate template, String lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(byte[] lockKey, Runnable runnable) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(String lockKey, Runnable runnable) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(byte[] lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean lockAndRun(String lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return lockAndRun(template, new LockKey(lockKey), runnable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Callable<T> callable) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(CamelliaRedisTemplate template, String lockKey, Callable<T> callable) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(CamelliaRedisTemplate template, String lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(byte[] lockKey, Callable<T> callable) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(String lockKey, Callable<T> callable) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(byte[] lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> lockAndRun(String lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return lockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Callable<T> callable) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(CamelliaRedisTemplate template, String lockKey, Callable<T> callable) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(CamelliaRedisTemplate template, byte[] lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(CamelliaRedisTemplate template, String lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(byte[] lockKey, Callable<T> callable) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(String lockKey, Callable<T> callable) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(byte[] lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public <T> LockTaskResult<T> tryLockAndRun(String lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        return tryLockAndRun(template, new LockKey(lockKey), callable, acquireTimeoutMillis, expireTimeoutMillis);
    }

    public boolean release(String lockKey) {
        return release(new LockKey(lockKey));
    }

    public boolean release(byte[] lockKey) {
        return release(new LockKey(lockKey));
    }

    public boolean clear(String lockKey) {
        return clear(new LockKey(lockKey));
    }

    public boolean clear(byte[] lockKey) {
        return clear(new LockKey(lockKey));
    }

    public CamelliaRedisLock getLock(String lockKey) {
        return getLock(new LockKey(lockKey));
    }

    public CamelliaRedisLock getLock(byte[] lockKey) {
        return getLock(new LockKey(lockKey));
    }

    public void clearAll() {
        for (Map.Entry<LockKey, CamelliaRedisLock> entry : lockMap.entrySet()) {
            entry.getValue().release();
        }
        for (Map.Entry<LockKey, ScheduledFuture<?>> entry : futureMap.entrySet()) {
            entry.getValue().cancel(false);
        }
        lockMap.clear();
        futureMap.clear();
    }

    private boolean lock(CamelliaRedisTemplate template, LockKey lockKey,
                        long acquireTimeoutMillis, long expireTimeoutMillis) {
        if (template == null) {
            throw new IllegalArgumentException("camellia redis template is null");
        }
        //尝试获取一个锁，并且会定时续约该锁，以避免锁在中途被释放
        final CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey.getKey(), acquireTimeoutMillis, expireTimeoutMillis);
        boolean lockOk = lock.lock();
        if (lockOk) {
            lockMap.put(lockKey, lock);
            ScheduledFuture<?> future = scheduledExec.scheduleAtFixedRate(lock::renew, expireTimeoutMillis / 5, expireTimeoutMillis / 5, TimeUnit.MILLISECONDS);
            futureMap.put(lockKey, future);
        }
        return lockOk;
    }

    private boolean tryLock(CamelliaRedisTemplate template, LockKey lockKey,
                         long acquireTimeoutMillis, long expireTimeoutMillis) {
        if (template == null) {
            throw new IllegalArgumentException("camellia redis template is null");
        }
        //尝试获取一个锁，并且会定时续约该锁，以避免锁在中途被释放
        final CamelliaRedisLock lock = CamelliaRedisLock.newLock(template, lockKey.getKey(), acquireTimeoutMillis, expireTimeoutMillis);
        boolean lockOk = lock.tryLock();
        if (lockOk) {
            lockMap.put(lockKey, lock);
            ScheduledFuture<?> future = scheduledExec.scheduleAtFixedRate(lock::renew, expireTimeoutMillis / 5, expireTimeoutMillis / 5, TimeUnit.MILLISECONDS);
            futureMap.put(lockKey, future);
        }
        return lockOk;
    }

    private CamelliaRedisLock getLock(LockKey lockKey) {
        return lockMap.get(lockKey);
    }

    private boolean release(LockKey lockKey) {
        CamelliaRedisLock lock = lockMap.remove(lockKey);
        boolean result = false;
        if (lock != null) {
            result = lock.release();
        }
        ScheduledFuture<?> future = futureMap.remove(lockKey);
        if (future != null) {
            future.cancel(false);
        }
        return result;
    }

    private boolean clear(LockKey lockKey) {
        CamelliaRedisLock lock = lockMap.remove(lockKey);
        boolean result = false;
        if (lock != null) {
            result = lock.clear();
        }
        ScheduledFuture<?> future = futureMap.remove(lockKey);
        if (future != null) {
            future.cancel(false);
        }
        return result;
    }

    private  <T> LockTaskResult<T> tryLockAndRun(CamelliaRedisTemplate template, LockKey lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        boolean lock = tryLock(template, lockKey, acquireTimeoutMillis, expireTimeoutMillis);
        if (!lock) return new LockTaskResult<>(false, null);
        try {
            T result = callable.call();
            return new LockTaskResult<>(true, result);
        } finally {
            release(lockKey);
        }
    }

    private boolean tryLockAndRun(CamelliaRedisTemplate template, LockKey lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        boolean lock = tryLock(template, lockKey, acquireTimeoutMillis, expireTimeoutMillis);
        if (!lock) return false;
        try {
            runnable.run();
            return true;
        } finally {
            release(lockKey);
        }
    }

    private  <T> LockTaskResult<T> lockAndRun(CamelliaRedisTemplate template, LockKey lockKey, Callable<T> callable, long acquireTimeoutMillis, long expireTimeoutMillis) throws Exception {
        boolean lock = lock(template, lockKey, acquireTimeoutMillis, expireTimeoutMillis);
        if (!lock) return new LockTaskResult<>(false, null);
        try {
            T result = callable.call();
            return new LockTaskResult<>(true, result);
        } finally {
            release(lockKey);
        }
    }

    private boolean lockAndRun(CamelliaRedisTemplate template, LockKey lockKey, Runnable runnable, long acquireTimeoutMillis, long expireTimeoutMillis) {
        boolean lock = lock(template, lockKey, acquireTimeoutMillis, expireTimeoutMillis);
        if (!lock) return false;
        try {
            runnable.run();
            return true;
        } finally {
            release(lockKey);
        }
    }

    private static class LockKey {
        private byte[] key;
        private int hashCode;

        public LockKey(byte[] key) {
            this.key = key;
        }

        public LockKey(String key) {
            this.key = key.getBytes(StandardCharsets.UTF_8);
        }

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            LockKey lockKey = (LockKey) object;

            return Arrays.equals(key, lockKey.key);
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = Arrays.hashCode(key);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            if (key == null) return null;
            return new String(key, StandardCharsets.UTF_8);
        }
    }
}

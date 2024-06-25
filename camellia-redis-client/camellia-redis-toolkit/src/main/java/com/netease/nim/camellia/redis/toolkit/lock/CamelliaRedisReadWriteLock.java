package com.netease.nim.camellia.redis.toolkit.lock;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/5/22
 */
public class CamelliaRedisReadWriteLock {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisReadWriteLock.class);
    private static final int UN_LOCK = 0;
    private static final int READ_LOCK = 1;
    private static final int WRITE_LOCK = 2;

    private final CamelliaRedisTemplate template;
    private final byte[] lockKey;
    private final byte[] lockId;
    private final byte[] readKey;
    private final byte[] writeStatusKey;
    private final byte[] writePendingKey;
    private final long acquireTimeoutMillis;//获取锁的等待时间
    private final long expireTimeoutMillis;//锁的过期时间
    private final long tryLockIntervalMillis;//两次尝试获取锁时的间隔
    private long expireTimestamp = -1;//锁的过期时间戳
    private int status = UN_LOCK;

    private final ReentrantLock lock = new ReentrantLock();

    private CamelliaRedisReadWriteLock(CamelliaRedisTemplate template, byte[] lockKey, byte[] lockId,
                                      long acquireTimeoutMillis, long expireTimeoutMillis, long tryLockIntervalMillis) {
        this.template = template;
        this.lockKey = lockKey;
        this.lockId = lockId;
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        this.expireTimeoutMillis = expireTimeoutMillis;
        this.tryLockIntervalMillis = tryLockIntervalMillis;

        byte[] prefix = merge("{".getBytes(StandardCharsets.UTF_8), lockKey);
        prefix = merge(prefix, "}".getBytes(StandardCharsets.UTF_8));
        this.readKey = merge(prefix, ":read".getBytes(StandardCharsets.UTF_8));
        this.writeStatusKey = merge(prefix, ":write".getBytes(StandardCharsets.UTF_8));
        this.writePendingKey = merge(prefix, ":write_pending".getBytes(StandardCharsets.UTF_8));
    }

    private byte [] merge(final byte [] a, final byte [] b) {
        byte [] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * 获取一个锁对象
     * @param template redis客户端
     * @param lockKey 锁key
     * @param acquireTimeoutMillis 获取锁的超时时间
     * @param expireTimeoutMillis 锁的过期时间
     * @return 锁对象
     */
    public static CamelliaRedisReadWriteLock newLock(CamelliaRedisTemplate template, String lockKey, long acquireTimeoutMillis, long expireTimeoutMillis) {
        byte[] lockId = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        return new CamelliaRedisReadWriteLock(template, lockKey.getBytes(StandardCharsets.UTF_8), lockId, acquireTimeoutMillis, expireTimeoutMillis, 5);
    }

    /**
     * 读锁是否获取到了
     * @return 成功/失败
     */
    public boolean isReadLockOk() {
        lock.lock();
        try {
            if (status == READ_LOCK) {
                if (System.currentTimeMillis() < this.expireTimestamp) {
                    return true;
                }
                clearExpiredKeys();
                boolean lockOk = template.hget(readKey, lockId) != null;
                if (!lockOk) {
                    status = UN_LOCK;
                    expireTimestamp = -1;
                }
                return lockOk;
            }
            return false;
        } catch (Exception e) {
            logger.error("isReadLockOk error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 写锁是否获取到了
     * @return 成功/失败
     */
    public boolean isWriteLockOk() {
        lock.lock();
        try {
            if (status == WRITE_LOCK) {
                if (System.currentTimeMillis() < this.expireTimestamp) {
                    return true;
                }
                clearExpiredKeys();
                byte[] bytes = template.get(writeStatusKey);
                boolean lockOk = bytes != null && Arrays.equals(bytes, lockId);
                if (!lockOk) {
                    status = UN_LOCK;
                    expireTimestamp = -1;
                }
                return lockOk;
            }
            return false;
        } catch (Exception e) {
            logger.error("isWriteLockOk error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private final byte[] readLockScript = ("local arg = redis.call('exists', KEYS[2], KEYS[3]);\n" +
            "if tonumber(arg) >= 1 then\n" +
            "    return 1;\n" +
            "end\n" +
            "redis.call('hset', KEYS[1], ARGV[1], ARGV[2]);\n" +
            "redis.call('pexpireat', KEYS[1], ARGV[2]);\n" +
            "return 2;").getBytes(StandardCharsets.UTF_8);

    /**
     * 获取读锁
     * @return 成功/失败
     */
    public boolean readLock() {
        long start = System.currentTimeMillis();
        if (isReadLockOk()) {
            return true;
        }
        if (isWriteLockOk()) {
            return true;
        }
        lock.lock();
        try {
            while (true) {
                clearExpiredKeys();
                long lockExpireTimestamp = System.currentTimeMillis() + expireTimeoutMillis;
                Object eval = template.eval(readLockScript, Arrays.asList(readKey, writePendingKey, writeStatusKey),
                        Arrays.asList(lockId, String.valueOf(lockExpireTimestamp).getBytes(StandardCharsets.UTF_8)));
                boolean success = String.valueOf(eval).equals("2");
                if (success) {
                    status = READ_LOCK;
                    expireTimestamp = lockExpireTimestamp;
                    return true;
                }
                if (System.currentTimeMillis() - start > acquireTimeoutMillis) {
                    return false;
                }
                TimeUnit.MILLISECONDS.sleep(tryLockIntervalMillis);
            }
        } catch (Exception e) {
            logger.error("READ lock error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private final byte[] writeLockScript = ("local arg = redis.call('exists', KEYS[1], KEYS[3]);\n" +
            "if tonumber(arg) == 0 then\n" +
            "    redis.call('psetex', KEYS[3], ARGV[2], ARGV[1]);\n" +
            "    redis.call('hdel', KEYS[2], ARGV[1]);\n" +
            "    return 2;\n" +
            "end\n" +
            "redis.call('hset', KEYS[2], ARGV[1], ARGV[3]);\n" +
            "redis.call('pexpireat', KEYS[2], ARGV[3]);\n" +
            "return 1;").getBytes(StandardCharsets.UTF_8);

    /**
     * 获取写锁
     * @return 成功/失败
     */
    public boolean writeLock() {
        long start = System.currentTimeMillis();
        if (isWriteLockOk()) {
            return true;
        }
        if (isReadLockOk()) {
            release();
        }
        lock.lock();
        try {
            while (true) {
                clearExpiredKeys();
                long lockExpireTimestamp = System.currentTimeMillis() + expireTimeoutMillis;
                Object eval = template.eval(writeLockScript, 3, readKey, writePendingKey, writeStatusKey,
                        lockId, String.valueOf(expireTimeoutMillis).getBytes(StandardCharsets.UTF_8), String.valueOf(lockExpireTimestamp).getBytes(StandardCharsets.UTF_8));
                boolean success = String.valueOf(eval).equals("2");
                if (success) {
                    status = WRITE_LOCK;
                    expireTimestamp = lockExpireTimestamp;
                    return true;
                }
                if (System.currentTimeMillis() - start > acquireTimeoutMillis) {
                    return false;
                }
                TimeUnit.MILLISECONDS.sleep(tryLockIntervalMillis);
            }
        } catch (Exception e) {
            logger.error("WRITE lock error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private static final byte[] writeLockReleaseScript = ("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end").getBytes(StandardCharsets.UTF_8);

    /**
     * 释放锁
     * @return 成功/失败
     */
    public boolean release() {
        lock.lock();
        try {
            clearExpiredKeys();
            if (status == READ_LOCK) {
                Long hdel = template.hdel(readKey, lockId);
                status = UN_LOCK;
                return hdel > 0;
            } else if (status == WRITE_LOCK) {
                Object eval = template.eval(writeLockReleaseScript, 1, writeStatusKey, lockId);
                if (eval != null && String.valueOf(eval).equals("1")) {
                    status = UN_LOCK;
                    return true;
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            if (status == READ_LOCK) {
                logger.error("release READ lock error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            } else if (status == WRITE_LOCK) {
                logger.error("release WRITE lock error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清空锁相关的key，会释放不是自己获取到的锁
     * @return 成功/失败
     */
    public boolean clear() {
        lock.lock();
        try {
            template.del(readKey, writePendingKey, writeStatusKey);
            return true;
        } catch (Exception e) {
            logger.error("clear lock error, lockKey = {}", new String(lockKey, StandardCharsets.UTF_8), e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private static final byte[] clearExpiredKeysScript = ("local arg = redis.call('hget', KEYS[1], ARGV[1]);\n" +
            "if tonumber(arg) < tonumber(ARGV[2]) then\n" +
            "    local ret = redis.call('hdel', KEYS[1], ARGV[1]);\n" +
            "    return 1;\n" +
            "end\n" +
            "return 2;").getBytes(StandardCharsets.UTF_8);

    private void clearExpiredKeys() {
        clearExpiredHashFields(readKey);
        clearExpiredHashFields(writePendingKey);
    }

    private void clearExpiredHashFields(byte[] key) {
        Map<byte[], byte[]> map = template.hgetAll(key);
        if (!map.isEmpty()) {
            List<byte[]> expiredFields = new ArrayList<>();
            for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
                String string = new String(entry.getValue(), StandardCharsets.UTF_8);
                if (Long.parseLong(string) < System.currentTimeMillis()) {
                    expiredFields.add(entry.getKey());
                }
            }
            if (!expiredFields.isEmpty()) {
                for (byte[] field : expiredFields) {
                    template.eval(clearExpiredKeysScript, 1, key, field, String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

}

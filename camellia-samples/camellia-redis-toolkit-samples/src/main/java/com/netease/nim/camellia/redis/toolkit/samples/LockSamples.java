package com.netease.nim.camellia.redis.toolkit.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;

/**
 *
 * Created by caojiajun on 2020/4/14.
 */
public class LockSamples {

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://abc@127.0.0.1:6379");

        String lockKey = "lockKey123";//锁key
        long acquireTimeoutMillis = 3000;//获取锁的超时时间
        long expireTimeoutMillis = 3000;//锁的过期时间
        CamelliaRedisLock redisLock = CamelliaRedisLock.newLock(template, lockKey, acquireTimeoutMillis, expireTimeoutMillis);

        //1. 获取锁，若获取失败，则会阻塞直到acquireTimeoutMillis
        boolean lock = redisLock.lock();
        if (lock) {
            try {
                System.out.println("do some thing");
            } finally {
                redisLock.release();
            }
        }

        //2.获取锁，若获取成功则执行Runnable，若获取失败，则会阻塞直到acquireTimeoutMillis
        boolean ok = redisLock.lockAndRun(() -> System.out.println("do some thing"));

        //3.尝试获取锁，若获取失败，立即返回
        boolean tryLock = redisLock.tryLock();
        if (tryLock) {
            try {
                System.out.println("do some thing");
            } finally {
                redisLock.release();
            }
        }

        //4.延长锁过期时间
        boolean renew = redisLock.renew();

        //5.清空锁相关的key，会释放不是自己获取到的锁
        redisLock.clear();
    }
}

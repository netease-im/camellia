package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisReadWriteLock;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Created by caojiajun on 2024/5/22
 */
public class ReadWriteLockSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");

        System.out.println("start");

        //case1
        {
            String string = UUID.randomUUID().toString();
            CamelliaRedisReadWriteLock lock1 = CamelliaRedisReadWriteLock.newLock(template, string, 100, 3000);
            CamelliaRedisReadWriteLock lock2 = CamelliaRedisReadWriteLock.newLock(template, string, 100, 3000);
            CamelliaRedisReadWriteLock lock3 = CamelliaRedisReadWriteLock.newLock(template, string, 100, 3000);
            CamelliaRedisReadWriteLock lock4 = CamelliaRedisReadWriteLock.newLock(template, string, 100, 3000);
            System.out.println("case1");
            assertEquals(lock1.readLock(), true);
            assertEquals(lock1.isReadLockOk(), true);
            assertEquals(lock1.isWriteLockOk(), false);
            assertEquals(lock2.readLock(), true);
            assertEquals(lock3.writeLock(), false);
            assertEquals(lock3.isWriteLockOk(), false);
            assertEquals(lock1.release(), true);
            assertEquals(lock3.writeLock(), false);
            assertEquals(lock2.release(), true);
            assertEquals(lock3.writeLock(), true);
            assertEquals(lock3.isWriteLockOk(), true);
            assertEquals(lock4.readLock(), false);
        }

        CountDownLatch latch = new CountDownLatch(4);
        {
            String string = UUID.randomUUID().toString();
            CamelliaRedisReadWriteLock lock1 = CamelliaRedisReadWriteLock.newLock(template, string, 100, 1000);
            CamelliaRedisReadWriteLock lock2 = CamelliaRedisReadWriteLock.newLock(template, string, 2000, 300000);
            CamelliaRedisReadWriteLock lock3 = CamelliaRedisReadWriteLock.newLock(template, string, 2000, 300000);
            CamelliaRedisReadWriteLock lock4 = CamelliaRedisReadWriteLock.newLock(template, string, 2000, 5000);
            CamelliaRedisReadWriteLock lock5 = CamelliaRedisReadWriteLock.newLock(template, string, 6000, 300000);
            CamelliaRedisReadWriteLock lock6 = CamelliaRedisReadWriteLock.newLock(template, string, 100, 300000);

            System.out.println("case2");

            assertEquals(lock1.readLock(), true);
            CountDownLatch latch1 = new CountDownLatch(1);
            new Thread(() -> {
                try {
                    latch1.await();
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                assertEquals(lock2.readLock(), false);
                latch.countDown();
            }).start();
            new Thread(() -> {
                try {
                    latch1.await();
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                assertEquals(lock3.readLock(), false);
                latch.countDown();
            }).start();
            new Thread(() -> {
                try {
                    latch1.await();
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                assertEquals(lock4.writeLock(), true);
                latch.countDown();
            }).start();
            new Thread(() -> {
                try {
                    latch1.await();
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                assertEquals(lock5.writeLock(), true);
                //
                assertEquals(lock5.release(), true);
                assertEquals(lock6.writeLock(), true);
                assertEquals(lock2.readLock(), false);
                assertEquals(lock3.readLock(), false);
                assertEquals(lock6.release(), true);
                assertEquals(lock2.readLock(), true);
                assertEquals(lock3.readLock(), true);
                latch.countDown();
            }).start();
            latch1.countDown();
        }

        latch.await();
        System.out.println("end");
        System.exit(-1);
    }

    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS");
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}

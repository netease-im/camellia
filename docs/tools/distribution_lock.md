

# CamelliaRedisLock/CamelliaRedisLockManager
## 简介
* 分布式锁
* 基于CamelliaRedisTemplate实现
* 基于lua实现安全加锁和释放锁
* CamelliaRedisLockManager支持锁自动续约（适用于无法准确预知任务执行时长的case）

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-toolkit</artifactId>
    <version>1.2.0</version>
</dependency>
```

## 示例
```java
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
        System.out.println("lockAndRun = " + ok);

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
        System.out.println("renew = " + renew);

        //5.清空锁相关的key，会释放不是自己获取到的锁
        redisLock.clear();

        //////对于可能需要执行很久的任务，如果要自动续约，可以使用CamelliaRedisLockManager
        int poolSize = Runtime.getRuntime().availableProcessors();
        CamelliaRedisLockManager manager = new CamelliaRedisLockManager(template, poolSize, acquireTimeoutMillis, expireTimeoutMillis);
        boolean lockOk = manager.lock(lockKey);
        if (lockOk) {
            try {
                System.out.println("do some thing long time start");
                try {
                    TimeUnit.MILLISECONDS.sleep(expireTimeoutMillis * 3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("do some thing long time end");
            } finally {
                manager.release(lockKey);
            }
        }
    }
}


```
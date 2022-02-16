
# camellia-redis-toolkit
## 简介  
基于camellia-redis实现的几个工具  

## 示例  
### 分布式锁（详情见CamelliaRedisLock）  
```
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
                    TimeUnit.SECONDS.sleep(expireTimeoutMillis * 3);
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
### 计数器（详情见CamelliaCounterCache）  
适用于count计算比较耗时，且对计数要求不是非常准确，但是比较关心是否超过阈值的情况（希望尽可能不要超限，但真的偶尔超限了也能接受）  
```
public class CounterCacheSamples {

    private static final ConcurrentHashMap<Long, AtomicLong> db = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://abc@127.0.0.1:6379");

        CamelliaCounterCache<Long> counterCache = new CamelliaCounterCache.Builder<Long>()
                .redisTemplate(template)
                .adjustCacheIntervalSeconds(() -> 30)//缓存多久更新一次
                .exceedThresholdAdjustCacheIntervalSeconds(() -> 5)//阈值超限的情况下，缓存多久更新一次
                .tagToCacheKey(tag -> "counter_" + tag)//tag转成缓存key
                .threshold(() -> 100L)//计数器阈值
                .expireSeconds(() -> 3600)//计数器缓存时长
                .counterGetter(tag -> {//获取精确值的回调方法
                    AtomicLong count = db.computeIfAbsent(tag, k -> new AtomicLong(0));
                    System.out.println("hit to db, tag = " + tag);
                    return count.get();
                }).build();

        Long tag = 100L;
        System.out.println(counterCache.getCount(tag));

        incrDb(tag);
        counterCache.incr(tag);

        System.out.println(counterCache.getCount(tag));
    }

    private static void incrDb(Long tag) {
        AtomicLong count = db.computeIfAbsent(tag, k -> new AtomicLong(0));
        count.incrementAndGet();
    }
}
```

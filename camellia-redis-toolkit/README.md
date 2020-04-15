
# camellia-redis-toolkit
## 简介  
基于camellia-redis实现的几个工具  

## maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-toolkit</artifactId>
    <version>a.b.c</version>
</dependency>
```

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

```
### ID生成（详情见CamelliaIDGenerator）  
构造严格递增序列的ID生成器  
使用redis的队列实现  
可以实现动态调整的步长  
```
public class IDGeneratorSamples {

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://abc@127.0.0.1:6379");

        CamelliaIDGenerator<Long> idGenerator = new CamelliaIDGenerator.Builder<Long>()
                .idLoader(new DbIDLoader())//从db里获取一段id
                .redisTemplate(template)
                .tagToCacheKey(tag -> "id_" + tag)//tag转成缓存key
                .defaultStep(() -> 5)//默认步长（每次从db获取一段id的数量的默认值，也是最小步长）
                .maxStep(() -> 1000)//最大步长（从db获取一段id的数量的最大值）
                .cacheHoldSeconds(() -> 600)//如果从db获取的一段id在这个时间段内就触发了loadFromDb，则调整步长（小于则增大步长，大于则减少步长）
                .expireSeconds(() -> 7*24*3600)//id在redis里缓存的时间
                .build();

        Long tag = 1L;
        for (int i=0; i<20; i++) {
            System.out.println(idGenerator.generate(tag));
        }
    }

    private static class DbIDLoader implements IDLoader<Long> {

        private static ConcurrentHashMap<Long, AtomicLong> db = new ConcurrentHashMap<>();

        @Override
        public IDRange load(Long tag, int step) {
            AtomicLong count = db.computeIfAbsent(tag, k -> new AtomicLong(0));
            long start = count.getAndAdd(step);
            IDRange idRange = new IDRange(start + 1, start + 1 + step);
            System.out.println("hit to db, return [" + idRange.getStart() + "," + idRange.getEnd() + "]");
            return idRange;
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

### 更多示例和源码
[示例源码](/camellia-samples/camellia-redis-toolkit-samples)

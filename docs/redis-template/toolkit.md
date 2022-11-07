
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

### 频控（详情见CamelliaFreq）
* 支持单机频控，也支持集群频控，还支持混合
* 频控参数详见CamelliaFreqConfig

```java
public enum CamelliaFreqType {
    //单机模式
    STANDALONE,
    //集群模式，走redis
    CLUSTER,
    //混合，先过单机，再过集群，主要是用于输入qps非常高，但是频控后的目标qps又很低的场景
    //假设输入10w的QPS，目标是20的QPS
    // 如果是普通的集群模式，则10w的QPS都会打到redis
    // 如果用混合模式，且一共有10个节点在处理，则穿透到redis最多是20*10=200QPS，最终通过的也只有20QPS，可以极大的降低redis的压力
    MISC,
    ;
}
```

```java
/**
 * 示例一：
 * checkTime=1000，threshold=10，banTime=2000，delayBanEnable=true
 * 表示1s内最多10次请求，如果超过了，则2s内不允许请求，如果还有请求，2s会一直顺延，直到连续2s内没有新的请求进来，频控才会取消
 * 
 * 示例二：
 * checkTime=1000，threshold=10，banTime=2000，delayBanEnable=false
 * 表示1s内最多10次请求，如果超过了，则2s内不允许请求，2s之后直接频控自动取消
 *
 * 示例三：
 * checkTime=1000，threshold=10，banTime=0
 * 表示1s内最多10次请求，如果超过了，则返回失败，等当前这个周期（1s）过去了，则频控自动取消
 * Created by caojiajun on 2022/8/1
 */
public class CamelliaFreqConfig {

    private long checkTime;//检查周期，单位ms
    private long threshold;//阈值，一个周期内的最大请求数
    private long banTime;//超过阈值后的惩罚屏蔽时间
    private boolean delayBanEnable;//超过阈值后进入屏蔽时间，此时如果有新请求过来，是否要顺延屏蔽时间
}
```

```java
public class FreqSamples {

    private static void test1() throws InterruptedException {
        CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        String freqKey = "k1";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        for (int i = 0; i < 20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }
}
```

### 任务合并框架（详情见CamelliaMergeTask和CamelliaMergeTaskExecutor）
* 有相同请求参数的查询请求，高并发或者高tps查询，对于数据一致性要求不是那么高
* 此时为了避免每次请求都落到底层（DB或者复杂的cache计算），CamelliaMergeTask会控制相同查询请求的并发，穿透过去一个请求，并把结果分发给等待队列中的其他请求
* 此外，还可以对结果进行短暂的缓存，从而提高请求merge的效果
* 支持单机合并，也支持集群合并（需要redis）

```java
public class MergeTaskSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaMergeTaskExecutor executor = new CamelliaMergeTaskExecutor(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        CamelliaStatisticsManager statisticsManager = new CamelliaStatisticsManager();
        int c = 10000;
        CountDownLatch latch = new CountDownLatch(c);
        for (int i=0; i<c; i++) {
            long start = System.currentTimeMillis();
            CamelliaMergeTaskFuture<String> future = executor.submit(new BusinessMergeTask(ThreadLocalRandom.current().nextInt(5)));
            future.thenAccept(result -> {
                statisticsManager.update(result.getType().name(), (System.currentTimeMillis() - start));
                System.out.println("type=" + result.getType() + ",result=" + result.getResult() + ",spend=" + (System.currentTimeMillis() - start));
                latch.countDown();
            });
        }
        latch.await();
        Map<String, CamelliaStatsData> map = statisticsManager.getStatsDataAndReset();
        for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
            System.out.println("key=" + entry.getKey() + ",stats=" + JSONObject.toJSONString(entry.getValue()));
        }
    }

    private static class BusinessMergeTask implements CamelliaMergeTask<BusinessMergeTaskRequest, String> {

        private final int num;

        public BusinessMergeTask(int num) {
            this.num = num;
        }

        @Override
        public CamelliaMergeTaskType getType() {
            return CamelliaMergeTaskType.CLUSTER;
        }

        @Override
        public long resultCacheMillis() {
            return 1000;
        }

        @Override
        public CamelliaMergeTaskResultSerializer<String> getResultSerializer() {
            return CamelliaMergeTaskResultStringSerializer.INSTANCE;
        }

        @Override
        public BusinessMergeTaskRequest getKey() {
            return new BusinessMergeTaskRequest(num);
        }

        @Override
        public String getTag() {
            return "test";
        }

        @Override
        public String execute(BusinessMergeTaskRequest key) throws Exception {
            return businessMethod(key.getNum());
        }
    }

    private static class BusinessMergeTaskRequest implements CamelliaMergeTaskKey {

        private final int num;

        public BusinessMergeTaskRequest(int num) {
            this.num = num;
        }

        public int getNum() {
            return num;
        }

        @Override
        public String serialize() {
            return String.valueOf(num);
        }
    }

    private static String businessMethod(int num) throws InterruptedException {
        int c = 0;
        for (int i=0; i<num; i++) {
            c += i;
            TimeUnit.MILLISECONDS.sleep(i * 100L);
        }
        return String.valueOf(c);
    }
}
```
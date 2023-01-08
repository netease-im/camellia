
## 介绍
提供了一些工具类，包括：
* 压缩工具类CamelliaCompressor，支持LZ4压缩算法，压缩时支持判断阈值，解压时会检查是否压缩过（向下兼容）
* 加解密工具类CamelliaEncryptor，支持AES相关算法，解密时会检查是否加密过（向下兼容）
* 本地缓存工具类CamelliaLoadingCache，提供区别于Caffeine和Guava的load策略，适用于特定场景
* 线程池工具类CamelliaHashedExecutor，提供哈希策略，相同hashKey确保顺序执行  
* 熔断器工具类CamelliaCircuitBreaker，支持动态配置
* 线程池工具类CamelliaDynamicExecutor，可以动态修改线程池的参数
* 线程池工具类CamelliaDynamicIsolationExecutor，支持根据isolationKey统计任务执行时间，并根据执行快慢进行动态隔离（使用不同的底层线程池执行），从而避免慢任务影响快任务的执行

## maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>a.b.c</version>
</dependency>
```

## 示例
### CamelliaCompressor示例
CamelliaCompressor可以设置压缩阈值，此外解压缩之前会判断是否压缩过，从而在某些业务场景下可以做到向下兼容  
```java
public class CompressSamples {

    public static void main(String[] args) {
        CamelliaCompressor compressor = new CamelliaCompressor();

        StringBuilder data = new StringBuilder();
        for (int i=0; i<2048; i++) {
            data.append("abc");
        }
        //原始数据
        System.out.println(data.length());
        //压缩
        byte[] compress = compressor.compress(data.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(Base64.getEncoder().encodeToString(compress));
        //解压
        byte[] decompress = compressor.decompress(compress);
        System.out.println(new String(decompress).length());
        //判断是否一致
        System.out.println(new String(decompress).equals(data.toString()));

        //直接解压原始数据，CamelliaCompressor会发现没有做过压缩，因此会直接返回
        byte[] decompress1 = compressor.decompress(data.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(decompress1).length());
        System.out.println(new String(decompress1).equals(data.toString()));
    }
}

```

### CamelliaEncryptor示例  
CamelliaEncryptor是线程安全的，支持AES加密，可以选择不同的模式（默认是AES/CBC/PKCS5Padding）；此外解密之前会判断是否加密过，从而在某些业务场景下可以做到向下兼容  
```java
public class EncryptSamples {

    public static void main(String[] args) {

        for (CamelliaEncryptAesConfig.Type type : CamelliaEncryptAesConfig.Type.values()) {
            System.out.println(">>>>>START>>>>" + type.getDesc());
            CamelliaEncryptor encryptor = new CamelliaEncryptor(new CamelliaEncryptAesConfig(type, "111"));//设置秘钥seed
            String data = "Hello Camellia";

            //原始数据
            System.out.println(data);
            //加密
            byte[] encrypt = encryptor.encrypt(data.getBytes(StandardCharsets.UTF_8));
            System.out.println(Base64.getEncoder().encodeToString(encrypt));
            //解密
            byte[] decrypt = encryptor.decrypt(encrypt);
            System.out.println(new String(decrypt));
            //判断解密后是否和原始数据一致
            System.out.println(new String(decrypt).equals(data));

            //直接对原始数据解密，CamelliaEncryptor会发现没有加密过，直接返回
            byte[] decrypt1 = encryptor.decrypt(data.getBytes(StandardCharsets.UTF_8));
            System.out.println(new String(decrypt1));
            System.out.println(new String(decrypt1).equals(data));

            System.out.println(">>>>>END>>>>" + type.getDesc());
        }
    }
}

```

### CamelliaLoadingCache示例
CamelliaLoadingCache类似于Caffeine和Guava，底层基于ConcurrentLinkedHashMap，是一个LRU的缓存  
CamelliaLoadingCache会在后台线程定时更新那些刚刚访问过的热key，从而尽量避免在用户调用线程进行缓存更新操作        
特别的，当缓存穿透时，如果更新缓存操作出现异常，会返回上一次获取到的缓存旧值  

对比Caffeine的refreshAfterWrite策略：  
当一个key很久没有访问过了，此时调用get方法，会触发一个缓存更新操作，但是当前的请求可能返回一个过期的数据，相反CamelliaLoadingCache会确保返回最新的  
对比Caffeine的expireAfterWrite策略：  
当一个key很久没有访问过了，此时调用get方法，会触发一个缓存更新操作，但是如果缓存更新失败了会返回一个异常，相反CamelliaLoadingCache会返回一个旧值  

CamelliaLoadingCache适合于一些特定的使用场景，请按需使用  
```java
public class LoadingCacheSamples {
    public static void main(String[] args) throws Exception {
        int count = 10000*10000;
        boolean testError = false;
        boolean printValue = false;
        long sleepMs = 0;
        while (true) {
            testCamellia(count, testError, printValue, sleepMs);
            System.out.println();
            testCaffeine(count, testError, printValue, sleepMs);
            TimeUnit.SECONDS.sleep(1);
            System.out.println();
        }
    }

    private static void testCamellia(int count, boolean testError, boolean printValue, long sleepMs) throws Exception {
        AtomicLong id = new AtomicLong();
        CamelliaLoadingCache<String, String> cache = new CamelliaLoadingCache.Builder<String, String>()
                .initialCapacity(100)
                .maxCapacity(100)
                .expireMillis(1000)
                .cacheNull(true)//是否缓存null
                .build(key -> {
                    TimeUnit.MILLISECONDS.sleep(10);
                    String value = key + id.incrementAndGet();
                    System.out.println("camellia load, key = " + key + ",value=" + value + ",thread=" + Thread.currentThread().getName());
                    if (testError && id.get() == 2) {
                        throw new IllegalArgumentException();
                    }
                    return value;
                });
        long start = System.currentTimeMillis();
        int i=count;
        while (i-->0) {
            String k1 = cache.get("key");
            if (printValue) {
                System.out.println("camellia, value=" + k1);
            }
            if (sleepMs > 0) {
                TimeUnit.MILLISECONDS.sleep(sleepMs);
            }
        }
        System.out.println("[STATS]CamelliaLoadingCache, spendMs=" + (System.currentTimeMillis() - start));
    }

    private static void testCaffeine(int count, boolean testError, boolean printValue, long sleepMs) throws Exception {
        AtomicLong id = new AtomicLong();
        LoadingCache<String, String> cache = Caffeine.newBuilder()
                .refreshAfterWrite(1, TimeUnit.SECONDS)
//                .expireAfterWrite(1, TimeUnit.SECONDS)
                .maximumSize(100)
                .build(key -> {
                    TimeUnit.MILLISECONDS.sleep(10);
                    String value = key + id.incrementAndGet();
                    System.out.println("caffeine load, key = " + key + ",value=" + value + ",thread=" + Thread.currentThread().getName());
                    if (testError && id.get() == 2) {
                        throw new IllegalArgumentException();
                    }
                    return value;
                });
        long start = System.currentTimeMillis();
        int i=count;
        while (i-->0) {
            String k1 = cache.get("key");
            if (printValue) {
                System.out.println("caffeine, value=" + k1);
            }
            if (sleepMs > 0) {
                TimeUnit.MILLISECONDS.sleep(sleepMs);
            }
        }
        System.out.println("[STATS]CaffeineLoadingCache, spendMs=" + (System.currentTimeMillis() - start));
    }
}

```

### CamelliaHashedExecutor示例
在某些使用线程池的场景下，我们希望其中部分任务是顺序执行的，而常规的线程池是按照提交顺序执行的，如果线程池线程数超过1个，则可能导致乱序或者并发   
CamelliaHashedExecutor在提交任务时额外提供了hashKey这样的参数，当hashKey参数是一样的时候，会确保所有任务都是相同线程执行的   

```java
public class CamelliaHashedExecutorSamples {
    public static void main(String[] args) {
        String name = "sample";
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        int queueSize = 100000;
        CamelliaHashedExecutor.RejectedExecutionHandler rejectedExecutionHandler = new CamelliaHashedExecutor.CallerRunsPolicy();
        CamelliaHashedExecutor executor = new CamelliaHashedExecutor(name, poolSize, queueSize, rejectedExecutionHandler);

        //相同hashKey的两个任务确保是单线程顺序执行的
        
        executor.submit("key1", () -> {
            System.out.println("key1 start1, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key1 end1, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key2", () -> {
            System.out.println("key2 start1, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key2 end1, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key1", () -> {
            System.out.println("key1 start2, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key1 end2, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key2", () -> {
            System.out.println("key2 start2, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key2 end2, thread=" + Thread.currentThread().getName());
        });
    }
}
```

### CamelliaCircuitBreaker示例
一个熔断器的实现，支持动态配置（如动态打开/关闭，强制打开，失败比例阈值、半开间隔等）


```java
public class CircuitBreakerSamples {
    public static void main(String[] args) {
        CircuitBreakerConfig config = new CircuitBreakerConfig();
        //以下参数不可以动态配置
//        config.setName("camellia-circuit-breaker");
//        config.setStatisticSlidingWindowTime(10*1000L);//统计成功失败的滑动窗口的大小，单位ms，默认10s
//        config.setStatisticSlidingWindowBucketSize(10);//滑动窗口分割为多少个bucket，默认10个
        //以下参数可以动态配置，通过lambda表达式来实现动态配置
//        config.setEnable(() -> true);//熔断器开关，默认true，若配置false，则所有请求都通过
//        config.setForceOpen(() -> false);//强制打开开关，默认false，若配置true，则所有请求都不通过
//        config.setFailThresholdPercentage(() -> 0.5);////滑动窗口范围内失败比例超过多少触发熔断，默认50%
//        config.setSingleTestIntervalMillis(() -> 5000L);//当熔断器打开的情况下，间隔多久尝试一次探测（也就是半开）
//        config.setRequestVolumeThreshold(() -> 20L);//滑动窗口内至少多少个请求才会触发熔断，默认20个
        CamelliaCircuitBreaker circuitBreaker = new CamelliaCircuitBreaker(config);

        //核心接口就三个，allowRequest、incrementFail、incrementSuccess
        AtomicLong success = new AtomicLong();
        AtomicLong fail = new AtomicLong();
        long start = System.currentTimeMillis();
        new Thread(() -> {
            while (true) {
                if (System.currentTimeMillis() - start <= 10000*2) {
                    //请求之前询问熔断器是否可以执行
                    boolean allowRequest = circuitBreaker.allowRequest();
                    if (!allowRequest) {
                        System.out.println("quick fail of fail");
                        fail.incrementAndGet();
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    long ret = fail.incrementAndGet();
                    if (ret % 100 == 1) {
                        System.out.println("fail=" + ret);
                    }
                    //如果请求失败了，要告诉熔断器
                    circuitBreaker.incrementFail();
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    break;
                }
            }
            System.out.println("end fail");
        }).start();

        new Thread(() -> {
            while (true) {
                //请求之前询问熔断器是否可以执行
                boolean allowRequest = circuitBreaker.allowRequest();
                if (!allowRequest) {
                    System.out.println("quick fail of success");
                    fail.incrementAndGet();
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                long ret = success.incrementAndGet();
                if (ret % 100 == 1) {
                    System.out.println("success=" + ret);
                }
                //如果请求成功了，要告诉熔断器
                circuitBreaker.incrementSuccess();
                try {
                    TimeUnit.MILLISECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
```

## CamelliaDynamicIsolationExecutor
一个可以根据isolationKey自动选择不同线程池的执行器

### 设计目标：
* 在一个多租户的系统中，每个租户的表现可能是不一样的，有的租户执行任务快，有的执行任务慢，因此就会导致执行慢的租户影响到执行快的租户
* 因为系统资源是有限的，因此我们无法通过给每个租户设置一个线程池的方式来做完全的隔离
* 因此催生了本线程池工具
* CamelliaDynamicIsolationExecutor的基本原理是通过任务执行的统计数据和线程池的工作状态，动态分配线程资源
* 目标是执行快的租户不受执行慢的租户的影响，尽可能保证任务执行延迟保持在一个较短的水平

### 一个典型场景：
* 每个租户绑定一个http的请求地址，不同租户的http地址响应时间不一样，有的快，有的慢，不同租户的请求量也不一样
* 我们期望http响应慢的租户不要影响http响应快的租户

### 内部分为六个线程池：
* 1）fastExecutor，执行耗时较短的任务
* 2）fastBackUpExecutor，fastExecutor的backup
* 3）slowExecutor，执行耗时较长的任务
* 4）slowBackupExecutor，slowExecutor的backup
* 5）whiteListExecutor，白名单isolationKey在这里执行，不关心统计数据
* 6）isolationExecutor，隔离线程池，如果上述五个线程池都执行不了，则最终使用isolationExecutor，如果还是执行不了，则走fallback放弃执行任务

### 规则：
* 1）默认走fastExecutor
* 2）[选择阶段] 如果统计为快（默认阈值1000ms），则使用fastExecutor；如果统计为慢（默认阈值1000ms），则使用slowExecutor
* 3）[选择阶段] 如果fastExecutor/slowExecutor任务执行延迟超过阈值（默认300ms），且fastBackUpExecutor/slowBackupExecutor的延迟小于fastExecutor/slowExecutor，则使用fastBackUpExecutor/slowBackupExecutor
* 4）[提交阶段] 如果因为fastExecutor/slowExecutor繁忙而提交失败，则进入fastBackUpExecutor/slowBackupExecutor，如果仍然繁忙，则转交给isolationExecutor
* 5）[执行阶段] 如果某个isolationKey的最新统计数据和当前线程池不匹配，则转交给匹配的线程池
* 6）[执行阶段] 如果某个线程池执行任务延迟超过阈值（默认300ms），且其他线程池有空闲的（有空闲的线程），则转交给其他线程池（fast会找fastBackup+isolation，fastBackup会找fast+isolation，slow会找slowBackup+isolation，slowBackup会找slow+isolation）
* 7）[执行阶段] 如果某个isolationKey在fastExecutor/slowExecutor中占有线程数比例超过阈值（默认0.3），则转交给fastBackUpExecutor/slowBackupExecutor执行
* 8）[执行阶段] 如果某个isolationKey在fastBackUpExecutor/slowBackupExecutor占有线程数比例也超过阈值（默认0.5），则转交给isolationExecutor执行
* 9）[选择阶段] 在白名单列表里的isolationKey，直接在whiteListExecutor中执行；[提交阶段] 如果whiteListExecutor繁忙，则转交给isolationExecutor
* 10）最终所有任务都会把isolationExecutor作为兜底，如果isolationExecutor因为繁忙处理不了任务，则走fallback回调告诉任务提交者任务被放弃执行了
* 11）可以设置任务过期时间（默认不过期），任务如果过期而被放弃也会走fallback
* 12）fallback方法务必不要有阻塞，fallback方法会告知任务不执行的原因（当前定义了2个原因：任务已过期、任务被拒绝）

### 示例
* 有8个执行较快的租户（执行耗时分别为：50ms、50ms、100ms、100ms、200ms、300ms、400ms、500ms）
* 有2个执行较慢的租户（执行耗时分别为：2000ms、3000ms）
* 一共执行100s
```java
public class CamelliaDynamicIsolationExecutorSamples {

    private static final AtomicBoolean stop = new AtomicBoolean(false);
    private static final long statTime = System.currentTimeMillis();
    private static final long maxRunTime = 100*1000L;

    public static void main(String[] args) throws InterruptedException {
        CamelliaStatisticsManager manager = new CamelliaStatisticsManager();
        int thread = 10;
        CamelliaDynamicIsolationExecutorConfig config = new CamelliaDynamicIsolationExecutorConfig("test", () -> thread);
        config.setIsolationThresholdPercentage(() -> 0.3);
        CamelliaDynamicIsolationExecutor executor1 = new CamelliaDynamicIsolationExecutor(config);

        ThreadPoolExecutor executor2 = new ThreadPoolExecutor(thread * 5, thread * 5, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

        //test CamelliaDynamicIsolationExecutor
        test(manager, executor1, null);

        //test CamelliaDynamicIsolationExecutor
        //test(manager, null, executor2);

        Thread.sleep(1000);
        System.exit(-1);
    }

    private static void test(CamelliaStatisticsManager manager, CamelliaDynamicIsolationExecutor executor1, ThreadPoolExecutor executor2) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);
        Set<String> isolationKeys = new HashSet<>();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast1", 10000, 50, 50)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast2", 10000, 50, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast3", 10000, 100, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast4", 10000, 100, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast5", 10000, 200, 200)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast6", 10000, 300, 300)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast7", 10000, 400, 400)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "fast8", 10000, 500, 500)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "slow1", 1000,2000, 100)).start();
        new Thread(() -> doTask(manager, executor1, executor2, isolationKeys, latch, "slow2", 800,3000, 100)).start();

        latch.await();

        System.out.println("======end,spend=" + (System.currentTimeMillis() - statTime) + "======");
        System.out.println("======total======");
        Map<String, CamelliaStatsData> statsDataAndReset = manager.getStatsDataAndReset();
        for (String isolationKey : isolationKeys) {
            CamelliaStatsData camelliaStatsData = statsDataAndReset.get(isolationKey);
            System.out.println(isolationKey + ",stats=" + JSONObject.toJSON(camelliaStatsData));
            statsDataAndReset.remove(isolationKey);
        }
        System.out.println("======detail======");
        for (String isolationKey : isolationKeys) {
            for (Map.Entry<String, CamelliaStatsData> entry : statsDataAndReset.entrySet()) {
                if (!entry.getKey().startsWith(isolationKey)) continue;
                System.out.println(entry.getKey() + ",stats=" + JSONObject.toJSON(entry.getValue()));
            }
        }
        System.out.println("======type======");
        for (CamelliaDynamicIsolationExecutor.Type type : CamelliaDynamicIsolationExecutor.Type.values()) {
            for (Map.Entry<String, CamelliaStatsData> entry : statsDataAndReset.entrySet()) {
                if (!entry.getKey().equals(type.name())) continue;
                System.out.println(entry.getKey() + ",stats=" + JSONObject.toJSON(entry.getValue()));
            }
        }
    }

    private static void doTask(CamelliaStatisticsManager manager, CamelliaDynamicIsolationExecutor executor1, ThreadPoolExecutor executor2, Set<String> isolationKeys,
                               CountDownLatch latch, String isolationKey, int taskCount, long taskSpendMs, int taskIntervalMs) {
        isolationKeys.add(isolationKey);
        CountDownLatch latch1 = new CountDownLatch(taskCount);
        boolean isBreak = false;
        for (int i=0; i<taskCount; i++) {
            if (isStop()) {
                isBreak = true;
                break;
            }
            final long id = i;
            final long start = System.currentTimeMillis();
            if (executor1 != null) {
                executor1.submit(isolationKey, () -> doTask(id, start, isolationKey, manager, taskSpendMs, latch1));
                sleep(taskIntervalMs);
                continue;
            }
            if (executor2 != null) {
                executor2.submit(() -> doTask(id, start, isolationKey, manager, taskSpendMs, latch1));
                sleep(taskIntervalMs);
            }
        }
        try {
            if (!isBreak) {
                latch1.await();
            }
            latch.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void doTask(long id, long start, String isolationKey, CamelliaStatisticsManager manager, long taskSpendMs, CountDownLatch latch1) {
        if (isStop()) {
            latch1.countDown();
            return;
        }
        long latency = System.currentTimeMillis() - start;
        CamelliaDynamicIsolationExecutor.Type type = CamelliaDynamicIsolationExecutor.getCurrentExecutorType();
        System.out.println("key=" + isolationKey + ", start, latency = " + latency + ", id = " + id
                + ", thread=" + Thread.currentThread().getName() + ",type=" + type + ",time=" + (System.currentTimeMillis() - statTime));
        manager.update(isolationKey + "|" + type, latency);
        manager.update(isolationKey, latency);
        manager.update(String.valueOf(type), latency);
        sleep(taskSpendMs);
        latch1.countDown();
    }

    private static boolean isStop() {
        if (stop.get()) return true;
        if (System.currentTimeMillis() - statTime > maxRunTime) {
            stop.set(true);
        }
        return stop.get();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

执行结果（CamelliaDynamicIsolationExecutor）：
```
======end,spend=101849======
======total======
fast7,stats={"p99":520,"avg":34.318548387096776,"max":588,"p90":17,"count":248,"p50":0,"p999":633,"sum":8511,"p95":350,"p75":0}
fast6,stats={"p99":510,"avg":31.48181818181818,"max":583,"p90":28,"count":330,"p50":0,"p999":520,"sum":10389,"p95":287,"p75":0}
fast5,stats={"p99":520,"avg":33.78093306288032,"max":613,"p90":21,"count":493,"p50":0,"p999":650,"sum":16654,"p95":300,"p75":0}
fast4,stats={"p99":518,"avg":36.5041067761807,"max":617,"p90":100,"count":974,"p50":0,"p999":675,"sum":35555,"p95":370,"p75":0}
slow2,stats={"p99":69442,"avg":19216.124338624337,"max":70826,"p90":57678,"count":378,"p50":5200,"p999":70480,"sum":7263695,"p95":64252,"p75":37957}
fast8,stats={"p99":520,"avg":34.96482412060301,"max":583,"p90":13,"count":199,"p50":0,"p999":625,"sum":6958,"p95":370,"p75":0}
slow1,stats={"p99":69355,"avg":13639.958592132505,"max":71212,"p90":53019,"count":483,"p50":1,"p999":70840,"sum":6588100,"p95":61930,"p75":19000}
fast3,stats={"p99":516,"avg":36.239465570400824,"max":656,"p90":77,"count":973,"p50":0,"p999":700,"sum":35261,"p95":362,"p75":0}
fast2,stats={"p99":520,"avg":39.5374358974359,"max":616,"p90":152,"count":975,"p50":0,"p999":683,"sum":38549,"p95":412,"p75":0}
fast1,stats={"p99":519,"avg":34.40853979968371,"max":657,"p90":39,"count":1897,"p50":0,"p999":675,"sum":65273,"p95":350,"p75":0}
======detail======
fast7|ISOLATION,stats={"p99":0,"avg":320.0,"max":320,"p90":0,"count":1,"p50":0,"p999":0,"sum":320,"p95":0,"p75":0}
fast7|FAST_BACKUP,stats={"p99":24,"avg":50.0,"max":370,"p90":24,"count":9,"p50":6,"p999":24,"sum":450,"p95":24,"p75":16}
fast7|FAST,stats={"p99":520,"avg":32.52521008403362,"max":588,"p90":1,"count":238,"p50":0,"p999":633,"sum":7741,"p95":350,"p75":0}
fast6|FAST_BACKUP,stats={"p99":160,"avg":51.6,"max":450,"p90":84,"count":15,"p50":0,"p999":160,"sum":774,"p95":160,"p75":28}
fast6|FAST,stats={"p99":510,"avg":30.523809523809526,"max":583,"p90":1,"count":315,"p50":0,"p999":520,"sum":9615,"p95":287,"p75":0}
fast5|FAST,stats={"p99":518,"avg":30.625531914893617,"max":613,"p90":1,"count":470,"p50":0,"p999":650,"sum":14394,"p95":290,"p75":0}
fast5|FAST_BACKUP,stats={"p99":520,"avg":88.27272727272727,"max":572,"p90":180,"count":22,"p50":1,"p999":520,"sum":1942,"p95":510,"p75":22}
fast5|ISOLATION,stats={"p99":0,"avg":318.0,"max":318,"p90":0,"count":1,"p50":0,"p999":0,"sum":318,"p95":0,"p75":0}
fast4|FAST_BACKUP,stats={"p99":520,"avg":106.23076923076923,"max":603,"p90":400,"count":39,"p50":47,"p999":520,"sum":4143,"p95":460,"p75":115}
fast4|FAST,stats={"p99":517,"avg":32.961414790996784,"max":617,"p90":1,"count":933,"p50":0,"p999":650,"sum":30753,"p95":320,"p75":0}
fast4|ISOLATION,stats={"p99":320,"avg":329.5,"max":353,"p90":320,"count":2,"p50":320,"p999":320,"sum":659,"p95":320,"p75":320}
slow2|ISOLATION,stats={"p99":69788,"avg":35016.85024154589,"max":70826,"p90":63560,"count":207,"p50":34843,"p999":70480,"sum":7248488,"p95":67020,"p75":52834}
slow2|SLOW_BACKUP,stats={"p99":1900,"avg":91.70666666666666,"max":2173,"p90":1,"count":75,"p50":0,"p999":1900,"sum":6878,"p95":625,"p75":0}
slow2|SLOW,stats={"p99":370,"avg":19.80821917808219,"max":1076,"p90":1,"count":73,"p50":0,"p999":370,"sum":1446,"p95":1,"p75":0}
slow2|FAST_BACKUP,stats={"p99":480,"avg":196.22222222222223,"max":481,"p90":480,"count":9,"p50":120,"p999":480,"sum":1766,"p95":480,"p75":165}
slow2|FAST,stats={"p99":625,"avg":365.5,"max":563,"p90":520,"count":14,"p50":420,"p999":625,"sum":5117,"p95":625,"p75":505}
fast8|FAST,stats={"p99":520,"avg":34.00523560209424,"max":583,"p90":1,"count":191,"p50":0,"p999":625,"sum":6495,"p95":320,"p75":0}
fast8|FAST_BACKUP,stats={"p99":42,"avg":57.875,"max":384,"p90":42,"count":8,"p50":12,"p999":42,"sum":463,"p95":42,"p75":13}
slow1|ISOLATION,stats={"p99":70469,"avg":35318.1129032258,"max":71212,"p90":64157,"count":186,"p50":36683,"p999":70840,"sum":6569169,"p95":67499,"p75":53761}
slow1|SLOW_BACKUP,stats={"p99":1650,"avg":53.857142857142854,"max":2095,"p90":1,"count":133,"p50":0,"p999":1750,"sum":7163,"p95":1,"p75":0}
slow1|FAST,stats={"p99":650,"avg":403.94117647058823,"max":616,"p90":625,"count":17,"p50":440,"p999":650,"sum":6867,"p95":650,"p75":513}
slow1|FAST_BACKUP,stats={"p99":470,"avg":205.33333333333334,"max":605,"p90":430,"count":15,"p50":90,"p999":470,"sum":3080,"p95":470,"p75":380}
slow1|SLOW,stats={"p99":1,"avg":13.795454545454545,"max":1813,"p90":0,"count":132,"p50":0,"p999":1,"sum":1821,"p95":1,"p75":0}
fast3|FAST,stats={"p99":515,"avg":33.554126473740624,"max":656,"p90":1,"count":933,"p50":0,"p999":700,"sum":31306,"p95":360,"p75":0}
fast3|FAST_BACKUP,stats={"p99":400,"avg":74.47222222222223,"max":581,"p90":313,"count":36,"p50":17,"p999":400,"sum":2681,"p95":320,"p75":51}
fast3|ISOLATION,stats={"p99":330,"avg":318.5,"max":353,"p90":330,"count":4,"p50":320,"p999":330,"sum":1274,"p95":330,"p75":330}
fast2|ISOLATION,stats={"p99":370,"avg":333.75,"max":361,"p90":370,"count":4,"p50":330,"p999":370,"sum":1335,"p95":370,"p75":370}
fast2|FAST,stats={"p99":520,"avg":35.79424307036248,"max":616,"p90":1,"count":938,"p50":0,"p999":683,"sum":33575,"p95":390,"p75":0}
fast2|FAST_BACKUP,stats={"p99":460,"avg":110.27272727272727,"max":495,"p90":410,"count":33,"p50":23,"p999":460,"sum":3639,"p95":455,"p75":152}
fast1|FAST,stats={"p99":518,"avg":31.86685082872928,"max":657,"p90":1,"count":1810,"p50":0,"p999":675,"sum":57679,"p95":296,"p75":0}
fast1|ISOLATION,stats={"p99":340,"avg":334.0,"max":343,"p90":340,"count":2,"p50":340,"p999":340,"sum":668,"p95":340,"p75":340}
fast1|FAST_BACKUP,stats={"p99":625,"avg":81.48235294117647,"max":587,"p90":386,"count":85,"p50":4,"p999":625,"sum":6926,"p95":490,"p75":55}
======type======
FAST,stats={"p99":519,"avg":34.74005803038061,"max":657,"p90":6,"count":5859,"p50":0,"p999":677,"sum":203542,"p95":368,"p75":0}
FAST_BACKUP,stats={"p99":637,"avg":95.43911439114392,"max":605,"p90":397,"count":271,"p50":17,"p999":675,"sum":25864,"p95":490,"p75":90}
SLOW,stats={"p99":1,"avg":15.93658536585366,"max":1813,"p90":1,"count":205,"p50":0,"p999":1150,"sum":3267,"p95":1,"p75":0}
SLOW_BACKUP,stats={"p99":1750,"avg":67.5048076923077,"max":2173,"p90":1,"count":208,"p50":0,"p999":2150,"sum":14041,"p95":520,"p75":0}
ISOLATION,stats={"p99":70312,"avg":33961.255528255526,"max":71212,"p90":63833,"count":407,"p50":34499,"p999":71032,"sum":13822231,"p95":67432,"p75":52855}
```
执行结果（ThreadPoolExecutor）：
```
======end,spend=100228======
======total======
fast7,stats={"p99":10350,"avg":5478.679324894515,"max":10377,"p90":9720,"count":237,"p50":5885,"p999":10383,"sum":1298447,"p95":10200,"p75":8240}
fast6,stats={"p99":10346,"avg":5482.53164556962,"max":10391,"p90":9733,"count":316,"p50":5866,"p999":10386,"sum":1732480,"p95":10171,"p75":8300}
fast5,stats={"p99":10356,"avg":5486.577494692145,"max":10396,"p90":9711,"count":471,"p50":5880,"p999":10391,"sum":2584178,"p95":10180,"p75":8283}
fast4,stats={"p99":10354,"avg":5491.987110633727,"max":10393,"p90":9726,"count":931,"p50":5884,"p999":10395,"sum":5113040,"p95":10171,"p75":8290}
slow2,stats={"p99":10350,"avg":5131.4225,"max":10389,"p90":9533,"count":800,"p50":5360,"p999":10393,"sum":4105138,"p95":10085,"p75":7975}
fast8,stats={"p99":10355,"avg":5475.642105263158,"max":10390,"p90":9700,"count":190,"p50":5866,"p999":10377,"sum":1040372,"p95":10150,"p75":8250}
slow1,stats={"p99":10355,"avg":5491.793991416309,"max":10427,"p90":9723,"count":932,"p50":5904,"p999":10395,"sum":5118352,"p95":10180,"p75":8285}
fast3,stats={"p99":10353,"avg":5488.774436090225,"max":10389,"p90":9730,"count":931,"p50":5890,"p999":10395,"sum":5110049,"p95":10163,"p75":8290}
fast2,stats={"p99":10356,"avg":5495.666309012876,"max":10429,"p90":9736,"count":932,"p50":5895,"p999":10395,"sum":5121961,"p95":10190,"p75":8300}
fast1,stats={"p99":10355,"avg":5495.532089961602,"max":10402,"p90":9738,"count":1823,"p50":5897,"p999":10395,"sum":10018355,"p95":10169,"p75":8294}
======detail======
fast7|null,stats={"p99":10350,"avg":5478.679324894515,"max":10377,"p90":9720,"count":237,"p50":5885,"p999":10383,"sum":1298447,"p95":10200,"p75":8240}
fast6|null,stats={"p99":10346,"avg":5482.53164556962,"max":10391,"p90":9733,"count":316,"p50":5866,"p999":10386,"sum":1732480,"p95":10171,"p75":8300}
fast5|null,stats={"p99":10356,"avg":5486.577494692145,"max":10396,"p90":9711,"count":471,"p50":5880,"p999":10391,"sum":2584178,"p95":10180,"p75":8283}
fast4|null,stats={"p99":10354,"avg":5491.987110633727,"max":10393,"p90":9726,"count":931,"p50":5884,"p999":10395,"sum":5113040,"p95":10171,"p75":8290}
slow2|null,stats={"p99":10350,"avg":5131.4225,"max":10389,"p90":9533,"count":800,"p50":5360,"p999":10393,"sum":4105138,"p95":10085,"p75":7975}
fast8|null,stats={"p99":10355,"avg":5475.642105263158,"max":10390,"p90":9700,"count":190,"p50":5866,"p999":10377,"sum":1040372,"p95":10150,"p75":8250}
slow1|null,stats={"p99":10355,"avg":5491.793991416309,"max":10427,"p90":9723,"count":932,"p50":5904,"p999":10395,"sum":5118352,"p95":10180,"p75":8285}
fast3|null,stats={"p99":10353,"avg":5488.774436090225,"max":10389,"p90":9730,"count":931,"p50":5890,"p999":10395,"sum":5110049,"p95":10163,"p75":8290}
fast2|null,stats={"p99":10356,"avg":5495.666309012876,"max":10429,"p90":9736,"count":932,"p50":5895,"p999":10395,"sum":5121961,"p95":10190,"p75":8300}
fast1|null,stats={"p99":10355,"avg":5495.532089961602,"max":10402,"p90":9738,"count":1823,"p50":5897,"p999":10395,"sum":10018355,"p95":10169,"p75":8294}
======type======
```
结果分析：  
* 使用普通线程池ThreadPoolExecutor的场景下，响应慢的租户影响到了响应快的租户，所有租户一视同仁，因此所有任务都有较高的延迟  
* 使用CamelliaDynamicIsolationExecutor的场景下，响应慢的租户被自动隔离了，因此响应快的租户可以保持任务执行延迟保持在较低水平而不受响应慢的租户的影响  

### 更多示例和源码
[示例源码](/camellia-samples/camellia-tools-samples)

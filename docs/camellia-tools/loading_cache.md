
# CamelliaLoadingCache

## 简介
CamelliaLoadingCache类似于Caffeine和Guava，底层基于ConcurrentLinkedHashMap，是一个LRU的缓存   
CamelliaLoadingCache会在后台线程定时更新那些刚刚访问过的热key，从而尽量避免在用户调用线程进行缓存更新操作        
特别的，当缓存穿透时，如果更新缓存操作出现异常，会返回上一次获取到的缓存旧值  

对比Caffeine的refreshAfterWrite策略：   
当一个key很久没有访问过了，此时调用get方法，会触发一个缓存更新操作，但是当前的请求可能返回一个过期的数据，相反CamelliaLoadingCache会确保返回最新的   
对比Caffeine的expireAfterWrite策略：   
当一个key很久没有访问过了，此时调用get方法，会触发一个缓存更新操作，但是如果缓存更新失败了会返回一个异常，相反CamelliaLoadingCache会返回一个旧值   

CamelliaLoadingCache适合于一些特定的使用场景，请按需使用  

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.3.6</version>
</dependency>
```

## 示例
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
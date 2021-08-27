
## 介绍
提供了一些工具类，包括：压缩工具类CamelliaCompressor、加解密工具类CamelliaEncryptor、本地缓存工具类CamelliaLoadingCache等  

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

### 更多示例和源码
[示例源码](/camellia-samples/camellia-tools-samples)

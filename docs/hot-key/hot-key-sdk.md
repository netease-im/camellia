
## SDK部分

包括两个SDK：
* CamelliaHotKeyMonitorSdk（仅用于监控热key）
* CamelliaHotKeyCacheSdk（用于监控热key，并且提供本地缓存功能）

### CamelliaHotKeyMonitorSdk

* 该sdk仅用于监控热key

```java
public interface ICamelliaHotKeyMonitorSdk {

    /**
     * 推送一个key用于统计和检测热key
     * @param namespace namespace
     * @param key key
     * @param count count
     * @return Result 结果
     */
    Result push(String namespace, String key, long count);

    /**
     * 获取当配置
     * @return 配置
     */
    CamelliaHotKeyMonitorSdkConfig getConfig();
}


public class Result {

    public static final Result HOT = new Result(true);
    public static final Result NOT_HOT = new Result(false);

    private final boolean hot;

    public Result(boolean hot) {
        this.hot = hot;
    }

    public boolean isHot() {
        return hot;
    }
}
```

### CamelliaHotKeyCacheSdk

#### 接口说明

* 该sdk支持热key的监控，并且在检测热key后，会自动走本地缓存逻辑，从而保护底层
* 数据有更新时，内部会自动同步更新给所有sdk，从而保证数据的一致性（弱一致性，最多有百毫秒级的延迟）


```java
public interface ICamelliaHotKeyCacheSdk {

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则会走loader穿透
     * 如果不是热key，则通过loader获取到value后返回
     *
     * 如果key有更新了，hot-key-server会广播给所有sdk去更新本地缓存，从而保证缓存值的时效性
     *
     * 如果key没有更新，sdk也会在配置的expireMillis之前尝试刷新一下（单机只会穿透一次）
     *
     * @param namespace namespace
     * @param key key
     * @param loader value loader
     * @return value
     */
    <T> T getValue(String namespace, String key, ValueLoader<T> loader);

    /**
     * key的value被更新了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param namespace namespace
     * @param key key
     */
    void keyUpdate(String namespace, String key);

    /**
     * key的value被删除了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param namespace namespace
     * @param key key
     */
    void keyDelete(String namespace, String key);

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则返回null
     * 即使是热key，也会在expireMillis到一半时返回一个null，从而让上层进行cache更新
     *
     * @param namespace namespace
     * @param key       key
     * @return value
     */
    <T> T getValue(String namespace, String key);

    /**
     * 尝试设置cache
     * 只有是hot-key才会设置成功，否则会被忽略
     *
     * @param namespace namespace
     * @param key       key
     * @param value    value
     */
    <T> void setValue(String namespace, String key, T value);

    /**
     * 判断一个key是否是热key，本方法的调用不会计入访问次数，只是一个单纯的查询接口
     *
     * @param namespace namespace
     * @param key       key
     * @return true/false
     */
    boolean isHotKey(String namespace, String key);
}

public interface ValueLoader<T> {

    /**
     * load 一个value
     * @param key key
     * @return value
     */
    T load(String key);
}

```

#### 示例代码

```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-hot-key-sdk</artifactId>
  <version>1.2.17</version>
</dependency>
```

```java
public class Test {

    public static void main(String[] args) {

        //非必填，可以标识来源，进程内唯一，从而hot-key-server回调热key的时候，会带上source列表，从而帮助定位热key的来源
        HotKeyConstants.Client.source = "xxx";
        
        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();

        config.setDiscovery(null);//设置一个发现器，默认提供zk/eureka，也可以自己实现基于etcd/consul/nacos等其他注册中心
        config.setCollectorType(CollectorType.Caffeine);//默认是Caffeine，还可以使用ConcurrentLinkedHashMap
        config.setAsync(false);//是否异步，默认false，如果Collector的延迟不满足业务要求，则可以使用异步采集(异步采集会产生大量的线程上下文切换，可能得不偿失)
        config.setAsyncQueueCapacity(100000);//异步队列的大小，默认10w
        //如果需要同时访问多个集群，则需要初始化多个sdk，否则初始化一个实例即可
        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        //基于CamelliaHotKeySdk，可以构造CamelliaHotKeyMonitorSdk
        testMonitor(sdk);

        //基于CamelliaHotKeySdk，可以构造CamelliaHotKeyCacheSdk
        testCache(sdk);
    }

    private static void testMonitor(CamelliaHotKeySdk sdk) {
        //设置相关参数，一般来说默认即可
        CamelliaHotKeyMonitorSdkConfig config = new CamelliaHotKeyMonitorSdkConfig();
        //初始化CamelliaHotKeyMonitorSdk，一般全局一个即可
        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, config);

        //把key的访问push给server即可
        String namespace1 = "db_cache";
        monitorSdk.push(namespace1, "key1", 1);
        monitorSdk.push(namespace1, "key2", 1);
        monitorSdk.push(namespace1, "key2", 1);

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx", 1);
        monitorSdk.push(namespace2, "/xx/xx2", 1);
    }

    private static void testCache(CamelliaHotKeySdk sdk) {
        //设置相关参数，按需设置，一般来说默认即可
        CamelliaHotKeyCacheSdkConfig config = new CamelliaHotKeyCacheSdkConfig();
        config.setCapacity(1000);//最多保留多少个热key的缓存，各个namespace之间是隔离的，独立计算容量
        config.setCacheNull(true);//是否缓存null

        //初始化CamelliaHotKeyCacheSdk，一般来说如果对于上述配置策略没有特殊要求的话，或者缓存不想互相挤占的话，全局一个即可
        CamelliaHotKeyCacheSdk cacheSdk = new CamelliaHotKeyCacheSdk(sdk, config);

        String namespace1 = "db";
        String value1 = cacheSdk.getValue(namespace1, "key1", Test::getValueFromDb);
        System.out.println(value1);

        String namespace2 = "redis";
        String value2 = cacheSdk.getValue(namespace2, "key1", Test::getValueRedis);
        System.out.println(value2);
    }

    private static String getValueFromDb(String key) {
        return key + "-value";
    }

    private static String getValueRedis(String key) {
        return key + "-value";
    }
}
```
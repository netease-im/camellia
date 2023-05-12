
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
     * @return Result 结果
     */
    Result push(String namespace, String key);

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

```java
public class Test {

    public static void main(String[] args) {
        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(null);//设置一个发现器，默认提供zk/eureka，也可以自己实现基于etcd/consul/nacos等其他注册中心
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
        monitorSdk.push(namespace1, "key1");
        monitorSdk.push(namespace1, "key2");
        monitorSdk.push(namespace1, "key2");

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx");
        monitorSdk.push(namespace2, "/xx/xx2");
    }

    private static void testCache(CamelliaHotKeySdk sdk) {
        //设置相关参数，按需设置，一般来说默认即可
        CamelliaHotKeyCacheSdkConfig config = new CamelliaHotKeyCacheSdkConfig();
        config.setCapacity(1000);//最多保留多少个热key的缓存，各个namespace之间是隔离的，独立计算容量
        config.setCacheNull(true);//是否缓存null
        config.setLoadTryLockRetry(10);//对于热key，当缓存穿透时，如果有并发锁，锁等待的次数
        config.setLoadTryLockSleepMs(1);//对于热key，当缓存穿透时，如果有并发锁，锁每次等待的ms

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
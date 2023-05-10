
# camellia-hot-key

## 简介

* 一个热key探测和缓存的工具
* 包括SDK、Server两个模块
* 参考了：京东hotkey(https://gitee.com/jd-platform-opensource/hotkey)、搜狐hotCaffeine(https://github.com/sohutv/hotcaffeine)、云音乐music_hot_caffeine的架构和实现

## 基本架构

<img src="hot-key.png" width="90%" height="90%">  

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
     */
    void push(String namespace, String key);

}
```

### CamelliaHotKeyCacheSdk

#### 接口说明

* 该sdk支持热key的监控，并且支持设置本地缓存
* IValueLoaderLock内置了单机并发控制lock和集群并发控制lock（基于redis），也可以自定义实现

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

public interface IValueLoaderLock {

    /**
     * 尝试获取一把锁
     * @param key key
     * @return 成功/失败
     */
    boolean tryLock(String key);

    /**
     * 释放一把锁
     * @param key key
     * @return 成功/失败
     */
    boolean release(String key);
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

## Server部分

* 服务器基于namespace来管理热key相关配置，namespace有monitor和cache两种类型，对应于两种sdk，其中cache的功能覆盖了monitor
* 每个namespace下可以配置多个rule
* rule主要用于设置key的匹配模式、热key的定义（多少时间内多少次请求），热key缓存过期时间（只有cache模式下需要）

### 配置

#### 字段说明
|字段名|类型|说明|
|:---:|:---:|:---:|
|namespace|string|命名空间|
|namespace.type|string|命名空间，包括：monitor和cache两种类型|
|rule.name|string|规则名字|
|rule.type|string|规则类型，包括：exact_match（精确匹配）、prefix_match（前缀匹配）、match_all（匹配所有）|
|rule.keyConfig|string|exact_match时表示key本身，prefix_match时表示前缀，match_all没有这个配置项|
|rule.checkMillis|long|检查周期，单位ms|
|rule.checkThreshold|long|检查阈值|
|rule.expireMills|long|过期时间，只有cache这种命名空间下的rule需要配置这个，如果缺失或者小于等于0则表示cache不启用|

#### 配置示例（monitor）

```json
{
    "namespace": "test",
    "type": "monitor",
    "rules":
    [
        {
            "name": "rule1",
            "type": "exact_match",
            "keyConfig": "abcdef",
            "checkMillis": 1000,
            "checkThreshold": 100
        },
        {
            "name": "rule1",
            "type": "prefix_match",
            "keyConfig": "xyz",
            "checkMillis": 1000,
            "checkThreshold": 100
        },
        {
            "name": "rule1",
            "type": "match_all",
            "checkMillis": 1000,
            "checkThreshold": 100
        }
    ]
}
```

#### 配置示例（cache）
```json

{
    "namespace": "test",
    "type": "cache",
    "rules":
    [
        {
            "name": "rule1",
            "type": "exact_match",
            "keyConfig": "abcdef",
            "checkMillis": 1000,
            "checkThreshold": 100,
            "expireMills": 10000
        },
        {
            "name": "rule1",
            "type": "prefix_match",
            "keyConfig": "xyz",
            "checkMillis": 1000,
            "checkThreshold": 100,
            "expireMills": 10000
        },
        {
            "name": "rule1",
            "type": "match_all",
            "checkMillis": 1000,
            "checkThreshold": 100,
            "expireMills": 10000
        }
    ]
}
```


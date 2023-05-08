
# camellia-hot-key

## 简介

* 一个热key探测和缓存的工具
* 包括SDK、Server两个模块

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
     * @param key key
     */
    void push(String key);

}
```

### CamelliaHotKeyCacheSdk

* 该sdk支持热key的监控，并且支持设置本地缓存
* IValueLoaderLock内置了单机并发控制lock和集群并发控制lock（基于redis），也可以自定义实现

```java
public interface ICamelliaHotKeyCacheSdk {

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则会走loader穿透（穿透时支持设置并发控制）
     * 如果不是热key，则通过loader获取到value后返回
     *
     * 如果key有更新了，hot-key-server会广播给所有sdk去更新本地缓存，从而保证缓存值的时效性
     *
     * @param key key
     * @param loader value loader
     * @param loaderLock loader lock，用于并发控制，如果不传，则不进行并发控制，可以是单机的锁，也可以是集群的锁（如redis）
     * @return value
     */
    <T> T getValue(String key, ValueLoader<T> loader, IValueLoaderLock loaderLock);

    /**
     * key的value被更新了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param key key
     */
    void keyUpdate(String key);

    /**
     * key的value被删除了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param key key
     */
    void keyDelete(String key);
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

## Server部分

* 服务器基于namespace来管理热key相关配置，namespace有monitor和cache两种类型，对应于两种sdk，其中cache的功能覆盖了monitor
* 每个namespace下可以配置多个rule
* rule主要用于设置key的匹配模式、热key的定义（多少时间内多少次请求），热key缓存过期时间（只有cache模式下需要）

### 配置示例（monitor）
```json
{
    "namespace": "test",
    "type": "monitor",
    "rules":
    [
        {
            "name": "rule1",
            "type": "exact_match",
            "config": "abcdef",
            "checkMillis": 1000,
            "checkThreshold": 100
        },
        {
            "name": "rule1",
            "type": "prefix_match",
            "config": "xyz",
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

### 配置示例（cache）
```json

{
    "namespace": "test",
    "type": "cache",
    "rules":
    [
        {
            "name": "rule1",
            "type": "exact_match",
            "config": "abcdef",
            "checkMillis": 1000,
            "checkThreshold": 100,
            "expireMills": 10000
        },
        {
            "name": "rule1",
            "type": "prefix_match",
            "config": "xyz",
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


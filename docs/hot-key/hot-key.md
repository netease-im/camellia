
# camellia-hot-key

## 简介

* 一个热key探测和缓存的工具
* 包括SDK、Server两个模块
* 设计时参考了以下内外部开源的项目：[京东hotkey](https://gitee.com/jd-platform-opensource/hotkey) 、[搜狐hotCaffeine](https://github.com/sohutv/hotcaffeine) 、云音乐music_hot_caffeine

## 特性

* 支持`热key探测`
* 支持`热key本地缓存`，本地缓存如果有更新，hot-key-server会自动广播给所有sdk，从而保证数据的`弱`一致性（可能有百ms级的延迟）
* `namespace`维度划分服务，一个hot-key-server支持同时服务多个`namespace`
* `namespace`包括`monitor`和`cache`两种类型（对应于`CamelliaHotKeyMonitorSdk`和`CamelliaHotKeyCacheSdk`两个SDK）
* 一个namespace下可以配置多条`rule`
* rule里配置key的匹配规则（字符串完全匹配、字符串前缀匹配、匹配所有），以及热key的定义（时间窗口+阈值）
* 特别的，对于cache类型的namespace，还需要配置检测到热key转为本地cache后的过期时间（如果一直是热key，cache会一直delay，并且定期load）
* 支持自定义热key配置数据源（默认为本地文件配置），从而可以对接到你们的`配置中心`或者`管理后台`
* 支持topN统计（全局维度），依赖redis进行数据汇总，对redis仅有少量的调用
* 支持服务器自定义事件回调（热key回调+topN回调），从而可以对接到你们的`监控报警系统`或者`频控流控系统`
* 支持自定义的注册发现器（内置了zk和eureka两种实现）
* 支持丰富的监控，用于监控服务运行状态

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

## Server部分

* 服务器基于namespace来管理热key相关配置，namespace有monitor和cache两种类型，对应于两种sdk，其中cache的功能覆盖了monitor
* 每个namespace下可以配置多个rule
* rule主要用于设置key的匹配模式、热key的定义（多少时间内多少次请求），热key缓存过期时间（只有cache模式下需要）

### 快速开始
1) 首先创建一个spring-boot的工程，然后添加以下依赖:
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-hot-key-server-spring-boot-starter</artifactId>
  <version>1.2.8-SNAPSHOT</version>
</dependency>
```
2) 编写主类Application.java, 如下:
```java
@SpringBootApplication
@EnableCamelliaHotKeyServer
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

```
3) 配置application.yml, 如下:
```yaml
server:
  port: 7070
spring:
  application:
    name: camellia-hot-key-server

camellia-hot-key-server:
  #工作线程和工作队列
  biz-work-thread: -1 #默认使用cpu核数的一半，不建议修改
  biz-queue-capacity: 1000000 #队列容量，默认100w
  #netty部分
  netty:
    boss-thread: 1 #默认1，不建议修改
    work-thread: -1 #默认cpu核数的一半，不建议修改
  #console，一个简单的http服务器，可以做优雅上下线和监控数据暴露，也可以自定义接口
  console-port: 17070
  #公共部分
  max-namespace: 1000 #预期的最大的namespace数量，默认1000
  hot-key-config-service-class-name: com.netease.nim.camellia.hot.key.server.conf.FileBasedHotKeyConfigService #热key配置数据源，默认使用本地配置文件，业务可以自定义实现
  #热key探测部分
  hot-key-cache-counter-capacity: 100000 #LRU-key计数器的容量，默认10w(每个namespace下)
  hot-key-cache-capacity: 10000 #每个namespace最多的热key数量，默认1w
  hot-key-callback-interval-seconds: 10 #同一个热key回调给业务自行处理的最小间隔，默认10s
  hot-key-callback-class-name: com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyCallback #探测到热key后的自定义回调，默认是打日志
  ##topn探测部分，topn只会统计符合热key探测规则的key，topn统计的窗口固定为1s
  topn-count: 100 #每个namespace下，topn的key的数量，默认100
  topn-cache-counter-capacity: 100000 #topn统计中的LRU-key计数器(每个namespace下)
  topn-collect-seconds: 60 #topn统计结果收集间隔，也是回调的间隔，默认60s
  topn-tiny-collect-seconds: 5 #topn初步统计结果收集间隔，topn-collect-seconds需要大于topn-tiny-collect-seconds
  topn-redis-key-prefix: camellia #topn统计结果会通过redis进行聚合从而获取全局视图
  topn-redis-expire-seconds: 3600 #topn统计结果在redis中的保留时间
  topn-callback-class-name: com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyTopNCallback #topN统计结果的自定义回调，默认打印日志
  ##监控（通过console-http接口暴露）
  monitor-interval-seconds: 60 #监控数据刷新间隔，默认60s
  monitor-hot-key-max-count: 20 #每个namespace最多展示几个hot-key

#topn统计依赖redis
camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379
  redis-conf:
    jedis:
      timeout: 2000
      min-idle: 0
      max-idle: 32
      max-active: 32
      max-wait-millis: 2000
    jedis-cluster:
      max-wait-millis: 2000
      min-idle: 0
      max-idle: 16
      max-active: 16
      max-attempts: 5
      timeout: 2000
```
4）启动即可

### 扩展口

#### HotKeyConfigService
* 热key配置的数据源，默认是FileBasedHotKeyConfigService，会读取本地文件
* 你也可以自定义实现，只要实现`HotKeyConfig get(String namespace)`方法，并且在配置变更时回调`void invokeUpdate(namespace)`即可

```java
public abstract class HotKeyConfigService {
    /**
     * 获取HotKeyConfig
     * @param namespace namespace
     * @return HotKeyConfig
     */
    public abstract HotKeyConfig get(String namespace);

    //回调方法
    protected final void invokeUpdate(String namespace) {
        //xxxx
    }
}
```

#### HotKeyCallback
* 发现热key后的回调，默认是LoggingHotKeyCallback，仅打印日志
* 你也可以自己实现本接口，从而对接到你们的监控报警系统，或者频控/限流系统等

```java
public interface HotKeyCallback {

    /**
     * 热key回调接口
     * @param hotKeyInfo 热key信息
     */
    void newHotKey(HotKeyInfo hotKeyInfo);
}
```
```java
public class HotKeyInfo {
    private final String namespace;
    private final String key;
    private final KeyAction action;
    private final Rule rule;
    private final long count;
}
public enum KeyAction {
    QUERY(1),
    UPDATE(2),
    DELETE(3),
    ;
}
```

#### HotKeyTopNCallback
* 定期回调每个namespace下topN的key，默认是LoggingHotKeyTopNCallback，仅打印日志
* 你也可以自己实现，从而对接到你们的监控系统，做数据的留存

```java
public interface HotKeyTopNCallback {

    /***
     * 回调topN的key数据，注意回调结果是全局的topN，而非单机的topN
     * 一个周期内，一个namespace只会回调一次（集群内会任选一台机器，通过redis锁来实现）
     * @param result 结果
     */
    void topN(TopNStatsResult result);
}
```
```java
public class TopNStatsResult {
    private String namespace;
    private List<TopNStats> topN;
}
public class TopNStats implements Comparable<TopNStats> {
    private final String key;
    private final KeyAction action;
    private final long total;
    private final long maxQps;
}
```

### 配置

#### 字段说明
|字段名|类型|说明|
|:---:|:---:|:---:|
|namespace|string|命名空间|
|namespace.type|string|命名空间，包括：monitor和cache两种类型|
|rule.name|string|规则名字|
|rule.type|string|规则类型，包括：exact_match（精确匹配）、prefix_match（前缀匹配）、match_all（匹配所有）|
|rule.keyConfig|string|exact_match时表示key本身，prefix_match时表示前缀，match_all没有这个配置项|
|rule.checkMillis|long|检查周期，单位ms，需要是100ms的整数倍，否则会被四舍五入|
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


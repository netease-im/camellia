## Server部分

* 服务器基于namespace来管理热key相关配置
* 每个namespace下可以配置多个rule
* rule主要用于设置key的匹配模式、热key的定义（多少时间内多少次请求），热key缓存过期时间
* rule里的expireMillis是可选的，只要配置了本字段：CamelliaHotKeyMonitorSdk才能获取到是否热key的结果，CamelliaHotKeyCacheSdk才真正启用本地缓存
* 提供了丰富的扩展口
* 提供了丰富的监控数据

### 快速开始
1) 首先创建一个spring-boot的工程，然后添加以下依赖:
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-hot-key-server-spring-boot-starter</artifactId>
  <version>1.2.8</version>
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
  biz-work-thread: -1 #默认使用cpu核数
  biz-queue-capacity: 100000 #队列容量，默认10w
  #netty部分
  netty:
    boss-thread: 1 #默认1，不建议修改
    work-thread: -1 #默认cpu核数
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
  hot-key-cache-stats-callback-class-name: com.netease.nim.camellia.hot.key.server.callback.LoggingHotKeyCacheStatsCallback #热key缓存功能的统计数据（命中情况），默认是打印日志
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

    /**
     * 初始化后会调用本方法，你可以重写本方法去获取到HotKeyServerProperties中的相关配置
     * @param properties properties
     */
    public void init(HotKeyServerProperties properties) {
    }

    //回调方法
    protected final void invokeUpdate(String namespace) {
        //xxxx
    }
}
```

更多配置方式参考：[hot-key-config](hot-key-config.md)  

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
    private final Set<String> sourceSet;
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
    private final Set<String> sourceSet;
}
```

#### HotKeyCacheStatsCallback
* 使用CamelliaHotKeyCacheSdk时缓存命中的统计数据，默认是LoggingHotKeyCacheStatsCallback，仅打印日志
* 你也可以自己实现，从而对接到你们的监控系统，做数据的留存

```java
public interface HotKeyCacheStatsCallback {

    /**
     * callback the hot key cache stats
     * @param identityInfo IdentityInfo
     * @param hotKeyCacheStats HotKeyCacheInfo
     * @param checkMillis checkMillis
     * @param checkThreshold checkThreshold
     */
    void callback(IdentityInfo identityInfo, HotKeyCacheInfo hotKeyCacheStats, long checkMillis, long checkThreshold);
}
```

### 配置

#### 字段说明
|字段名|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|namespace|string|是|命名空间|
|rule.name|string|是|规则名字，每个namespace下的rule.name需要唯一|
|rule.type|string|是|规则类型，包括：`exact_match`（精确匹配）、`prefix_match`（前缀匹配）、`match_all`（匹配所有）、`contains`（包含子串）|
|rule.keyConfig|string|否|exact_match时表示key本身，prefix_match时表示前缀，match_all没有这个配置项|
|rule.checkMillis|long|是|检查周期，单位ms，需要是100ms的整数倍，否则会被四舍五入|
|rule.checkThreshold|long|是|检查阈值|
|rule.expireMills|long|否|检测到热key后的过期时间，只有配置了本字段，hot-key的信息才会广播给SDK，从而CamelliaHotKeyMonitorSdk才能获取的Result，CamelliaHotKeyCacheSdk本地缓存功能才生效，否则仅做服务器统计|

备注：服务器会根据rules数组的顺序依次进行规则匹配，匹配到一个之后就返回

#### 配置示例

```json
{
  "namespace": "test",
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
      "name": "rule2",
      "type": "prefix_match",
      "keyConfig": "xyz",
      "checkMillis": 1000,
      "checkThreshold": 100,
      "expireMills": 10000
    },
    {
      "name": "rule3",
      "type": "contains",
      "keyConfig": "opq",
      "checkMillis": 1000,
      "checkThreshold": 100
    },
    {
      "name": "rule4",
      "type": "match_all",
      "checkMillis": 1000,
      "checkThreshold": 100,
      "expireMills": 10000
    }
  ]
}
```


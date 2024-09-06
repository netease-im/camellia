## HotKeyPlugin

### 说明

- 使用场景：单机Proxy或者集群proxy之间需要共享hot-key。
- 使用[hot-key-sdk](https://github.com/21want28k/camellia/blob/master/docs/hot-key/hot-key.md)，接入hot-key-server服务的插件，从而监控hot-key的状态。
- 集群伪cluster模式下直接使用内置HotKey插件，客户端自带功能，会把请求映射到固定的proxy上，所以无需proxy之间共享。
- 相比与内建的热key监控插件和热key缓存插件，本插件对性能影响较大，**高并发场景下不建议使用**

### 使用方式

#### Dependency：

```xml
<dependency>
	<groupId>com.netease.nim</groupId>
	<artifactId>camellia-redis-proxy-hot-key-monitor-plugin</artifactId>
	<version>1.2.29</version>
</dependency>
```

#### 连接本地hot-key-server（默认）

Proxy：camellia-redis-proxy.properties

```properties
# local hot-key-server(default)
# 监控类总开关
monitor.enable=true
# 插件全限定名
proxy.plugin.list=com.netease.nim.camellia.redis.proxy.hotkey.monitor.plugin.HotKeyMonitorPlugin
# 监控开关
hot.key.monitor.enable=true

# 本地server地址，默认连接本地的server
hot.key.server.local.addr=127.0.0.1:7070
hot.key.server.local.name=local
```

Hot-key-server配置

```json
{
  "namespace": "1|default",
  "rules":
  [
    {
      "name": "rule3",
      "type": "match_all",
      "checkMillis": 1000,
      "checkThreshold": 500,
      "expireMillis": 10000
    }
  ]
}
```

主要是namespace，要配置对，如果不配置多租户namespace就是`default|default`   
你也可以设置namespace的别名，如下：
```properties
1.default.hot.key.server.monitor.namespace=namespace1
2.default.hot.key.server.monitor.namespace=namespace2
```

## HotKeyCachePlugin

### 说明

比HotKeyPlugin，多了本地cache功能

### 使用方式

#### Dependency：

```xml
<dependency>
   <groupId>com.netease.nim</groupId>
   <artifactId>camellia-redis-proxy-hot-key-cache-plugin</artifactId>
   <version>1.2.29</version>
</dependency>
```

Proxy：camellia-redis-proxy.properties

```properties
#启用插件
proxy.plugin.list=com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin.HotKeyCachePlugin
#开关
hot.key.cache.enable=true

# 本地server地址，默认连接本地的server
hot.key.server.local.addr=127.0.0.1:7070
hot.key.server.local.name=local
```

## 两种插件共同支持

#### 多租户支持

多租户目前只支持，开关，也就是说，你可以对某些个租户启用，某些个租户不启用。`bid+|+bgroup`，比如`1|default`,组成hot-key-server对应的namespace。

camellia-redis-proxy.properties

```properties
# multiTalent
1.default.hot.key.cache.enable=true
2.default.hot.key.cache.enable=false
```

你也可以设置namespace的别名，如下：  
```properties
1.default.hot.key.server.cache.namespace=namespace1
2.default.hot.key.server.cache.namespace=namespace2
```

#### 基于ZK发现机制

在依赖基础上，添加zk，不用本地hot-key-server服务，也就是说 proxy, 插件发现，hot-key-server，都在zk上注册了。

在proxy中添加proxy-zk-registry:

```xml
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-zk-registry-spring-boot-starter</artifactId>
    <version>1.2.29</version>
</dependency>
```

在hot-key-server中添加hot-key-server-zk-registry:

```xml
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-hot-key-server-zk-registry-spring-boot-starter</artifactId>
    <version>1.2.29</version>
</dependency>
```

proxy配置文件-camellia-redis-proxy.properties

```properties
hot.key.server.zk.zkUrl=192.168.88.130:2181
hot.key.server.zk.basePath=/camellia-hot-key
hot.key.server.zk.applicationName=camellia-hot-key-server
hot.key.server.discovery.className=com.netease.nim.camellia.redis.proxy.hotkey.discovery.zk.ProxyZkHotKeyServerDiscoveryFactory
```

在server和proxy，application.yaml中加入zk配置

```yaml
camellia-hot-key-zk-registry:
  enable: true
  zkUrl: 192.168.88.130:2181
  basePath: /camellia-hot-key
  
camellia-redis-zk-registry:
  enable: true
  zk-url: 192.168.88.130:2181
  base-path: /camellia
```

#### 基于Eureka发现机制

目前proxy本身还没有实现在Eureka中注册，但是可以使得hotkey插件和server两个部分在Eureka中进行注册，步骤如下：

Proxy处添加依赖，代表插件的发现机制。

```xml
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-hot-key-discovery-eureka</artifactId>
    <version>1.2.29</version>
</dependency>
```

proxy配置文件-camellia-redis-proxy.properties

```properties
hot.key.server.eureka.applicationName=camellia-hot-key-server
hot.key.server.eureka.refreshIntervalSeconds=5
hot.key.server.discovery.className=com.netease.nim.camellia.redis.proxy.hotkey.discovery.eureka.ProxyEurekaHotKeyServerDiscoveryFactory
```

hot-key-server处添加依赖：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

hot-key-server处application.yaml添加配置

```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    registryFetchIntervalSeconds: 5
  instance:
    leaseExpirationDurationInSeconds: 15
    leaseRenewalIntervalInSeconds: 5
    prefer-ip-address: true
```

### standalone performance report：

#### environment

|          node          |  specs   |
| :--------------------: | :------: |
|      redis-proxy       | 8C16G VM |
|    redis-standalone    | 8C16G VM |
| redis-benchmark client | 8C16G VM |

#### direct to redis

```linux
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6366 -h 127.0.0.1   

SET: 71911.41 requests per second, p50=1.447 msec                   
GET: 70516.89 requests per second, p50=1.503 msec                   
INCR: 68236.10 requests per second, p50=1.431 msec                   
LPUSH: 72653.30 requests per second, p50=1.415 msec                   
RPUSH: 70234.59 requests per second, p50=1.431 msec                   
LPOP: 72103.25 requests per second, p50=1.455 msec                   
RPOP: 69391.44 requests per second, p50=1.439 msec                   
SADD: 69725.28 requests per second, p50=1.471 msec                   
HSET: 70781.42 requests per second, p50=1.455 msec                   
SPOP: 72202.16 requests per second, p50=1.455 msec
LPUSH (needed to benchmark LRANGE): 70866.70 requests per second, p50=1.431 msec
LRANGE_100 (first 100 elements): 45783.36 requests per second, p50=2.999 msec
LRANGE_300 (first 300 elements): 24005.57 requests per second, p50=6.103 msec
LRANGE_500 (first 500 elements): 14815.25 requests per second, p50=8.959 msec
LRANGE_600 (first 600 elements): 11830.96 requests per second, p50=10.271 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6366 -h 127.0.0.1 
SET: 514403.28 requests per second, p50=1.439 msec                    
GET: 695894.19 requests per second, p50=1.047 msec 

./redis-benchmark -n 1000000 -t mset -P 16 -q -p 6366 -h 127.0.0.1 
MSET (10 keys): 163185.39 requests per second, p50=4.135 msec
```

#### camellia-proxy-1.2.10

proxy-config：

```yaml
server:
  port: 6405
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console port, default 16379, if setting -16379, proxy will choose a random port, if setting 0, will disable console
  password: pass123   #password of proxy, priority less than custom client-auth-provider-class-name
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6366 #target transpond redis address
```

```linux
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6405 -h 127.0.0.1 -a pass123

SET: 84545.15 requests per second, p50=1.023 msec                     
GET: 69516.86 requests per second, p50=1.143 msec                    
INCR: 70116.39 requests per second, p50=1.135 msec                    
LPUSH: 77041.60 requests per second, p50=1.079 msec                    
RPUSH: 73844.34 requests per second, p50=1.119 msec                    
LPOP: 82712.98 requests per second, p50=1.071 msec                    
RPOP: 79668.58 requests per second, p50=1.087 msec                    
SADD: 77857.37 requests per second, p50=1.079 msec                    
HSET: 70796.46 requests per second, p50=1.143 msec                    
SPOP: 75500.19 requests per second, p50=1.071 msec  
LPUSH (needed to benchmark LRANGE): 81645.98 requests per second, p50=1.055 msec
LRANGE_100 (first 100 elements): 47481.12 requests per second, p50=1.991 msec
LRANGE_300 (first 300 elements): 23552.69 requests per second, p50=4.807 msec
LRANGE_500 (first 500 elements): 14483.10 requests per second, p50=8.711 msec
LRANGE_600 (first 600 elements): 10681.93 requests per second, p50=10.927 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
SET: 465766.16 requests per second, p50=0.927 msec                    
GET: 512820.50 requests per second, p50=0.855 msec 

./redis-benchmark -n 1000000 -t mset -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
MSET (10 keys): 184535.89 requests per second, p50=3.247 msec
```

#### camellia-proxy-1.2.10-hot-key-built-in

内建插件（不基于hot-key-sdk/hot-key-server）

camellia-redis-proxy.properties：

```properties
proxy.plugin.list=hotKeyPlugin
#开关
hot.key.monitor.enable=true
#热key监控LRU计数器的容量，一般不需要配置
hot.key.monitor.cache.max.capacity=100000
#热key监控统计的时间窗口，默认1000ms
hot.key.monitor.counter.check.millis=3000
#热key监控统计在时间窗口内超过多少阈值，判定为热key，默认500
hot.key.monitor.counter.check.threshold=10
#单个周期内最多上报多少个热key，默认32（取top）
hot.key.monitor.max.hot.key.count=32
```

```linux
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6405 -h 127.0.0.1 -a pass123
SET: 76161.46 requests per second, p50=1.063 msec                    
GET: 73795.30 requests per second, p50=1.071 msec                    
INCR: 80128.21 requests per second, p50=1.063 msec                    
LPUSH: 78511.42 requests per second, p50=1.071 msec                    
RPUSH: 79132.70 requests per second, p50=1.071 msec                    
LPOP: 78003.12 requests per second, p50=1.079 msec                    
RPOP: 79802.09 requests per second, p50=1.039 msec                     
SADD: 72950.10 requests per second, p50=1.071 msec                    
HSET: 74783.13 requests per second, p50=1.087 msec                    
SPOP: 78462.14 requests per second, p50=1.055 msec
LPUSH (needed to benchmark LRANGE): 70511.91 requests per second, p50=1.127 msec
LRANGE_100 (first 100 elements): 43318.17 requests per second, p50=2.087 msec
LRANGE_300 (first 300 elements): 25678.55 requests per second, p50=4.143 msec
LRANGE_500 (first 500 elements): 15365.47 requests per second, p50=8.439 msec
LRANGE_600 (first 600 elements): 13466.74 requests per second, p50=10.063 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
SET: 462107.19 requests per second, p50=0.927 msec                    
GET: 424808.81 requests per second, p50=0.983 msec 

./redis-benchmark -n 1000000 -t mset -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
MSET (10 keys): 193498.44 requests per second, p50=3.087 msec
```

#### camellia-proxy-1.2.10-hot-key

基于hot-key-sdk/hot-key-server

camellia-redis-proxy.properties:

```properties
monitor.enable=true
proxy.plugin.list=com.netease.nim.camellia.redis.proxy.hotkey.monitor.plugin.HotKeyMonitorPlugin

# local hot-key-server(default)
hot.key.monitor.enable=true

hot.key.server.local.addr=127.0.0.1:7070
hot.key.server.local.name=local
```

hot-key-server.yaml

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
    resource: redis://@192.168.88.130:6366
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

config.json

```json
{
  "namespace": "default|default",
  "rules":
  [
    {
      "name": "rule3",
      "type": "match_all",
      "checkMillis": 3000,
      "checkThreshold": 10,
      "expireMillis": 10000
    }
  ]
}
```

```linux
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6405 -h 127.0.0.1 -a pass123
SET: 44638.87 requests per second, p50=1.687 msec                     
GET: 49684.50 requests per second, p50=1.647 msec                   
INCR: 46195.78 requests per second, p50=1.927 msec                   
LPUSH: 67645.27 requests per second, p50=1.151 msec                    
RPUSH: 67801.21 requests per second, p50=1.183 msec                    
LPOP: 61289.53 requests per second, p50=1.671 msec                    
RPOP: 65011.05 requests per second, p50=1.303 msec                    
SADD: 60448.53 requests per second, p50=1.775 msec                     
HSET: 61102.29 requests per second, p50=1.631 msec                    
SPOP: 61282.02 requests per second, p50=1.687 msec 
LPUSH (needed to benchmark LRANGE): 62441.46 requests per second, p50=1.511 msec
LRANGE_100 (first 100 elements): 34579.34 requests per second, p50=3.471 msec
LRANGE_300 (first 300 elements): 22318.44 requests per second, p50=4.911 msec
LRANGE_500 (first 500 elements): 14847.15 requests per second, p50=8.327 msec
LRANGE_600 (first 600 elements): 11533.23 requests per second, p50=10.151 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
SET: 349040.12 requests per second, p50=1.047 msec                    
GET: 361141.19 requests per second, p50=1.023 msec

./redis-benchmark -n 1000000 -t mset -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
MSET (10 keys): 171939.48 requests per second, p50=3.783 msec
```

#### camellia-proxy-1.2.10-hot-key-cache-built-in

内建插件（不基于hot-key-sdk/hot-key-server）

camellia-redis-proxy.properties：

```properties
proxy.plugin.list=hotKeyCachePlugin
##热key缓存相关的配置
#热key缓存功能的开关，默认true
hot.key.cache.enable=true
#用于判断是否是热key的LRU计数器的容量
hot.key.cache.counter.capacity=100000
#用于判断是否是热key的LRU计数器的时间窗口，默认1000ms
hot.key.cache.counter.check.millis=3000
#判定为热key的阈值，默认100
hot.key.cache.check.threshold=10
#是否缓存null的value，默认true
hot.key.cache.null=true
#热key缓存的时长，默认10s，过期一半的时候会穿透一个GET请求到后端
hot.key.cache.expire.millis=10000
#最多多少个缓存的热key，默认1000
hot.key.cache.max.capacity=1000
```

```linux
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6405 -h 127.0.0.1 -a pass123
SET: 82149.02 requests per second, p50=1.039 msec                    
GET: 87206.77 requests per second, p50=1.039 msec                    
INCR: 87389.67 requests per second, p50=1.031 msec                    
LPUSH: 78308.53 requests per second, p50=1.063 msec                    
RPUSH: 73795.30 requests per second, p50=1.111 msec                    
LPOP: 89341.55 requests per second, p50=1.007 msec                    
RPOP: 85859.02 requests per second, p50=1.023 msec                    
SADD: 90334.24 requests per second, p50=1.007 msec                     
HSET: 87512.03 requests per second, p50=1.031 msec                    
SPOP: 83430.67 requests per second, p50=1.039 msec 
LPUSH (needed to benchmark LRANGE): 76429.23 requests per second, p50=1.071 msec
LRANGE_100 (first 100 elements): 44756.75 requests per second, p50=2.039 msec
LRANGE_300 (first 300 elements): 22993.26 requests per second, p50=4.687 msec
LRANGE_500 (first 500 elements): 15890.42 requests per second, p50=7.951 msec
LRANGE_600 (first 600 elements): 14477.44 requests per second, p50=9.159 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
SET: 408329.94 requests per second, p50=0.975 msec                    
GET: 464252.53 requests per second, p50=0.935 msec

./redis-benchmark -n 1000000 -t mset -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
MSET (10 keys): 206910.81 requests per second, p50=3.303 msec
```

#### camellia-proxy-1.2.10-hot-key-cache

基于hot-key-sdk/hot-key-server

camellia-redis-proxy.properties:

```properties
proxy.plugin.list=com.netease.nim.camellia.redis.proxy.hotkey.cache.plugin.HotKeyCachePlugin
hot.key.cache.enable=true
hot.key.server.local.addr=127.0.0.1:7070
hot.key.server.local.name=local
```

hot-key-server.yaml：同上

```linux
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6405 -h 127.0.0.1 -a pass123
SET: 74710.49 requests per second, p50=1.063 msec                     
GET: 59368.32 requests per second, p50=1.215 msec                    
INCR: 74660.30 requests per second, p50=1.063 msec                    
LPUSH: 74283.17 requests per second, p50=1.063 msec                    
RPUSH: 68343.36 requests per second, p50=1.127 msec                    
LPOP: 75545.81 requests per second, p50=1.063 msec                    
RPOP: 75580.08 requests per second, p50=1.063 msec                    
SADD: 66264.66 requests per second, p50=1.215 msec                    
HSET: 78542.25 requests per second, p50=1.047 msec                    
SPOP: 81182.01 requests per second, p50=1.039 msec 
LPUSH (needed to benchmark LRANGE): 79579.82 requests per second, p50=1.031 msec
LRANGE_100 (first 100 elements): 44810.90 requests per second, p50=2.007 msec
LRANGE_300 (first 300 elements): 22620.34 requests per second, p50=4.855 msec
LRANGE_500 (first 500 elements): 14684.50 requests per second, p50=8.599 msec
LRANGE_600 (first 600 elements): 13499.10 requests per second, p50=9.687 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
SET: 415110.03 requests per second, p50=0.975 msec                    
GET: 460829.47 requests per second, p50=0.807 msec 

./redis-benchmark -n 1000000 -t mset -P 16 -q -p 6405 -h 127.0.0.1 -a pass123
MSET (10 keys): 195198.12 requests per second, p50=3.375 msec
```


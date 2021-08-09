## 监控
camellia-redis-proxy提供了丰富的监控功能，包括：
* 请求tps、请求rt
* 慢查询监控
* 热key监控
* 大key监控
* 热key缓存功能
* 通过httpAPI获取监控数据
* 监控配置的动态修改
* 通过info命令获取服务器相关信息(包括后端redis集群的信息)

### 请求tps、请求rt
默认是关闭的，你可以这样打开：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: 6380 #优先级高于server.port，如果缺失，则使用server.port
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  password: pass123   #proxy的密码
  monitor-enable: true  #是否开启监控
  command-spend-time-monitor-enable: true #是否开启请求耗时的监控，只有monitor-enable=true才有效
  monitor-interval-seconds: 60 #监控回调的间隔
  monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback #监控回调类
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```
* 上面的配置表示proxy会以60s一个周期收集监控指标（请求量、请求tps），并且还会收集每个方法的耗时，统计周期是60s
* proxy通过回调的方式将统计数据进行暴露，默认的监控回调类是LoggingMonitorCallback，也就是说每隔60s，会将所有数据打印到日志文件里（你可以设置日志的目标文件）
* 当然你也可以设置自定义的MonitorCallback，从而可以自定义的处理监控数据（注：回调方法内不要有阻塞性的操作，如http请求等，如果有请在线程池内异步执行）

### 慢查询监控
* 依赖于开启监控总开关，并且开启时长监控时才生效
* 可以设置慢查询的耗时阈值
* 可以设置回调类，从而方便对接到你的监控系统中，默认的回调类会打印慢查询相关的信息到日志里（注：回调方法内不要有阻塞性的操作，如http请求等，如果有请在线程池内异步执行）
* 示例配置如下：  

```yaml
camellia-redis-proxy:
  password: pass123   #proxy的密码
  monitor-enable: true  #是否开启监控
  command-spend-time-monitor-enable: true #是否开启请求耗时的监控，只有monitor-enable=true才有效
  monitor-interval-seconds: 60 #监控回调的间隔
  slow-command-threshold-millis-time: 1000 #慢查询的阈值，单位毫秒，只有command-spend-time-monitor-enable=true才有效
  slow-command-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.spendtime.LoggingSlowCommandMonitorCallback #慢查询的回调类
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```

### 热key监控
* 单独的开关，默认关闭
* 可以设置热key的阈值，如多少毫秒内多少次请求算热key
* 可以设置回调类，从而方便对接到你的监控系统中，默认的回调类会打印热key相关的统计信息到日志里（注：回调方法内不要有阻塞性的操作，如http请求等，如果有请在线程池内异步执行）
* 示例配置如下：

```yaml
camellia-redis-proxy:
  password: pass123   #proxy的密码
  hot-key-monitor-enable: true #是否监控热key
  hot-key-monitor-config:
    check-millis: 1000 #热key的检查周期
    check-threshold: 2097152 #热key的阈值，检查周期内请求次数超过该阈值被判定为热key
    check-cache-max-capacity: 1000 #检查的计数器集合的size，本身是LRU的
    max-hot-key-count: 100 #每次回调的热key个数的最大值（前N个）
    hot-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkey.LoggingHotKeyMonitorCallback #热key的回调类
```

## 大key的监控及相关回调
* 单独的开关，默认关闭
* 可以设置各种数据结构下，怎么样才算大key
* 可以设置回调类，从而方便对接到你的监控系统中，默认的回调类会打印大key相关的信息到日志里（注：回调方法内不要有阻塞性的操作，如http请求等，如果有请在线程池内异步执行）
* 示例配置如下：

```yaml
camellia-redis-proxy:
  big-key-monitor-enable: true #大key检测
  big-key-monitor-config:
    string-size-threshold: 10 #字符串类型，value大小超过多少认为是大key
    hash-size-threshold: 2000 #hash类型，集合大小超过多少认为是大key
    zset-size-threshold: 2000 #zset类型，集合大小超过多少认为是大key
    list-size-threshold: 2000 #list类型，集合大小超过多少认为是大key
    set-size-threshold: 2000 #set类型，集合大小超过多少认为是大key
    big-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.bigkey.LoggingBigKeyMonitorCallback #大key的回调类
```

## 热key缓存功能及相关回调
* 单独的开关，默认关闭，和热key监控是两个功能
* 可以设置热key的阈值，如多少毫秒内多少次请求算热key，可以设置热key缓存多久
* 可以设置哪些key才需要缓存
* 可以设置热key缓存集合的大小，即最多缓存多少个热key
* 只支持GET命令
* 当热key被缓存后，proxy会直接返回结果而不转发到后端redis
* 在缓存过期(一半的过期时间)之前会漏过一个请求用于更新缓存（若设置5000ms过期，则2500ms的时候，会有一个请求穿透到后端redis用于更新本地缓存，其他请求仍然走本地缓存）
* 因此当该key的tps持续维持在热key的阈值之上，则触发热key缓存之后的几乎所有请求都会命中本地缓存直接返回，并且proxy会以一个较小的间隔不断的更新缓存，来保证本地缓存的时效性
* 当tps下降到热key阈值之下，则该key的所有请求会恢复为全部穿透到redis，恢复时间不超过设置的缓存过期时间
* 可以设置回调类，从而方便对接到你的监控系统中，默认的回调类会打印热key缓存命中相关的统计信息到日志里（注：回调方法内不要有阻塞性的操作，如http请求等，如果有请在线程池内异步执行）
* 示例配置如下：

```yaml
camellia-redis-proxy:
  hot-key-cache-enable: true #热key缓存开关
  hot-key-cache-config:
    counter-check-millis: 1000 #检查周期，单位毫秒
    counter-check-threshold: 100 #检查阈值，超过才算热key，才触发热key的缓存
    counter-max-capacity: 1000 #检查计数器集合的size，本身是LRU的
    need-cache-null: true #是否缓存null
    cache-max-capacity: 1000 #缓存集合的size，本身是LRU的
    cache-expire-millis: 5000 #缓存时间，单位毫秒
    hot-key-cache-stats-callback-interval-seconds: 20 #热key缓存的统计数据回调周期
    hot-key-cache-stats-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.LoggingHotKeyCacheStatsCallback #热key缓存的回调类
    hot-key-cache-key-checker-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.DummyHotKeyCacheKeyChecker #判断这个key是否需要缓存的接口
```

## 通过httpAPI获取监控数据
* 所有的监控数据都可以通过设置回调类来获取并自定义处理，此外proxy还提供了一个httpAPI来获取汇总后的监控数据
* 只要开启了上述的相关监控功能，则相关的监控数据除了通过回调类来暴露外，还会内部汇总，并允许外部通过httpAPI来访问
* 具体可见：[监控数据](monitor-data.md)

## 监控配置的动态修改
以下展示了application.yml的较完整配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: 6380 #优先级高于server.port，如果缺失，则使用server.port
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  password: pass123   #proxy的密码
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  monitor-data-mask-password: false #监控相关数据（包括日志）是否把密码隐藏，默认false（例：用***代替abc）
  monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback #监控回调类
  command-spend-time-monitor-enable: true #是否开启请求耗时的监控，只有monitor-enable=true才有效
  slow-command-threshold-millis-time: 1000 #慢查询的阈值，单位毫秒，只有command-spend-time-monitor-enable=true才有效
  slow-command-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.spendtime.LoggingSlowCommandMonitorCallback #慢查询的回调类
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomCommandInterceptor #方法拦截器
  converter-enable: false #是否开启value转换
  converter-config:
    string-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomStringConverter #string相关命令的自定义转换器
  hot-key-monitor-enable: true #是否监控热key
  hot-key-monitor-config:
    check-millis: 1000 #热key的检查周期
    check-threshold: 100 #热key的阈值，检查周期内请求次数超过该阈值被判定为热key
    check-cache-max-capacity: 1000 #检查的计数器集合的size，本身是LRU的
    max-hot-key-count: 100 #每次回调的热key个数的最大值（前N个）
    hot-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkey.LoggingHotKeyMonitorCallback #热key的回调类
  hot-key-cache-enable: true #热key缓存开关
  hot-key-cache-config:
    counter-check-millis: 1000 #检查周期，单位毫秒
    counter-check-threshold: 100 #检查阈值，超过才算热key，才触发热key的缓存
    counter-max-capacity: 1000 #检查计数器集合的size，本身是LRU的
    need-cache-null: true #是否缓存null
    cache-max-capacity: 1000 #缓存集合的size，本身是LRU的
    cache-expire-millis: 5000 #缓存时间，单位毫秒
    hot-key-cache-stats-callback-interval-seconds: 20 #热key缓存的统计数据回调周期
    hot-key-cache-stats-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.LoggingHotKeyCacheStatsCallback #热key缓存的回调类
    hot-key-cache-key-checker-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.DummyHotKeyCacheKeyChecker #判断这个key是否需要缓存的接口
  big-key-monitor-enable: true #大key检测
  big-key-monitor-config:
    string-size-threshold: 2097152 #字符串类型，value大小超过多少认为是大key
    hash-size-threshold: 2000 #hash类型，集合大小超过多少认为是大key
    zset-size-threshold: 2000 #zset类型，集合大小超过多少认为是大key
    list-size-threshold: 2000 #list类型，集合大小超过多少认为是大key
    set-size-threshold: 2000 #set类型，集合大小超过多少认为是大key
    big-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.bigkey.LoggingBigKeyMonitorCallback #大key的回调类
  transpond:
    type: local #使用本地配置
    local:
      resource: redis://@127.0.0.1:6379 #转发的redis地址
    redis-conf:
      multi-write-mode: first_resource_only #双写的模式，默认第一个地址返回就返回
      shading-func: com.netease.nim.camellia.redis.proxy.samples.CustomShadingFunc #分片函数

camellia-redis-zk-registry: #需要引入相关的依赖才有效
  enable: false #是否注册到zk
  zk-url: 127.0.0.1:2181 #zk地址
  base-path: /camellia #注册到zk的base-path
```
application.yml中的部分配置支持进程启动期间进行动态修改，详见[动态配置](dynamic-conf.md)

## 通过info命令获取服务器相关信息
proxy实现了info命令，支持返回如下信息：Server/Clients/Route/Upstream/Memory/GC/Stats/Upstream-Info  
详见[info命令](info.md)
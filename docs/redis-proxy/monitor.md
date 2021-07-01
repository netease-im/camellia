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
proxy实现了info命令，支持返回如下信息：Server/Clients/Route/Upstream/Memory/GC/Upstream-Info，含义分别表示：   
* Server 表示服务器的信息，包括proxy版本、proxy端口、操作系统类型和版本、虚拟机版本、java版本等
* Clients 会返回连接proxy的客户端连接数
* Route 路由信息，包括路由配置数量和路由配置
* Upstream 后端redis连接数
* Memory 内存
* GC 垃圾回收相关信息
* Upstream-Info 后端redis集群的信息，包括后端redis的内存使用率、版本、主从分布情况、slot分布情况等

你可以直接输入info，则返回除了Upstream-Info之外的所有信息，如下：
```
127.0.0.1:6380> info
# Server
camellia_redis_proxy_version:v1.0.31     ##proxy版本
available_processors:4      ##cpu核数
netty_boss_thread:1     ##netty的bossGroup的线程数，默认=1
netty_work_thread:4   ##netty的工作线程数，默认=cpu核数
arch:amd64   ##系统架构
os_name:Linux  ##操作系统名称                                 
os_version:4.9.0-3-amd64  ##操作系统版本
system_load_average:0.22  ##系统load
tcp_port:6380   ##proxy的服务端口
uptime_in_seconds:295   ##proxy启动时长（秒）
uptime_in_days:0    ##proxy启动时长（天）
vm_vendor:Oracle Corporation   ##虚拟机厂商
vm_name:Java HotSpot(TM) 64-Bit Server VM   ##虚拟机名称
vm_version:25.202-b08  ##虚拟机版本
jvm_info:mixed mode  ##虚拟机info
java_version:1.8.0_202  ##java版本

# Clients
connect_clients:14  ##客户端连接数

# Route   
route_nums:6  ##路由配置数量
route_conf_20_default:redis://***********@10.201.48.171:6379  ##路由配置，表示bid=1以及bgroup=default的配置
route_conf_1_default:redis-cluster://@10.177.0.64:8801,10.177.0.64:8802,10.177.0.65:8803,10.177.0.65:8804,10.177.0.66:8805,10.177.0.66:8806
route_conf_11_default:redis://***********@10.201.48.171:6379
route_conf_27_default:redis://***********@10.201.48.171:6379
route_conf_9_default:redis://***********@10.201.48.171:6379
route_conf_3_default:{"type":"simple","operation":{"read":"redis-cluster://@10.177.0.64:8801,10.177.0.64:8802,10.177.0.65:8803,10.177.0.65:8804,10.177.0.66:8805,10.177.0.66:8806","type":"rw_separate","write":{"resources":["redis-cluster://@10.177.0.64:8801,10.177.0.64:8802,10.177.0.65:8803,10.177.0.65:8804,10.177.0.66:8805,10.177.0.66:8806","redis://***********@10.201.48.171:6379"],"type":"multi"}}}

# Upstream
upstream_redis_nums:17  ##后端redis连接数
upstream_redis_nums[@10.177.0.69:8803]:4    ##后端redis连接数，具体某个ip:port的连接数
upstream_redis_nums[***********@10.201.48.171:6379]:5
upstream_redis_nums[@10.177.0.64:8802]:4
upstream_redis_nums[@10.177.0.64:8801]:4

# Memory ##proxy的内存信息
free_memory:309393192
free_memory_human:295.06M
total_memory:377487360
total_memory_human:360.00M
max_memory:7635730432
max_memory_human:7.11G
heap_memory_init:536870912
heap_memory_init_human:512.00M
heap_memory_used:68094168
heap_memory_used_human:64.94M
heap_memory_max:7635730432
heap_memory_max_human:7.11G
heap_memory_committed:377487360
heap_memory_committed_human:360.00M
non_heap_memory_init:2555904
non_heap_memory_init_human:2.44M
non_heap_memory_used:32339792
non_heap_memory_used_human:30.84M
non_heap_memory_max:-1
non_heap_memory_max_human:-1B
non_heap_memory_committed:33882112
non_heap_memory_committed_human:32.31M

# GC  ##GC相关信息
young_gc_name:G1 Young Generation  ##young gc的回收器类型
young_gc_collection_count:6  ##young gc累计次数
young_gc_collection_time:97  ##young gc累计时间
old_gc_name:G1 Old Generation  ##old gc的回收器类型
old_gc_collection_count:0  ##old gc累计次数
old_gc_collection_time:0  ##old gc累计时长

```

你也可以只打印其中一项，比如只想看内存信息，则如下（info后面的参数是忽略大小写的）：
```
127.0.0.1:6380> info memory
# Memory
free_memory:309393192
free_memory_human:295.06M
total_memory:377487360
total_memory_human:360.00M
max_memory:7635730432
max_memory_human:7.11G
heap_memory_init:536870912
heap_memory_init_human:512.00M
heap_memory_used:68094168
heap_memory_used_human:64.94M
heap_memory_max:7635730432
heap_memory_max_human:7.11G
heap_memory_committed:377487360
heap_memory_committed_human:360.00M
non_heap_memory_init:2555904
non_heap_memory_init_human:2.44M
non_heap_memory_used:32339792
non_heap_memory_used_human:30.84M
non_heap_memory_max:-1
non_heap_memory_max_human:-1B
non_heap_memory_committed:33882112
non_heap_memory_committed_human:32.31M

```

特别的，Upstream-Info必须是指定之后才能返回（此时返回的是默认路由），如下：  
```
127.0.0.1:6381> info upstream-info
# Upstream-Info
route_conf:{"type":"shading","operation":{"operationMap":{"0-2-4":{"read":"redis-sentinel-slaves://@127.0.0.1:26379/master1?withMaster=true","type":"rw_separate","write":"redis-sentinel://@127.0.0.1:26379/master1"},"1-3-5":"redis-cluster://@10.189.28.62:7008,10.189.28.60:7001,10.189.28.62:7011"},"bucketSize":6}}
upstream_cluster_count:3  ##本路由后端redis集群数量
upstream0_url:redis-cluster://@10.189.28.62:7008,10.189.28.60:7001,10.189.28.62:7011  ##本路由包含的redis集群地址1
upstream1_url:redis-sentinel://@127.0.0.1:26379/master1    ##本路由包含的redis集群地址2
upstream2_url:redis-sentinel-slaves://@127.0.0.1:26379/master1?withMaster=true  ##本路由包含的redis集群地址3

## Upstream0  ##本路由包含的某个redis集群相关的信息
url:redis-cluster://@10.189.28.62:7008,10.189.28.60:7001,10.189.28.62:7011  ##地址，这是一个redis-cluster
### redis-cluster-info  
cluster_state:ok    ##集群状态
cluster_slots_assigned:16384   ##集群分配的slot数量
cluster_slots_ok:16384   ##集群状态ok的slot数量
cluster_slots_pfail:0  ##pfail状态的slot数量
cluster_slots_fail:0  ##fail状态的slot数量
cluster_known_nodes:6  ##集群的节点数
cluster_size:3  ##集群大小，即主节点数量
cluster_safety:yes    ## 超过一半的主节点在同一个ip下，在判定为不安全
cluster_maxmemory:9663676416  ##集群总内存大小
cluster_maxmemory_human:9.00G  ##集群总内存大小（可读性）
cluster_used_memory:2304452928  ##集群已用内存大小
cluster_used_memory_human:2.15G  ##集群已用内存大小（可读性）
cluster_memory_used_rate:0.2384654482205709  ##集群内存使用率
cluster_memory_used_rate_human:23.87%   ##集群内存使用率（百分比）
### redis-cluster-node-info  ##集群节点信息（主从节点分别情况、slot分布情况、内存使用情况）
node0:master=10.189.28.62:7008@17008,slave=[10.189.28.60:7003@17003],slots=5461-10922,maxmemory=3.00G,used_memory=733.38M,memory_used_rate=23.87%
node1:master=10.189.28.60:7001@17001,slave=[10.189.28.62:7010@17010],slots=10923-16383,maxmemory=3.00G,used_memory=733.38M,memory_used_rate=23.87%
node2:master=10.189.28.62:7011@17011,slave=[10.189.28.62:7009@17009],slots=0-5460,maxmemory=3.00G,used_memory=733.38M,memory_used_rate=23.87%
### redis-node-info
#### node0  ##节点信息
master_url=10.189.28.62:7008@17008  ##节点url
redis_version:4.0.9  ##redis版本
used_memory:768150976  ##已用内存大小
used_memory_human:732.57M  ##已用内存大小（可读性）
maxmemory:3221225472  ##最大内存
maxmemory_human:3.00G  ##最大内存（可读性）
memory_used_rate:0.2384654482205709  ##内存使用率
memory_used_rate_human:23.85% ##内存使用率（百分比）
maxmemory_policy:allkeys-lru  ##key淘汰策略
hz:10  ##hz，提高它的值将会占用更多的cpu,当然相应的redis将会更快的处理同时到期的许多key,以及更精确的去处理超时
role:master  ##节点类型
connected_slaves:1  ##从节点数量
slave0:ip=10.189.28.60,port=7003,state=online,offset=88682163709,lag=0  ##从节点信息
db0:keys=3639485,expires=3639482,avg_ttl=1621629354933212  ##key的分布情况（数量、带ttl的数量、平均ttl）
#### node1
master_url=10.189.28.60:7001@17001
redis_version:4.0.9
used_memory:768150976
used_memory_human:3.77M
maxmemory:3221225472
maxmemory_human:3.00G
memory_used_rate:0.2384654482205709
memory_used_rate_human:23.85%
maxmemory_policy:allkeys-lru
hz:10
role:master
connected_slaves:1
slave0:ip=10.189.28.62,port=7010,state=online,offset=253642106463,lag=0
db0:keys=297,expires=294,avg_ttl=62496349
#### node2
master_url=10.189.28.62:7011@17011
redis_version:4.0.9
used_memory:768150976
used_memory_human:732.05M
maxmemory:3221225472
maxmemory_human:3.00G
memory_used_rate:0.2382984757423401
memory_used_rate_human:23.83%
maxmemory_policy:allkeys-lru
hz:10
role:master
connected_slaves:1
slave0:ip=10.189.28.62,port=7009,state=online,offset=186569832085,lag=1
db0:keys=3634796,expires=3634791,avg_ttl=1621629354943862

## Upstream1
url:redis-sentinel://@127.0.0.1:26379/master1
### redis-node-info
master_url:@127.0.0.1:6380
redis_version:6.0.6
used_memory:2437680
used_memory_human:2.32M
maxmemory:3221225472
maxmemory_human:3.00G
memory_used_rate:0.23873232305049896
memory_used_rate_human:23.87%
maxmemory_policy:noeviction
hz:10
role:master
connected_slaves:1
slave0:ip=127.0.0.1,port=6379,state=online,offset=570473,lag=1
db0:keys=12231212,expires=3634791,avg_ttl=123444

## Upstream2
url:redis-sentinel-slaves://@127.0.0.1:26379/master1?withMaster=true
### redis-node-info
master_url:@127.0.0.1:6380
redis_version:6.0.6
used_memory:2437680
used_memory_human:2.32M
maxmemory:3.00G
maxmemory_human:3.00G
memory_used_rate:0.23873232305049896
memory_used_rate_human:23.87%
maxmemory_policy:noeviction
hz:10
role:master
connected_slaves:1
slave0:ip=127.0.0.1,port=6379,state=online,offset=570473,lag=1
db0:keys=12231212,expires=3634791,avg_ttl=123444

```

此外，如果proxy上配置了多条路由，那么你可以指定bid和bgroup返回特定路由的后端upstream信息，如下（例子中返回了bid=1以及bgroup=default的后端信息）：
```
127.0.0.1:6381> info upstream-info 1 default
# Upstream-Info
route_conf:redis-sentinel://@127.0.0.1:26379/master1
bid:1
bgroup:default
upstream_cluster_count:1

## Upstream0
url:redis-sentinel://@127.0.0.1:26379/master1
### redis-node-info
master_url:@127.0.0.1:6380
redis_version:6.0.6
used_memory:2503216
used_memory_human:2.39M
maxmemory:3221225472
maxmemory_human:3.00G
memory_used_rate:0.23873232305049896
memory_used_rate_human:23.87%
maxmemory_policy:noeviction
hz:10
role:master
connected_slaves:1
slave0:ip=127.0.0.1,port=6379,state=online,offset=570473,lag=1
db0:keys=12231212,expires=0,avg_ttl=123444
```
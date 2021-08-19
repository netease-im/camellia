## info命令获取相关信息示例
proxy实现了info命令，支持返回如下信息：Server/Clients/Route/Upstream/Memory/GC/Stats/Upstream-Info，含义分别表示：   
* Server 表示服务器的信息，包括proxy版本、proxy端口、操作系统类型和版本、虚拟机版本、java版本等
* Clients 会返回连接proxy的客户端连接数
* Route 路由信息，包括路由配置数量和路由配置
* Upstream 后端redis连接数
* Memory 内存
* GC 垃圾回收相关信息
* Stats 统计信息（请求次数、QPS等）
* Upstream-Info 后端redis集群的信息，包括后端redis的内存使用率、版本、主从分布情况、slot分布情况等

你可以直接输入info，则返回除了Upstream-Info之外的所有信息，如下：
```
127.0.0.1:6380> info
# Server
camellia_redis_proxy_version:v1.0.36     ##proxy版本
available_processors:4      ##cpu核数
netty_boss_thread:1     ##netty的bossGroup的线程数，默认=1
netty_work_thread:4   ##netty的工作线程数，默认=cpu核数
arch:amd64   ##系统架构
os_name:Linux  ##操作系统名称                                 
os_version:4.9.0-3-amd64  ##操作系统版本
system_load_average:0.22  ##系统load
tcp_port:6380   ##proxy的服务端口
http_console_port:16379   ##proxy的console端口
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

# Stats
commands.count:4158008   ##proxy启动至今的请求数
read.commands.count:928037   ##proxy启动至今的读请求数
write.commands.count:3229970   ##proxy启动至今的写请求数
avg.commands.qps:34183.18138097157   ##proxy启动至今的平均QPS
avg.read.commands.qps:7629.436282771151    ##proxy启动至今的平均读QPS
avg.write.commands.qps:26553.73687715289   ##proxy启动至今的平均写QPS
last.commands.qps:29304.433333333334   ##proxy上一个统计周期的QPS
last.read.commands.qps:6426.05   ##proxy上一个统计周期的读QPS
last.write.commands.qps:22878.383333333335   ##proxy上一个统计周期的写QPS

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
cluster_safety:yes    ## 超过一半的主节点在同一个ip下，则判定为不安全
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
route_conf:redis://@127.0.0.1:6379
bid:1
bgroup:default
upstream_cluster_count:1
upstream0_url:redis://@127.0.0.1:6379

## Upstream0
url:redis://@127.0.0.1:6379
### redis-node-info
redis_version:6.0.6
used_memory:2082800
used_memory_human:1.99M
maxmemory:0
maxmemory_human:0B
memory_used_rate:0.0
memory_used_rate_human:0.00%
maxmemory_policy:noeviction
hz:10
role:master
connected_slaves:0
db0:keys=39,expires=0,avg_ttl=0
```
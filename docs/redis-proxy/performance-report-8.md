## 性能测试

### 机器配置

|机器|规格|参数
|:---:|:---:|:---:|
|redis-proxy|4C8G云主机 |-Xms4096m -Xmx4096m -XX:+UseG1GC|
|redis cluster|4C8G云主机 |单机混部，3台机器部署了6主6从的集群|
|压测机|4C8G云主机 |5台压测机，使用jedis作为客户端，每台10个线程，每个线程占用一个连接，无间隔请求，压榨proxy的极限性能||

### 测试方法
* key是固定前缀+数字，长度范围9字符到14字符，value是固定的30字符的随机串
* setex，1w个key，随机选择1个key进行写入，连续运行5分钟
* get，1w个key，随机选择1个key读取，每个key都有value结果返回，连续运行5分钟
* mget，1w个key，随机选择30个key读取，每个key都有value结果返回，连续运行5分钟
* pipelineGet，1w个key，随机选择30个key读取，每个key都有value结果返回，连续运行5分钟

### proxy配置
```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
  transpond:
    type: local
    local:
      resource: redis-cluster://@10.189.28.60:7000,10.189.28.61:7001,10.189.28.62:7002
```

### 单机性能上限(不开启监控的情况下)
|命令|1.0.18|1.0.19|性能提升|
|:---:|:---:|:---:|:---:|
|setex|61817.61|79379.84|28.4%|
|get|63071.87|83256.5|32%|
|mget|13282.05|20703.62|55.9%|
|pipline get|5776.47|8239.85|42.6%|

### 单机性能上限（开启监控的情况下）
|开启的监控|setex||get||mget||pipelineGet||
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
||tps|比例|tps|比例|tps|比例|tps|比例|
|无监控|79379.84|-|83256.5|-|20703.62|-|8239.85|-|
|仅基础监控|75797.43|-4.7%|80216.09|-3.8%|19832.83|-4.4%|7955.48|-3.6%|
|仅大key监控|74398.03|-6.7%|78209.01|-6.5%|19785.53|-4.6%|8072.62|-2.1%|
|仅热key缓存功能（不命中）|73973.5|-7.3%|77941.27|-6.8%|19393.54|-6.4%|7988.35|-3.1%|
|仅基础监控+耗时监控+慢查询监控|73735.42|-7.7%|77146.23|-7.9%|19466.83|-6.8%|7809.15|-5.5%|
|仅热key监控|72204.54|-9.9%|74529.84|-11.7%|16692.83|-24.0%|7643.37|-7.8%|
|所有监控均开启|71308.11|-11.3%|71688.82|-16.1%|16980.51|-21.9%|7248.74|-13.7%|

### 和其他开源实现比较

|proxy类型|语言|版本|redis-cluster|redis-sentinel|阻塞式命令|pubsub命令|streams命令|读写分离|自定义分片|双写|动态配置|管控/监控相关|
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
|camellia|java|1.0.19|支持|支持|支持|支持|支持|支持|redis-cluster/redis-sentinel/redis-standalone任意组合|支持|支持|连接数<br>请求量（tps）<br>请求rt<br>热key监控<br>大key监控<br>慢查询监控<br>命令拦截<br>密码|
|predixy|c++|1.0.5|支持|支持|支持|支持|不支持|支持|redis-sentinel或redis-standalone支持分片|不支持|不支持|CPU<br>Memory<br>Requests/Responses<br>Latency<br>readonly/readwrite/admin permission<br>密码|
|官方redis-cluster-proxy|c|1.0-beta2|支持|不支持|支持|支持|支持|不支持|不支持|不支持|不支持|ACL|
|bilibili-overlord|go|1.9.4|支持|支持|不支持|不支持|不支持|不支持|redis-sentinel或redis-standalone支持分片|不支持|支持|-|



* predixy配置(版本：1.0.5，测试了多次，最终WorkerThreads设置为3获取了最高的性能)  

predixy.conf  
```
Name PredixyExample

Bind 0.0.0.0:6380

WorkerThreads 3

ClientTimeout 300

LogVerbSample 0
LogDebugSample 0
LogInfoSample 10000
LogNoticeSample 1
LogWarnSample 1
LogErrorSample 1

Include auth.conf
Include cluster.conf
```
cluster.conf
```
ClusterServerPool {
    MasterReadPriority 60
    StaticSlaveReadPriority 50
    DynamicSlaveReadPriority 50
    RefreshInterval 1
    ServerTimeout 1
    ServerFailureLimit 10
    ServerRetryTimeout 1
    KeepAlive 120
    Servers {
        + 10.189.28.60:7000
        + 10.189.28.61:7001
        + 10.189.28.62:7002
    }
}
```

* 官方redis-cluster-proxy配置（版本：1.0-beta2）
```
./redis-cluster-proxy --port 6380 --enable-cross-slot 10.189.28.60:7000 10.189.28.61:7001
```
* bilibili-overlord配置（版本：1.9.4，使用了官方推荐的node_connections=2）

overlord.toml
```
[[clusters]]
# This be used to specify the name of cache cluster.
name = "test-redis-cluster"
# The name of the hash function. Possible values are: sha1.
hash_method = "fnv1a_64"
# The key distribution mode. Possible values are: ketama.
hash_distribution = "ketama"
# A two character string that specifies the part of the key used for hashing. Eg "{}".
hash_tag = "{}"
# cache type: memcache | memcache_binary | redis | redis_cluster
cache_type = "redis_cluster"
# proxy listen proto: tcp | unix
listen_proto = "tcp"
# proxy listen addr: tcp addr | unix sock path
listen_addr = "0.0.0.0:6380"
# Authenticate to the Redis server on connect.
redis_auth = ""
# The dial timeout value in msec that we wait for to establish a connection to the server. By default, we wait indefinitely.
dial_timeout = 1000
# The read timeout value in msec that we wait for to receive a response from a server. By default, we wait indefinitely.
read_timeout = 1000
# The write timeout value in msec that we wait for to write a response to a server. By default, we wait indefinitely.
write_timeout = 1000
# The number of connections that can be opened to each server. By default, we open at most 1 server connection.
node_connections = 2
# The number of consecutive failures on a server that would lead to it being temporarily ejected when auto_eject is set to true. Defaults to 3.
ping_fail_limit = 3
# A boolean value that controls if server should be ejected temporarily when it fails consecutively ping_fail_limit times.
ping_auto_eject = false

slowlog_slower_than = 10
# A list of server address, port (name:port or ip:port) for this server pool when cache type is redis_cluster.
servers = [
    "10.189.28.60:7000",
    "10.189.28.61:7001",
]
```

* 测试结果

|proxy类型|语言|setex||get||mget||pipelineGet||
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
|||tps|比例|tps|比例|tps|比例|tps|比例|
|camellia-redis-proxy v1.0.19无监控|java|79379.84|-|83256.5|-|20703.62|-|8239.85|-|
|predixy|c++|96532.42|21.6%|101165.94|21.5%|29582.98|42.9%|31087.22|277.3%|
|官方redis-cluster-proxy|c|87203.55|9.9%|90314.03|8.5%|6898.63|-66.7%|7512.78|-8.8%|
|bilibili-overlord|go|69815|-12.0%|74328.19|-10.7%|15971.57|-22.9%|11909.06|44.5%|
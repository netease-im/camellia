## 性能测试

### 机器配置

|机器|规格|备注
|:---:|:---:|:---:|
|redis-proxy|4C8G云主机 ||
|redis cluster|4C8G云主机 |单机混部，3台机器部署了6主6从的集群|
|压测机|4C8G云主机 |||

### proxy配置
#### camellia-redis-proxy配置(1.0.37)
```
-Xms4096m -Xmx4096m -XX:+UseG1GC
```

```yaml
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
      resource: redis-cluster://@nim-redis-perftest-jd-1.v1.yunxin.jd1.vpc:7000,nim-redis-perftest-jd-2.v1.yunxin.jd1.vpc:7006,nim-redis-perftest-jd-3.v1.yunxin.jd1.vpc:7010
```

#### predixy配置(版本：1.0.5，WorkerThreads设置为4获取了最高的性能)  

predixy.conf  
```
Name PredixyExample

Bind 0.0.0.0:6380

WorkerThreads 4

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
        + nim-redis-perftest-jd-1.v1.yunxin.jd1.vpc:7000
        + nim-redis-perftest-jd-2.v1.yunxin.jd1.vpc:7006
    }
}
```

#### 官方redis-cluster-proxy配置（版本：1.0-beta2）
```
./redis-cluster-proxy --port 6380 --enable-cross-slot nim-redis-perftest-jd-1.v1.yunxin.jd1.vpc:7000 nim-redis-perftest-jd-2.v1.yunxin.jd1.vpc:7006
```

#### bilibili-overlord配置（版本：1.9.4，使用了官方推荐的node_connections=2）
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
    "nim-redis-perftest-jd-1.v1.yunxin.jd1.vpc:7000",
    "nim-redis-perftest-jd-2.v1.yunxin.jd1.vpc:7006",
]
```

### 测试结果

````
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h 10.189.28.132

camellia-redis-proxy
SET: 92064.07 requests per second, p50=1.127 msec
GET: 81546.12 requests per second, p50=1.223 msec
INCR: 81512.88 requests per second, p50=1.247 msec
LPUSH: 87214.38 requests per second, p50=1.215 msec
RPUSH: 82189.53 requests per second, p50=1.327 msec
LPOP: 90277.16 requests per second, p50=1.207 msec
RPOP: 85287.84 requests per second, p50=1.295 msec
SADD: 94921.69 requests per second, p50=1.263 msec
HSET: 91124.48 requests per second, p50=1.271 msec
SPOP: 80173.18 requests per second, p50=1.343 msec
LPUSH (needed to benchmark LRANGE): 82501.45 requests per second, p50=1.287 msec
LRANGE_100 (first 100 elements): 49183.55 requests per second, p50=2.215 msec
LRANGE_300 (first 300 elements): 18384.72 requests per second, p50=5.559 msec
LRANGE_500 (first 500 elements): 11434.98 requests per second, p50=9.223 msec
LRANGE_600 (first 600 elements): 8238.1 requests per second, p50=19.337 msec

predixy
SET: 88707.53 requests per second, p50=1.263 msec
GET: 86692.67 requests per second, p50=1.295 msec
INCR: 94037.99 requests per second, p50=1.159 msec
LPUSH: 100441.94 requests per second, p50=1.135 msec
RPUSH: 84702.70 requests per second, p50=1.327 msec
LPOP: 87191.56 requests per second, p50=1.311 msec
RPOP: 97323.60 requests per second, p50=1.159 msec
SADD: 91954.02 requests per second, p50=1.191 msec
HSET: 98347.76 requests per second, p50=1.183 msec
SPOP: 96079.94 requests per second, p50=1.159 msec
LPUSH (needed to benchmark LRANGE): 85969.74 requests per second, p50=1.295 msec
LRANGE_100 (first 100 elements): 49541.74 requests per second, p50=2.183 msec
LRANGE_300 (first 300 elements): 18672.74 requests per second, p50=5.495 msec
LRANGE_500 (first 500 elements): 11521.93 requests per second, p50=9.175 msec
LRANGE_600 (first 600 elements): 9119.01 requests per second, p50=19.087 msec

overlord
SET: 85667.78 requests per second, p50=1.519 msec
GET: 86363.24 requests per second, p50=1.463 msec
INCR: 88652.48 requests per second, p50=1.527 msec
LPUSH: 78939.05 requests per second, p50=1.927 msec
RPUSH: 80749.35 requests per second, p50=1.935 msec
LPOP: 85142.62 requests per second, p50=1.687 msec
RPOP: 83605.05 requests per second, p50=1.799 msec
SADD: 86767.90 requests per second, p50=1.551 msec
HSET: 68799.45 requests per second, p50=2.439 msec
SPOP: 87657.78 requests per second, p50=1.455 msec
LPUSH (needed to benchmark LRANGE): 79884.97 requests per second, p50=2.023 msec
LRANGE_100 (first 100 elements): 23806.12 requests per second, p50=7.967 msec
LRANGE_300 (first 300 elements): 9316.97 requests per second, p50=20.607 msec
LRANGE_500 (first 500 elements): 5792.30 requests per second, p50=33.375 msec
LRANGE_600 (first 600 elements): 4504.0 requests per second, p50=42.907 msec

redis-cluster-proxy
SET: 95785.45 requests per second, p50=1.167 msec
GET: 84196.34 requests per second, p50=1.311 msec
INCR: 88683.93 requests per second, p50=1.287 msec
LPUSH: 90464.99 requests per second, p50=1.319 msec
RPUSH: 86685.16 requests per second, p50=1.351 msec
LPOP: 95347.06 requests per second, p50=1.287 msec
RPOP: 87519.70 requests per second, p50=1.367 msec
SADD: 86565.09 requests per second, p50=1.327 msec
HSET: 91257.52 requests per second, p50=1.343 msec
SPOP: 98599.88 requests per second, p50=1.199 msec
LPUSH (needed to benchmark LRANGE): 96506.46 requests per second, p50=1.279 msec
LRANGE_100 (first 100 elements): 48704.46 requests per second, p50=2.319 msec
LRANGE_300 (first 300 elements): 19223.01 requests per second, p50=5.599 msec
LRANGE_500 (first 500 elements): 11977.91 requests per second, p50=8.991 msec
LRANGE_600 (first 600 elements): 9968.60 requests per second, p50=11.455 msec

````
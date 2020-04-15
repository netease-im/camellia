
## 代理到redis-cluster

|机器|规格|参数
|:---:|:---:|:---:|
|压测机|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|JedisPool: minIdle=0,maxIdle=10,maxTotal=20,maxWaitMillis=1000,maxAttempts=5,timeout=1000|
|camellia-redis-proxy-sync|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|camellia-redis-proxy-async|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|bilibili-overlord|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|B站开源的proxy，使用Go实现，https://github.com/bilibili/overlord|
|predixy|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|一款C++开发的proxy，https://github.com/joyieldInc/predixy|
|redis cluster|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单机混部，3主3从|

```
camellia-sync配置：

camellia-redis-proxy:
  type: sync
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005

proxy-async配置：

camellia-redis-proxy:
  type: async
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005

bilibili-overlord配置：

[[clusters]]

name = "test-mc"

hash_method = "fnv1a_64"
hash_distribution = "ketama"
hash_tag = ""
cache_type = "redis_cluster"
listen_proto = "tcp"
listen_addr = "0.0.0.0:16379"

redis_auth = ""

dial_timeout = 1000
read_timeout = 1000
write_timeout = 1000
#官方认定的最优值，测试结果也类似
node_connections = 2
ping_fail_limit = 3
ping_auto_eject = true

servers = [
    #"127.0.0.1:11211:1 mc1",
    "10.196.8.94:7000:1",
    "10.196.8.94:7001:1",
    "10.196.8.94:7002:1",
    "10.196.8.94:7003:1",
    "10.196.8.94:7004:1",
    "10.196.8.94:7005:1"
]


predixy配置：

#测试了几次选择的最优值
WorkerThreads 2 

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
      + 10.196.8.94:7000
      + 10.196.8.94:7001
      + 10.196.8.94:7002
      + 10.196.8.94:7003
      + 10.196.8.94:7004
      + 10.196.8.94:7005
    }
}


```
测试结果一（使用NPT平台）：  

|命令|参数|模式|tps|mrt|system|
|:---:|:---:|:---:|:---:|:---:|:---:|
|get|单并发，5分钟|直连redis cluster（使用JedisCluster）|16971.76|0.05||
|||camellia-redis-proxy-sync|8493.01|0.11|上下文切换增加3.55w|
|||camellia-redis-proxy-async|7148.67|0.13|上下文切换增加5.86w|
|||bilibili-overlord|8173.81|0.12|上下文切换增加10.05w|
|||predixy|9831.14|0.09|上下文切换增加4.11w|
|get|10并发，5分钟|直连redis cluster（使用JedisCluster）|107825.47|0.05||
|||camellia-redis-proxy-sync|75969.22|0.12|上下文切换增加32.25w|
|||camellia-redis-proxy-async|62355.57|0.15|上下文切换增加38.3w|
|||bilibili-overlord|62655.59|0.14|上下文切换增加23.8w|
|||predixy|75251.83|0.12|上下文切换增加7.87w|
|get|120并发（3台压测机，每台的连接池配置40，起40个线程），5分钟|直连redis cluster（使用JedisCluster）|265604.85|0.17|三台redis的cpu=95%|
|||camellia-redis-proxy-sync|262536.84|0.28|上下文切换增加74.41w，三台redis的cpu=82%|
|||camellia-redis-proxy-async|242234.8|0.42|上下文切换增加80.5w，三台redis的cpu=36%|
|||bilibili-overlord|261707.02|0.24|上下文切换增加60.5w，三台redis的cpu=30%|
|||predixy|165759.71|0.68|上下文切换增加0.68w，三台redis的cpu=14%|
|setex|key/value=30字节/30字节，单并发，5分钟|直连redis cluster（使用JedisCluster）|13593.91|0.07||
|||camellia-redis-proxy-sync|7414.64|0.13|上下文切换增加3.04w|
|||camellia-redis-proxy-async|6443.97|0.15|上下文切换增加5.33w|
|||bilibili-overlord|7071.5|0.13|上下文切换增加8.83w|
|||predixy|8244.26|0.12|上下文切换增加3.41w|
|setex|key/value=30字节/30字节，10并发，5分钟|直连redis cluster（使用JedisCluster）|73568.05|0.11||
|||camellia-redis-proxy-sync|54472.02|0.17|上下文切换增加27.31w|
|||camellia-redis-proxy-async|47076.56|0.2|上下文切换增加30.45w|
|||bilibili-overlord|45666.69|0.2|上下文切换增加19.68w|
|||predixy|52951.29|0.17|上下文切换增加7.9w|
|pipeline-get|每次20个get，单并发，5分钟|直连redis cluster（使用CamelliaRedisTemplate）|10310.61|0.09||
|||camellia-redis-proxy-sync|3863.38|0.25|上下文切换增加6.87w|
|||camellia-redis-proxy-async|3596.64|0.27|上下文切换增加6.16w|
|||bilibili-overlord|4048.25|0.24|上下文切换增加14.12w|
|||predixy|5235.84|0.18|上下文切换增加2.75w|
|pipeline-get|每次20个get，10并发，5分钟|直连redis cluster（使用CamelliaRedisTemplate）|47553.13|0.18||
|||camellia-redis-proxy-sync|30010.11|0.33|上下文切换增加52.95w|
|||camellia-redis-proxy-async|26301.92|0.36|上下文切换增加24.76w|
|||bilibili-overlord|26877.37|0.36|上下文切换增加17.49w|
|||predixy|29696.02|0.32|上下文切换增加2.12w|
|pipeline-get|每次20个get，120并发（3台压测机，每台的连接池配置40，起40个线程），5分钟|直连redis cluster（使用CamelliaRedisTemplate）|61156.56|1.91|三台redis的cpu=100%|
|||camellia-redis-proxy-sync|62424.56|1.9|上下文切换增加96w，三台redis的cpu=98%|
|||camellia-redis-proxy-async|41800.08|2.84|上下文切换增加28.74w，三台redis的cpu=50%|
|||bilibili-overlord|39068.63|3.05|上下文切换增加17.3w，三台redis的cpu=40%|
|||predixy|37493.87|3.18|上下文切换增加17.16w，三台redis的cpu=33%|
|mget|每次20个key的mget，单并发，5分钟|直连redis cluster（使用CamelliaRedisTemplate）|9946.66|0.1||
|||camellia-redis-proxy-sync|5234.8|0.18|上下文切换增加9.25w|
|||camellia-redis-proxy-async|4542.86|0.21|上下文切换增加7.44w|
|||bilibili-overlord|4248.38|0.23|上下文切换增加14.42w|
|||predixy|5483.14|0.18|上下文切换增加2.59w|
|mget|每次20个key的mget，10并发，5分钟|直连redis cluster（使用CamelliaRedisTemplate）|42902.42|0.21||
|||camellia-redis-proxy-sync|34068.73|0.28|上下文切换增加59.98w|
|||camellia-redis-proxy-async|30070.06|0.32|上下文切换增加26.89w|
|||bilibili-overlord|26160.65|0.37|上下文切换增加16.74w|
|||predixy|27953.5|0.35|上下文切换增加2.55w|
|mget|每次20个key的mget，120并发（3台压测机，每台的连接池配置40，起40个线程），5分钟|直连redis cluster（使用CamelliaRedisTemplate）|60505.79|1.93|三台redis的cpu=100%|
|||camellia-redis-proxy-sync|62565.98|1.89|上下文切换增加96.97w，三台redis的cpu=98%|
|||camellia-redis-proxy-async|66833.19|1.78|上下文切换增加39.26w，三台redis的cpu=66%|
|||bilibili-overlord|39161.77|3.04|上下文切换增加16.52w，三台redis的cpu=42%|
|||predixy|36631.81|3.25|上下文切换增加0.2w，三台redis的cpu=33%|
|mset|key/value=30字节/30字节，每次20个key，单并发，5分钟|直连redis cluster（使用CamelliaRedisTemplate）|6306.15|0.14||
|||camellia-redis-proxy-sync|3985.33|0.24|上下文切换增加7.34w|
|||camellia-redis-proxy-async|3447.41|0.27|上下文切换增加5.69w|
|||bilibili-overlord|3439.42|0.28|上下文切换增加12.18w|
|||predixy|3825|0.25|上下文切换增加2.21w|
|mset|key/value=30字节/30字节，每次20个key，10并发，5分钟|直连redis cluster（使用CamelliaRedisTemplate）|23110.54|0.4||
|||camellia-redis-proxy-sync|21586.39|0.44|上下文切换增加41.65w|
|||camellia-redis-proxy-async|20716.66|0.46|上下文切换增加22.03w|
|||bilibili-overlord|15013.02|0.51|上下文切换增加14.38w|
|||predixy|19629.06|0.49|上下文切换增加3.61w|


测试结果二（使用redis-benchmark，测试多次选择较好的一次）： 

```
单命令
redis-benchmark -n 1000000 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 16379

predixy（C++）  
SET: 93826.23 requests per second
GET: 97924.02 requests per second
INCR: 97981.58 requests per second
LPUSH: 95428.95 requests per second
RPUSH: 95374.34 requests per second
LPOP: 96385.54 requests per second
RPOP: 96255.66 requests per second
SADD: 98000.78 requests per second
HSET: 98970.70 requests per second
SPOP: 97732.60 requests per second
LPUSH (needed to benchmark LRANGE): 96190.84 requests per second
LRANGE_100 (first 100 elements): 37560.09 requests per second
LRANGE_300 (first 300 elements): 13464.39 requests per second
LRANGE_500 (first 450 elements): 9532.07 requests per second
LRANGE_600 (first 600 elements): 7345.81 requests per second

bilibili-overlord（Go）  
SET: 93005.95 requests per second
GET: 92729.97 requests per second
INCR: 92259.44 requests per second
LPUSH: 93327.11 requests per second
RPUSH: 92876.38 requests per second
LPOP: 93327.11 requests per second
RPOP: 90727.63 requests per second
SADD: 93283.58 requests per second
HSET: 93764.65 requests per second
SPOP: 93484.16 requests per second
LPUSH (needed to benchmark LRANGE): 92807.43 requests per second
LRANGE_100 (first 100 elements): 26952.00 requests per second
LRANGE_300 (first 300 elements): 9919.55 requests per second
LRANGE_500 (first 450 elements): 6726.94 requests per second
LRANGE_600 (first 600 elements): 5133.63 requests per second

camellia-redis-proxy-async（Java）  
SET: 85012.33 requests per second
GET: 85375.22 requests per second
INCR: 81659.31 requests per second
LPUSH: 83970.10 requests per second
RPUSH: 82263.90 requests per second
LPOP: 86625.09 requests per second
RPOP: 84104.29 requests per second
SADD: 85741.23 requests per second
HSET: 84224.71 requests per second
SPOP: 80205.32 requests per second
LPUSH (needed to benchmark LRANGE): 82284.21 requests per second
LRANGE_100 (first 100 elements): 35187.73 requests per second
LRANGE_300 (first 300 elements): 12700.35 requests per second
LRANGE_500 (first 450 elements): 8987.07 requests per second
LRANGE_600 (first 600 elements): 6935.01 requests per second

camellia-redis-proxy-sync（Java）  
SET: 87750.09 requests per second
GET: 88849.40 requests per second
INCR: 86888.52 requests per second
LPUSH: 89031.34 requests per second
RPUSH: 89645.90 requests per second
LPOP: 88936.32 requests per second
RPOP: 90057.63 requests per second
SADD: 90106.33 requests per second
HSET: 88991.72 requests per second
SPOP: 89269.77 requests per second
LPUSH (needed to benchmark LRANGE): 89453.44 requests per second
LRANGE_100 (first 100 elements): 36965.84 requests per second
LRANGE_300 (first 300 elements): 12847.03 requests per second
LRANGE_500 (first 450 elements): 9042.08 requests per second
LRANGE_600 (first 600 elements): 6964.22 requests per second

```


```
pipeline
redis-benchmark -n 1000000 -t set,get -P 16 -q -p 16379

predixy（C++）  
SET: 471920.72 requests per second
GET: 825082.50 requests per second

bilibili-overlord（Go）
SET: 164122.77 requests per second
GET: 208637.59 requests per second

camellia-redis-proxy-async（Java）
SET: 465766.16 requests per second
GET: 525486.06 requests per second

camellia-redis-proxy-sync（Java）
SET: 411353.34 requests per second
GET: 557413.56 requests per second

```

```
mset
redis-benchmark -n 1000000 -t mset -q -p 16379

predixy（C++）  
MSET (10 keys): 43557.80 requests per second

bilibili-overlord（Go）
MSET (10 keys): 13468.19 requests per second

camellia-redis-proxy-async（Java）
MSET (10 keys): 47187.62 requests per second

camellia-redis-proxy-sync（Java）
MSET (10 keys): 35027.50 requests per second

```
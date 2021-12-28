
## 分片

|机器|规格|参数
|:---:|:---:|:---:|
|camellia-redis-proxy-sync|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|camellia-redis-proxy-async|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|redis cluster|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单机混部，3主3从|
|redis|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单节点|

```
配置  
{
  "type": "shading",
  "operation": {
    "operationMap": {
      "0-1": "redis://test123@10.196.8.92:6379",
      "2-3-4-5-6-7": "redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005"
    },
    "bucketSize": 8
  }
}
```      

```
单命令
redis-benchmark -n 1000000 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 16379

camellia-redis-proxy-async（Java）
SET: 86798.02 requests per second
GET: 84146.75 requests per second
INCR: 85755.94 requests per second
LPUSH: 83836.35 requests per second
RPUSH: 83022.00 requests per second
LPOP: 81195.20 requests per second
RPOP: 83752.09 requests per second
SADD: 79020.15 requests per second
HSET: 85711.84 requests per second
SPOP: 81719.38 requests per second
LPUSH (needed to benchmark LRANGE): 85367.94 requests per second
LRANGE_100 (first 100 elements): 34990.73 requests per second
LRANGE_300 (first 300 elements): 12677.00 requests per second
LRANGE_500 (first 450 elements): 8948.95 requests per second
LRANGE_600 (first 600 elements): 6891.09 requests per second


camellia-redis-proxy-sync（Java）  
SET: 86437.89 requests per second
GET: 89613.76 requests per second
INCR: 89992.80 requests per second
LPUSH: 87989.45 requests per second
RPUSH: 90195.73 requests per second
LPOP: 90744.10 requests per second
RPOP: 91066.39 requests per second
SADD: 92541.18 requests per second
HSET: 89309.64 requests per second
SPOP: 89485.46 requests per second
LPUSH (needed to benchmark LRANGE): 89118.62 requests per second
LRANGE_100 (first 100 elements): 37021.95 requests per second
LRANGE_300 (first 300 elements): 12876.64 requests per second
LRANGE_500 (first 450 elements): 9059.04 requests per second
LRANGE_600 (first 600 elements): 6942.71 requests per second

```

```
pipeline
redis-benchmark -n 1000000 -t set,get -P 16 -q -p 16379

camellia-redis-proxy-async（Java）
SET: 520833.34 requests per second
GET: 534759.38 requests per second

camellia-redis-proxy-sync（Java）
SET: 538502.94 requests per second
GET: 558971.50 requests per second

```

```
mset
redis-benchmark -n 1000000 -t mset -q -p 16379

camellia-redis-proxy-async（Java）
MSET (10 keys): 81599.34 requests per second

camellia-redis-proxy-sync（Java）
MSET (10 keys): 73855.24 requests per second

```
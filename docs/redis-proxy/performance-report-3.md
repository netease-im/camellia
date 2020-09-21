
## 双写

|机器|规格|参数
|:---:|:---:|:---:|
|camellia-redis-proxy-sync|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|camellia-redis-proxy-async|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|-Xms4096m -Xmx4096m -XX:+UseG1GC|
|redis cluster|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单机混部，3主3从|
|redis|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单节点|

```
{
  "type": "simple",
  "operation": {
    "read": "redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005",
    "type": "rw_separate",
    "write": {
      "resources": [
        "redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005",
        "redis://test123@10.196.8.92:6379"
      ],
      "type": "multi"
    }
  }
}

```      

```
单命令
redis-benchmark -n 1000000 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 16379

camellia-redis-proxy-async（Java）
SET: 84882.44 requests per second
GET: 85411.68 requests per second
INCR: 84005.38 requests per second
LPUSH: 81960.49 requests per second
RPUSH: 84566.59 requests per second
LPOP: 83035.79 requests per second
RPOP: 80000.00 requests per second
SADD: 86926.29 requests per second
HSET: 87199.16 requests per second
SPOP: 77148.59 requests per second
LPUSH (needed to benchmark LRANGE): 84882.44 requests per second
LRANGE_100 (first 100 elements): 35226.15 requests per second
LRANGE_300 (first 300 elements): 12717.31 requests per second
LRANGE_500 (first 450 elements): 8997.82 requests per second
LRANGE_600 (first 600 elements): 6936.21 requests per second

camellia-redis-proxy-sync（Java）  
SET: 76289.29 requests per second
GET: 87229.59 requests per second
INCR: 77357.47 requests per second
LPUSH: 78106.69 requests per second
RPUSH: 77960.55 requests per second
LPOP: 78302.41 requests per second
RPOP: 77651.80 requests per second
SADD: 78326.94 requests per second
HSET: 77730.27 requests per second
SPOP: 79465.99 requests per second
LPUSH (needed to benchmark LRANGE): 78210.54 requests per second
LRANGE_100 (first 100 elements): 36812.07 requests per second
LRANGE_300 (first 300 elements): 12887.10 requests per second
LRANGE_500 (first 450 elements): 9065.11 requests per second
LRANGE_600 (first 600 elements): 6971.26 requests per second

```

```
pipeline
redis-benchmark -n 1000000 -t set,get -P 16 -q -p 16379

camellia-redis-proxy-async（Java）
SET: 473260.78 requests per second
GET: 520020.81 requests per second

camellia-redis-proxy-sync（Java）
SET: 405022.25 requests per second
GET: 558971.50 requests per second

```

```
mset
redis-benchmark -n 1000000 -t mset -q -p 16379

camellia-redis-proxy-async（Java）
MSET (10 keys): 46748.63 requests per second

camellia-redis-proxy-sync（Java）
MSET (10 keys): 36519.01 requests per second

```
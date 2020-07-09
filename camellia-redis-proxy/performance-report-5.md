
## 对比测试（v1.0.6和v1.0.7)

|机器|规格|参数
|:---:|:---:|:---:|
|camellia-redis-proxy-async|4C8G云主机 |-Xms4096m -Xmx4096m -XX:+UseG1GC|
|redis cluster|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单机混部，3主3从|
|压测机|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核||

### 配置  

```
camellia-redis-proxy:
  password: pass123
  type: async
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.196.8.94:7000,10.196.8.94:7001,10.196.8.94:7002,10.196.8.94:7003,10.196.8.94:7004,10.196.8.94:7005

```      

### 场景一（单命令）
```
./redis-benchmark -n 10000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h 10.177.0.35 -a pass123

v1.0.6  
SET: 75241.72 requests per second
GET: 75321.05 requests per second
INCR: 77109.92 requests per second
LPUSH: 75238.88 requests per second
LPOP: 74063.10 requests per second
SADD: 73193.05 requests per second
SPOP: 70264.20 requests per second
LPUSH (needed to benchmark LRANGE): 74526.75 requests per second
LRANGE_100 (first 100 elements): 32614.72 requests per second
LRANGE_300 (first 300 elements): 11366.28 requests per second
LRANGE_500 (first 450 elements): 7859.32 requests per second
LRANGE_600 (first 600 elements): 5974.76 requests per second

v1.0.7  
SET: 80160.32 requests per second
GET: 80453.76 requests per second
INCR: 80032.02 requests per second
LPUSH: 81264.48 requests per second
LPOP: 81307.42 requests per second
SADD: 79507.05 requests per second
SPOP: 80817.88 requests per second
LPUSH (needed to benchmark LRANGE): 82284.21 requests per second
LRANGE_100 (first 100 elements): 30578.70 requests per second
LRANGE_300 (first 300 elements): 11496.37 requests per second
LRANGE_500 (first 450 elements): 7149.88 requests per second
LRANGE_600 (first 600 elements): 5029.80 requests per second


```

### 场景二（pipeline）
```
./redis-benchmark -n 10000000 -r 10000 -c 200 -t set,get -P 16 -q -p 6380 -h 10.177.0.35 -a pass123

v1.0.6
SET: 219881.70 requests per second
GET: 216900.92 requests per second

v1.0.7
SET: 220084.95 requests per second
GET: 216835.08 requests per second

```

### 场景三（mset）
```
./redis-benchmark -n 10000000 -r 10000 -c 200 -t mset -q -p 6380 -h 10.177.0.35 -a pass123

v1.0.6
MSET (10 keys): 36836.08 requests per second

v1.0.7
MSET (10 keys): 45676.30 requests per second

```
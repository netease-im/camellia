
## 云主机环境测试（基于v1.0.7）

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

SET: 77181.34 requests per second
GET: 76306.76 requests per second
INCR: 77068.32 requests per second
LPUSH: 72838.52 requests per second
LPOP: 71144.00 requests per second
SADD: 76734.20 requests per second
SPOP: 80385.85 requests per second
LPUSH (needed to benchmark LRANGE): 81241.37 requests per second
LRANGE_100 (first 100 elements): 33396.23 requests per second
LRANGE_300 (first 300 elements): 11325.03 requests per second
LRANGE_500 (first 450 elements): 7632.60 requests per second
LRANGE_600 (first 600 elements): 5966.43 requests per second


```

### 场景二（pipeline）
```
./redis-benchmark -n 10000000 -r 10000 -c 200 -t set,get -P 16 -q -p 6380 -h 10.177.0.35 -a pass123

SET: 214938.20 requests per second
GET: 211403.08 requests per second

```

### 场景三（mset）
```
./redis-benchmark -n 10000000 -r 10000 -c 200 -t mset -q -p 6380 -h 10.177.0.35 -a pass123

MSET (10 keys): 41413.01 requests per second

```

## performance report

### environment
|node|specs|
|:---:|:---:|
|redis-proxy|4C8G VM |
|redis-cluster|4C8G VM 4-master/0-slave|
|redis-benchmark client|4C8G VM |

### direct to redis
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6101 -h x.x.x.x --cluster -a xxxx
Cluster has 4 master nodes:

Master 0: 4dbb76a2f1313a0be583c69289975bf1c4b23f21 x.x.x.x:6101
Master 1: e5690b4c19542b0df12ae05a5d7223e7605cf323 x.x.x.x:6102
Master 2: 12dba35d91ddadd83d85ca7a7b7d74f35dee39b5 x.x.x.x:6104
Master 3: b3afaab2aa8a82cb2e5ec4e7124e74435da43c64 x.x.x.x:6103

SET: 249128.06 requests per second, p50=0.599 msec
GET: 249004.00 requests per second, p50=0.607 msec
INCR: 234576.59 requests per second, p50=0.631 msec
LPUSH: 249314.38 requests per second, p50=0.631 msec
RPUSH: 249190.12 requests per second, p50=0.631 msec
LPOP: 248632.53 requests per second, p50=0.615 msec
RPOP: 249376.55 requests per second, p50=0.639 msec
SADD: 249314.38 requests per second, p50=0.639 msec
HSET: 249190.12 requests per second, p50=0.615 msec
SPOP: 249252.23 requests per second, p50=0.607 msec
LPUSH (needed to benchmark LRANGE): 249687.89 requests per second, p50=0.631 msec
LRANGE_100 (first 100 elements): 120992.13 requests per second, p50=0.887 msec
LRANGE_300 (first 300 elements): 42720.44 requests per second, p50=2.359 msec
LRANGE_500 (first 500 elements): 26943.28 requests per second, p50=5.727 msec
LRANGE_600 (first 600 elements): 22727.27 requests per second, p50=6.791 msec


./redis-benchmark -n 10000000 -t set,get -P 16 -q -p 6101 -h x.x.x.x --cluster -a xxxx
Cluster has 4 master nodes:

Master 0: 4dbb76a2f1313a0be583c69289975bf1c4b23f21 x.x.x.x:6101
Master 1: e5690b4c19542b0df12ae05a5d7223e7605cf323 x.x.x.x:6102
Master 2: 12dba35d91ddadd83d85ca7a7b7d74f35dee39b5 x.x.x.x:6104
Master 3: b3afaab2aa8a82cb2e5ec4e7124e74435da43c64 x.x.x.x:6103

SET: 2099076.50 requests per second, p50=0.279 msec
GET: 2104377.25 requests per second, p50=0.263 msec


./redis-benchmark -n 1000000 -t mset -q -p 6101 -h x.x.x.x --cluster -a xxxx
Cluster has 4 master nodes:

Master 0: 4dbb76a2f1313a0be583c69289975bf1c4b23f21 x.x.x.x:6101
Master 1: e5690b4c19542b0df12ae05a5d7223e7605cf323 x.x.x.x:6102
Master 2: 12dba35d91ddadd83d85ca7a7b7d74f35dee39b5 x.x.x.x:6104
Master 3: b3afaab2aa8a82cb2e5ec4e7124e74435da43c64 x.x.x.x:6103

MSET (10 keys): 181587.08 requests per second, p50=0.215 msec

```

### proxy-config
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口
  monitor-enable: false  #是否开启监控
  transpond:
    type: local #使用本地配置
    local:
      resource: redis-cluster://xxxx@x.x.x.x:6101,x.x.x.x:6102,x.x.x.x:6103,x.x.x.x:6104 #转发的redis地址

```
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```

### camellia-1.0.37
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 75024.38 requests per second, p50=1.431 msec
GET: 73416.05 requests per second, p50=1.447 msec
INCR: 72600.55 requests per second, p50=1.447 msec
LPUSH: 76103.50 requests per second, p50=1.447 msec
RPUSH: 75024.38 requests per second, p50=1.487 msec
LPOP: 70741.38 requests per second, p50=1.511 msec
RPOP: 75728.89 requests per second, p50=1.423 msec
SADD: 78548.42 requests per second, p50=1.407 msec
HSET: 76616.61 requests per second, p50=1.471 msec
SPOP: 72743.15 requests per second, p50=1.503 msec
LPUSH (needed to benchmark LRANGE): 74118.00 requests per second, p50=1.463 msec
LRANGE_100 (first 100 elements): 33251.31 requests per second, p50=3.135 msec
LRANGE_300 (first 300 elements): 11768.17 requests per second, p50=8.639 msec
LRANGE_500 (first 500 elements): 7571.74 requests per second, p50=13.255 msec
LRANGE_600 (first 600 elements): 6537.83 requests per second, p50=15.431 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 333000.34 requests per second, p50=1.959 msec
GET: 331564.97 requests per second, p50=2.031 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 61800.88 requests per second, p50=0.631 msec

```

### camellia-1.0.61
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 73616.02 requests per second, p50=1.463 msec
GET: 74900.76 requests per second, p50=1.431 msec
INCR: 73335.29 requests per second, p50=1.471 msec
LPUSH: 76704.77 requests per second, p50=1.463 msec
RPUSH: 78198.31 requests per second, p50=1.439 msec
LPOP: 76190.48 requests per second, p50=1.455 msec
RPOP: 71679.45 requests per second, p50=1.551 msec
SADD: 70756.38 requests per second, p50=1.575 msec
HSET: 72479.52 requests per second, p50=1.495 msec
SPOP: 75832.26 requests per second, p50=1.463 msec
LPUSH (needed to benchmark LRANGE): 69851.91 requests per second, p50=1.575 msec
LRANGE_100 (first 100 elements): 31956.03 requests per second, p50=3.215 msec
LRANGE_300 (first 300 elements): 11527.38 requests per second, p50=8.791 msec
LRANGE_500 (first 500 elements): 7701.31 requests per second, p50=13.031 msec
LRANGE_600 (first 600 elements): 6459.28 requests per second, p50=15.631 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 315457.41 requests per second, p50=1.935 msec
GET: 348796.62 requests per second, p50=1.983 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 62351.91 requests per second, p50=0.631 msec

```

### camellia-1.1.3
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 72774.91 requests per second, p50=1.487 msec
GET: 70982.40 requests per second, p50=1.495 msec
INCR: 76481.84 requests per second, p50=1.415 msec
LPUSH: 78143.31 requests per second, p50=1.415 msec
RPUSH: 78100.59 requests per second, p50=1.415 msec
LPOP: 77561.47 requests per second, p50=1.447 msec
RPOP: 69280.87 requests per second, p50=1.591 msec
SADD: 70427.49 requests per second, p50=1.535 msec
HSET: 76347.54 requests per second, p50=1.447 msec
SPOP: 75488.79 requests per second, p50=1.463 msec
LPUSH (needed to benchmark LRANGE): 75108.91 requests per second, p50=1.455 msec
LRANGE_100 (first 100 elements): 32132.64 requests per second, p50=3.207 msec
LRANGE_300 (first 300 elements): 11797.32 requests per second, p50=8.527 msec
LRANGE_500 (first 500 elements): 7718.19 requests per second, p50=13.071 msec
LRANGE_600 (first 600 elements): 6567.20 requests per second, p50=15.359 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 334224.59 requests per second, p50=1.983 msec
GET: 357525.94 requests per second, p50=1.839 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 61428.84 requests per second, p50=0.655 msec

```

### camellia-1.1.8
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 70462.23 requests per second, p50=1.535 msec
GET: 74454.62 requests per second, p50=1.455 msec
INCR: 75671.59 requests per second, p50=1.415 msec
LPUSH: 78100.59 requests per second, p50=1.431 msec
RPUSH: 76126.68 requests per second, p50=1.455 msec
LPOP: 76581.41 requests per second, p50=1.447 msec
RPOP: 70422.54 requests per second, p50=1.551 msec
SADD: 72971.39 requests per second, p50=1.503 msec
HSET: 73046.02 requests per second, p50=1.495 msec
SPOP: 75517.30 requests per second, p50=1.431 msec
LPUSH (needed to benchmark LRANGE): 72817.30 requests per second, p50=1.487 msec
LRANGE_100 (first 100 elements): 31885.72 requests per second, p50=3.255 msec
LRANGE_300 (first 300 elements): 11729.38 requests per second, p50=8.663 msec
LRANGE_500 (first 500 elements): 7660.37 requests per second, p50=13.223 msec
LRANGE_600 (first 600 elements): 6525.16 requests per second, p50=15.495 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 326264.28 requests per second, p50=1.999 msec
GET: 345184.66 requests per second, p50=1.927 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 60543.68 requests per second, p50=0.663 msec

```

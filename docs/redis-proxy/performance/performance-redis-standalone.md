
## performance report

### environment
|node|specs|
|:---:|:---:|
|redis-proxy|4C8G VM |
|redis-standalone|4C8G VM |
|redis-benchmark client|4C8G VM |

### direct to redis
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6379 -h x.x.x.x
SET: 72690.27 requests per second, p50=1.535 msec
GET: 74244.56 requests per second, p50=1.383 msec
INCR: 74771.95 requests per second, p50=1.415 msec
LPUSH: 71459.20 requests per second, p50=1.527 msec
RPUSH: 75631.52 requests per second, p50=1.351 msec
LPOP: 74222.52 requests per second, p50=1.471 msec
RPOP: 74576.77 requests per second, p50=1.471 msec
SADD: 71464.30 requests per second, p50=1.463 msec
HSET: 75551.52 requests per second, p50=1.351 msec
SPOP: 78529.92 requests per second, p50=1.327 msec
LPUSH (needed to benchmark LRANGE): 76628.35 requests per second, p50=1.343 msec
LRANGE_100 (first 100 elements): 32997.86 requests per second, p50=3.071 msec
LRANGE_300 (first 300 elements): 11780.37 requests per second, p50=8.479 msec
LRANGE_500 (first 500 elements): 7846.65 requests per second, p50=12.783 msec
LRANGE_600 (first 600 elements): 6658.06 requests per second, p50=15.071 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6379 -h x.x.x.x
SET: 654450.25 requests per second, p50=1.039 msec
GET: 740192.50 requests per second, p50=0.871 msec

./redis-benchmark -n 1000000 -t mset -q -p 6379 -h x.x.x.x
MSET (10 keys): 74878.33 requests per second, p50=0.415 msec

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
      resource: redis://@x.x.x.x:6379 #转发的redis地址

```
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```

### camellia-1.0.37
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 73238.61 requests per second, p50=1.519 msec
GET: 72711.41 requests per second, p50=1.535 msec
INCR: 70876.74 requests per second, p50=1.559 msec
LPUSH: 73915.29 requests per second, p50=1.495 msec
RPUSH: 76400.03 requests per second, p50=1.463 msec
LPOP: 76858.05 requests per second, p50=1.423 msec
RPOP: 77106.95 requests per second, p50=1.423 msec
SADD: 71296.16 requests per second, p50=1.519 msec
HSET: 76196.28 requests per second, p50=1.455 msec
SPOP: 76516.95 requests per second, p50=1.423 msec
LPUSH (needed to benchmark LRANGE): 72611.09 requests per second, p50=1.511 msec
LRANGE_100 (first 100 elements): 32267.43 requests per second, p50=3.247 msec
LRANGE_300 (first 300 elements): 11528.31 requests per second, p50=8.735 msec
LRANGE_500 (first 500 elements): 7589.27 requests per second, p50=13.343 msec
LRANGE_600 (first 600 elements): 6541.46 requests per second, p50=15.391 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 336927.22 requests per second, p50=2.023 msec
GET: 327976.38 requests per second, p50=1.999 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 68596.52 requests per second, p50=0.535 msec

```

### camellia-1.0.61
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 70417.58 requests per second, p50=1.559 msec
GET: 74145.47 requests per second, p50=1.479 msec
INCR: 75809.27 requests per second, p50=1.447 msec
LPUSH: 76799.02 requests per second, p50=1.423 msec
RPUSH: 78634.90 requests per second, p50=1.415 msec
LPOP: 75728.89 requests per second, p50=1.463 msec
RPOP: 75046.91 requests per second, p50=1.463 msec
SADD: 73778.96 requests per second, p50=1.511 msec
HSET: 68662.45 requests per second, p50=1.575 msec
SPOP: 72238.67 requests per second, p50=1.503 msec
LPUSH (needed to benchmark LRANGE): 77220.08 requests per second, p50=1.439 msec
LRANGE_100 (first 100 elements): 33056.76 requests per second, p50=3.175 msec
LRANGE_300 (first 300 elements): 11812.37 requests per second, p50=8.607 msec
LRANGE_500 (first 500 elements): 7627.59 requests per second, p50=13.239 msec
LRANGE_600 (first 600 elements): 6484.4 requests per second, p50=16.034 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 348796.62 requests per second, p50=1.943 msec
GET: 341413.47 requests per second, p50=1.935 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 66907.53 requests per second, p50=0.551 msec

```

### camellia-1.1.3
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x
SET: 71418.37 requests per second, p50=1.559 msec
GET: 75363.63 requests per second, p50=1.447 msec
INCR: 75705.96 requests per second, p50=1.463 msec
LPUSH: 76846.23 requests per second, p50=1.439 msec
RPUSH: 71823.60 requests per second, p50=1.503 msec
LPOP: 75210.59 requests per second, p50=1.455 msec
RPOP: 72928.82 requests per second, p50=1.479 msec
SADD: 72448.02 requests per second, p50=1.503 msec
HSET: 72912.87 requests per second, p50=1.527 msec
SPOP: 71844.24 requests per second, p50=1.535 msec
LPUSH (needed to benchmark LRANGE): 73217.16 requests per second, p50=1.503 msec
LRANGE_100 (first 100 elements): 33007.66 requests per second, p50=3.175 msec
LRANGE_300 (first 300 elements): 11793.43 requests per second, p50=8.655 msec
LRANGE_500 (first 500 elements): 7695.50 requests per second, p50=13.127 msec
LRANGE_600 (first 600 elements): 6592.4 requests per second, p50=15.699 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 343053.19 requests per second, p50=1.919 msec
GET: 342935.53 requests per second, p50=1.879 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 67636.12 requests per second, p50=0.543 msec

```

### camellia-1.1.8
```
./redis-benchmark -n 1000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h x.x.x.x

SET: 72254.34 requests per second, p50=1.543 msec
GET: 73545.63 requests per second, p50=1.503 msec
INCR: 75568.66 requests per second, p50=1.471 msec
LPUSH: 73904.37 requests per second, p50=1.471 msec
RPUSH: 75809.27 requests per second, p50=1.423 msec
LPOP: 75953.21 requests per second, p50=1.439 msec
RPOP: 74861.51 requests per second, p50=1.431 msec
SADD: 70716.36 requests per second, p50=1.527 msec
HSET: 73014.02 requests per second, p50=1.511 msec
SPOP: 74057.62 requests per second, p50=1.471 msec
LPUSH (needed to benchmark LRANGE): 73035.34 requests per second, p50=1.503 msec
LRANGE_100 (first 100 elements): 31407.04 requests per second, p50=3.327 msec
LRANGE_300 (first 300 elements): 11814.05 requests per second, p50=8.623 msec
LRANGE_500 (first 500 elements): 7643.1 requests per second, p50=13.888 msec
LRANGE_600 (first 600 elements): 6599.3 requests per second, p50=15.358 msec

./redis-benchmark -n 1000000 -t set,get -P 16 -q -p 6380 -h x.x.x.x
SET: 330797.22 requests per second, p50=1.983 msec
GET: 326797.41 requests per second, p50=2.023 msec

./redis-benchmark -n 1000000 -t mset -q -p 6380 -h x.x.x.x
MSET (10 keys): 71042.91 requests per second, p50=0.535 msec

```

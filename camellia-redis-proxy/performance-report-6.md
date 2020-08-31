
## 云主机环境测试（比较1.0.8和1.0.9）

|机器|规格|参数
|:---:|:---:|:---:|
|camellia-redis-proxy-async|4C8G云主机 |-Xms4096m -Xmx4096m -XX:+UseG1GC|
|redis cluster|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核|单机混部|
|压测机|Intel(R)Xeon(R)E5-2650v3@2.30GHz 40核||

## 代理到单点redis

### 场景一（单命令）
```
./redis-benchmark -n 2000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h 10.177.0.35

v1.0.8
SET: 75605.80 requests per second
GET: 72542.62 requests per second
INCR: 74479.57 requests per second
LPUSH: 75216.25 requests per second
LPOP: 75030.01 requests per second
SADD: 75369.31 requests per second
SPOP: 71010.12 requests per second
LPUSH (needed to benchmark LRANGE): 77942.32 requests per second
LRANGE_100 (first 100 elements): 30580.10 requests per second
LRANGE_300 (first 300 elements): 9394.21 requests per second
LRANGE_500 (first 450 elements): 5807.37 requests per second
LRANGE_600 (first 600 elements): 4667.60 requests per second

v1.0.9（None）
SET: 73898.91 requests per second
GET: 75058.17 requests per second
INCR: 75522.99 requests per second
LPUSH: 74679.81 requests per second
LPOP: 75534.41 requests per second
SADD: 74841.90 requests per second
SPOP: 75001.88 requests per second
LPUSH (needed to benchmark LRANGE): 76388.36 requests per second
LRANGE_100 (first 100 elements): 33414.09 requests per second
LRANGE_300 (first 300 elements): 9888.31 requests per second
LRANGE_500 (first 450 elements): 6628.73 requests per second
LRANGE_600 (first 600 elements): 4895.13 requests per second

v1.0.9（LinkedBlockingQueue）
SET: 77181.34 requests per second
GET: 78085.34 requests per second
INCR: 77118.84 requests per second
LPUSH: 78855.02 requests per second
LPOP: 78560.77 requests per second
SADD: 77639.75 requests per second
SPOP: 78597.82 requests per second
LPUSH (needed to benchmark LRANGE): 78397.55 requests per second
LRANGE_100 (first 100 elements): 34220.79 requests per second
LRANGE_300 (first 300 elements): 10799.43 requests per second
LRANGE_500 (first 450 elements): 6362.09 requests per second
LRANGE_600 (first 600 elements): 4398.43 requests per second

v1.0.9（Disruptor）
SET: 80044.83 requests per second
GET: 80615.91 requests per second
INCR: 81709.36 requests per second
LPUSH: 82037.82 requests per second
LPOP: 80906.15 requests per second
SADD: 81446.49 requests per second
SPOP: 80537.99 requests per second
LPUSH (needed to benchmark LRANGE): 80099.32 requests per second
LRANGE_100 (first 100 elements): 34083.16 requests per second
LRANGE_300 (first 300 elements): 9472.39 requests per second
LRANGE_500 (first 450 elements): 5871.54 requests per second
LRANGE_600 (first 600 elements): 4251.78 requests per second


```

### 场景二（pipeline）
```
./redis-benchmark -n 3000000 -r 10000 -c 200 -t set,get -P 16 -q -p 6380 -h 10.177.0.35

v1.0.8
SET: 272826.47 requests per second
GET: 273025.12 requests per second

v1.0.9（None）
SET: 296413.38 requests per second
GET: 306936.78 requests per second

v1.0.9（LinkedBlockingQueue）
SET: 220296.67 requests per second
GET: 238549.61 requests per second

v1.0.9（Disruptor）
SET: 232306.03 requests per second
GET: 243210.38 requests per second

```

### 场景三（mset）
```
./redis-benchmark -n 100000000 -r 10000 -c 200 -t mset -q -p 6380 -h 10.177.0.35 -a pass123

v1.0.8
MSET (10 keys): 76643.03 requests per second

v1.0.9（None）
MSET (10 keys): 63109.40 requests per second

v1.0.9（LinkedBlockingQueue）
MSET (10 keys): 70836.58 requests per second

v1.0.9（Disruptor）
MSET (10 keys): 73078.05 requests per second

```

## 代理到redis cluster(3主3从)
  
### 场景一（单命令）
```
./redis-benchmark -n 2000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h 10.177.0.35

v1.0.8
SET: 75789.16 requests per second
GET: 75622.95 requests per second
INCR: 75264.37 requests per second
LPUSH: 75406.25 requests per second
LPOP: 71833.92 requests per second
SADD: 72634.83 requests per second
SPOP: 69842.16 requests per second
LPUSH (needed to benchmark LRANGE): 75284.20 requests per second
LRANGE_100 (first 100 elements): 29785.84 requests per second
LRANGE_300 (first 300 elements): 9143.78 requests per second
LRANGE_500 (first 450 elements): 5970.38 requests per second
LRANGE_600 (first 600 elements): 4222.69 requests per second

v1.0.9（None）
SET: 74123.49 requests per second
GET: 73211.80 requests per second
INCR: 75468.85 requests per second
LPUSH: 74979.38 requests per second
LPOP: 75685.91 requests per second
SADD: 74576.77 requests per second
SPOP: 75457.46 requests per second
LPUSH (needed to benchmark LRANGE): 75284.20 requests per second
LRANGE_100 (first 100 elements): 34193.88 requests per second
LRANGE_300 (first 300 elements): 10533.36 requests per second
LRANGE_500 (first 450 elements): 6056.77 requests per second
LRANGE_600 (first 600 elements): 4487.36 requests per second

v1.0.9（LinkedBlockingQueue）
SET: 75740.36 requests per second
GET: 75809.27 requests per second
INCR: 76074.55 requests per second
LPUSH: 79497.57 requests per second
LPOP: 79469.15 requests per second
SADD: 79805.27 requests per second
SPOP: 79611.50 requests per second
LPUSH (needed to benchmark LRANGE): 79617.83 requests per second
LRANGE_100 (first 100 elements): 34029.81 requests per second
LRANGE_300 (first 300 elements): 10347.47 requests per second
LRANGE_500 (first 450 elements): 6374.89 requests per second
LRANGE_600 (first 600 elements): 4404.17 requests per second

v1.0.9（Disruptor）
SET: 78287.08 requests per second
GET: 78256.45 requests per second
INCR: 79431.27 requests per second
LPUSH: 80318.06 requests per second
LPOP: 80144.26 requests per second
SADD: 80752.62 requests per second
SPOP: 81396.77 requests per second
LPUSH (needed to benchmark LRANGE): 79646.37 requests per second
LRANGE_100 (first 100 elements): 34123.87 requests per second
LRANGE_300 (first 300 elements): 9785.41 requests per second
LRANGE_500 (first 450 elements): 6006.91 requests per second
LRANGE_600 (first 600 elements): 4330.07 requests per second


```

### 场景二（pipeline）
```
./redis-benchmark -n 3000000 -r 10000 -c 200 -t set,get -P 16 -q -p 6380 -h 10.177.0.35

v1.0.8
SET: 220393.77 requests per second
GET: 219667.58 requests per second

v1.0.9（None）
SET: 214132.77 requests per second
GET: 200910.80 requests per second

v1.0.9（LinkedBlockingQueue）
SET: 201531.64 requests per second
GET: 185299.56 requests per second

v1.0.9（Disruptor）
SET: 191607.59 requests per second
GET: 194086.83 requests per second

```

### 场景三（mset）
```
./redis-benchmark -n 2000000 -r 10000 -c 200 -t mset -q -p 6380 -h 10.177.0.35 -a pass123

v1.0.8
MSET (10 keys): 37822.20 requests per second

v1.0.9（None）
MSET (10 keys): 31978.00 requests per second

v1.0.9（LinkedBlockingQueue）
MSET (10 keys): 45439.05 requests per second

v1.0.9（Disruptor）
MSET (10 keys): 44930.70 requests per second

```

## 代理到redis cluster(15主15从)
  
### 场景一（单命令）
```
./redis-benchmark -n 2000000 -r 10000 -c 200 -t set,get,incr,lpush,rpush,lpop,rpop,sadd,hset,spop,lpush,lrange -q -p 6380 -h 10.177.0.35

v1.0.8
SET: 64061.50 requests per second
GET: 64680.96 requests per second
INCR: 63451.78 requests per second
LPUSH: 72790.80 requests per second
LPOP: 69584.58 requests per second
SADD: 67303.81 requests per second
SPOP: 71252.98 requests per second
LPUSH (needed to benchmark LRANGE): 76988.22 requests per second
LRANGE_100 (first 100 elements): 31930.52 requests per second
LRANGE_300 (first 300 elements): 8983.47 requests per second
LRANGE_500 (first 450 elements): 5718.04 requests per second
LRANGE_600 (first 600 elements): 4366.57 requests per second

v1.0.9（None）
SET: 66717.82 requests per second
GET: 67127.61 requests per second
INCR: 70884.28 requests per second
LPUSH: 73217.16 requests per second
LPOP: 75457.46 requests per second
SADD: 73327.23 requests per second
SPOP: 75431.84 requests per second
LPUSH (needed to benchmark LRANGE): 75106.09 requests per second
LRANGE_100 (first 100 elements): 33301.70 requests per second
LRANGE_300 (first 300 elements): 10515.80 requests per second
LRANGE_500 (first 450 elements): 6325.41 requests per second
LRANGE_600 (first 600 elements): 4738.10 requests per second

v1.0.9（LinkedBlockingQueue）
SET: 67183.98 requests per second
GET: 69859.23 requests per second
INCR: 67326.47 requests per second
LPUSH: 79154.62 requests per second
LPOP: 79767.08 requests per second
SADD: 77121.81 requests per second
SPOP: 80260.04 requests per second
LPUSH (needed to benchmark LRANGE): 79814.83 requests per second
LRANGE_100 (first 100 elements): 33849.54 requests per second
LRANGE_300 (first 300 elements): 10168.39 requests per second
LRANGE_500 (first 450 elements): 6208.52 requests per second
LRANGE_600 (first 600 elements): 4362.34 requests per second

v1.0.9（Disruptor）
SET: 70333.38 requests per second
GET: 70846.62 requests per second
INCR: 71189.58 requests per second
LPUSH: 80971.66 requests per second
LPOP: 81126.03 requests per second
SADD: 80301.94 requests per second
SPOP: 80363.24 requests per second
LPUSH (needed to benchmark LRANGE): 80824.41 requests per second
LRANGE_100 (first 100 elements): 33925.33 requests per second
LRANGE_300 (first 300 elements): 9633.12 requests per second
LRANGE_500 (first 450 elements): 5969.17 requests per second
LRANGE_600 (first 600 elements): 4282.90 requests per second


```

### 场景二（pipeline）
```
./redis-benchmark -n 3000000 -r 10000 -c 200 -t set,get -P 16 -q -p 6380 -h 10.177.0.35

v1.0.8
SET: 119303.27 requests per second
GET: 113589.04 requests per second

v1.0.9（None）
SET: 131492.44 requests per second
GET: 123874.80 requests per second

v1.0.9（LinkedBlockingQueue）
SET: 117980.18 requests per second
GET: 110213.08 requests per second

v1.0.9（Disruptor）
SET: 119479.08 requests per second
GET: 122124.97 requests per second

```

### 场景三（mset）
```
./redis-benchmark -n 2000000 -r 10000 -c 200 -t mset -q -p 6380 -h 10.177.0.35 -a pass123

v1.0.8
MSET (10 keys): 13067.06 requests per second

v1.0.9（None）
MSET (10 keys): 18196.04 requests per second

v1.0.9（LinkedBlockingQueue）
MSET (10 keys): 24467.83 requests per second

v1.0.9（Disruptor）
MSET (10 keys): 31183.73 requests per second

```
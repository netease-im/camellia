
## 性能测试

|          集群          |                                        配置                                         | 数量 |
|:--------------------:|:---------------------------------------------------------------------------------:|---:|
|         压测机          |                       cpu：Intel(R)Xeon(R)E5-2670v3@2.30GHz                        |  1 |
| camellia-redis-proxy |                       cpu：Intel(R)Xeon(R)E5-2670v3@2.30GHz                        |  2 |
|         kv后端         | cpu: Intel(R)Xeon(R)Gold5220R@2.20GHz <br> 磁盘：3*intel p4510 nvme-ssd <br> 内存：512G |  3 |

## 版本

|          集群          |    版本    |                                       说明 |
|:--------------------:|:--------:|-----------------------------------------:|
|  memtier_benchmark   |  2.1.1   |                                          |
| camellia-redis-proxy |  1.3.0   |                        组成redis-cluster集群 |
|         obkv         | 4.2.1bp9 |               租户unit <br>  cpu72核/内存288G |
|         tikv         |  7.5.2   | max-thread-count 32 <br> block-cache 60G |
|        hbase         |  2.4.18  |           单台region-server配置了260g的堆内+堆外内存 |

## 压测程序

```
##obkv
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 6380 -a fbe086bdd54d --cluster-mode -t 5 -c 50 -n 1000000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=2str --key-maximum=50000000 --key-minimum=1 

##tikv
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 5380 -a 648d04f8c3f9 --cluster-mode -t 5 -c 50 -n 1000000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=2str --key-maximum=50000000 --key-minimum=1 

##hbase
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 4380 -a ab39725fb75f --cluster-mode -t 5 -c 50 -n 1000000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=2str --key-maximum=50000000 --key-minimum=1 
```

* 5*50个客户端连接，每个连接发100w个set请求，累计2.5亿个请求，get和set的比例为默认的10:1
* key的数量为5000w，value的大小为512字节的随机字符
* 单个proxy节点，lru-cache最多缓存600w个key
* 也就是每个key大约会执行4.5次get，0.45次set

## 压测结果

|  后端   |    qps    |  rt-avg |   rt-p99 |  rt-p999 |   KB/sec | cpu(proxy) | cpu(kv) |
|:-----:|:---------:|--------:|---------:|---------:|---------:|-----------:|--------:|
| obkv  | 402804.55 | 1.18214 |  3.66300 |  8.03100 | 69227.34 |        55% |     25% |
| tikv  | 245247.75 | 1.98863 | 10.36700 | 16.06300 | 42148.05 |        48% |     11% |
| hbase | 394377.43 | 1.29439 |  5.02300 |  9.98300 | 67777.76 |        54% |     10% |


## 压测数据明细

### obkv

#### memtier_benchmark
```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 6380 -a fbe086bdd54d --cluster-mode -t 5 -c 50 -n 1000000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=2str --key-maximum=50000000 --key-minimum=1
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 592 secs]  0 threads:   250000000 ops,  402885 (avg:  421956) ops/sec, 96.23MB/sec (avg: 70.82MB/sec),  1.15 (avg:  1.18) msec latency

5         Threads
50        Connections per thread
1000000   Requests per client


ALL STATS
======================================================================================================================================================
Type         Ops/sec     Hits/sec   Misses/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
------------------------------------------------------------------------------------------------------------------------------------------------------
Sets        36618.96          ---          ---         0.00         0.00         0.94966         0.79100         3.18300         7.26300     19910.70
Gets       366185.59     71909.65    294275.93         0.00         0.00         1.20539         1.14300         3.71100         8.09500     49316.64
Waits           0.00          ---          ---          ---          ---             ---             ---             ---             ---          ---
Totals     402804.55     71909.65    294275.93         0.00         0.00         1.18214         1.09500         3.66300         8.03100     69227.34

```

#### 压测客户端

![img_9.png](img_9.png)

#### proxy

![img_10.png](img_10.png)

#### kv后端

![img_11.png](img_11.png)

### tikv

#### memtier_benchmark

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 5380 -a 648d04f8c3f9 --cluster-mode -t 5 -c 50 -n 1000000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=2str --key-maximum=50000000 --key-minimum=1
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 1019 secs]  0 threads:   250000000 ops,  166964 (avg:  245197) ops/sec, 40.66MB/sec (avg: 41.15MB/sec),  1.50 (avg:  1.99) msec latency

5         Threads
50        Connections per thread
1000000   Requests per client


ALL STATS
======================================================================================================================================================
Type         Ops/sec     Hits/sec   Misses/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
------------------------------------------------------------------------------------------------------------------------------------------------------
Sets        22295.47          ---          ---         0.00         0.00         1.61950         0.75900         9.66300        15.35900     12122.64
Gets       222952.28     43780.15    179172.13         0.00         0.00         2.02554         1.48700        10.43100        16.12700     30025.42
Waits           0.00          ---          ---          ---          ---             ---             ---             ---             ---          ---
Totals     245247.75     43780.15    179172.13         0.00         0.00         1.98863         1.42300        10.36700        16.06300     42148.05

```

#### 压测客户端

![img_12.png](img_12.png)

#### proxy

![img_13.png](img_13.png)

#### kv后端

![img_14.png](img_14.png)

### hbase

#### memtier_benchmark

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 4380 -a ab39725fb75f --cluster-mode -t 5 -c 50 -n 1000000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=2str --key-maximum=50000000 --key-minimum=1
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 648 secs]  0 threads:   250000000 ops,  400352 (avg:  385636) ops/sec, 96.15MB/sec (avg: 64.72MB/sec),  1.13 (avg:  1.29) msec latency

5         Threads
50        Connections per thread
1000000   Requests per client


ALL STATS
======================================================================================================================================================
Type         Ops/sec     Hits/sec   Misses/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
------------------------------------------------------------------------------------------------------------------------------------------------------
Sets        35852.85          ---          ---         0.00         0.00         1.03282         0.79900         4.51100         9.53500     19494.14
Gets       358524.57     70402.71    288121.86         0.00         0.00         1.32054         1.14300         5.08700         9.98300     48283.62
Waits           0.00          ---          ---          ---          ---             ---             ---             ---             ---          ---
Totals     394377.43     70402.71    288121.86         0.00         0.00         1.29439         1.09500         5.02300         9.98300     67777.76

```


#### 压测客户端

![img_15.png](img_15.png)

#### proxy

![img_16.png](img_16.png)

#### kv后端

![img_17.png](img_17.png)

## camellia-redis-proxy关键配置

```properties
kv.hash.encode.version=1
kv.zset.encode.version=0
kv.set.encode.version=1

kv.key.meta.local.cache.enable=true
kv.hash.local.cache.enable=true
kv.zset.local.cache.enable=true
kv.set.local.cache.enable=true

kv.key.meta.lru.cache.capacity=6000000
kv.hash.lru.cache.capacity=2000000
kv.zset.lru.cache.capacity=2000000
kv.zset.index.lru.cache.capacity=2000000
kv.set.lru.cache.capacity=2000000

kv.write.buffer.key.meta.enable=true
kv.write.buffer.hash.enable=true
kv.write.buffer.zset.enable=true
kv.write.buffer.set.enable=true

kv.write.buffer.key.meta.max.size=100000
kv.write.buffer.hash.max.size=100000
kv.write.buffer.zset.max.size=100000
kv.write.buffer.set.max.size=100000
```

启动参数：

```
-Xms32768m, -Xmx32768m -XX:+UseZGC -XX:+ZGenerational -Dio.netty.tryReflectionSetAccessible=true --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.math=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.access=ALL-UNNAMED --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED --add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED -Xlog:async -Xlog:gc*=info:file=/home/popo/logs/proxy_gc.log:time,uptime,level,tags,tid:filesize=512M,filecount=2
```

java版本：`21.0.1`


## 建表语句

### obkv

```
CREATE TABLE `camellia_kv` (
    `slot` int(9) NOT NULL,
    `k` varbinary(1024) NOT NULL,
    `v` varbinary(1024) NOT NULL
    PRIMARY KEY (`slot`, `k`))
PARTITION BY KEY(slot) PARTITIONS 97;

```

#### hbase

```
create 'camellia_kv',{NAME => 'd', VERSIONS => '1',COMPRESSION=>'SNAPPY'},{ NUMREGIONS => 20 , SPLITALGO => 'UniformSplit'}
```


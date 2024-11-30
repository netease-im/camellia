
## 性能测试

|          集群          |                                        配置                                         | 数量 |
|:--------------------:|:---------------------------------------------------------------------------------:|---:|
|         压测机          |                       cpu：Intel(R)Xeon(R)E5-2670v3@2.30GHz                        |  1 |
| camellia-redis-proxy |                       cpu：Intel(R)Xeon(R)E5-2670v3@2.30GHz                        |  2 |
|         kv后端         | cpu: Intel(R)Xeon(R)Gold5220R@2.20GHz <br> 磁盘：3*intel p4510 nvme-ssd <br> 内存：512G |  3 |

## 版本

|          集群          |   版本    |                                        说明 |
|:--------------------:|:-------:|------------------------------------------:|
|  memtier_benchmark   |  2.1.1  |                                           |
| camellia-redis-proxy |  1.3.0  |                         组成redis-cluster集群 |
|         obkv         | 4.2.5.1 |                租户unit <br>  cpu72核/内存100G |
|         tikv         |  7.5.2  | max-thread-count 72 <br> block-cache 100G |
|        hbase         | 2.4.18  |            单台region-server配置了100g的堆内+堆外内存 |

## 压测程序

```
##obkv
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 6380 -a fbe086bdd54d --cluster-mode -t 20 -c 20 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=rrr --key-maximum=30000000 --key-minimum=1 

##tikv
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 5380 -a 648d04f8c3f9 --cluster-mode -t 20 -c 20 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=rrr --key-maximum=30000000 --key-minimum=1 

##hbase
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 4380 -a ab39725fb75f --cluster-mode -t 20 -c 20 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=rrr --key-maximum=30000000 --key-minimum=1  
```

* 读写混合场景
* 20*20个客户端连接，每个连接发100w个请求，累计1.2亿个请求，get和set的比例为默认的10:1
* key的数量为3000w，value的大小为512字节的随机字符
* key的分布为高斯分布
* 单个proxy节点，lru-cache最多缓存600w个key
* 也就是每个key大约会执行3.6次get，0.36次set（高斯分布下每个key的请求次数是不一样的）

## 压测结果

|  后端   |    qps    |  rt-avg |   rt-p99 |  rt-p999 |   KB/sec | cpu(proxy) | cpu(kv) |
|:-----:|:---------:|--------:|---------:|---------:|---------:|-----------:|--------:|
| obkv  | 532718.51 | 1.50003 |  8.76700 | 13.75900 | 82491.71 |        67% |     13% |
| tikv  | 306750.94 | 2.54298 | 16.63900 | 26.49500 | 47499.60 |        53% |     11% |
| hbase | 496481.53 | 1.59984 | 10.11100 | 20.60700 | 76880.44 |        63% |      8% |


## 压测数据明细

### memtier_benchmark(obkv)

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 6380 -a fbe086bdd54d --cluster-mode -t 20 -c 20 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=rrr --key-maximum=30000000 --key-minimum=1
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 225 secs]  0 threads:   120000000 ops,  715530 (avg:  532228) ops/sec, 153.56MB/sec (avg: 80.48MB/sec),  1.00 (avg:  1.50) msec latency

20        Threads
20        Connections per thread
300000    Requests per client


ALL STATS
======================================================================================================================================================
Type         Ops/sec     Hits/sec   Misses/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
------------------------------------------------------------------------------------------------------------------------------------------------------
Sets        48429.44          ---          ---         0.00         0.00         1.69197         1.07900         8.83100        13.82300     26276.56
Gets       484289.07     78301.91    405987.16         0.00         0.00         1.48084         0.29500         8.76700        13.75900     56215.14
Waits           0.00          ---          ---          ---          ---             ---             ---             ---             ---          ---
Totals     532718.51     78301.91    405987.16         0.00         0.00         1.50003         0.57500         8.76700        13.75900     82491.71

```

### memtier_benchmark(tikv)

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 5380 -a 648d04f8c3f9 --cluster-mode -t 20 -c 20 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=rrr --key-maximum=30000000 --key-minimum=1
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 391 secs]  0 threads:   120000000 ops,  313293 (avg:  306391) ops/sec, 67.26MB/sec (avg: 46.33MB/sec),  1.28 (avg:  2.54) msec latency

20        Threads
20        Connections per thread
300000    Requests per client


ALL STATS
======================================================================================================================================================
Type         Ops/sec     Hits/sec   Misses/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
------------------------------------------------------------------------------------------------------------------------------------------------------
Sets        27886.73          ---          ---         0.00         0.00         2.95672         1.67100        17.02300        26.75100     15130.62
Gets       278864.21     45086.10    233778.11         0.00         0.00         2.50161         0.23100        16.51100        26.49500     32368.98
Waits           0.00          ---          ---          ---          ---             ---             ---             ---             ---          ---
Totals     306750.94     45086.10    233778.11         0.00         0.00         2.54298         0.65500        16.63900        26.49500     47499.60

```

### memtier_benchmark(hbase)

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 4380 -a ab39725fb75f --cluster-mode -t 20 -c 20 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=rrr --key-maximum=30000000 --key-minimum=1
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 241 secs]  0 threads:   120000000 ops,  337465 (avg:  496899) ops/sec, 72.37MB/sec (avg: 75.14MB/sec),  1.19 (avg:  1.60) msec latency

20        Threads
20        Connections per thread
300000    Requests per client


ALL STATS
======================================================================================================================================================
Type         Ops/sec     Hits/sec   Misses/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
------------------------------------------------------------------------------------------------------------------------------------------------------
Sets        45135.14          ---          ---         0.00         0.00         1.85634         1.06300        10.43100        21.50300     24489.16
Gets       451346.39     72975.68    378370.72         0.00         0.00         1.57419         0.25500        10.11100        20.47900     52391.28
Waits           0.00          ---          ---          ---          ---             ---             ---             ---             ---          ---
Totals     496481.53     72975.68    378370.72         0.00         0.00         1.59984         0.44700        10.11100        20.60700     76880.44


```


### 压测客户端

![img_4.png](img_4.png)

### proxy

![img_5.png](img_5.png)

### kv后端

![img_6.png](img_6.png)

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

```
set global binlog_row_image='minimal';
alter system set kv_group_commit_batch_size = 10;
```

#### hbase

```
create 'camellia_kv',{NAME => 'd', VERSIONS => '1',COMPRESSION=>'SNAPPY'},{ NUMREGIONS => 20 , SPLITALGO => 'UniformSplit'}
```


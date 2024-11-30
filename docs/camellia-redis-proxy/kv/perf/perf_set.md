
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
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 6380 -a fbe086bdd54d --cluster-mode -t 10 -c 30 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=ccc --key-maximum=20000000 --key-minimum=1 --command='set __key__ __value__' --command-key-pattern=G

##tikv
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 5380 -a 648d04f8c3f9 --cluster-mode -t 10 -c 30 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=ccc --key-maximum=20000000 --key-minimum=1 --command='set __key__ __value__' --command-key-pattern=G

##hbase
/usr/bin/memtier_benchmark -s 10.44.40.23 -p 4380 -a ab39725fb75f --cluster-mode -t 10 -c 30 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=ccc --key-maximum=20000000 --key-minimum=1 --command='set __key__ __value__' --command-key-pattern=G
```

* 纯写场景
* 10*30个客户端连接，每个连接发30w个请求，累计9000w个set请求
* key为固定前缀+数字，key的数量为2000w，value的大小为512字节的随机字符
* key的分布为高斯分布
* 也就是每个key大约会重复执行4.5次set请求（高斯分布下每个key的写入次数是不一样的）

## 压测结果

|  后端   |    qps    |  rt-avg |   rt-p99 |  rt-p999 |   KB/sec | cpu(proxy) | cpu(kv) |
|:-----:|:---------:|--------:|---------:|---------:|---------:|-----------:|--------:|
| obkv  | 298548.22 | 2.00889 |  8.25500 | 15.74300 | 29444.04 |        45% |     23% |
| tikv  | 127568.44 | 4.54139 | 19.07100 | 28.28700 | 12581.32 |        31% |     15% | 
| hbase | 290160.55 | 2.04030 |  8.83100 | 50.94300 | 28616.82 |        44% |     10% |     


## 压测数据明细

### memtier_benchmark(obkv)

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 6380 -a fbe086bdd54d --cluster-mode -t 10 -c 30 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=ccc --key-maximum=20000000 --key-minimum=1 --command='set __key__ __value__' --command-key-pattern=G
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...

[RUN #1 100%, 301 secs]  0 threads:    90000000 ops,  279881 (avg:  298253) ops/sec, 13.48MB/sec (avg: 14.36MB/sec),  2.00 (avg:  2.01) msec latency

10        Threads
30        Connections per thread
300000    Requests per client


ALL STATS
============================================================================================================================
Type         Ops/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
----------------------------------------------------------------------------------------------------------------------------
Sets       298548.22         0.00         0.00         2.00889         1.80700         8.25500        15.74300     14722.02
Totals     298548.22         0.00         0.00         2.00889         1.80700         8.25500        15.74300     29444.04

```

### memtier_benchmark(tikv)

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 5380 -a 648d04f8c3f9 --cluster-mode -t 10 -c 30 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=ccc --key-maximum=20000000 --key-minimum=1 --command='set __key__ __value__' --command-key-pattern=G
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 705 secs]  0 threads:    90000000 ops,  115158 (avg:  127497) ops/sec, 5.55MB/sec (avg: 6.14MB/sec),  2.61 (avg:  4.54) msec latency

10        Threads
30        Connections per thread
300000    Requests per client


ALL STATS
============================================================================================================================
Type         Ops/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
----------------------------------------------------------------------------------------------------------------------------
Sets       127568.44         0.00         0.00         4.54139         3.95100        19.07100        28.28700      6290.66
Totals     127568.44         0.00         0.00         4.54139         3.95100        19.07100        28.28700     12581.32

```

### memtier_benchmark(hbase)

```
hzcaojiajun@nim-test-db3:~$ /usr/bin/memtier_benchmark -s 10.44.40.23 -p 4380 -a ab39725fb75f --cluster-mode -t 10 -c 30 -n 300000 --pipeline=1 --distinct-client-seed --random-data --data-size=512 --key-prefix=ccc --key-maximum=20000000 --key-minimum=1 --command='set __key__ __value__' --command-key-pattern=G
Writing results to stdout
[RUN #1] Preparing benchmark client...
[RUN #1] Launching threads now...
[RUN #1 100%, 310 secs]  0 threads:    90000000 ops,  178473 (avg:  289932) ops/sec, 8.60MB/sec (avg: 13.96MB/sec),  1.68 (avg:  2.04) msec latency

10        Threads
30        Connections per thread
300000    Requests per client


ALL STATS
============================================================================================================================
Type         Ops/sec    MOVED/sec      ASK/sec    Avg. Latency     p50 Latency     p99 Latency   p99.9 Latency       KB/sec
----------------------------------------------------------------------------------------------------------------------------
Sets       290160.55         0.00         0.00         2.04030         1.63900         8.83100        50.94300     14308.41
Totals     290160.55         0.00         0.00         2.04030         1.63900         8.83100        50.94300     28616.82

```


### 压测客户端(依次为obkv、tikv、hbase)

![img_1.png](img_1.png)

### proxy(依次为obkv、tikv、hbase)

![img_2.png](img_2.png)

### kv后端(依次为obkv、tikv、hbase)

![img_3.png](img_3.png)

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


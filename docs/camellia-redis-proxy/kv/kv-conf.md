
## 通用配置

```properties
port=6381
console.port=16378
password=pass123
route.conf=redis-kv://d

proxy.mode=cluster
redis.consensus.leader.selector.redis.url=redis://@127.0.0.1:6379
redis.consensus.leader.selector.redis.key=xxx
proxy.cluster.mode.command.move.always=true
```


```properties

#只有zset的encode-version设置为1时才依赖redis
kv.redis.store.url=redis://@127.0.0.1:6379
kv.redis.cache.url=redis://@127.0.0.1:6379

#支持0、1
#支持不重启动态修改，仅对新key有效
kv.hash.encode.version=0
#支持0、1
#支持不重启动态修改，仅对新key有效
kv.zset.encode.version=0
#支持0、1
#支持不重启动态修改，仅对新key有效
kv.set.encode.version=0

#本地lru缓存开关，默认true
#支持不重启动态修改
kv.key.mete.local.cache.enable=true
kv.hash.local.cache.enable=true
kv.zset.local.cache.enable=true
kv.set.local.cache.enable=true

#本地lru缓存的容量，建议参考jvm堆内存配置一个合适的值
#支持不重启动态修改
kv.key.meta.lru.cache.size=64M
kv.key.meta.lru.null.cache.size=64M
kv.hash.lru.write.cache.size=64M
kv.hash.lru.read.cache.size=64M
kv.set.lru.write.cache.size=64M
kv.set.lru.read.cache.size=64M
kv.zset.lru.write.cache.size=64M
kv.zset.lru.read.cache.size=64M
kv.zset.index.lru.write.cache.size=64M
kv.zset.index.lru.read.cache.size=64M
kv.lru.cache.max.capacity=10000000

#write-buffer的开关，默认true
#支持不重启动态修改
kv.write.buffer.key.meta.enable=true
kv.write.buffer.hash.enable=true
kv.write.buffer.zset.enable=true
kv.write.buffer.set.enable=true

#write-buffer的容量
#支持不重启动态修改
kv.write.buffer.key.meta.max.size=100000
kv.write.buffer.hash.max.size=100000
kv.write.buffer.zset.max.size=100000
kv.write.buffer.set.max.size=100000

##gc
#支持不重启动态修改
kv.gc.schedule.enable=true
kv.gc.schedule.interval.minute=1440
kv.gc.schedule.time.range=01:30-05:30
#如果设置以下两个参数，则会通过redis锁来保证同一个周期内，只有1个节点执行gc操作
kv.gc.lock.redis.url=redis://@127.0.0.1:6379
kv.gc.lock.redis.key=f3a9d0ae-343b-aac0-9a6b-31b55b71b55a
```

## use hbase as kv-store 

```properties
kv.client.class.name=com.netease.nim.camellia.redis.proxy.kv.hbase.HBaseKVClient

kv.store.hbase.url=hbase://127.0.0.1:2181/hbase
kv.store.hbase.conf={"hbase.rpc.timeout":1500,"hbase.client.retries.number":2}
kv.store.hbase.table.name=camellia_kv
```

```
create 'camellia_kv',{NAME => 'd', VERSIONS => '1',COMPRESSION=>'SNAPPY'},{ NUMREGIONS => 20 , SPLITALGO => 'UniformSplit'}
```

## use obkv as kv-store

```properties
kv.obkv.full.user.name=xxx
kv.obkv.param.url=xxx
kv.obkv.password=xxx
kv.obkv.sys.user.name=xxx
kv.obkv.sys.password=xxx

kv.client.class.name=com.netease.nim.camellia.redis.proxy.kv.obkv.OBKVClient

kv.obkv.table.name=camellia_kv

```

```
CREATE TABLE `camellia_kv` (
    `slot` int(9) NOT NULL,
    `k` varbinary(16384) NOT NULL,
    `v` varbinary(1048576) NOT NULL,
    PRIMARY KEY (`slot`, `k`))
PARTITION BY KEY(slot) PARTITIONS 97;

```

```
set global binlog_row_image='minimal';
```


## use tikv as kv-store

```properties
kv.tikv.pd.address=127.0.0.1:2379
kv.client.class.name=com.netease.nim.camellia.redis.proxy.kv.tikv.TiKVClient
```
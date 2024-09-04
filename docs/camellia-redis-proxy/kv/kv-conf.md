
## 通用配置

```yaml
server:
  port: 6381
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16378
  password: pass123
  monitor-enable: false
  monitor-interval-seconds: 60
  cluster-mode-enable: true
  cluster-mode-provider-class-name: com.netease.nim.camellia.redis.proxy.cluster.provider.ConsensusProxyClusterModeProvider
  config:
    "proxy.cluster.mode.command.move.always": true
    "cluster.mode.consensus.leader.selector.class.name": "com.netease.nim.camellia.redis.proxy.cluster.provider.RedisConsensusLeaderSelector"
    "redis.consensus.leader.selector.redis.url": "redis://@127.0.0.1:6379"
    "redis.consensus.leader.selector.redis.key": "xxxxx" 
  plugins:
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyPlugin
  transpond:
    type: local
    local:
      type: simple
      resource: redis-kv://d
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

#本地lru缓存的容量（key的数量），建议参考jvm堆内存和key大小后配置一个合适的值
#支持不重启动态修改
kv.key.meta.lru.cache.capacity=500000
kv.hash.lru.cache.capacity=100000
kv.zset.lru.cache.capacity=100000
kv.zset.index.lru.cache.capacity=100000
kv.set.lru.cache.capacity=100000

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
create 'camellia_kv',{NAME => 'd', VERSIONS => '1',COMPRESSION=>'SNAPPY'}
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
    `k` varbinary(1024) NOT NULL,
    `v` varbinary(1024) NOT NULL,
    `t` DATETIME(3),
    PRIMARY KEY (`slot`, `k`))
TTL (t + INTERVAL 1 SECOND)
PARTITION BY KEY(slot) PARTITIONS 97;
```


## use tikv as kv-store

```properties
kv.tikv.pd.address=127.0.0.1:2379
kv.client.class.name=com.netease.nim.camellia.redis.proxy.kv.tikv.TiKVClient
```
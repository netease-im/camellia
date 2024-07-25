
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

kv.redis.cache.url=redis://@127.0.0.1:6379
kv.redis.store.url=redis://@127.0.0.1:6379

#支持0、1、2、3
#支持不重启动态修改
kv.hash.key.meta.version=0
#支持0、1、2、3
#支持不重启动态修改
kv.zset.key.meta.version=0

#key-meta是否支持redis缓存，默认true
#不支持动态修改，启动后不变
kv.key.meta.cache.enable=true

#本地lru缓存开关，默认true
#支持不重启动态修改
kv.key.mete.local.cache.enable=true
kv.hash.local.cache.enable=true
kv.zset.local.cache.enable=true

#本地lru缓存的容量（key的数量），建议参考jvm堆内存和key大小后配置一个合适的值
#支持不重启动态修改
kv.key.meta.lru.cache.capacity=500000
kv.hash.lru.cache.capacity=1000000
kv.zset.lru.cache.capacity=1000000

#write-buffer的开关，默认true
#支持不重启动态修改
kv.write.buffer.key.meta.enable=true
kv.write.buffer.hash.enable=true
kv.write.buffer.zset.enable=true

#write-buffer的容量
#支持不重启动态修改
kv.write.buffer.key.meta.max.size=100000
kv.write.buffer.hash.max.size=100000
kv.write.buffer.zset.max.size=100000

##gc
#支持不重启动态修改
kv.gc.schedule.enable=true
kv.gc.schedule.interval.minute=1440
kv.gc.schedule.time.range=01:30-05:30

```
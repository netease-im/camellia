
# camellia-redis-proxy-hbase
## 简介  
基于camellia-redis、camellia-hbase、camellia-redis-proxy开发   
目前实现了string/hash/zset相关的命令，可以实现自动的冷热数据分离（冷数据存hbase，热数据存redis）

## 基本原理
对于redis中部分key的value部分（如zset的value，hash的value），可能占用较大字节  
同时整个key的ttl较长，但是又不是时刻访问，可能这个key不是高频访问，或者这个key的某些部分不是高频访问（如zset根据score取部分value，hash根据field取部分value）     
proxy会将这些value进行拆分，原始key仅存储value的一个索引，索引=md5(key) + md5(value)，并且将原始value保存在持久化k-v里，我们选择的是hbase（后续可以支持其他持久化k-v服务，如mysql、RocksDB等）  
### 写操作
一个写命令（如set、hset、zadd）请求到proxy，proxy会判断value部分是否超过阈值，如果没有超过则直接写入，如果超过了则生成索引，实际写入的是索引，同时把索引和实际value的关系写入hbase  
为了提高性能和降低rt，索引和实际value的关系会同步写入到redis（给一个较小的ttl），并且异步批量刷新到hbase  
### 读操作
一个读命令（如get、zrange、hget）请求到proxy，proxy先直接获取该key的value，然后判断value是否是一个索引，如果不是则直接返回，否则尝试去redis里获取value，获取不到再到hbase里获取（获取后会回填到redis，并给一个较小的ttl）  

## zset
zset作为redis中的有序集合，由key、score、value三部分组成，value会有二级索引结构  
## hash
hash是redis中的哈希结构，由key、field、value三部分组成，value部分有二级索引结构（field没有）  
## string
string是redis中的k-v结构，其冷热分离存储的原理和zset/hash不同，其完整数据会持久化到hbase里面（而不是二级索引），redis里只会保留较短ttl，当get时，会先检查redis，如果没有，则会穿透到hbase去reload

### 配置
* 所有的配置参考RedisHBaseConfiguration（配置文件是：camellia-redis-proxy.properties）

### 监控
* 监控数据通过RedisHBaseMonitor类进行获取
* camellia-redis-proxy-hbase支持camellia-redis-proxy的通用监控（连接数、请求量、请求tps、请求RT、慢查询、大key、热key等）

### 服务依赖
* 依赖redis（支持redis-standalone/redis-sentinel/redis-cluster）和hbase  
* hbase建表语句（表名可以自行修改，在camellia-redis-proxy.properties修改配置即可）：
```
create 'nim:nim_camellia',{NAME=>'d',VERSIONS=>1,BLOCKCACHE=>true,BLOOMFILTER=>'ROW',COMPRESSION=>'LZO',TTL=>'5184000'},{NUMREGIONS => 12 , SPLITALGO => 'UniformSplit'}
```

## maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-hbase-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
```

## 支持的命令
```
##数据库
PING,AUTH,QUIT,EXISTS,DEL,TYPE,EXPIRE,
EXPIREAT,TTL,PEXPIRE,PEXPIREAT,PTTL,
##String
SET,GET,MGET,SETEX,MSET,
##有序集合
ZADD,ZINCRBY,ZRANK,ZCARD,ZSCORE,ZCOUNT,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZLEXCOUNT,
##Hash
HSET,HGET,HSETNX,HMSET,HMGET,HEXISTS,HDEL,HLEN,HKEYS,
HVALS,HGETALL,
```

## 配置示例
```yaml
server:
  port: 6381
spring:
  application:
    name: camellia-redis-proxy-hbase

#see CamelliaRedisProxyProperties
camellia-redis-proxy:
  console-port: 16379
  #  password: xxxx
  monitor-enable: false
  monitor-interval-seconds: 60
  upstream-client-template-factory-class-name: com.netease.nim.camellia.redis.proxy.hbase.UpstreamRedisHBaseMixClientTemplateChooser
  plugins: #plugin list
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyPlugin
  #  config: #you can get this config from ProxyDynamicConf.java, priority less than camellia-redis-proxy.properties
  #    "k": v
  transpond: ## transpond conf is noneffective for upstream-client-template-factory-class-name is rewrite
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address

#see CamelliaHBaseProperties
camellia-hbase:
  type: local
  local:
    xml:
      xml-file: hbase.xml

#see CamelliaRedisProperties
camellia-redis:
  type: local
  local:
    resource: redis://abc@127.0.0.1:6379
```


### 更多示例和源码
[示例源码](/camellia-samples/camellia-redis-proxy-hbase-samples)

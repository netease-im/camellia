# camellia framework（[中文版](README.md)）
Camellia is originally develop as basic architecture for netease-yunxin's servers，all the modules is running in netease-yunxin's online-env.

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-green.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netease.nim/camellia/badge.svg)
  
## Summary

Camellia provide easy-to-use server toolkits:  

* [redis-proxy](/docs/redis-proxy/redis-proxy-en.md)
* [id-generator](/docs/id-gen/id-gen.md)
* [delay-queue](/docs/delay-queue/delay-queue.md)
* [hot-key](/docs/hot-key/hot-key.md)

camellia also provide enhanced sdk (based on other open source sdk): 
* [redis-client](/docs/redis-client/redis-client.md)
* [hbase-client](/docs/hbase-client/hbase-client.md)
* [feign-client](/docs/feign/feign.md)
* [dao-cache](/docs/cache/cache.md)
* [tools](/docs/tools/tools.md)

## Instruction

### camellia-redis-proxy  
high performance redis-proxy:  
* base on netty4, support redis-standalone/redis-sentinel/redis-cluster
* support [twemproxy](https://github.com/twitter/twemproxy) 、[codis](https://github.com/CodisLabs/codis) as the upstream(such as migration scenarios)
* support [kvrocks](https://github.com/apache/kvrocks) 、 [pika](https://github.com/OpenAtomFoundation/pika) 、 [tendis](https://github.com/Tencent/Tendis)  as the upstream
* support GET/SET/EVAL, support MGET/MSET, support blocking BLPOP, support PUBSUB/TRANSACTION, support STREAMS/JSON/SEARCH, support TAIR_HASH/TAIR_ZSET/TAIR_STRING
* support sharding/read-write-separate/double-write on proxy  
* support multi-route-conf   
* support ssl/tls, both client to proxy and proxy to upstream redis
* support unix-domain-socket, both client to proxy and proxy to upstream redis
* support tps/rt/big-key/hot-key/slow-command monitor  
* support custom command interceptor, support hot-key-cache(GET command)，support value converter and so on    
[QUICK START](/docs/redis-proxy/redis-proxy-en.md)  

### camellia-id-gen
provide some id gen algorithm:   
* snowflake, support setting region tag
* strict-increment id-gen
* db-segment id-gen, support setting region tag         
[QUICK START](/docs/id-gen/id-gen.md)

### camellia-delay-queue
delay queue base on redis:   
* independent deployment of delay-queue-server, scale out, support multi-topic, support http-api
* provide java-sdk, support spring-boot-starter
* provide monitor data    
[QUICK START](/docs/delay-queue/delay-queue.md)

### camellia-hot-key
hot key detect and cache:  
* support hot key detect, hot key cache, hot key topN stats
* support custom hot-key-config、hot-key-callback、hot-key-topN-callback、hot-key-cache-stats-callback  
* support custom registry, such as eureka、zk and so on
* provide monitor data     
[QUICK START](/docs/hot-key/hot-key.md)


## Release-Version
latest version is 1.2.17, have deploy to maven central repository on 2023/10/10    
[CHANGE_LOG](/update-en.md)  

## SNAPSHOT-version
latest version is 1.2.18-SNAPSHOT  
```xml
<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <name>Sonatype Snapshot Repository</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

## User List
If you are using Camellia and feel it helps or you'd like to do some contributions, please add your company to [user list](https://github.com/netease-im/camellia/issues/10) and let us know your needs 

## Contact
wechat-id: hdnxttl  
email: zj_caojiajun@163.com  
# camellia framework（[中文版](README.md)）
Camellia is originally develop as basic architecture for netease-yunxin's servers，all the modules is running in netease-yunxin's online-env.

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-blue.svg)
[![GitHub release](https://img.shields.io/github/release/netease-im/camellia.svg)](https://github.com/netease-im/camellia/releases)
![java_language](https://img.shields.io/badge/java--language-1.8%20%7C%2021-blue.svg)
[![docker pull](https://img.shields.io/docker/pulls/48n6e/camellia-redis-proxy.svg)](https://hub.docker.com/r/48n6e/camellia-redis-proxy)
  
## Summary

Camellia provide easy-to-use server toolkits:

### server

* [redis-proxy](/docs/camellia-redis-proxy/redis-proxy-en.md)
* [id-generator](/docs/camellia-id-gen/id-gen.md)
* [delay-queue](/docs/camellia-delay-queue/delay-queue.md)
* [hot-key](/docs/camellia-hot-key/hot-key.md)

### sdk

camellia also provide enhanced sdk (based on other open source sdk): 
* [redis-client](/docs/camellia-redis-client/redis-client.md)
* [hbase-client](/docs/camellia-hbase/hbase-client.md)
* [feign-client](/docs/camellia-feign/feign.md)
* [dao-cache](/docs/camellia-cache/cache.md)
* [tools](/docs/camellia-tools/tools.md)

## Instruction

### camellia-redis-proxy  
high performance redis-proxy:  
* base on netty4 and java21, support redis-standalone/redis-sentinel/redis-cluster
* support [twemproxy](https://github.com/twitter/twemproxy) 、[codis](https://github.com/CodisLabs/codis) as the upstream(such as migration scenarios)
* support [kvrocks](https://github.com/apache/kvrocks) 、 [pika](https://github.com/OpenAtomFoundation/pika) 、 [tendis](https://github.com/Tencent/Tendis)  as the upstream
* support GET/SET/EVAL, support MGET/MSET, support blocking BLPOP, support PUBSUB/TRANSACTION, support STREAMS/JSON/SEARCH/BloomFilter/CuckooFilter, support TAIR_HASH/TAIR_ZSET/TAIR_STRING
* all supported commands: [supported_commands](docs/camellia-redis-proxy/supported_commands.md)
* support sharding/read-write-separate/double-write on proxy  
* support multi-route-conf   
* support ssl/tls, both client to proxy and proxy to upstream redis
* support unix-domain-socket, both client to proxy and proxy to upstream redis
* support use http to access proxy, like [webdis](https://github.com/nicolasff/webdis) , see: [redis_over_http](/docs/camellia-redis-proxy/other/redis_over_http.md)
* support tps/rt/big-key/hot-key/slow-command monitor  
* support disguise as redis-cluster/redis-sentinel for high availability
* support custom command plugin, support hot-key-cache(GET command)，support value converter and so on    
[QUICK START](/docs/camellia-redis-proxy/redis-proxy-en.md)  

### camellia-id-gen
provide some id gen algorithm:   
* server require java21, sdk require java8
* snowflake, support setting region tag
* strict-increment id-gen
* db-segment id-gen, support setting region tag         
[QUICK START](/docs/camellia-id-gen/id-gen.md)

### camellia-delay-queue
delay queue base on redis:   
* server require java21, sdk require java8
* independent deployment of delay-queue-server, scale out, support multi-topic, support http-api
* provide java-sdk, support spring-boot-starter
* provide monitor data    
[QUICK START](/docs/camellia-delay-queue/delay-queue.md)

### camellia-hot-key
hot key detect and cache:  
* server require java21, sdk require java8  
* support hot key detect, hot key cache, hot key topN stats
* support custom hot-key-config、hot-key-callback、hot-key-topN-callback、hot-key-cache-stats-callback  
* support custom registry, such as eureka、zk and so on
* provide monitor data     
[QUICK START](/docs/camellia-hot-key/hot-key.md)


## Release-Version
latest version is 1.3.6, have deploy to maven central repository on 2025/06/23  
[CHANGE_LOG](/update-en.md)  

## SNAPSHOT-version
latest version is 1.3.7-SNAPSHOT  
```xml
<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <name>Sonatype Snapshot Repository</name>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
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

## Stargazers Over Time

[![Stargazers Over Time](https://starchart.cc/netease-im/camellia.svg)](https://starchart.cc/netease-im/camellia) 
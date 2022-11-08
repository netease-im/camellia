# camellia framework（[中文版](README.md)）
Camellia is originally develop as basic architecture for netease-yunxin's servers，all the modules is running in netease-yunxin's online-env.

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-green.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netease.nim/camellia/badge.svg)
  
## Instruction
### camellia-redis  
* base on camellia-core and jedis(2.9.3)，main class is CamelliaRedisTemplate, can invoke redis-standalone/redis-sentinel/redis-cluster in identical way，support pipeline    
* support client sharding/read-write-separate/double-write   
* support read from slave(redis-sentinel)
* provide CamelliaRedisLock、CamelliaFreq utils  
[QUICK START](/docs/redis-template/redis-template.md)

### camellia-redis-proxy  
high performance redis-proxy:  
* base on netty4, support redis-standalone/redis-sentinel/redis-cluster  
* support sharding/read-write-separate/double-write on proxy  
* support multi-route-conf            
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

### camellia-hbase  
* base on camellia-core and hbase-client，main class is CamelliaHBaseTemplate    
* support client read-write-separate/double-write  
[QUICK START](/docs/hbase-template/hbase-template.md)  

### camellia-feign
join camellia-core and open-feign, so your feign-client have this feature: 
* support dynamic route
* support custom route by request param
* support custom load balance policy by request param
* support read-write-separate/double-write
* support dynamic conf, such like timeout  
[QUICK START](/docs/feign/feign.md)

### camellia-tools
* provide some tools, such as：
* compress utils CamelliaCompressor
* encrypt utils CamelliaEncryptor
* local cache utils CamelliaLoadingCache
* thread pool utils CamelliaHashedExecutor
* dynamic conf CamelliaCircuitBreaker  
[QUICK START](/docs/tools/tools.md)

## Version
latest version is 1.1.4, have deploy to maven central repository on 2022/11/08  
[CHANGE_LOG](/update-en.md)  

## User List
If you are using Camellia and feel it helps or you'd like to do some contributions, please add your company to [user list](https://github.com/netease-im/camellia/issues/10) and let us know your needs 

## Contact
wechat-id: hdnxttl  
email: zj_caojiajun@163.com  
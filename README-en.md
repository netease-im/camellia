# camellia framework（[中文版](README.md)）
Camellia is originally develop as basic architecture for netease-yunxin's servers，all the modules is running in netease-yunxin's online-env.

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-green.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netease.nim/camellia/badge.svg)
  

## Catalog
├─`camellia-core`   
├─`camellia-dashboard`    
├─`camellia-hbase`  
├─`camellia-redis`  
├─`camellia-redis-proxy`   
├─`camellia-redis-proxy-hbase`  
├─`camellia-redis-toolkit`  
├─`camellia-redis-zk`  
│ ├─`camellia-redis-zk-common`  
│ ├─`camellia-redis-zk-discovery`  
│ ├─`camellia-redis-zk-registry`    
├─`camellia-samples`               
│ ├─`camellia-core-samples`  
│ ├─`camellia-dashboard-samples`  
│ ├─`camellia-redis-samples`  
│ ├─`camellia-redis-proxy-samples`   
│ ├─`camellia-redis-proxy-hbase-samples`  
│ ├─`camellia-redis-toolkit-samples`  
│ ├─`camellia-hbase-samples`   
│ ├─`camellia-spring-redis-samples`   
├─`camellia-spring-boot-starters`               
│ ├─`camellia-dashboard-spring-boot-starter`  
│ ├─`camellia-hbase-spring-boot-starter`  
│ ├─`camellia-redis-eureka-base`  
│ ├─`camellia-redis-eureka-spring-boot-starter`  
│ ├─`camellia-redis-proxy-hbase-spring-boot-starter`  
│ ├─`camellia-redis-proxy-spring-boot-starter`  
│ ├─`camellia-redis-spring-temaplate-adaptor-spring-boot-starter`   
│ ├─`camellia-redis-proxy-zk-registry-spring-boot-starter`                     
│ ├─`camellia-redis-spring-boot-starter`  
│ ├─`camellia-redis-spring-boot-starter`  
│ ├─`camellia-redis-zk-discovery-spring-boot-starter`    
│ ├─`camellia-spring-redis-base`         
│ ├─`camellia-spring-redis-eureka-discovery-spring-boot-starter`     
│ ├─`camellia-spring-redis-zk-discovery-spring-boot-starter`    
                     
## Instruction
### 1、camellia-core  
base on cglib, support client shading/read-write-separate/double-write  
[QUICK START](/docs/core/core.md)  
### 2、camellia-dashboard  
a web service, depends on mysql/redis，manage camellia-core's config，client can get and update config from dashboard  
[QUICK START](/docs/dashboard/dashboard.md)  
### 3、camellia-redis  
base on camellia-core and jedis，main class is CamelliaRedisTemplate, can invoke redis/redis-sentinel/redis-cluster in identical way，support pipeline    
support client shading/read-write-separate/double-write   
support jedis adaptor to migrate from jedis easily   
support spring-redis-template adaptor  
[QUICK START](/docs/redis-template/redis-template.md)
### 4、camellia-redis-proxy  
base on netty4, support redis/redis-sentinel/redis-cluster  
support shading/read-write-separate/double-write on proxy  
support tps/rt/big-key/hot-key/slow-command monitor  
support custom command interceptor, support hot-key-cache(GET command)，support value converter and so on    
[QUICK START](/docs/redis-proxy/redis-proxy-en.md)  
### 5、camellia-hbase  
base on camellia-core and hbase-client，main class is CamelliaHBaseTemplate    
support client read-write-separate/double-write  
[QUICK START](/docs/hbase-template/hbase-template.md)  
### 6、camellia-redis-proxy-hbase    
base on camellia-redis-proxy、CamelliaRedisTemplate、CamelliaHBaseTemplate, support hot-code-sepatation on redis string/hash/zset commands  
[QUICK START](/docs/redis-proxy-hbase/redis-proxy-hbase.md)  
### 7、camellia-redis-toolkit  
base on CamelliaRedisTemplate，provide some toolkit of redis, such as redis-lock  
[QUICK START](/docs/toolkit/toolkit.md)  

## Version
latest version is 1.0.35，have deploy to maven central repository on 2021/08/13  
[CHANGE_LOG](/update-en.md)  

## Contact
wechat-id: hdnxttl  
email: zj_caojiajun@163.com  
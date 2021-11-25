# camellia framework（[ENGLISH](README-en.md)）
Camellia是网易云信开发的服务器基础组件，所有模块均已应用于网易云信线上环境

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-green.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netease.nim/camellia/badge.svg)

## 代码目录
├─`camellia-core`   
├─`camellia-dashboard`    
├─`camellia-hbase`  
├─`camellia-redis`  
├─`camellia-redis-proxy`   
├─`camellia-redis-proxy-plugins`  
│ ├─`camellia-redis-proxy-hbase`    
│ ├─`camellia-redis-proxy-mq`  
│ │ ├─`camellia-redis-proxy-mq-common`   
│ │ ├─`camellia-redis-proxy-mq-kafka`  
├─`camellia-tools`    
├─`camellia-id-gen`  
│ ├─`camellia-id-gen-core`  
│ ├─`camellia-id-gen-sdk`      
├─`camellia-redis-zk`  
│ ├─`camellia-redis-zk-common`  
│ ├─`camellia-redis-zk-discovery`  
│ ├─`camellia-redis-zk-registry`    
├─`camellia-samples`               
│ ├─`camellia-core-samples`  
│ ├─`camellia-dashboard-samples`  
│ ├─`camellia-redis-samples`  
│ ├─`camellia-id-gen-snowflake-samples`    
│ ├─`camellia-id-gen-strict-samples`    
│ ├─`camellia-id-gen-segment-samples`    
│ ├─`camellia-redis-proxy-samples`   
│ ├─`camellia-redis-proxy-hbase-samples`  
│ ├─`camellia-hbase-samples`   
│ ├─`camellia-spring-redis-samples`   
├─`camellia-spring-boot-starters`               
│ ├─`camellia-dashboard-spring-boot-starter`  
│ ├─`camellia-hbase-spring-boot-starter`  
│ ├─`camellia-redis-eureka-base`  
│ ├─`camellia-redis-eureka-spring-boot-starter`  
│ ├─`camellia-redis-proxy-hbase-spring-boot-starter`  
│ ├─`camellia-redis-proxy-spring-boot-starter`  
│ ├─`camellia-redis-proxy-naocs-spring-boot-starter`  
│ ├─`camellia-redis-spring-temaplate-adaptor-spring-boot-starter`   
│ ├─`camellia-redis-proxy-zk-registry-spring-boot-starter`                     
│ ├─`camellia-redis-spring-boot-starter`  
│ ├─`camellia-redis-zk-discovery-spring-boot-starter`    
│ ├─`camellia-spring-redis-base`         
│ ├─`camellia-spring-redis-eureka-discovery-spring-boot-starter`     
│ ├─`camellia-spring-redis-zk-discovery-spring-boot-starter`  
│ ├─`camellia-id-gen-spring-boot-starters`  
│ │ ├─`camellia-id-gen-id-loader`        
│ │ ├─`camellia-id-gen-snowflake-spring-boot-starter`       
│ │ ├─`camellia-id-gen-strict-spring-boot-starter`    
│ │ ├─`camellia-id-gen-segment-spring-boot-starter`   
              
## 介绍
### 1、camellia-core  
基于cglib，支持客户端分片/读写分离/双写  
[快速开始](/docs/core/core.md)  
### 2、camellia-dashboard
一个web服务，依赖mysql/redis，用于管理camellia-core的配置，端侧可以获取并动态更新配置  
[快速开始](/docs/dashboard/dashboard.md)  
### 3、camellia-redis  
基于camellia-core和jedis，主要的类是CamelliaRedisTemplate，可以使用统一的api来调用redis/redis-sentinel/redis-cluster，支持pipeline、mget/mset等     
支持自定义分片、读写分离、双（多）写、双（多）读     
支持Jedis适配器，一行代码从Jedis切换到CamelliaRedisTemplate  
支持SpringRedisTemplate适配器，不修改一行代码迁移到CamelliaRedisTemplate    
[快速开始](/docs/redis-template/redis-template.md)
### 4、camellia-redis-proxy  
基于netty4开发，支持redis/redis-sentinel/redis-cluster    
支持自定义分片、读写分离、双（多）写、双（多）读  
支持一个proxy代理多组路由配置       
支持TPS、RT、热key、大key、慢查询的监控     
支持自定义方法拦截，支持热key缓存（GET命令），支持透明的数据转换（如解压缩、加解密）等      
[快速开始](/docs/redis-proxy/redis-proxy-zh.md)  
### 5、camellia-hbase  
基于camellia-core和hbase-client，主要的类是CamelliaHBaseTemplate  
支持读写分离、双（多）写    
[快速开始](/docs/hbase-template/hbase-template.md)  
### 6、camellia-redis-proxy-hbase    
基于camellia-redis-proxy、CamelliaRedisTemplate、CamelliaHBaseTemplate  
支持string/hash/zset相关命令的冷热分离存储  
[快速开始](/docs/redis-proxy-hbase/redis-proxy-hbase.md)    
### 7、camellia-tools
提供了一些工具类，包括：压缩工具类CamelliaCompressor、加解密工具类CamelliaEncryptor、本地缓存工具类CamelliaLoadingCache等  
[快速开始](/docs/tools/tools.md)     
### 8、camellia-id-gen
提供了多种id生成算法，开箱即用，包括雪花算法、严格递增的id生成算法、趋势递增的id生成算法等    
[快速开始](/docs/id-gen/id-gen.md)       

## 版本
最新版本是1.0.43，已经发布到maven中央仓库（2021/11/23）  
[更新日志](/update-zh.md)  

## 联系方式
微信id: hdnxttl  
email: zj_caojiajun@163.com  
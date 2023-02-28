# camellia（[ENGLISH](README-en.md)）
Camellia是网易云信开发的服务器基础组件，所有模块均已应用于网易云信线上环境

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-green.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netease.nim/camellia/badge.svg)

## 介绍
camellia主要包括以下功能模块：

### camellia-redis-proxy
基于netty4开发的一款高性能redis代理  
* 支持redis-standalone/redis-sentinel/redis-cluster    
* 支持自定义分片、读写分离、双（多）写、双（多）读   
* 支持多租户（可以同时代理多组路由，可以通过不同的登录密码来区分）     
* 支持多租户动态路由，支持自定义的动态路由数据源
* 支持读从节点（redis-sentinel、redis-cluster都支持）
* 高可用，可以基于lb组成集群，也可以基于注册中心组成集群，也可以伪装成redis-cluster组成集群
* 支持自定义插件，并且内置了很多插件，可以按需使用（包括：大key监控、热key监控、热key缓存、key命名空间、ip黑白名单、速率控制等等） 
* 支持丰富的监控，如TPS、RT、热key、大key、慢查询、连接数等   
* 支持整合hbase实现string/zset/hash等数据结构的冷热分离存储操作     
[快速开始](/docs/redis-proxy/redis-proxy-zh.md)  

### camellia-id-gen
提供了多种id生成算法，开箱即用，包括：  
* 雪花算法（支持设置单元标记）   
* 严格递增的id生成算法（步长支持动态调整）  
* 趋势递增的id生成算法（支持设置单元标记，支持多单元id同步）         
[快速开始](/docs/id-gen/id-gen.md)

### camellia-delay-queue
基于redis实现的延迟队列服务：
* 独立部署delay-queue-server服务器，支持水平扩展，支持多topic，以http协议对外提供服务（短轮询or长轮询），支持多语言客户端
* 提供了一个java-sdk，并且支持以spring-boot方式快速接入
* 支持丰富的监控数据    
[快速开始](/docs/delay-queue/delay-queue.md)

### camellia-redis(enhanced-redis-client)
这是一个封装了jedis（2.9.3/3.6.3）的redis客户端，主要的类是CamelliaRedisTemplate  
* 屏蔽了访问redis-standalone/redis-sentinel/redis-cluster的区别（jedis访问上述三种redis服务器的api是不一样的）
* 支持pipeline、mget、mset等操作（jedis不支持使用pipeline访问redis-cluster，也不支持跨slot场景下使用mget、mset命令访问redis-cluster）
* 支持透明的访问从节点（当前支持redis-sentinel）
* 支持自定义分片、读写分离、双（多）写、双（多）读
* 支持动态配置变更
* 提供了一些常用的工具类，如分布式锁、计数器缓存、频控等  
  [快速开始](/docs/redis-client/redis-client.md)

### camellia-hbase(enhanced-hbase-client)
基于hbase-client封装的hbase客户端，主要的类是CamelliaHBaseTemplate    
* 支持读写分离、双（多）写   
* 支持动态配置变更  
[快速开始](/docs/hbase-client/hbase-client.md)

### camellia-feign(enhanced-feign-client)  
整合了camellia-core和open-feign，从而你的feign客户端可以：
* 支持动态路由
* 支持根据请求参数做自定义路由
* 支持根据请求参数做自定义负载均衡
* 支持双写、支持读写分离
* 支持动态调整参数，如超时时间   
[快速开始](/docs/feign/feign.md)

### camellia-cache(enhanced-spring-cache)
基于spring-cache二次开发：  
* 支持redis，也支持本地缓存（Caffeine）
* 支持基于注解执行mget，mevict等批量操作
* 支持不同的过期时间、支持设置是否缓存null值
* 支持自定义的序列化/反序列化，默认使用jackson，并且支持缓存值的压缩
* 支持一键刷新缓存（动态调整缓存key的前缀）  
[快速开始](/docs/cache/cache.md)

### camellia-tools
提供了一些简单实用的工具类，包括：  
* 解压缩
* 加解密
* 线程池
* 熔断
* 分布式锁
* ......  
[快速开始](/docs/tools/tools.md)  

## 版本
最新版本是1.2.2，已经发布到maven中央仓库（2023/02/28）  
[更新日志](/update-zh.md)  

## 谁在使用Camellia
如果觉得 Camellia 对你有用，欢迎Star/Fork  
欢迎所有 Camellia 用户及贡献者在 [这里](https://github.com/netease-im/camellia/issues/10) 分享您在当前工作中开发/使用 Camellia 的故事  

## 联系方式
微信: hdnxttl  
email: zj_caojiajun@163.com  
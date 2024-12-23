# camellia（[ENGLISH](README-en.md)）
Camellia是网易云信开发的服务器基础组件，所有模块均已应用于网易云信线上环境

<img src="/docs/img/logo.png" width = "500"/>

![GitHub](https://img.shields.io/badge/license-MIT-blue.svg)
[![GitHub release](https://img.shields.io/github/release/netease-im/camellia.svg)](https://github.com/netease-im/camellia/releases)
![java_language](https://img.shields.io/badge/java--language-1.8%20%7C%2021-blue.svg)

## 模块介绍

Camellia提供了一系列简单易用的服务器组件，包括但不限于：

### 独立部署的中间件服务（server）

最低要求java21    

* [redis代理](/docs/camellia-redis-proxy/redis-proxy-zh.md) 
* [发号器](/docs/camellia-id-gen/id-gen.md)
* [延迟队列](/docs/camellia-delay-queue/delay-queue.md)
* [热点key探测](/docs/camellia-hot-key/hot-key.md)

### 增强的三方库（sdk）

基于开源版本进行二次增强，最低要求java8   

* [redis客户端](/docs/camellia-redis-client/redis-client.md) 
* [hbase客户端 ](/docs/camellia-hbase/hbase-client.md)
* [微服务feign客户端](/docs/camellia-feign/feign.md)
* [数据库缓存框架](/docs/camellia-cache/cache.md)
* [工具类](/docs/camellia-tools/tools.md)

## 功能简介

### camellia-redis-proxy
基于netty4开发的一款高性能redis代理，最低要求java21  
* 支持redis-standalone/redis-sentinel/redis-cluster
* 支持其他proxy作为后端（如双写迁移场景），如 [twemproxy](https://github.com/twitter/twemproxy) 、[codis](https://github.com/CodisLabs/codis) 等
* 支持 [kvrocks](https://github.com/apache/kvrocks) 、 [pika](https://github.com/OpenAtomFoundation/pika) 、 [tendis](https://github.com/Tencent/Tendis) 等作为后端
* 支持使用hbase/obkv/tikv等作为底层存储，构建一个类redis的系统，具体见：[kv](docs/camellia-redis-proxy/kv/kv.md)
* 支持普通的GET/SET/EVAL，也支持MGET/MSET，也支持阻塞型的BLPOP，也支持PUBSUB和TRANSACTION，也支持STREAMS/JSON/SEARCH/BloomFilter/CuckooFilter，也支持TAIR_HASH/TAIR_ZSET/TAIR_STRING
* 支持SCAN命令，即使后端是redis-cluster或者自定义分片，也可以透明的扫描到所有key
* 支持SELECT命令，从而可以使用多database
* 所有支持的命令: [supported_commands](docs/camellia-redis-proxy/supported_commands.md)
* 支持SSL/TLS（client到proxy支持，proxy到redis也支持）
* 支持unix-domain-socket（client到proxy支持，proxy到redis也支持）
* 支持使用http协议访问proxy，类似于 [webdis](https://github.com/nicolasff/webdis) ，但是接口定义不一样，具体见：[redis_over_http](/docs/camellia-redis-proxy/other/redis_over_http.md)
* 支持自定义分片、读写分离、双（多）写、双（多）读   
* 支持多租户（可以同时代理多组路由，可以通过不同的登录密码来区分）     
* 支持动态路由，支持自定义的动态路由数据源（内置：本地配置文件、nacos、etcd等，也可以自定义）
* 支持读从节点（redis-sentinel、redis-cluster都支持）
* 高可用，可以基于lb组成集群，也可以基于注册中心组成集群，也可以伪装成redis-cluster组成集群，也可以伪装成redis-sentinel组成集群
* 支持自定义插件，并且内置了很多插件，可以按需使用（包括：大key监控、热key监控、热key缓存、key命名空间、ip黑白名单、速率控制等等）  
* 支持丰富的监控，如TPS、RT、热key、大key、慢查询、连接数等
* 支持使用prometheus/grafana来监控proxy集群  
[快速开始](/docs/camellia-redis-proxy/redis-proxy-zh.md)  

### camellia-id-gen
提供了多种id生成算法，开箱即用，包括：  
* 服务器最低要求java21，sdk最低要求java8，对外暴露http接口，支持多语言客户端    
* 雪花算法（支持设置单元标记）   
* 严格递增的id生成算法（步长支持动态调整）  
* 趋势递增的id生成算法（支持设置单元标记，支持多单元id同步）    
* 支持使用prometheus/grafana来监控id-gen-server集群   
[快速开始](/docs/camellia-id-gen/id-gen.md)

### camellia-delay-queue
基于redis实现的延迟队列服务：   
* 服务器最低要求java21，sdk最低要求java8  
* 独立部署delay-queue-server服务器，支持水平扩展，支持多topic，以http协议对外提供服务（短轮询or长轮询），支持多语言客户端
* 提供了一个java-sdk，并且支持以spring-boot方式快速接入
* 支持丰富的监控数据     
* 支持使用prometheus/grafana来监控delay-queue-server集群    
[快速开始](/docs/camellia-delay-queue/delay-queue.md)

### camellia-hot-key  
热key探测和缓存服务： 
* 服务器最低要求java21，sdk最低要求java8  
* 支持热key探测，也支持热key缓存，也支持topN统计  
* 支持丰富的自定义扩展口（热key通知、topN通知、热key规则数据源、热key缓存命中统计）
* 支持自定义数据源（内置：本地配置文件、nacos、etcd，也可以自己实现）  
* 支持自定义注册中心（内置：zk、eureka，也可以自己实现）  
* 支持丰富的监控数据     
* 支持使用prometheus/grafana来监控hot-key-server集群   
[快速开始](/docs/camellia-hot-key/hot-key.md)  


## RELEASE版本
最新版本是1.3.1，已经发布到maven中央仓库（2024/12/23）  
[更新日志](/update-zh.md)  

## SNAPSHOT版本
当前最新是1.4.0-SNAPSHOT  
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

## 谁在使用Camellia
如果觉得 Camellia 对你有用，欢迎Star/Fork  
欢迎所有 Camellia 用户及贡献者在 [这里](https://github.com/netease-im/camellia/issues/10) 分享您在当前工作中开发/使用 Camellia 的故事  

## 联系方式
微信: hdnxttl（加此微信拉进技术交流群）    
email: zj_caojiajun@163.com 

## Stargazers Over Time

[![Stargazers Over Time](https://starchart.cc/netease-im/camellia.svg)](https://starchart.cc/netease-im/camellia)
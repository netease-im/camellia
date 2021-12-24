# camellia（[ENGLISH](README-en.md)）
Camellia是网易云信开发的服务器基础组件，所有模块均已应用于网易云信线上环境

<img src="/docs/img/logo.png" width = "500"/>
 
![GitHub](https://img.shields.io/badge/license-MIT-green.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.netease.nim/camellia/badge.svg)

## 介绍
camellia主要包括以下功能模块：
### camellia-redis
* 这是一个封装了jedis（2.9.3）的redis客户端，主要的类是CamelliaRedisTemplate  
* 屏蔽了访问redis-standalone/redis-sentinel/redis-cluster的区别（jedis访问上述三种redis服务器的api是不一样的）   
* 支持pipeline、mget、mset等操作（jedis不支持使用pipeline访问redis-cluster，也不支持跨slot场景下使用mget、mset命令访问redis-cluster）    
* 支持自定义分片、读写分离、双（多）写、双（多）读  
* 支持Jedis适配器，一行代码从Jedis切换到CamelliaRedisTemplate   
* 支持SpringRedisTemplate适配器，不修改一行代码迁移到CamelliaRedisTemplate     
[快速开始](/docs/redis-template/redis-template.md)

### camellia-redis-proxy
* 基于netty4开发的一款redis代理，支持redis-standalone/redis-sentinel/redis-cluster    
* 支持自定义分片、读写分离、双（多）写、双（多）读   
* 支持多租户（可以同时代理多组路由，可以通过不同的登录密码来区分）     
* 支持读从节点（redis-sentinel、redis-cluster都支持）
* 支持丰富的监控，如TPS、RT、热key、大key、慢查询、连接数等     
* 支持自定义方法拦截，支持热key缓存（GET命令），支持透明的数据转换（如解压缩、加解密）等  
* 支持整合hbase实现string/zset/set等数据结构的冷热分离存储操作    
* 支持整合mq（如kafka）实现异地的数据双写同步  
[快速开始](/docs/redis-proxy/redis-proxy-zh.md)  

### camellia-id-gen
* 提供了多种id生成算法，开箱即用，包括：雪花算法、严格递增的id生成算法、趋势递增的id生成算法等      
* 上述id生成算法支持多单元部署（保证多单元id不冲突）  
[快速开始](/docs/id-gen/id-gen.md)

### camellia-hbase
* 基于hbase-client封装的hbase客户端，主要的类是CamelliaHBaseTemplate  
* 支持读写分离、双（多）写   
[快速开始](/docs/hbase-template/hbase-template.md)

### camellia-tools
* 提供了一些工具类，包括：压缩工具类CamelliaCompressor、加解密工具类CamelliaEncryptor、本地缓存工具类CamelliaLoadingCache等  
[快速开始](/docs/tools/tools.md)

## 版本
最新版本是1.0.45，已经发布到maven中央仓库（2021/12/24）  
[更新日志](/update-zh.md)  

## 谁在使用Camellia
欢迎所有 Camellia 用户及贡献者在 [这里](https://github.com/netease-im/camellia/issues/10) 分享您在当前工作中开发/使用 Camellia 的故事

## 联系方式
微信: hdnxttl  
email: zj_caojiajun@163.com  
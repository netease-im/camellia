# camellia框架  
Camellia是网易云信内部开发使用的一个公共组件库，以下模块均已应用于云信线上环境  

![GitHub](https://img.shields.io/badge/license-MIT-green.svg)  

## 目录结构
├─`camellia-core`   
├─`camellia-dashboard`    
├─`camellia-hbase`  
├─`camellia-redis`  
├─`camellia-redis-proxy`   
├─`camellia-redis-proxy-hbase`  
├─`camellia-redis-toolkit`  
├─`camellia-samples`               
│ ├─`camellia-core-samples`  
│ ├─`camellia-dashboard-samples`  
│ ├─`camellia-redis-samples`  
│ ├─`camellia-redis-proxy-samples`   
│ ├─`camellia-redis-proxy-hbase-samples`  
│ ├─`camellia-redis-toolkit-samples`  
│ ├─`camellia-hbase-samples`   
├─`camellia-spring-boot-starters`               
│ ├─`camellia-dashboard-spring-boot-starter`  
│ ├─`camellia-redis-spring-boot-starter`  
│ ├─`camellia-redis-eureka-spring-boot-starter`      
│ ├─`camellia-redis-proxy-spring-boot-starter`   
│ ├─`camellia-redis-proxy-hbase-spring-boot-starter`    
│ ├─`camellia-hbase-spring-boot-starter`  
              
## 模块简介
### 1、camellia-core  
一个基于cglib开发的支持**客户端多读多写**和**客户端分片**的代理框架  
[详情和示例](/camellia-core/README.md)  
### 2、camellia-dashboard  
一个web服务，可以管理和下发camellia-core的代理配置，从而可以**动态**更新代理配置  
[详情和示例](/camellia-dashboard/README.md)  
### 3、camellia-redis  
一个基于camellia-core和jedis开发的CamelliaRedisTemplate，支持redis、redis sentinel、redis cluster，支持pipeline，对外暴露统一的api（方法和参数同jedis）  
[详情和示例](/camellia-redis/README.md)  
### 4、camellia-redis-proxy  
一个基于netty和camellia-redis开发的redis代理服务，实现了redis协议，可以使用标准redis客户端连接，从而可以让不方便修改业务端代码的服务能够使用camellia-redis  
[详情和示例](/camellia-redis-proxy/README.md)  
### 5、camellia-hbase  
一个基于camellia-core和hbase-client封装的hbase客户端，支持双写、读写分离等  
[详情和示例](/camellia-hbase/README.md)  
### 6、camellia-redis-proxy-hbase    
一个基于camellia-redis、camellia-hbase的camellia-redis-proxy插件，用于构建冷热分离存储的redis-proxy服务，当前实现了zset相关的命令  
[详情和示例](/camellia-redis-proxy-hbase/README.md)  
### 7、camellia-redis-toolkit  
一个基于camellia-redis的工具集，包括分布式锁、id生成、计数器等组件  
[详情和示例](/camellia-redis-toolkit/README.md)  

## 版本
当前最新版本1.0.6，已经发布到maven中央仓库（2020/05/22）  
[更新日志](/update.md)  

## 交流
* 遇到问题：欢迎查看各个模块的详情和示例来帮助你解决疑惑  
* 提交缺陷：在确保使用最新版本依然存在问题时请尽量以简洁的语言描述清楚复现该问题的步骤并提交issue            
* 功能建议：如果你有什么好的想法或者提案，欢迎提交 issue 与我们交流  
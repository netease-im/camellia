# camellia framework（[ENGLISH](README.md)）  
Camellia是网易云信开发的服务器基础组件，所有模块均已应用于网易云信线上环境

![GitHub](https://img.shields.io/badge/license-MIT-green.svg)  

## 代码目录
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
├─`camellia-spring-boot-starters`               
│ ├─`camellia-dashboard-spring-boot-starter`  
│ ├─`camellia-redis-spring-boot-starter`  
│ ├─`camellia-redis-eureka-spring-boot-starter`      
│ ├─`camellia-redis-proxy-spring-boot-starter`   
│ ├─`camellia-redis-proxy-zk-registry-spring-boot-starter`   
│ ├─`camellia-redis-proxy-hbase-spring-boot-starter`    
│ ├─`camellia-hbase-spring-boot-starter`  
│ ├─`camellia-redis-zk-discovery-spring-boot-starter`  
              
## 介绍
### 1、camellia-core  
基于cglib，支持客户端分片/读写分离/双写  
[快速开始](/docs/core/core.md)  
### 2、camellia-dashboard
一个web服务，依赖mysql/redis，用于管理camellia-core的配置，端侧可以获取并动态更新配置  
[快速开始](/docs/dashboard/dashboard.md)  
### 3、camellia-redis  
基于camellia-core和jedis，主要的类是CamelliaRedisTemplate，可以使用统一的api来调用redis/redis-sentinel/redis-cluster，支持pipeline  
支持客户端分片/读写分离/双写  
[快速开始](/docs/redis-template/redis-template.md)
### 4、camellia-redis-proxy  
基于netty4和camellia-core，支持redis/redis-sentinel/redis-cluster  
支持分片/读写分离/双写，支持自定义方法拦截等  
[快速开始](/docs/redis-proxy/redis-proxy.md)  
### 5、camellia-hbase  
基于camellia-core和hbase-client，主要的类是CamelliaHBaseTemplate  
支持客户端分片/读写分离/双写  
[快速开始](/docs/hbase-template/hbase-template.md)  
### 6、camellia-redis-proxy-hbase    
基于camellia-redis-proxy、CamelliaRedisTemplate、CamelliaHBaseTemplate  
支持zset命令的冷热分离  
[快速开始](/docs/redis-proxy-hbase/redis-proxy-hbase.md)    
### 7、camellia-redis-toolkit  
基于CamelliaRedisTemplate，提供了redis相关的一些工具，如分布式锁等    
[快速开始](/docs/toolkit/toolkit.md)    

## 版本
最新版本是1.0.9，已经发布到maven中央仓库（2020/09/08）  
[更新日志](/update-zh.md)  

## 联系方式
微信id: hdnxttl  
email: zj_caojiajun@163.com  
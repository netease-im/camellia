
# camellia-dashboard
## 简介  
可以管理和下发camellia-core的代理配置，从而可以**动态**更新代理配置  
提供了一个spring-boot-starter，可以快速的搭建dashboard  

## 名词说明
bid表示业务类型，bgroup表示业务分组  
bid+bgroup唯一确定了一份配置  

## 步骤
* 先调用/camellia/admin/createResourceTable创建一份配置，获取配置tid
* 再调用/camellia/admin/createOrUpdateTableRef创建一份bid+bgroup到tid的引用
* 客户端初始化时会根据bid+bgroup请求dashboard获取配置和MD5值，并使用MD5来定时请求，用于检查配置是否有更新（默认5s）

## maven依赖
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-dashboard-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```
## 示例
[数据库建表语句](/camellia-dashboard/doc/table.sql)  
[示例源码](/camellia-samples/camellia-dashboard-samples)  
[配置示例](/camellia-samples/camellia-dashboard-samples/src/main/resources/samples.txt)  
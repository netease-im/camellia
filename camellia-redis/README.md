
# camellia-redis 
## 简介
基于camellia-core和jedis开发的Redis客户端CamelliaRedisTemplate  
支持redis、redis sentinel、redis cluster，支持pipeline，对外暴露统一的api（方法和参数同jedis）    
可以基于本地的静态配置构造客户端  
也可以基于远程dashboard的动态配置构造客户端  
提供了一个spring boot starter  

## 使用场景
* 需要从redis迁移到redis cluster，CamelliaRedisTemplate的接口定义和Jedis一致，并且支持了mget/mset/pipeline等批量命令    
* 需要让数据在redis/redis cluster之间进行迁移，可以使用CamelliaRedisTemplate的双写功能    
* 单个集群容量不够（比如redis cluster单集群容量超过1T可能会崩溃），可以使用分片和双写，逐步迁移到N个集群进行客户端分片  

## maven依赖
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```

## 支持的命令
参考ICamelliaRedisTemplate和ICamelliaRedisPipeline两个接口定义

## 示例（详细配置参考CamelliaRedisProperties）

### 使用本地配置（普通redis）
```
camellia-redis:
  type: local
  local:
    resource: redis://abc@127.0.0.1:6379
```
### 使用本地配置（redis-cluster）
```
camellia-redis:
  type: local
  local:
    resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
```
### 使用本地配置（复杂配置，单独的一个json文件）  
```
camellia-redis:
  type: local
  local:
    type: complex
    json-file: resource-table.json
```
```
{
  "type": "shading",
  "operation": {
    "operationMap": {
      "4": {
        "read": "redis://password1@127.0.0.1:6379",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381",
      "1-3-5": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
### 使用dashboard动态配置
```
camellia-redis:
  type: remote
  remote:
    bid: 1
    bgroup: default
    url: http://127.0.0.1:8080
```

### 客户端使用
```
//用法和jedis一样，但是封装掉了连接池取连接和还连接的操作
String set = template.set("k1", "v1");
String k1 = template.get("k1");
String setex = template.setex("k2", 10, "v2");
List<String> mget = template.mget("k1", "k2");
Long del = template.del("k1", "k2", "k3");

//使用pipeline
//pipeline对象实现了Closeable接口，使用完毕请close，或者使用try-resource的语法，这是和jedis的pipeline使用有差别的地方
try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
    Response<String> response1 = pipelined.set("k3", "v3");
    Response<Long> response2 = pipelined.setnx("k3", "v3");
    Response<Long> response3 = pipelined.zadd("k3", 1.0, "v3");

    pipelined.sync();

    Response<String> response4 = pipelined.get("k1");
    Response<Map<String, String>> response5 = pipelined.hgetAll("hk");

    pipelined.sync();
}
```
### 示例源码
[示例源码](/camellia-samples/camellia-hbase-samples)
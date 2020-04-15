
# camellia-redis-proxy
## 简介  
基于netty和camellia-redis开发的redis代理服务  
实现了redis协议，可以使用标准redis客户端连接  
从而可以让不方便修改业务端代码的服务能够使用camellia-redis  
可以基于本地的静态配置（yml文件）  
也可以基于远程dashboard的动态配置（配置可以动态变更，yml配置dashboard地址即可）  
并且支持多个业务共享一组代理服务器（需要使用camellia-dashboard管理多组代理配置），即A业务代理到redis1集群，B业务代理到redis2集群  
提供sync和async两种转发模式，sync使用camellia-redis进行转发，是同步的；async使用netty进行转发，是异步的，建议使用默认的async模式    
对于双（N）写场景，sync模式会等待配置的两（N）个地址均响应之后再返回给客户端，而async模式会在配置的第一个双（N）写地址响应之后就返回，因此客户端RT表现会优于sync模式（async模式会保证来自于同一个连接的命令的执行顺序在双写场景下是一致的）    
提供了一个spring-boot-starter，可以快速的搭建redis-proxy  
## 使用场景
* 需要从redis迁移到redis cluster，但是客户端代码不方便修改  
* 客户端直连redis cluster，导致cluster服务器连接过多  
* 需要使用camellia-redis的多读多写和分片功能，但是客户端代码不方便修改  
## 部署架构
redis-proxy本身无状态，可以水平扩展，为了实现高可用和负载均衡，有如下两种部署方式：     
#### 部署方式一
可以通过前端添加四层代理（如LVS等）来统一访问入口，此时使用方式跟单节点redis没有区别    
<img src="doc/1.png" width="80%" height="80%">  
#### 部署方式二
可以通过eureka、zk等注册中心进行注册，在客户端进行负载均衡   
此时客户端可以使用RedisProxyJedisPool代替JedisPool即可使用标准Jedis访问代理服务  
<img src="doc/2.png" width="80%" height="80%">  

## maven依赖
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-proxy-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```

## 支持的命令
```
##数据库
PING,AUTH,ECHO,CLIENT,QUIT,EXISTS,DEL,TYPE,EXPIRE,
EXPIREAT,TTL,PERSIST,PEXPIRE,PEXPIREAT,PTTL,SORT
##字符串
SET,GET,GETSET,MGET,SETNX,SETEX,MSET,DECRBY,DECR,INCRBY,INCR,APPEND,
STRLEN,INCRBYFLOAT,PSETEX,SETRANGE,GETRANGE,SUBSTR,
##哈希表
HSET,HGET,HSETNX,HMSET,HMGET,HINCRBY,HEXISTS,HDEL,HLEN,HKEYS,
HVALS,HGETALL,HINCRBYFLOAT,HSCAN,
##队列
RPUSH,LPUSH,LLEN,LRANGE,LTRIM,LINDEX,LSET,LREM,LPOP,RPOP,LINSERT,LPUSHX,RPUSHX,
##集合
SADD,SMEMBERS,SREM,SPOP,SCARD,SISMEMBER,SRANDMEMBER,SSCAN,
##有序集合
ZADD,ZINCRBY,ZRANK,ZCARD,ZSCORE,ZCOUNT,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZLEXCOUNT,ZSCAN,
##位图
SETBIT,GETBIT,BITPOS,BITCOUNT,BITFIELD,
##地理位置
GEOADD,GEODIST,GEOHASH,GEOPOS,GEORADIUS,GEORADIUSBYMEMBER,

```

## 示例  
### 代理到redis cluster
```
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  type: async #支持两种转发模式（sync和async，分别使用jedis和netty转发）
  monitor-enable: true
  monitor-interval-seconds: 30
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
```
### 复杂配置（包含读写分离和分片，单独的一个json文件）
```
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
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
### 使用dashboard管理代理配置
```
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: remote
    remote:
      bid: 1
      bgroup: default
      dynamic: true
      url: http://127.0.0.1:8080
```
### 更多示例和源码
[示例源码](/camellia-samples/camellia-redis-proxy-samples)
  

## 性能测试
[代理到redis cluster](performance-report-1.md)  
[分片](performance-report-2.md)  
[双写](performance-report-3.md)  
[异常测试](performance-report-4.md)  

# camellia-redis-proxy([中文版](redis-proxy-zh.md))
## Instruction  
camellia-redis-proxy is a high performance proxy for redis, which base on netty4.  

## Features
* support redis-standalone、redis-sentinel、redis-cluster
* support read write separation
* support double(multi) write
* support double(multi) read
* support set password
* support blocking commands, such as BLPOP/BRPOP/BRPOPLPUSH/BZPOPMIN/BZPOPMAX and so on
* support pub-sub commands
* support transaction command, only when proxy route to redis-standalone/redis-sentinel/redis-cluster with no-sharding/no-read-write-separate
* support stream commands of redis5.0
* support scan command of redis-standalone/redis-sentinel/redis-cluster, even custom sharding
* support custom sharding
* support read from slave(in redis-sentinel master-slave mode，support read slave, and proxy will automatic process node-down/master-switch/node-expansion）
* support route config refresh online
* support multi-tenancy, then proxy will route business-A to redis1, business-B to redis2 
* support plugin, and build in some plugin, such as bigKeyPlugin, hotKeyPlugin, converterPlugin and so on
* support monitor, such as commands request count、commands spend time, support setting MonitorCallback
* provider http api to get monitor metric data
* provide spring-boot-starter，you can quick start a proxy cluster
* provide default register/discovery component depends on zookeeper, if client's language is java, then you can adjust slightly by use RedisProxyJedisPool instead of JedisPool  
* provide spring-boot-starter, then you can use proxy in register/discovery mode when client is SpringRedisTemplate


## Supported Commands
* Full Supported
```
##DataBase
PING,AUTH,HELLO,ECHO,CLIENT,QUIT,EXISTS,DEL,TYPE,EXPIRE,
EXPIREAT,TTL,PERSIST,PEXPIRE,PEXPIREAT,PTTL,SORT,UNLINK,TOUCH,DUMP,RESTORE,SCAN,COMMAND,CONFIG,
##String
SET,GET,GETSET,MGET,SETNX,SETEX,MSET,DECRBY,DECR,INCRBY,INCR,APPEND,
STRLEN,INCRBYFLOAT,PSETEX,SETRANGE,GETRANGE,SUBSTR,GETEX,GETDEL,
##Hash
HSET,HGET,HSETNX,HMSET,HMGET,HINCRBY,HEXISTS,HDEL,HLEN,HKEYS,
HVALS,HGETALL,HINCRBYFLOAT,HSCAN,HSTRLEN,HRANDFIELD,
##List
RPUSH,LPUSH,LLEN,LRANGE,LTRIM,LINDEX,LSET,LREM,LPOP,RPOP,LINSERT,LPUSHX,RPUSHX,LPOS,
##Set
SADD,SMEMBERS,SREM,SPOP,SCARD,SISMEMBER,SRANDMEMBER,SSCAN,SMISMEMBER,
##ZSet
ZADD,ZINCRBY,ZRANK,ZCARD,ZSCORE,ZCOUNT,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZLEXCOUNT,ZSCAN,
ZPOPMAX,ZPOPMIN,ZMSCORE,ZRANDMEMBER,
##BitMap
SETBIT,GETBIT,BITPOS,BITCOUNT,BITFIELD,
##Geo
GEOADD,GEODIST,GEOHASH,GEOPOS,GEORADIUS,GEORADIUSBYMEMBER,GEOSEARCH,
##HyperLogLog
PFADD
##Stream
XACK,XADD,XCLAIM,XDEL,XLEN,XPENDING,XRANGE,XREVRANGE,XTRIM,XGROUP,XINFO,
##BloomFilter
BF.ADD,BF.EXISTS,BF.INFO,BF.INSERT,BF.LOADCHUNK,BF.MADD,BF.MEXISTS,BF.SCANDUMP,
```

* Restrictive Supported  
support only when all the keys in these command route to same redis-server or same redis-cluster slot  
especially, blocking command don't support multi-write  
```
##DataBase
RENAME,RENAMENX,
##String
MSETNX,
##Set
SINTER,SINTERSTORE,SUNION,SUNIONSTORE,SDIFF,SDIFFSTORE,SMOVE,
##List
BLPOP,BRPOP,RPOPLPUSH,BRPOPLPUSH,LMOVE,BLMOVE,
##ZSet
ZUNION,ZINTER,ZDIFF,ZUNIONSTORE,ZINTERSTORE,ZDIFFSTORE,
BZPOPMAX,BZPOPMIN,ZRANGESTORE,
##HyperLogLog
PFCOUNT,PFMERGE,
##BitMap
BITOP,
##Script
EVAL,EVALSHA,EVAL_RO,EVALSHA_RO,SCRIPT,
##Stream
XREADGROUP,XREAD,
##Geo
GEOSEARCHSTORE,
```

* Partially Supported 1   
only support while have singleton-upstream(no custom sharding) (standalone-redis or redis-sentinel or redis-cluster)
```
##PUBSUB
SUBSCRIBE,PUBLISH,UNSUBSCRIBE,PSUBSCRIBE,PUNSUBSCRIBE,PUBSUB,
##TRANSACTION(keys must in same slot)
MULTI,DISCARD,EXEC,WATCH,UNWATCH,
```

* Partially Supported 2   
only support while have singleton-upstream(no custom sharding) (standalone-redis or redis-sentinel)   
```
##DataBase
KEYS,RANDOMKEY,
``` 

* Partially Supported 3   
only support in special case or special parameter
```
##DataBase
#only support 'select 0'
SELECT,
#only support 'CONFIG GET XXX'
CONFIG,
#only support RESP2
HELLO,
#only proxy start with cluster-mode support
#only support: 'cluster info', 'cluster nodes', 'cluser slots', 'cluser proxy_heartbeat'
CLUSTER,
#direct reply OK for proxy start with cluster-mode
ASKING,
#proxy info
INFO,
``` 

## Quick Start
1) you need a spring-boot project first, then add dependency in your pom.xml，like this（see [sample-code](/camellia-samples/camellia-redis-proxy-samples)）:   
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-proxy-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```
2) code Main Class Application.java, like this: 
```java
import com.netease.nim.camellia.redis.proxy.springboot.EnableCamelliaRedisProxyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCamelliaRedisProxyServer
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```
3) add config in application.yml, like this:  
```yaml
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
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```
4) run Application.java, then a redis proxy to redis cluster is running.  
you can use redis-cli to connect proxy, port is 6380, password is pass123, if you don't want have a password, remove it in application.yml   
```
➜ ~ ./redis-cli -h 127.0.0.1 -p 6380 -a pass123
127.0.0.1:6380> set k1 v1
OK
127.0.0.1:6380> get k1
"v1"
127.0.0.1:6380> mget k1 k2 k3
1) "v1"
2) (nil)
3) (nil)
```

## Different Yaml Config Samples
### 1) route to standalone redis server  
* application.yml  
```yaml
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
      resource: redis://passwd@127.0.0.1:6379
```
### 2) route to redis sentinel  
* application.yml  
```yaml
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
      resource: redis-sentinel://passwd@127.0.0.1:6379,127.0.0.1:6377/master
```
### 3) route with read write separation(need two files)  
* application.yml  
```yaml
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
* resource-table.json  
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://passwd123@127.0.0.1:6379",
    "type": "rw_separate",
    "write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"
  }
}
```
it means write commands will route to redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master, and read commands will route to redis://passwd123@127.0.0.1:6379
### 4) route with sharding(need two files)  
* application.yml  
```yaml
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
* resource-table.json  
```json
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "0-2-4": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381"
    },
    "bucketSize": 6
  }
}
```
it means keys will sharding in 6 buckets, bucket.index=[0,2,4] route to redis://password1@127.0.0.1:6379, others route to redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381 
### 5) route with double write(need two files)  
* application.yml  
```yaml
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
* resource-table.json  
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://passwd1@127.0.0.1:6379",
    "type": "rw_separate",
    "write": {
      "resources": [
        "redis://passwd1@127.0.0.1:6379",
        "redis://passwd2@127.0.0.1:6380"
      ],
      "type": "multi"
    }
  }
}
```
it means:  
all the write-commands(like setex/zadd/hset and so on) will route to both redis://passwd1@127.0.0.1:6379 and redis://passwd2@127.0.0.1:6380, specially redis-cli wll get the command reply from first config write resource=redis://passwd1@127.0.0.1:6379;   
all the read-commands(like get/zrange/mget and so on) will route to redis://passwd1@127.0.0.1:6379.
### 6）route with multi read(need two files)  
* application.yml  
```yaml
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
* resource-table.json  
```json
{
  "type": "simple",
  "operation": {
    "read": {
      "resources": [
        "redis://password1@127.0.0.1:6379",
        "redis://password2@127.0.0.1:6380"
      ],
      "type": "random"
    },
    "type": "rw_separate",
    "write": "redis://passwd1@127.0.0.1:6379"
  }
}
```
it means:  
all the write-commands(like setex/zadd/hset and so on) will route to redis://passwd1@127.0.0.1:6379   
all the read-commands(like get/zrange/mget and so on) will route to redis://passwd1@127.0.0.1:6379 or redis://password2@127.0.0.1:6380 in random
### 7) proxy with sharding/read-write-separation/double-write/multi-read(need two files)  
* application.yml  
```yaml
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
* resource-table.json  
```json
{
  "type": "sharding",
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
      "5": {
        "read": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "random"
        },
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis://password1@127.0.0.1:6379",
      "1-3": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
it means keys sharding in 6 buckets, and bucket.index=4 is read-write-separation and double-write, bucket.index=5 is read-write-separation and double-write/multi-read 
### 7) config from camellia-dashboard  
* application.yml  
```yaml
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
      url: http://127.0.0.1:8080
      check-interval-millis: 5000
```
it means config will get from camellia-dashboard, config named bid=1 and bgroup=default will be used.   
furthermore, config will reload if config changed in camellia-dashboard, redis proxy use http protocol to check config if change, default check interval is 5s.  

### 8) config from camellia-dashboard with multi-config  
* application.yml  
```yaml
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
      url: http://127.0.0.1:8080
      check-interval-millis: 5000
      dynamic: true
```
it means redis proxy support route business-A to redis1, business-B to redis2.   
this feature requires redis-cli declare business type, client can use client setname command to declare, client name with camellia_10_default means use config with bid=10 and bgroup=defualt from camellia-dashboard.
```
➜ ~ ./redis-cli -h 127.0.0.1 -p 6380 -a pass123
127.0.0.1:6379> client setname camellia_10_default
OK
127.0.0.1:6380> set k1 v1
OK
127.0.0.1:6380> get k1
"v1"
127.0.0.1:6380> mget k1 k2 k3
1) "v1"
2) (nil)
3) (nil)
```
take java/jedis as example, you need init JedisPool like this:  
```java
public class Test {
    public static void main(String[] args) {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6380,
                2000, "pass123", 0, "camellia_10_default");
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.setex("k1", 10, "v1");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
```

### performance report
[v1.0.37](performance/performance-report.md)

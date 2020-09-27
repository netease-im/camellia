
# camellia-redis-proxy([English](redis-proxy.md))
## 介绍  
camellia-redis-proxy是一款高性能的redis代理，使用netty4和camellia-core开发

## 特性
* 支持代理到redis、redis sentinel、redis cluster
* 支持设置密码
* 支持阻塞式命令，如BLPOP/BRPOP/BRPOPLPUSH等
* 支持redis5.0的Streams
* 支持自定义分片
* 支持读写分离
* 支持双（多）写
* 支持配置在线变更（需要camellia-dashboard)
* 支持多配置，即业务A路由到redis1，B业务路由到redis2（需要camellia-dashboard)
* 支持自定义方法拦截器，可以用于拦截非法请求（如自定义key/value不得超过多少字节等）
* 支持监控，可以监控各命令的调用量、方法耗时等
* 提供了一个spring-boot-starter，可以快速搭建proxy集群
* 提供了一个默认的注册发现实现组件（依赖zookeeper），如果端侧是java，则可以很简单的将JedisPool替换为RedisProxyJedisPool，即可接入redis proxy 

## 支持的命令
* 完整支持
```
##DataBase
PING,AUTH,ECHO,CLIENT,QUIT,EXISTS,DEL,TYPE,EXPIRE,
EXPIREAT,TTL,PERSIST,PEXPIRE,PEXPIREAT,PTTL,SORT
##String
SET,GET,GETSET,MGET,SETNX,SETEX,MSET,DECRBY,DECR,INCRBY,INCR,APPEND,
STRLEN,INCRBYFLOAT,PSETEX,SETRANGE,GETRANGE,SUBSTR,
##Hash
HSET,HGET,HSETNX,HMSET,HMGET,HINCRBY,HEXISTS,HDEL,HLEN,HKEYS,
HVALS,HGETALL,HINCRBYFLOAT,HSCAN,
##List
RPUSH,LPUSH,LLEN,LRANGE,LTRIM,LINDEX,LSET,LREM,LPOP,RPOP,LINSERT,LPUSHX,RPUSHX,
##Set
SADD,SMEMBERS,SREM,SPOP,SCARD,SISMEMBER,SRANDMEMBER,SSCAN,
##ZSet
ZADD,ZINCRBY,ZRANK,ZCARD,ZSCORE,ZCOUNT,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZLEXCOUNT,ZSCAN,
##BitMap
SETBIT,GETBIT,BITPOS,BITCOUNT,BITFIELD,
##Geo
GEOADD,GEODIST,GEOHASH,GEOPOS,GEORADIUS,GEORADIUSBYMEMBER,
##HyperLogLog
PFADD
```

* 限制性支持（当前仅当以下命令涉及的key被分在相同分片，或者被分在redis cluster的相同slot下）  
```
##DataBase
RENAME,RENAMENX,
##String
MSETNX,
##Set
SINTER,SINTERSTORE,SUNION,SUNIONSTORE,SDIFF,SDIFFSTORE,SMOVE,
##List
BLPOP,BRPOP,RPOPLPUSH,BRPOPLPUSH,
##ZSet
ZUNIONSTORE,ZINTERSTORE,
##HyperLogLog
PFCOUNT,PFMERGE,
##BitMap
BITOP,
##Script
EVAL,EVALSHA,
```

* 部分支持（当前仅当路由后端是单个redis或者单个redis-sentinel）  
```
##DataBase
KEYS,SCAN,
``` 

## 快速开始
1) 首先创建一个spring-boot的工程，然后添加以下依赖，如下：（see [sample-code](/camellia-samples/camellia-redis-proxy-samples)）:   
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-proxy-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```
2) 编写主类Application.java, 如下: 
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
3) 配置application.yml, 如下:  
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
4) 启动Application.java即可.
你可以使用redis-cli去连接proxy，端口是6380，密码是pass123（如果不需要密码，则在application.yml里去掉这一行即可）
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

## 不同的YML配置示例
### 1) 代理到单点redis server  
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
### 2) 代理到redis sentinel  
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
### 3) 配置读写分离（需要单独的json文件）  
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
上面的配置表示：  
写命令会代理到redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master  
读命令会代理到redis://passwd123@127.0.0.1:6379  
### 4) 配置分片（需要单独的json文件） 
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
  "type": "shading",
  "operation": {
    "operationMap": {
      "0-2-4": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381"
    },
    "bucketSize": 6
  }
}
```
上面的配置表示key划分为6个分片，其中分片[0,2,4]代理到redis://password1@127.0.0.1:6379，其他的代理到redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
### 5) 配置双写（需要单独的json文件）  
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
上面的配置表示：
所有的写命令（如setex/zadd/hset）代理到redis://passwd1@127.0.0.1:6379和redis://passwd2@127.0.0.1:6380（即双写），特别的，客户端的回包是看的配置的第一个写地址代理到redis://passwd1@127.0.0.1:6379
所有的读命令（如get/zrange/mget）代理到redis://passwd1@127.0.0.1:6379.
### 6) 混合配置分片、双写、读写（需要单独的json文件）
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
      "0-2": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
上面的配置表示key被话费为6个分片，其中分片4配置了读写分离和双写的逻辑
### 7) 配置为从camellia-dashboard读取  
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
上面的配置表示proxy的路由配置会从camellia-dashboard获取，获取的是bid=1以及bgroup=default的那份配置  
此外，proxy会定时检查camellia-dashboard上的配置是否更新了，若更新了，则为更新本地配置，检查的间隔是5000ms

### 8) 从camellia-dashboard获取多份config  
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
上面的配置表示proxy使用camellia-dashboard获取动态配置，使用哪份配置取决于客户端建连时的声明（使用client setname命令来声明）  
下面的例子表示client要求proxy使用bid=10以及bgroup=default的那份配置  
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
如果端侧是Java，且使用了Jedis，则可以这样调用：
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

## 自定义CommandInterceptor
如果你想添加一个自定义的方法拦截器，则应该实现CommandInterceptor接口，如下：
```java
package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;

public class CustomCommandInterceptor implements CommandInterceptor {
    
    private static final CommandInterceptResponse KEY_TOO_LONG = new CommandInterceptResponse(false, "key too long");
    private static final CommandInterceptResponse VALUE_TOO_LONG = new CommandInterceptResponse(false, "value too long");

    @Override
    public CommandInterceptResponse check(Long bid, String bgroup, Command command) {
        if (command.getRedisCommand() == RedisCommand.SET) {
            byte[] key = command.getObjects()[1];
            if (key.length > 256) {
                return KEY_TOO_LONG;
            }
            byte[] value = command.getObjects()[2];
            if (value.length > 1024 * 1024) {
                return VALUE_TOO_LONG;
            }
        }
        return CommandInterceptResponse.SUCCESS;
    }
}
```
随后，在application.yml里这样配置：
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
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomCommandInterceptor
```
上面的配置表示如果一个set命令过来，那么key的长度不得超过256，value的长度不得超过1M，否则会返回指定的错误信息


## 双（多）写
proxy支持设置双（多）写的类型，有三个可选项：  
### first_resource_only
表示如果配置的第一个写地址返回了，则立即返回给客户端，这是默认的模式
### all_resources_no_check
表示需要配置的所有写地址都返回了，才返回给给客户端，返回的是第一个地址的返回结果，你可以这样配置来生效这种模式：  
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
      resource: redis://@127.0.0.1:6379
    redis-conf:
      multi-write-type: all_resources_no_check
```
### all_resources_check_error
表示需要配置的所有写地址都返回了，才返回给客户端，并且会校验是否所有地址都是返回的非error结果，如果是，则返回第一个地址的返回结果；否则返回第一个错误结果，你可以这样配置来生效这种模式：  
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
      resource: redis://@127.0.0.1:6379
    redis-conf:
      multi-write-type: all_resources_check_error
```  

## 自定义分片函数
你可以自定义分片函数，分片函数会计算出一个key的哈希值，和分片大小（bucketSize）取余后，得到该key所属的分片。  
默认的分片函数是com.netease.nim.camellia.core.client.env.DefaultShadingFunc  
你可以继承com.netease.nim.camellia.core.client.env.AbstractSimpleShadingFunc实现自己想要的分片函数，类似于这样：  
```java
package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.core.client.env.AbstractSimpleShadingFunc;

public class CustomShadingFunc extends AbstractSimpleShadingFunc {
    
    @Override
    public int shadingCode(byte[] key) {
        if (key == null) return 0;
        if (key.length == 0) return 0;
        int h = 0;
        for (byte d : key) {
            h = 31 * h + d;
        }
        return (h < 0) ? -h : h;
    }
}
```  
然后在application.yml配置即可，类似于这样：
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
    redis-conf:
      shading-func: com.netease.nim.camellia.redis.proxy.samples.CustomShadingFunc
```


## 部署架构
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的   
此时有两种方式来部署多实例的架构：  
* 前置四层代理(如lvs/阿里slb), 如下:   
<img src="redis-proxy-lb.png" width="60%" height="60%">  

此时，你可以像调用单点redis一样调用redis proxy

* 注册发现模式(使用zk/eureka/consul), 如下:  
<img src="redis-proxy-zk.png" width="60%" height="60%">
   
此时，你需要在客户端侧实现一下负载均衡策略
                                                                            
## 集成Zookeeper
camellia提供了一个基于zookeeper的注册发现模式的默认实现，你可以这样来使用它：
1) 首先在redis proxy上引入maven依赖： 
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-zk-registry-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
``` 
2) 在redis proxy的application.yml添加如下配置：
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
      resource: redis://@127.0.0.1:6379

camellia-redis-zk-registry:
  enable: true
  zk-url: 127.0.0.1:2181,127.0.0.2:2181
  base-path: /camellia
```
则启动后redis proxy会注册到zk(127.0.0.1:2181,127.0.0.2:2181)  
3) 客户端侧  
此时你需要自己从zk上获取proxy的地址列表，然后自己实现以下客户端侧的负载均衡策略。  
如果端侧是Java，那么camellia提供了RedisProxyJedisPool，方便你进行改造。  
首先，在客户端侧的工程里添加如下maven依赖：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-zk-discovery</artifactId>
    <version>a.b.c</version>
</dependency>
``` 
然后你就可以使用RedisProxyJedisPool代替你原先使用的JedisPool，其他的操作都一样：  
```java
import com.netease.nim.camellia.redis.proxy.RedisProxyJedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

public class TestClient {

    public static void main(String[] args) {
        String zkUrl = "127.0.0.1:2181,127.0.0.2:2181";
        String basePath = "/camellia";
        String applicationName = "camellia-redis-proxy-server";
        ZkProxyDiscovery zkProxyDiscovery = new ZkProxyDiscovery(zkUrl, basePath, applicationName);
        
        int timeout = 2000;
        String password = "pass123";
        RedisProxyJedisPool jedisPool = new RedisProxyJedisPool(zkProxyDiscovery, new JedisPoolConfig(), timeout, password);
        
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
如果redis proxy使用了camellia-dashboard，且使用了动态的多组配置，那么客户端侧可以这样写：  
```java
import com.netease.nim.camellia.redis.proxy.RedisProxyJedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

public class TestClient {

    public static void main(String[] args) {
        String zkUrl = "127.0.0.1:2181,127.0.0.2:2181";
        String basePath = "/camellia";
        String applicationName = "camellia-redis-proxy-server";
        ZkProxyDiscovery zkProxyDiscovery = new ZkProxyDiscovery(zkUrl, basePath, applicationName);
        
        long bid = 10;
        String bgroup = "default";
        int timeout = 2000;
        String password = "pass123";
        RedisProxyJedisPool jedisPool = new RedisProxyJedisPool(bid, bgroup, zkProxyDiscovery, new JedisPoolConfig(), timeout, password);
        
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

## Monitor
### local monitor
redis proxy自带监控，你可以使用如下的静态方法获取监控指标
```
com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor
```
的
``` 
public static Stats getStats();
```
监控默认是关闭，你可以这样打开：  
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  monitor-enable: true
  command-spend-time-monitor-enable: true
  monitor-interval-seconds: 10
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```
上面的配置表示RedisMonitor会以10s一个周期收集监控指标，并且还会收集每个方法的耗时  
每10s会刷新一次Stats对象  
### remote monitor
如果proxy使用了camellia-dashboard来管理配置，那么proxy默认会上报统计数据给dashboard（每分钟一次）  
dashboard会汇总所有proxy的数据  
如果想要关闭上报，你可以这样配置：  
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
      dynamic: true
      url: http://127.0.0.1:8080
      monitor: false
```

## Console Server
当redis proxy启动的时候，会同时启动一个http服务器console server，默认端口是16379  
我们可以用console server做一些监控指标采集、优雅上下线等操作，使用方法是自己实现一个ConsoleService（继承自ConsoleServiceAdaptor）即可，如下所示：  
```java
@Component
public class MyConsoleService extends ConsoleServiceAdaptor implements InitializingBean {

    @Autowired
    private CamelliaRedisProxyBoot redisProxyBoot;

    @Autowired
    private CamelliaRedisProxyZkRegisterBoot zkRegisterBoot;

    @Override
    public ConsoleResult online() {
        zkRegisterBoot.register();
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        zkRegisterBoot.deregister();
        return super.offline();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setServerPort(redisProxyBoot.getPort());
    }
}
```
console server包含五个http api:    
* /online
会将一个全局的内存变量status设置成ONLINE状态
* /offline
会将一个全局的内存变量status设置成OFFLINE状态    
并且如果此时proxy是idle的，则返回http.code=200，否则会返回http.code=500  
ps: 当且仅当最后一个命令执行完成已经超过10s了，才会处于idle     
* /status
如果status=ONLINE, 则返回http.code=200,    
否则返回http.code=500  
* /check
如果服务器端口可达（指的是proxy的服务端口），则返回200，否则返回500
* /custom
一个自定义接口，可以通过设置不同的http参数来表示不同的请求类型

在上面的例子中，MyConsoleService注入了CamelliaRedisProxyZkRegisterBoot，  
如果我们调用/online，则CamelliaRedisProxyZkRegisterBoot会注册到zk  
如果我们调用/offline，则CamelliaRedisProxyZkRegisterBoot会从zk上摘除，因为如果proxy没有idle，offline会返回500，因此我们可以反复调用offline直到返回200，此时我们就可以shutdown掉proxy了而不用担心命令执行中被打断了    

## 性能
redis proxy有3种工作模式(since v1.0.9)
### NoneQueue
这是默认的模式，此时命令会被proxy直接转发给目标的后端redis
### LinkedBlockingQueue
在这种模式下，命令首先会进入一个本地队列，然后再转发给后端，转发前会尽可能的将命令进行合并转发，一遍减少网络io次数，提供性能  
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
      resource: redis://@127.0.0.1:6379
    redis-conf:
      queue-type: linkedblockingqueue
```
### Disruptor
这种模式的工作流程和LinkedBlockingQueue是一样的，只是队列使用了无锁的Disruptor，这种模式有最高的性能，但是CPU开销也是最大的
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
      resource: redis://@127.0.0.1:6379
    redis-conf:
      queue-type: disruptor
```

### 性能测试报告(比较三种模式)
[使用redis-benchmark（v1.0.8 vs v1.0.9）](performance-report-6-zh.md)    
[使用网易NPT性能测试平台（v1.0.8 vs v1.0.9）](performance-report-7.md)

历史性能测试报告  
[代理到redis cluster（v1.0.4）](performance-report-1.md)  
[分片（v1.0.4）](performance-report-2.md)  
[双写（v1.0.4）](performance-report-3.md)  
[异常测试（v1.0.4）](performance-report-4.md)  
[云主机环境测试（v1.0.7）](performance-report-5.md)  

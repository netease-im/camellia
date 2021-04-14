
# camellia-redis-proxy([English](redis-proxy-en.md))
## 介绍  
camellia-redis-proxy是一款高性能的redis代理，使用netty4开发

## 特性
* 支持代理到redis、redis sentinel、redis cluster
* 支持设置密码
* 支持阻塞式命令，如BLPOP/BRPOP/BRPOPLPUSH/BZPOPMIN/BZPOPMAX等
* 支持pubsub命令
* 支持事务命令（MULTI/EXEC/DISCARD/WATCH/UNWATCH），当前仅当代理到redis/redis-sentinel且无分片/无读写分离时支持
* 支持redis5.0的Streams命令
* 支持自定义分片
* 支持读写分离
* 支持读slave（redis-sentinel的主从模式下，支持配置读slave，且proxy能自动感知节点宕机、主从切换和从节点扩容）
* 支持双（多）写
* 支持双（多）读
* 支持路由配置在线变更
* 支持多配置，即业务A路由到redis1，B业务路由到redis2（需要camellia-dashboard)
* 支持自定义方法拦截器，可以用于拦截非法请求（如自定义key/value不得超过多少字节等）
* 支持监控，可以监控各命令的调用量、方法耗时等，支持设置监控回调MonitorCallback
* 支持慢查询监控，支持设置SlowCommandMonitorCallback
* 支持热key监控，支持设置HotKeyMonitorCallback
* 支持热key在proxy层的本地缓存（仅支持GET命令），支持设置HotKeyCacheStatsCallback
* 支持大key监控，支持设置BigKeyMonitorCallback
* 支持监控配置（如开关、阈值等）的在线变更
* 提供了一个httpAPI用于获取监控指标数据
* 提供了一个spring-boot-starter，可以快速搭建proxy集群
* 提供了一个默认的注册发现实现组件（依赖zookeeper），如果端侧是java，则可以很简单的将JedisPool替换为RedisProxyJedisPool，即可接入redis proxy  
* 提供了一个spring-boot-starter用于SpringRedisTemplate以注册发现模式接入proxy

## 支持的命令
* 完整支持
```
##DataBase
PING,AUTH,ECHO,CLIENT,QUIT,EXISTS,DEL,TYPE,EXPIRE,
EXPIREAT,TTL,PERSIST,PEXPIRE,PEXPIREAT,PTTL,SORT,UNLINK,TOUCH,
##String
SET,GET,GETSET,MGET,SETNX,SETEX,MSET,DECRBY,DECR,INCRBY,INCR,APPEND,
STRLEN,INCRBYFLOAT,PSETEX,SETRANGE,GETRANGE,SUBSTR,
##Hash
HSET,HGET,HSETNX,HMSET,HMGET,HINCRBY,HEXISTS,HDEL,HLEN,HKEYS,
HVALS,HGETALL,HINCRBYFLOAT,HSCAN,HSTRLEN,
##List
RPUSH,LPUSH,LLEN,LRANGE,LTRIM,LINDEX,LSET,LREM,LPOP,RPOP,LINSERT,LPUSHX,RPUSHX,LPOS,
##Set
SADD,SMEMBERS,SREM,SPOP,SCARD,SISMEMBER,SRANDMEMBER,SSCAN,SMISMEMBER,
##ZSet
ZADD,ZINCRBY,ZRANK,ZCARD,ZSCORE,ZCOUNT,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZLEXCOUNT,ZSCAN,
ZPOPMAX,ZPOPMIN,ZMSCORE,
##BitMap
SETBIT,GETBIT,BITPOS,BITCOUNT,BITFIELD,
##Geo
GEOADD,GEODIST,GEOHASH,GEOPOS,GEORADIUS,GEORADIUSBYMEMBER,GEOSEARCH,
##HyperLogLog
PFADD
##Stream
XACK,XADD,XCLAIM,XDEL,XLEN,XPENDING,XRANGE,XREVRANGE,XTRIM,XGROUP,XINFO,
```

* 限制性支持
当前仅当以下命令涉及的key被分在相同分片，或者被分在redis cluster的相同slot下  
特别的，如果是阻塞式的命令，则不允许双（多）写  
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
EVAL,EVALSHA,
##Stream
XREADGROUP,XREAD,
##Geo
GEOSEARCHSTORE,
```

* 部分支持1
当前仅当路由后端是单个redis或者单个redis-sentinel或者单个redis-cluster  
```
##PUBSUB
SUBSCRIBE,PUBLISH,UNSUBSCRIBE,PSUBSCRIBE,PUNSUBSCRIBE,PUBSUB,
```

* 部分支持2
当前仅当路由后端是单个redis或者单个redis-sentinel  
```
##DataBase
KEYS,SCAN,
MULTI,DISCARD,EXEC,WATCH,UNWATCH,
``` 

## 快速开始一
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

## 快速开始二
下载安装包并解压：
```
wget https://github.com/netease-im/camellia/releases/download/v1.0.22/camellia-redis-proxy-1.0.22.tar.gz
tar zxvf camellia-redis-proxy-1.0.22.tar.gz
cd camellia-redis-proxy-1.0.22/
```
按需修改BOOT-INF/classes/下的配置文件：
* application.yml
* logback.xml
* camellia-redis-proxy.properties
* resource-table.json

按需调整start.sh的启动参数（主要是JVM参数），默认参数如下（确保已经安装了jdk8或以上，并添加到path）：
```
java -XX:+UseG1GC -Xms2048m -Xmx2048m -server org.springframework.boot.loader.JarLauncher
```
直接启动即可：
```
./start.sh
```

## 快速开始三（基于fatJar和sample-code)
下载源码
```
git clone https://github.com/netease-im/camellia.git
```
按需修改[sample-code](/camellia-samples/camellia-redis-proxy-samples) 中的配置文件：
* application.yml
* logback.xml
* camellia-redis-proxy.properties
* resource-table.json

使用maven编译
```
cd camellia
mvn clean install
```
找到可执行jar包，使用java -jar命令运行即可(注意设置内存和GC，并确保已经安装了jdk8或以上，并添加到path）：
```
cd camellia-samples/camellia-redis-proxy-samples/target
java -XX:+UseG1GC -Xms2048m -Xmx2048m -server -jar camellia-redis-proxy-samples-1.0.23-SNAPSHOT.jar 
```

## 路由配置
路由配置表示了camellia-redis-proxy在收到客户端的redis命令之后的转发规则，包括：
* 支持的后端redis类型
* 动态配置
* json-file配置示例（双写、读写分离、分片等）
* 集成camellia-dashboard
* 不同的双（多）写模式
* 自定义分片函数

具体可见：[路由配置](route.md)

## 控制
camellia-redis-proxy提供了自定义命令拦截器来达到控制客户端访问的目的  

具体可见：[控制](control.md)

## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的，包括：
* 部署模式
* 集成Zookeeper
* 优雅上下线
* 客户端接入（java之jedis）
* 客户端接入（java之SpringRedisTemplate)
* 客户端接入（其他语言）
* 注意事项

具体可见：[部署和接入](deploy.md)

## 监控
camellia-redis-proxy提供了丰富的监控功能，包括：
* 请求tps、请求rt
* 慢查询监控
* 热key监控
* 大key监控
* 热key缓存功能
* 通过httpAPI获取监控数据
* 监控配置的动态修改

具体可见：[监控](monitor.md)

## 应用场景
* 业务开始使用单点redis或者redis-sentinel，现在需要切换到redis-cluster，但是客户端需要改造（比如jedis访问redis-sentinel和redis-cluster是不一样的），此时你可以使用proxy，从而做到不改造（使用四层代理LB）或者很少的改造（使用注册中心）
* 使用双写功能进行集群的迁移
* 使用双写的功能进行集群灾备，比如双写到另外的机房
* 使用分片功能应对单集群容量不足的问题（单个redis-cluster集群有节点和容量上限）
* 使用自定义命令拦截器约束客户端侧的命令调用，比如屏蔽某些命令、限制key/value的格式或者大小、限制客户端的来源ip等
* 使用大key/热key/慢查询/tps等丰富的监控功能来检测你的系统
* 使用热key缓存功能来应对突发流量
* 使用双读功能来扩展集群的读能力上限
* 等等

## 性能测试报告
[基于v1.0.19的性能测试报告](performance-report-8.md)

历史性能测试报告  
[代理到redis cluster（v1.0.4）](performance-report-1.md)  
[分片（v1.0.4）](performance-report-2.md)  
[双写（v1.0.4）](performance-report-3.md)  
[异常测试（v1.0.4）](performance-report-4.md)  
[云主机环境测试（v1.0.7）](performance-report-5.md)  
[使用redis-benchmark（v1.0.8 vs v1.0.9）](performance-report-6-zh.md)    
[使用网易NPT性能测试平台（v1.0.8 vs v1.0.9）](performance-report-7.md)
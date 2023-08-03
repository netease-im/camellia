
# camellia-redis-proxy([English](redis-proxy-en.md))
## 介绍  
camellia-redis-proxy是一款高性能的redis代理，使用netty4开发

## 特性
* 支持代理到redis-standalone、redis-sentinel、redis-cluster
* 支持其他proxy作为后端（如双写迁移场景），如 [twemproxy](https://github.com/twitter/twemproxy) 、[codis](https://github.com/CodisLabs/codis) 等
* 支持 [kvrocks](https://github.com/apache/kvrocks) 、 [pika](https://github.com/OpenAtomFoundation/pika) 、 [tendis](https://github.com/Tencent/Tendis) 等作为后端
* 支持自定义分片
* 支持读写分离
* 支持双（多）写，可以proxy直连双写，也可以基于mq（如kafka）双写，也可以基于插件体系自定义双写规则
* 支持双（多）读
* 支持设置密码
* 支持SELECT命令，当前仅当后端redis不包含redis-cluster（此时仅支持SELECT 0），可以是redis-standalone/redis-sentinel/redis-proxies或者其组合（分片/读写分离）
* 支持阻塞式命令，如BLPOP/BRPOP/BRPOPLPUSH/BZPOPMIN/BZPOPMAX等
* 支持PUBSUB系列命令，代理到redis-standalone/redis-sentinel/redis-cluster均支持
* 支持事务命令（MULTI/EXEC/DISCARD/WATCH/UNWATCH），代理到redis-standalone/redis-sentinel/redis-cluster均支持
* 支持redis5.0的STREAMS系列命令
* 支持SCAN命令（代理到redis-standalone/redis-sentinel/redis-cluster均支持，自定义分片时也支持）
* 支持阿里TairZSet、TairHash、TairString系列命令
* 支持RedisJSON和RedisSearch系列命令
* 支持读slave（redis-sentinel/redis-cluster均支持配置读从节点）
* 支持多租户，即租户A路由到redis1，租户B路由到redis2（可以通过不同的clientname区分，也可以通过不同的password区分）
* 支持多租户动态路由，支持自定义的动态路由数据源(内置：本地配置文件、nacos、etcd等，也可以自定义)
* 支持自定义插件，并且内置了很多插件，可以按需使用（包括：大key监控、热key监控、热key缓存、key命名空间、ip黑白名单、速率控制等等）
* 支持丰富的监控，可以监控客户端连接数、调用量、方法耗时、大key、热key、后端redis连接数和耗时等，并且支持以http接口形式获取监控数据
* 支持info命令获取服务器相关信息（包括后端redis集群的信息）
* 提供了一个spring-boot-starter，可以快速搭建proxy集群
* 高可用，可以基于lb组成集群，也可以基于注册中心组成集群，也可以伪装成redis-cluster组成集群
* 提供了一个默认的注册发现实现组件（依赖zookeeper），如果端侧是java，则可以很简单的将JedisPool替换为RedisProxyJedisPool，即可接入redis proxy  
* 提供了一个spring-boot-starter用于SpringRedisTemplate以注册发现模式接入proxy
* 支持整合hbase实现string/zset/hash等数据结构的冷热分离存储操作，具体见: [redis-proxy-hbase](/docs/redis-proxy-hbase/redis-proxy-hbase.md)

## 支持的命令
* 完整支持
```
##DataBase
PING,AUTH,HELLO,ECHO,CLIENT,QUIT,EXISTS,DEL,TYPE,EXPIRE,
EXPIREAT,TTL,PERSIST,PEXPIRE,PEXPIREAT,PTTL,SORT,UNLINK,TOUCH,DUMP,RESTORE,SCAN,COMMAND,
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
##TairZSet
EXZADD,EXZINCRBY,EXZSCORE,EXZRANGE,EXZREVRANGE,EXZRANGEBYSCORE,EXZREVRANGEBYSCORE,
EXZRANGEBYLEX,EXZREVRANGEBYLEX,EXZREM,EXZREMRANGEBYSCORE,EXZREMRANGEBYRANK,EXZREMRANGEBYLEX,
EXZCARD,EXZRANK,EXZREVRANK,EXZCOUNT,EXZMSCORE,EXZLEXCOUNT,EXZRANDMEMBER,EXZSCAN,EXZPOPMAX,EXZPOPMIN,
##TairHash
EXHSET,EXHGET,EXHMSET,EXHPEXPIREAT,EXHPEXPIRE,EXHEXPIREAT,EXHEXPIRE,EXHPERSIST,EXHPTTL,EXHTTL,
EXHVER,EXHSETVER,EXHINCRBY,EXHINCRBYFLOAT,EXHGETWITHVER,EXHMGET,EXHMGETWITHVER,EXHDEL,EXHLEN,
EXHEXISTS,EXHSTRLEN,EXHKEYS,EXHVALS,EXHGETALL,EXHGETALLWITHVER,EXHSCAN,
##TairString
EXSET,EXGET,EXSETVER,EXINCRBY,EXINCRBYFLOAT,EXCAS,EXCAD,EXAPPEND,EXPREPEND,EXGAE,
##RedisJSON
JSON.ARRAPPEND,JSON.ARRINDEX,JSON.ARRINSERT,JSON.ARRLEN,JSON.ARRPOP,JSON.ARRTRIM,JSON.CLEAR,
JSON.DEL,JSON.FORGET,JSON.GET,JSON.MGET,JSON.NUMINCRBY,JSON.NUMMULTBY,JSON.OBJKEYS,JSON.OBJLEN,
JSON.RESP,JSON.SET,JSON.STRAPPEND,JSON.STRLEN,JSON.TOGGLE,JSON.TYPE,
```

* 限制性支持
当且仅当以下命令涉及的key被分在相同分片，或者被分在redis cluster的相同slot下  
特别的，如果是阻塞式的命令，则不允许双（多）写  
```
##DataBase
RENAME,RENAMENX,
##String
MSETNX,
##Set
SINTER,SINTERSTORE,SUNION,SUNIONSTORE,SDIFF,SDIFFSTORE,SMOVE,
##List
BLPOP,BRPOP,RPOPLPUSH,BRPOPLPUSH,LMOVE,BLMOVE,LMPOP,BLMPOP,
##ZSet
ZINTER,ZINTERSTORE,ZINTERCARD,ZUNION,ZUNIONSTORE,ZDIFF,ZDIFFSTORE,
BZPOPMAX,BZPOPMIN,ZRANGESTORE,ZMPOP,BZMPOP,
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
##TairZSet
EXZUNIONSTORE,EXZUNION,EXZINTERSTORE,EXZINTER,EXZINTERCARD,
EXZDIFFSTORE,EXZDIFF,EXBZPOPMIN,EXBZPOPMAX,
```

* 部分支持1  
当且仅当路由不是自定义分片    
备注：1.1.4开始，PUBSUB命令支持双写（会订阅双写中的第一个地址，发布给双写中的所有地址）  
```
##PUBSUB(will sub first write redis resource, pub all write redis resource)
SUBSCRIBE,PUBLISH,UNSUBSCRIBE,PSUBSCRIBE,PUNSUBSCRIBE,PUBSUB,
```

* 部分支持2  
当且仅当路由后端是单个redis-standalone或者单个redis-sentinel或者单个redis-cluster  
备注：1.2.6版本开始，TRANSACTION支持双写，但是要求读地址只能配置一个，且必须和写地址的第一个是一样的     
```
##TRANSACTION(keys must in same slot)
MULTI,DISCARD,EXEC,WATCH,UNWATCH,
##RedisSearch
FT.LIST,FT.AGGREGATE,FT.ALIASADD,FT.ALIASDEL,FT.ALIASUPDATE,FT.ALTER,FT.CONFIG,
FT.CREATE,FT.CURSOR,FT.DICTADD,FT.DICTDEL,FT.DICTDUMP,FT.DROPINDEX,FT.EXPLAIN,
FT.EXPLAINCLI,FT.INFO,FT.PROFILE,FT.SEARCH,FT.SPELLCHECK,FT.SYNDUMP,
FT.SYNUPDATE,FT.TAGVALS,
```

* 部分支持3
当且仅当路由后端是单个redis-standalone或者单个redis-sentinel  
```
##DataBase
KEYS,RANDOMKEY,
``` 

* 部分支持4
仅支持部分参数
```
##DataBase
#if upstream contains redis-cluster, only support 'select 0', other-wise, support select xx
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

## 快速开始一
1) 首先创建一个spring-boot的工程，然后添加以下依赖（最新1.2.12），如下：（see [sample-code](/camellia-samples/camellia-redis-proxy-samples)）:   
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-proxy-spring-boot-starter</artifactId>
  <version>1.2.12</version>
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

## 快速开始二（基于安装包）
参见：[quick-start-package](quickstart/quick-start-package.md)

## 快速开始三（基于fatJar和sample-code)
参见：[quick-start-fat-jar](quickstart/quick-start-fat-jar.md)

## 快速开始四（不使用spring-boot-stater)
参见：[quick-start-no-spring-boot](quickstart/quick-start-no-spring-boot.md)

## 源码解读
具体可见：[代码结构](code/proxy-code.md)

## 路由配置
路由配置表示了camellia-redis-proxy在收到客户端的redis命令之后的转发规则，包括：
* 最简单的示例
* 支持的后端redis类型
* 动态配置和复杂配置（读写分离、分片等）
* 多租户支持
* 使用camellia-dashboard管理多租户动态路由
* 集成ProxyRouteConfUpdater自定义管理多租户动态路由

具体可见：[路由配置](auth/route.md)

## 插件体系
* 1.1.x版本开始，重构了监控、大key、热key等功能，统一作为插件化体系的一部分，用户可以通过简单的配置按需引入内置的插件
* 插件使用统一的接口来拦截和控制请求和响应
* proxy内置了很多插件，可以通过简单配置后即可直接使用
* 你也可以实现自定义插件

具体可见：[插件](plugin/plugin.md)

## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的，包括：
* 部署模式
* 集成Zookeeper
* 伪redis-cluster模式
* 随机端口
* 优雅上下线
* 客户端接入（java之jedis）
* 客户端接入（java之SpringRedisTemplate)
* 客户端接入（其他语言）
* 注意事项（容器环境部署）
* 部署最佳实践

具体可见：[部署和接入](deploy/deploy.md)

## 监控
camellia-redis-proxy提供了丰富的监控功能，包括：
* 提供的监控项
* 监控数据获取方式
* 通过info命令获取服务器相关信息
* 把proxy当做一个监控redis集群状态的平台（通过http接口暴露）
* 使用prometheus和grafana监控proxy集群

具体可见：[监控](monitor/monitor.md)

## 其他
* 如何控制客户端连接数，具体见[客户端连接控制](other/connectlimit.md)
* netty配置，具体见：[netty-conf](other/netty-conf.md)
* 使用nacos托管proxy配置，具体见：[nacos-conf](other/nacos-conf.md)
* 关于双（多）写的若干问题，具体见：[multi-write](other/multi-write.md)
* 关于scan和相关说明，具体见：[scan](other/scan.md)
* 关于lua的相关说明，具体见：[lua](other/lua.md)
* 使用redis-shake进行数据迁移的说明，具体见：[redis-shake](other/redis-shake.md)
* 关于自定义分片函数，具体见：[sharding](other/sharding.md)
* 如何使用spring管理bean生成，具体见：[spring](other/spring.md)
* 关于多租户的一个完整示例，具体见：[multi-telant](other/multi-telant.md)
* 另一个关于多租户的一个完整示例，具体见：[multi-telant2](other/multi-telant2.md)
* 多读场景下自动摘除故障读节点读，具体见：[multi-read](other/multi-read.md)
* 关于ProxyDynamicConf(camellia-redis-proxy.properties)，具体见：[dynamic-conf](other/dynamic-conf.md)

## 应用场景
* 业务开始使用redis-standalone或者redis-sentinel，现在需要切换到redis-cluster，但是客户端需要改造（比如jedis访问redis-sentinel和redis-cluster是不一样的），此时你可以使用proxy，从而做到不改造（使用四层代理LB）或者很少的改造（使用注册中心）
* 使用双写功能进行集群的迁移或者灾备
* 使用分片功能应对单集群容量不足的问题（单个redis-cluster集群有节点和容量上限）
* 使用内建或者自定义的插件监控和控制客户端的访问（热key、大key、ip黑白名单、速率控制、key命名空间、数据加解密等）
* 使用丰富的监控功能控制系统的运行
* 等等

## 性能测试报告
[基于v1.2.10的性能测试报告](performance/performance.md)  

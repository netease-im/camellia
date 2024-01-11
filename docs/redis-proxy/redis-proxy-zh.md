
# camellia-redis-proxy([English](redis-proxy-en.md))
## 介绍  
camellia-redis-proxy是一款高性能的redis代理，使用netty4开发

## 特性
* 支持代理到redis-standalone、redis-sentinel、redis-cluster
* 支持其他proxy作为后端（如双写迁移场景），如 [twemproxy](https://github.com/twitter/twemproxy) 、[codis](https://github.com/CodisLabs/codis) 等
* 支持 [kvrocks](https://github.com/apache/kvrocks) 、 [pika](https://github.com/OpenAtomFoundation/pika) 、 [tendis](https://github.com/Tencent/Tendis) 等作为后端
* 支持普通的GET/SET/EVAL，也支持MGET/MSET，也支持阻塞型的BLPOP，也支持PUBSUB和TRANSACTION，也支持STREAMS/JSON/SEARCH，也支持TAIR_HASH/TAIR_ZSET/TAIR_STRING
* 支持的命令列表，具体见：[supported_commands](supported_commands.md)
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
* 支持SSL/TLS（proxy到client支持，proxy到redis也支持），具体见：[ssl/tls](/docs/redis-proxy/other/tls.md)
* 支持unix-domain-socket（client到proxy支持，proxy到redis也支持），具体见：[uds](/docs/redis-proxy/other/uds.md)
* 支持多租户，即租户A路由到redis1，租户B路由到redis2（可以通过不同的clientname区分，也可以通过不同的password区分）
* 支持多租户动态路由，支持自定义的动态路由数据源(内置：本地配置文件、nacos、etcd等，也可以自定义)
* 支持自定义插件，并且内置了很多插件，可以按需使用（包括：大key监控、热key监控、热key缓存、key命名空间、ip黑白名单、速率控制等等）
* 支持丰富的监控，可以监控客户端连接数、调用量、方法耗时、大key、热key、后端redis连接数和耗时等，并且支持以http接口形式获取监控数据
* 支持使用prometheus/grafana来监控proxy集群，参考：[prometheus-grafana](prometheus/prometheus-grafana.md)
* 支持info命令获取服务器相关信息（包括后端redis集群的信息）
* 提供了一个spring-boot-starter，可以快速搭建proxy集群
* 高可用，可以基于lb组成集群，也可以基于注册中心组成集群，也可以伪装成redis-cluster组成集群，也可以伪装成redis-sentinel组成集群
* 提供了一个默认的注册发现实现组件（依赖zookeeper），如果端侧是java，则可以很简单的将JedisPool替换为RedisProxyJedisPool，即可接入redis proxy  
* 提供了一个spring-boot-starter用于SpringRedisTemplate以注册发现模式接入proxy
* 支持整合hbase实现string/zset/hash等数据结构的冷热分离存储操作，具体见: [redis-proxy-hbase](/docs/redis-proxy-hbase/redis-proxy-hbase.md)

## 快速开始一
1) 首先创建一个spring-boot的工程，然后添加以下依赖（最新1.2.25），如下：（see [sample-code](/camellia-samples/camellia-redis-proxy-samples)）:   
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-proxy-spring-boot-starter</artifactId>
  <version>1.2.25</version>
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

## 快速开始三（不使用spring-boot-stater)
参见：[quick-start-no-spring-boot](quickstart/quick-start-no-spring-boot.md)

## 如果要使用java21/spring-boot3/docker  
参见：[camellia-jdk21-bootstraps](https://github.com/caojiajun/camellia-jdk21-bootstraps)  

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
* 插件使用统一的接口来拦截和控制请求和响应  
* proxy内置了很多插件，可以通过简单配置后即可直接使用，按需选择  
* 你也可以实现自定义插件  

具体可见：[插件](plugin/plugin.md)

## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的，包括：
* 基于lb组成集群（如lvs等，或者k8s中的service等）
* 基于注册中心组成集群
* 伪redis-cluster模式
* 伪redis-sentinel模式
* jvm-in-sidecar模式
* 优雅上下线

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
* 关于lua/function/tfunction的相关说明，具体见：[lua](other/lua_function.md)
* 使用redis-shake进行数据迁移的说明，具体见：[redis-shake](other/redis-shake.md)
* 关于自定义分片函数，具体见：[sharding](other/sharding.md)
* 如何使用spring管理bean生成，具体见：[spring](other/spring.md)
* 关于多租户的一个完整示例，具体见：[multi-telant](other/multi-telant.md)
* 另一个关于多租户的一个完整示例，具体见：[multi-telant2](other/multi-telant2.md)
* 多读场景下自动摘除故障读节点，具体见：[multi-read](other/multi-read.md)
* 关于ProxyDynamicConf(camellia-redis-proxy.properties)，具体见：[dynamic-conf](other/dynamic-conf.md)
* 在使用haproxy/nginx等四层负载均衡器时，redis-proxy如何获取真实的客户端地址，具体见：[proxy_protocol](other/proxy_protocol.md)
* 热key使用自定义转发路由的一个完整示例，具体见：[hot-key-route-rewrite-sample](other/hot-key-route-rewrite-sample.md)
* redis和proxy混合部署时使用UpstreamAddrConverter提升服务性能的一个例子，具体见：[upstream-addr-converter](other/upstream-addr-converter.md)
* 使用自定义分片时调整分片数量的一个思路，具体见：[custom_resharding](other/custom_resharding.md)
* 使用etcd管理proxy配置的一个完整示例【运维实施】，具体见：[etcd-sample](other/etcd_sample.md)
* 使用nacos管理proxy配置的一个完整示例【运维实施】，具体见：[nacos-sample](other/nacos_sample.md)
* 使用proxy命令批量管理proxy集群配置的说明，具体见：[proxy_command](other/proxy_command.md)
* 关于redis_proxy初始化和预热，具体见：[init](other/init.md)

## 应用场景
* 业务开始使用redis-standalone或者redis-sentinel，现在需要切换到redis-cluster，但是客户端需要改造（比如jedis访问redis-sentinel和redis-cluster是不一样的），此时你可以使用proxy，从而做到不改造（使用四层代理LB）或者很少的改造（使用注册中心）
* 使用双写功能进行集群的迁移或者灾备
* 使用分片功能应对单集群容量不足的问题（单个redis-cluster集群有节点和容量上限）
* 使用内建或者自定义的插件监控和控制客户端的访问（热key、大key、ip黑白名单、速率控制、key命名空间、数据加解密等）
* 使用丰富的监控功能控制系统的运行
* 等等

## 性能测试报告
[基于v1.2.10的性能测试报告](performance/performance.md)  

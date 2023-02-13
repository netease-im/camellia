
## 关于select db

* camellia-redis-proxy不支持select db这样的命令
* 不支持select db的原因是：camellia-redis-proxy屏蔽了后端redis的类型（redis-standalone、redis-sentinel、redis-cluster以及各种分片、读写分离的组合）
* 而redis-cluster只支持db0，因此为了语义上的一致性，就不支持select db的命令
* 因此，后端的redis的不同的db不直接通过select命令来暴露，而是通过不同的resource来定义
* 因此，如如果希望单个proxy集群支持同时访问不同的db，可以基于ClientAuthProvider和ProxyRouteConfUpdater这两个扩展口来实现，实现的效果就是访问proxy的不同的password，会映射到后端不同的db

### 示例
* application.yml
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.DynamicConfClientAuthProvider
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.DynamicConfProxyRouteConfUpdater
      dynamic: true
      bid: 1
      bgroup: db0
```
* camellia-redis-proxy.properties
```properties
##给DynamicConfProxyRouteConfUpdater使用
1.db0.route.conf=redis://@127.0.0.1:6379?db=0
1.db1.route.conf=redis://@127.0.0.1:6379?db=1
1.db2.route.conf=redis://@127.0.0.1:6379?db=2

##给DynamicConfClientAuthProvider使用
password0.auth.conf=1|db0
password1.auth.conf=1|db1
password2.auth.conf=1|db2
```

上述配置表示：  
* 使用password0连接proxy，则表示访问后端的db0
* 使用password1连接proxy，则表示访问后端的db1
* 使用password2连接proxy，则表示访问后端的db2

当然，你也可以自己实现一个自定义的ClientAuthProvider和ProxyRouteConfUpdater这两个扩展口，来达到自定义多租户和自定义db的效果
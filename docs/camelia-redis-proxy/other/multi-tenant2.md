
## 另一个多租户的简单例子

* camellia-redis-proxy支持多租户
* 也就是一个proxy实例可以同时代理多组路由
* 不同路由直接互相独立，对外可以通过不同的proxy密码来区分
* 基于ClientAuthProvider和ProxyRouteConfUpdater这两个扩展口来实现多租户的能力
* 你可以自己实现上述接口，也可以使用内置的，下面展示一个事例，使用内置的MultiTenantClientAuthProvider和MultiTenantProxyRouteConfUpdater来实现多租户的能力
* MultiTenantClientAuthProvider和MultiTenantProxyRouteConfUpdater使用ProxyDynamicConf作为动态路由的数据源（默认读取camellia-redis-proxy.properties）

### 示例
* application.yml
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.MultiTenantClientAuthProvider
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.MultiTenantProxyRouteConfUpdater
```
* camellia-redis-proxy.properties
```properties
#这是一个数组，每一项代表一个路由，支持多组路由
multi.tenant.route.config=[{"name":"route1", "password": "passwd1", "route": "redis://passxx@127.0.0.1:16379"},{"name":"route2", "password": "passwd2", "route": "redis-cluster://@127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382"},{"name":"route3", "password": "passwd3", "route": {"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}}]
```

上述配置表示：  
* 使用passwd1连接proxy，则表示访问route1的路由，指向一个redis-standalone
* 使用passwd2连接proxy，则表示访问route2的路由，指向一个redis-cluster
* 使用passwd3连接proxy，则表示访问route3的路由，指向一个读写分离的路由

如果希望把配置托管给配置中心，则可以使用ProxyDynamicConfLoader扩展口，下面展示一个etcd的例子
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.MultiTenantClientAuthProvider
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.config.etcd.EtcdProxyDynamicConfLoader
  config:
    "etcd.target": "ip:///etcd0:2379,etcd1:2379,etcd2:2379"
    #"etcd.endpoints": "http://etcd0:2379,http://etcd1:2379,http://etcd2:2379" #etcd.target和etcd.endpoints二选一，优先使用etcd.target
    "etcd.config.key": "index/camellia/test"
    "etcd.config.type": "json"
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.MultiTenantProxyRouteConfUpdater
```

配置如下：  
<img src="etcd.jpg" width="100%" height="100%">

如果希望是其他配置中心（如nacos等），则替换ProxyDynamicConfLoader实现即可，具体见: [dynamic-conf](dynamic-conf.md)  
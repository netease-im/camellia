
## 一个多租户的简单例子

* camellia-redis-proxy支持多租户
* 也就是一个proxy实例可以同时代理多组路由
* 不同路由直接互相独立，对外可以通过不同的proxy密码来区分
* 基于ClientAuthProvider和ProxyRouteConfUpdater这两个扩展口来实现多租户的能力

### 示例
* application.yml
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
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
1.default.route.conf=redis://@127.0.0.1:6379
2.default.route.conf=redis-cluster://@127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382
3.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}

##给DynamicConfClientAuthProvider使用
password123.auth.conf=1|default
password456.auth.conf=2|default
password789.auth.conf=3|default
```

上述配置表示：  
* 使用password123连接proxy，则表示访问bid/bgroup=1/default的路由，指向一个redis-standalone
* 使用password456连接proxy，则表示访问bid/bgroup=2/default的路由，指向一个redis-cluster
* 使用password789连接proxy，则表示访问bid/bgroup=3/default的路由，指向一个读写分离的路由

当然，你也可以自己实现一个自定义的ClientAuthProvider和ProxyRouteConfUpdater这两个扩展口（从而可以把多租户的配置托管给你希望的地方），来达到自定义多租户的效果
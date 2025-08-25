
## 使用simple_config管理多租户配置

### 简介
* 使用simple_config接口定义访问一个外部的http服务，动态获取多租户配置
* simple_config见：[simple_config](../../camellia-tools/simple_config.md)

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
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.SimpleConfigProxyRouteConfUpdater
```

* camellia-redis-proxy.properties

```properties
##给DynamicConfClientAuthProvider使用
password123.auth.conf=1|default
password456.auth.conf=2|default
password789.auth.conf=3|default

##给SimpleConfigProxyRouteConfUpdater使用
1.default.simple.config.biz=redis1
2.default.simple.config.biz=redis2
3.default.simple.config.biz=redis3

simple.config.fetch.url=http://xxx/xx
```
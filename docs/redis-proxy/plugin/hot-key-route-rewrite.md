
## HotKeyRouteRewriteProxyPlugin

### 说明
* 功能覆盖了HotKeyProxyPlugin，因此所有HotKeyProxyPlugin的配置项都支持，见：[HotKeyProxyPlugin](hot-key.md)
* 还提供了根据热key对单个命令进行自定义路由的功能，即当HotKeyProxyPlugin发现是热key后
* 仅支持命令仅包含一个key的情况，如get、zrange等，而mget等命令则不支持
* 发布订阅、事务命令、阻塞型命令不支持

### 启用方式
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
  monitor-enable: true
  monitor-interval-seconds: 60
  plugins:
    - hotKeyRouteRewritePlugin
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.DynamicConfClientAuthProvider
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.DynamicConfProxyRouteConfUpdater
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties

## HotKeyProxyPlugin之外的其他配置项

## HotKeyRouteRewriteProxyPlugin独有的配置

### 判定为热key后，需要rewrite的路由，是一个接口RouteRewriteChecker，可以自定义实现，默认是DefaultRouteRewriteChecker
hot.key.route.rewriter.className=com.netease.nim.camellia.redis.proxy.plugin.rewrite.DefaultRouteRewriter

### 使用DefaultRouteRewriteChecker时，可以配置以下参数
#### 表示来自租户bid=1,bgroup=default的命令，如果命令归属的key是热key，如果是get命令则路由到bid=100,bgroup=default，如果是zrange命令则路由到bid=200,bgroup=default
#### 特别的，你也可以把command字段设置为all_commands表示所有命令，设置为read_commands表示所有只读命令，设置为write_commands表示所有只写命令
1.default.hotkey.route.rewrite.config=[{"command":"get","bid":100,"bgroup":"default"},{"command":"zrange","bid":200,"bgroup":"default"}]
#### 表示来自租户bid=2,bgroup=default的命令，如果命令归属的key是热key，则全部转发到bid=300,bgroup=default
2.default.hotkey.route.rewrite.config=[{"command":"all_commands","bid":300,"bgroup":"default"}]

#### 也可以可以配置默认的转发路由，则没有租户级别的转发配置的会走本配置
hotkey.route.rewrite.default.config=[{"command":"read_commands","bid":300,"bgroup":"default"},{"command":"write_commands","bid":400,"bgroup":"default"}]
```

### 一个完整示例
具体见：[hot-key-route-rewrite-sample](../other/hot-key-route-rewrite-sample.md)
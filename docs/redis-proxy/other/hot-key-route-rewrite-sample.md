
## 热key转发插件的一个完整示例

* application.yml

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

* camellia-redis-proxy.properties

```properties

### 多租户配置
1.default.route.conf=redis://@127.0.0.1:6379
2.default.route.conf=redis://@127.0.0.1:6380
100.default.route.conf=redis://@127.0.0.1:6381
200.default.route.conf=redis://@127.0.0.1:6382
300.default.route.conf={"type":"simple","operation":{"read":{"resources":["redis://@127.0.0.1:6379","redis://@127.0.0.1:6380","redis://@127.0.0.1:6381"],"type":"random"},"type":"rw_separate","write":"redis://@127.0.0.1:6379"}}

##给DynamicConfClientAuthProvider使用
password123.auth.conf=1|default
password456.auth.conf=2|default

### 热key配置
#开关
hot.key.monitor.enable=true
#热key监控LRU计数器的容量，一般不需要配置
hot.key.monitor.cache.max.capacity=100000
#热key监控统计的时间窗口，默认1000ms
hot.key.monitor.counter.check.millis=1000
#热key监控统计在时间窗口内超过多少阈值，判定为热key，默认500
hot.key.monitor.counter.check.threshold=500
#单个周期内最多上报多少个热key，默认32（取top）
hot.key.monitor.max.hot.key.count=32

### 热key转发配置
hot.key.route.rewrite.className=com.netease.nim.camellia.redis.proxy.plugin.rewrite.DefaultRouteRewriteChecker
1.default.hotkey.route.rewrite.config=[{"command":"get","bid":100,"bgroup":"default"},{"command":"all_commands","bid":200,"bgroup":"default"}]
2.default.hotkey.route.rewrite.config=[{"command":"read_commands","bid":300,"bgroup":"default"}]
```

上述配置表示：  
* 配置了5个路由，bid=1，2，100，200，300，bgroup=default
* 配置了2个租户，使用password123登录表示租户1，默认路由给redis://@127.0.0.1:6379，使用password456登录表示租户2，默认路由给redis://@127.0.0.1:6380
* 如果租户1的命令达到了热key级别（1000ms内超过500次），如果是get命令，则转发给redis://@127.0.0.1:6381，其他命令则转发给redis://@127.0.0.1:6382
* 如果租户2的命令达到了热key级别（1000ms内超过500次），如果是read命令，则随机转发给redis://@127.0.0.1:6379、redis://@127.0.0.1:6380、redis://@127.0.0.1:6381，如果是write命令则不变


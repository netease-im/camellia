
## 关于redis-proxy初始化

#### step1 

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

如上配置下，redis-proxy在启动时会尝试初始化到`redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377`的连接，主要是为了获取`redis-cluster`的初始拓扑信息，并开启定时刷新拓扑的定时任务，`redis-cluster-slaves`资源类型也是类似的

如果配置的是`redis-sentinel`和`redis-sentinel-slaves`类型的后端，则会从sentinel节点获取master/slave的信息，并开启针对主从切换的监听（实时监听+定时刷新）

如果配置的是`redis-proxies`和`redis-proxies-discovery`类型的后端，则会从本地或者注册中心获取节点列表，并开启针对节点变更的监听（实时监听+定时刷新）

如果配置的是`redis`类型的后端，则没有其他逻辑

如果上述初始化流程失败，则proxy进程启动失败（但是进程不会退出）

如果不希望执行上述初始化逻辑，则可以开启延迟初始化，则初始化逻辑会在proxy收到第一个来自客户端的命令时执行（可能导致客户端的第一个命令请求变慢）

开启延迟初始化的方法：  

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  config:
    "upstream.lazy.init.enable": true
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```

也可以在camellia-redis-proxy.properties里配置：
```properties
upstream.lazy.init.enable=true
```


#### step2

此外，redis-proxy启动时默认会新建对所有后端redis节点的连接，并发送ping命令（用于检查和预热），如果失败，则proxy进程启动失败（但是进程不会退出）

`redis-cluster`类型的后端，会发送ping给所有master节点

`redis-cluster-slaves`类型的后端，会发送ping给所有master节点和slave节点（取决于参数）

`redis-sentinel`类型的后端，会发送ping给所有master节点

`redis-sentinel-slaves`类型的后端，会发送ping给所有master节点和slave节点（取决于参数）

`redis-proxies`和`redis-proxies-discovery`类型的后端，会发送ping给所有节点

如果不希望发送ping，则可以如下方式关闭：

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
    redis-conf:
      preheat: false
```

#### step3

对于开启多租户的proxy，初始化逻辑略有不同（remote和custom）  

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
      bid: 1
      bgroup: default
```

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.DynamicConfProxyRouteConfUpdater
      dynamic: true
      bid: 1
      bgroup: default
      reload-interval-millis: 600000
```

上述两种配置下（remote和custom），只有bid=1，bgroup=default的路由中的redis地址，会被初始化和预热，其他bid/bgroup的路由需要等来自客户端的请求后才会初始化

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

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.DynamicConfProxyRouteConfUpdater
      dynamic: true
      reload-interval-millis: 600000
```

上述两种配置下（remote和custom），因为没有配置初始的bid/bgroup，没有一个路由的redis地址会被初始化和预热




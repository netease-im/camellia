
## 关于redis-proxy初始化

#### 初始化 

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
```properties
upstream.lazy.init.enable=true
```


#### 预热

在开启初始化的前提下，redis-proxy启动时默认会新建对所有后端redis节点的连接，并发送ping命令（用于检查和预热），如果失败，则proxy进程启动失败（但是进程不会退出）

`redis-cluster`类型的后端，会发送ping给所有master节点

`redis-cluster-slaves`类型的后端，会发送ping给所有master节点和slave节点（取决于参数）

`redis-sentinel`类型的后端，会发送ping给所有master节点

`redis-sentinel-slaves`类型的后端，会发送ping给所有master节点和slave节点（取决于参数）

`redis-proxies`和`redis-proxies-discovery`类型的后端，会发送ping给所有节点

如果不希望启动时发送ping进行预热和检查，则可以如下方式关闭：
```properties
upstream.preheat.enable=false
```





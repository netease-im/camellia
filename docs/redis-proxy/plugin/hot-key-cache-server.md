## HotKeyCacheBasedServerPlugin

### 说明

- 使用场景：zk负载均衡请求，随机发往某一个proxy，集群proxy之间需要共享hot-key。

- 使用hot-key sdk，接入hot-key-server服务的插件。
- 搭配zookeeper的发现机制使用。
- 集群伪cluster模式下直接使用HotKeyCache，客户端自带功能，会把请求映射到固定的proxy上，所以无需proxy之间共享。

### 启动方式

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口，如果设置为0，则不启动console
  password: pass123   #proxy的密码，如果设置了自定义的client-auth-provider-class-name，则密码参数无效
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  plugins: #使用yml配置插件，内置插件可以直接使用别名启用，自定义插件需要配置全类名
    - hotKeyCacheBasedServerPlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

#### 配置（camellia-redis-proxy.properties）

```properties
#热key相关配置
#热key缓存功能的开关，默认true
hot.key.cache.enable=true
#是否缓存空值
hot.key.cache.null=true
#缓存的容量
hot.key.cache.max.capacity=1000
#zk相关配置
hot.key.cache.zkUrl=192.168.88.130:2181
hot.key.cache.basePath=/camellia-hot-key
hot.key.cache.applicationName=camellia-hot-key-server
```


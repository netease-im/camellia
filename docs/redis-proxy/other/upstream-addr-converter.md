
## upstream-addr-converter

* camellia-redis-proxy支持自定义UpstreamAddrConverter，从而可以窜改后端地址(ip或者udsPath)
* 一个典型应用场景是如果proxy和redis混部在同一组机器上，访问本机节点使用uds或者127.0.0.1加速访问，访问非本机节点则使用局域网ip
* 参考UpstreamAddrConverter接口和DefaultUpstreamAddrConverter实现类

### 配置示例一

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
  password: pass123  
  monitor-enable: false
  monitor-interval-seconds: 60
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.189.31.13:6601,10.189.31.14:6603,10.189.31.15:6605
```

```properties
upstream.addr.converter.enable=true
upstream.addr.converter.config=[{"originalHost": "@CurrentHost@", "targetHost": "127.0.0.1"}]
```

假设proxy部署在10.189.31.13这个节点上，则proxy访问本机的redis-server会走127.0.0.1，而不是走10.189.31.13


### 配置示例二

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
  password: pass123
  monitor-enable: false
  monitor-interval-seconds: 60
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@10.189.31.13:6601,10.189.31.14:6603,10.189.31.15:6605
```

```properties
upstream.addr.converter.enable=true
upstream.addr.converter.config=[{"originalHost": "@CurrentHost@", "targetUdsPath": "/Users/caojiajun/temp/redis.sock"}]
```

假设proxy部署在10.189.31.13这个节点上，则proxy访问本机的redis-server会走uds，而不是走10.189.31.13


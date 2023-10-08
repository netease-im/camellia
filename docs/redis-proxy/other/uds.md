
## unix-domain-socket

* proxy支持监听unix-domain-socket，客户端可以使用uds直接连接到proxy


### 开启方法

```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
  uds-path: /tmp/camellia-redis-proxy.sock
  transpond:
    type: local
    local:
      type: simple
      resource: redis://@127.0.0.1:6379
```

### 客户端连接

参考：  
* [jedis](https://github.com/redis/jedis/issues/3493)
* [lettuce](https://github.com/lettuce-io/lettuce-core/wiki/Unix-Domain-Sockets)

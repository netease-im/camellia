
## unix-domain-socket

* proxy支持unix-domain-socket
* client到proxy支持，proxy到redis也支持


### client到proxy开启方法

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


### proxy到redis开启方法

```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
  transpond:
    type: local
    local:
      type: simple
      resource: redis-uds://@/tmp/redis.sock
```

resource配置方法参考[redis-resource](../auth/redis-resources.md)
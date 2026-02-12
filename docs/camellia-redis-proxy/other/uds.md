
## unix-domain-socket

* proxy支持unix-domain-socket
* client到proxy支持，proxy到redis也支持


### client到proxy开启方法

```properties
uds.path=/tmp/camellia-redis-proxy.sock
```

### 客户端连接

参考：  
* [jedis](https://github.com/redis/jedis/issues/3493)
* [lettuce](https://github.com/lettuce-io/lettuce-core/wiki/Unix-Domain-Sockets)


### proxy到redis开启方法

resource配置方法参考[redis-resource](../route/redis-resources.md)
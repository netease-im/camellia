
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
upstream.addr.converter.config=[{"originalHost": "@CurrentHost@", "originalPort": 6601, "targetUdsPath": "/Users/caojiajun/temp/redis.sock"}]
```

假设proxy部署在10.189.31.13这个节点上，则proxy访问本机的redis-server会走uds，而不是走10.189.31.13


#### 允许的配置字段
```java
private static class Config {
    private String originalHost;
    private int originalPort;
    private String originalUdsPath;
    private String targetHost;
    private int targetPort;
    private String targetUdsPath;
}
```

匹配：  
* 如果originalPort大于0，先匹配originalHost+originalPort
* 如果originalPort缺失或者小于等于0，则只匹配originalHost
* 如果originalHost为空，则匹配originalUdsPath

备注：如果originalHost为特殊的@CurrentHost@字符串，则会检查originalHost会使用本机ip去匹配  
本机ip默认自动获取，如果要手动指定，则可以配置：    
```properties
proxy.node.current.host=10.189.31.13
```

返回：  
* 如果targetHost不为空，targetPort大于等于0，则替换为targetHost+targetPort
* 如果targetHost不为空，targetPort缺失或者小于等于0，则替换为targetHost+originalPort
* 如果targetHost为空，则替换为targetUdsPath
* 否则，保持不变


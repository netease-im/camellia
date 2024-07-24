
## ssl/tls

* 支持client到proxy启用ssl/tls
* 支持proxy到后端redis启用ssl/tls
* 对于redis-sentinel/redis-sentinel-slave的后端类型，sentinel和redis可以分开进行tls配置

### client到proxy

#### step1，生成证书

* 参考redis官网：[encryption](https://redis.io/docs/management/security/encryption/)

#### step2，配置proxy

* application.yml里开启tls-port，小于等于0则表示不开启

```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console port, default 16379, if setting -16379, proxy will choose a random port, if setting 0, will disable console
  password: pass123   #password of proxy, priority less than custom client-auth-provider-class-name
  monitor-enable: false  #monitor enable/disable configure
  monitor-interval-seconds: 60 #monitor data refresh interval seconds
  tls-port: 6381
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: rediss://@127.0.0.1:6379
```

* camellia-redis-proxy.properties配置证书路径  
* classpath下配置示例
```properties
proxy.frontend.tls.ca.cert.file=ca.crt
proxy.frontend.tls.cert.file=server.crt
proxy.frontend.tls.key.file=server.key
```
* 绝对路径下配置示例
```properties
proxy.frontend.tls.cert.file.path=/xxx/redis-7.0.11/tests/tls/server.crt
proxy.frontend.tls.ca.cert.file.path=/xxx/redis-7.0.11/tests/tls/ca.crt
proxy.frontend.tls.key.file.path=/xxx/redis-7.0.11/tests/tls/server.key
```

上述描述的是默认配置方法，你也可以自定义，实现ProxyFrontendTlsProvider接口即可，然后配置到application.yml里，如下：
```java
public interface ProxyFrontendTlsProvider {

    boolean init();

    SslHandler createSslHandler(SSLContext sslContext);
}
```
```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console port, default 16379, if setting -16379, proxy will choose a random port, if setting 0, will disable console
  password: pass123   #password of proxy, priority less than custom client-auth-provider-class-name
  monitor-enable: false  #monitor enable/disable configure
  monitor-interval-seconds: 60 #monitor data refresh interval seconds
  tls-port: 6381
  proxy-frontend-tls-provider-class-name: xxx.xxx.xxx.MyProxyFrontendTlsProvider
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: rediss://@127.0.0.1:6379
```

* 默认开启双向认证，如果要关闭客户端认证，可以这样配置
```properties
proxy.frontend.tls.need.client.auth=false
proxy.frontend.tls.want.client.auth=false
#此时，proxy.frontend.tls.ca.cert.file可以不配置
```



### proxy到后端redis

* 因为后端redis可能有多个地址，因此需要配置多组证书，并且声明和redis的关系
* 当没有多租户，且没有分片/多读/多写逻辑时，你可以配置一个默认的证书，这样就不需要配置映射关系了

* 配置默认证书（camellia-redis-proxy.properties)
* classpath下配置示例
```properties
proxy.upstream.tls.ca.cert.file=ca.crt
proxy.upstream.tls.cert.file=client.crt
proxy.upstream.tls.key.file=client.key
```
* 绝对路径下配置示例
```properties
proxy.upstream.tls.cert.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.crt
proxy.upstream.tls.ca.cert.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt
proxy.upstream.tls.key.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.key
```      

* 配置映射关系（camellia-redis-proxy.properties)
```properties
proxy.upstream.tls.config=[{"resource":"rediss://@127.0.0.1:6379","ca.cert.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt","cert.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.crt","key.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.key"}]
```
* 配置是一个json数组，每一项代表一个映射关系
* resource是redis地址(注意需要使用带tls带地址串，如rediss://@127.0.0.1:6379，而不能使用redis://@127.0.0.1:6379)，具体见：[redis-resources](../auth/redis-resources.md)
* ca.cert.file.path、cert.file.path、key.file.path是证书路径
* ca.cert.file、cert.file、key.file是证书名称（classpath下），和路径二选一
* cert.file和key.file是可选的，如果不配置，则表示走单向认证

json示例：
```json
{
    "resource": "rediss://@127.0.0.1:6379",
    "ca.cert.file.path": "/xxx/redis-7.0.11/tests/tls/ca.crt",
    "cert.file.path": "/xxx/redis-7.0.11/tests/tls/client.crt",
    "key.file.path": "/xxx/redis-7.0.11/tests/tls/client.key"
}
```


上述描述的是默认配置方法，你也可以自定义，实现ProxyUpstreamTlsProvider接口即可，然后配置到application.yml里，如下：
```java
public interface ProxyUpstreamTlsProvider {

    boolean init();

    SslHandler createSslHandler(Resource resource);
}
```
```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console port, default 16379, if setting -16379, proxy will choose a random port, if setting 0, will disable console
  password: pass123   #password of proxy, priority less than custom client-auth-provider-class-name
  monitor-enable: false  #monitor enable/disable configure
  monitor-interval-seconds: 60 #monitor data refresh interval seconds
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: rediss://@127.0.0.1:6379
    redis-conf:
      proxy-upstream-tls-provider-class-name: xx.xx.MyProxyUpstreamTlsProvider
```
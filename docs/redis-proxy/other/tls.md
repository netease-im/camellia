
## ssl/tls

* 支持client到proxy启用ssl/tls
* 支持proxy到后端redis启用ssl/tls

### client到proxy

#### step1，生成证书

* 参考redis官网：[encryption](https://redis.io/docs/management/security/encryption/)

#### step2，配置proxy

* application.yml里开启tls

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
  tls-enable: true
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
proxy.frontend.tls.cert.file=redis.crt
proxy.frontend.tls.key.file=redis.key
```
* 绝对路径下配置示例
```properties
proxy.frontend.tls.cert.file.path=/xxx/redis-7.0.11/tests/tls/redis.crt
proxy.frontend.tls.ca.cert.file.path=/xxx/redis-7.0.11/tests/tls/ca.crt
proxy.frontend.tls.key.file.path=/xxx/redis-7.0.11/tests/tls/redis.key
```

上述描述的是默认配置方法，你也可以自定义，实现ProxyFrontendTlsProvider接口即可，然后配置到application.yml里，如下：
```java
public interface ProxyFrontendTlsProvider {

    SSLContext createSSLContext();

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
  tls-enable: true
  proxy-frontend-tls-provider-class-name: xxx.xxx.xxx.MyProxyFrontendTlsProvider
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: rediss://@127.0.0.1:6379
```


### proxy到后端redis

* 因为后端redis可能有多个地址，因此需要配置多组证书，并且声明和redis的关系
* 当没有多租户，且没有分片/多读/多写逻辑时，你可以配置一个默认的证书，这样就不需要配置映射关系了

* 配置默认证书（camellia-redis-proxy.properties)
* classpath下配置示例
```properties
proxy.upstream.tls.ca.cert.file=ca.crt
proxy.upstream.tls.cert.file=redis.crt
proxy.upstream.tls.key.file=redis.key
```
* 绝对路径下配置示例
```properties
proxy.upstream.tls.cert.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/redis.crt
proxy.upstream.tls.ca.cert.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt
proxy.upstream.tls.key.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/redis.key
```      

* 配置映射关系（camellia-redis-proxy.properties)
```properties
proxy.upstream.tls.config=[{"resource":"rediss://@127.0.0.1:6379","ca.cert.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt","cert.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/redis.crt","key.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/redis.key"}]
```
* 配置是一个json数组，每一项代表一个映射关系
* resource是redis地址(注意需要使用带tls带地址串，如rediss://@127.0.0.1:6379，而不能使用redis://@127.0.0.1:6379)，具体见：[redis-resources](../auth/redis-resources.md)
* ca.cert.file.path、cert.file.path、key.file.path是证书路径
* ca.cert.file、cert.file、key.file是证书名称（classpath下），和路径二选一

json示例：
```json
{
    "resource": "rediss://@127.0.0.1:6379",
    "ca.cert.file.path": "/xxx/redis-7.0.11/tests/tls/ca.crt",
    "cert.file.path": "/xxx/redis-7.0.11/tests/tls/redis.crt",
    "key.file.path": "/xxx/redis-7.0.11/tests/tls/redis.key"
}
```


上述描述的是默认配置方法，你也可以自定义，实现ProxyUpstreamTlsProvider接口即可，然后配置到application.yml里，如下：
```java
public interface ProxyUpstreamTlsProvider {

    SSLContext createSSLContext(Resource resource);

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
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: rediss://@127.0.0.1:6379
    redis-conf:
      proxy-upstream-tls-provider-class-name: xx.xx.MyProxyUpstreamTlsProvider
```
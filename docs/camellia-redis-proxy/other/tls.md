
## ssl/tls

* 支持client到proxy启用ssl/tls
* 支持proxy到后端redis启用ssl/tls
* 对于redis-sentinel/redis-sentinel-slave的后端类型，sentinel和redis可以分开进行tls配置

### client到proxy

#### step1，生成证书

* 参考redis官网：[encryption](https://redis.io/docs/management/security/encryption/)

#### step2，配置proxy

* 开启tls-port，小于等于0则表示不开启

```properties
tls.port=6381
```

* camellia-redis-proxy.properties配置证书路径  
* classpath下配置示例
```properties
server.tls.ca.cert.file=ca.crt
server.tls.cert.file=server.crt
server.tls.key.file=server.key
```
* 绝对路径下配置示例
```properties
server.tls.cert.file.path=/xxx/redis-7.0.11/tests/tls/server.crt
server.tls.ca.cert.file.path=/xxx/redis-7.0.11/tests/tls/ca.crt
server.tls.key.file.path=/xxx/redis-7.0.11/tests/tls/server.key
```

上述描述的是默认配置方法，你也可以自定义，实现ServerTlsProvider接口即可，如下：
```java
public interface ServerTlsProvider {

    boolean init();

    SslHandler createSslHandler(SSLContext sslContext);
}
```
```properties
server.tls.provider.class.name=com.netease.nim.camellia.redis.proxy.tls.frontend.DefaultServerTlsProvider
```

* 默认开启双向认证，如果要关闭客户端认证，可以这样配置
```properties
server.tls.need.client.auth=false
server.tls.want.client.auth=false
#此时，server.tls.ca.cert.file可以不配置
```



### proxy到后端redis

* 因为后端redis可能有多个地址，因此需要配置多组证书，并且声明和redis的关系
* 当没有多租户，且没有分片/多读/多写逻辑时，你可以配置一个默认的证书，这样就不需要配置映射关系了

* 配置默认证书（camellia-redis-proxy.properties)
* classpath下配置示例
```properties
upstream.tls.ca.cert.file=ca.crt
upstream.tls.cert.file=client.crt
upstream.tls.key.file=client.key
```
* 绝对路径下配置示例
```properties
upstream.tls.cert.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.crt
upstream.tls.ca.cert.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt
upstream.tls.key.file.path=/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.key
```      

* 配置映射关系（camellia-redis-proxy.properties)
```properties
upstream.tls.config=[{"resource":"rediss://@127.0.0.1:6379","ca.cert.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/ca.crt","cert.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.crt","key.file.path":"/Users/caojiajun/tools/redis-7.0.11/tests/tls/client.key"}]
```
* 配置是一个json数组，每一项代表一个映射关系
* resource是redis地址(注意需要使用带tls带地址串，如rediss://@127.0.0.1:6379，而不能使用redis://@127.0.0.1:6379)，具体见：[redis-resources](../route/redis-resources.md)
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
public interface UpstreamTlsProvider {

    boolean init();

    SslHandler createSslHandler(Resource resource);
}
```
```properties
upstream.tls.provider.class.name=com.netease.nim.camellia.redis.proxy.tls.upstream.DefaultUpstreamTlsProvider
```
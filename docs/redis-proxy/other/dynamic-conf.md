
## ProxyDynamicConf(camellia-redis-proxy.properties)

* proxy支持kv自定义配置，并通过ProxyDynamicConf来提供服务，如配置路由、热key阈值、大key阈值等等
* ProxyDynamicConf默认读取camellia-redis-proxy.properties中的kv配置对
* ProxyDynamicConf也会读取application.yml中的config配置作为初始配置，并和camellia-redis-proxy.properties中的配置进行merge
* camellia-redis-proxy.properties的配置优先级高于application.yml的配置
* 你也可以自定义ProxyDynamicConf的数据源（自定义ProxyDynamicConfLoader即可）

### 配置示例
* application.yml
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader #这也是默认的loader
  config:
    "k1": "v1"
    "k2": "v2"
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address
```
* camellia-redis-proxy.properties
```properties
k2=v22
k3=v3
k4=v4
```
如上的配置示例中，ProxyDynamicConf最终获取到的配置是（注意到k2被覆盖了）：
```properties
k1=v1
k2=v22
k3=v3
k4=v4
```

### FileBasedProxyDynamicConfLoader
这是默认的ProxyDynamicConf的数据源，默认读取classpath下的camellia-redis-proxy.properties文件中的配置  
特别的，你可以修改FileBasedProxyDynamicConfLoader读取的配置文件名，如下：  

* application.yml
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader
  config:
    "dynamic.conf.file.name": "custom.properties"
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address
```
上述配置表示，proxy不再读取camellia-redis-proxy.properties，而是读取custom.properties  

你也可以配置为绝对路径，如下：  
* application.yml  
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader
  config:
    "dynamic.conf.file.path": "/xx/xx/custom.properties"
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address
```
上述配置表示，proxy除了读取默认的camellia-redis-proxy.properties之外（可以没有这个文件），还会读取/xx/xx/custom.properties，后者覆盖前者  

或者你也可以在启动时增加`-Ddynamic.conf.file.path=/xxx/xxx/camellia-redis-proxy.properties`启动参数来指定配置文件的绝对路径，这个优先级是最高的，会覆盖前面的

此外，你也可以使用json格式的配置文件，使用JsonFileBasedProxyDynamicConfLoader替换FileBasedProxyDynamicConfLoader即可，以下两个配置是等价的：  
```properties
k1=v1
k2=v2
k3={"k":"v"}
```
```json
{
  "k1": "v1",
  "k2": "v2",
  "k3": {
    "k": "v"
  }
}
```

### ApiBasedProxyDynamicConfLoader
这是proxy内置的另外一个loader，读取的是camellia-config服务器，具体见：[camellia-config](/docs/config/config.md)  
配置方法如下：  

* application.yml
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.ApiBasedProxyDynamicConfLoader
  config:
    "camellia.config.url": "http://127.0.0.1:8080"
    "camellia.config.namespace": "xxx"
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address
```

### NacosProxyDynamicConfLoader
这是proxy提供的集成nacos获取配置的一个loader，首先你需要额外引入maven依赖，如下：  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-config-nacos</artifactId>
    <version>1.2.23</version>
</dependency>
```
随后如下配置：   

* application.yml
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.config.nacos.NacosProxyDynamicConfLoader
  config:
    "nacos.serverAddr": "127.0.0.1:8848"
    "nacos.dataId": "xxx"
    "nacos.group": "xxx"
    "nacos.config.type": "properties" #也可以配置为json
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address
```
* 如果还需其他nacos相关的配置，请在application.yml配置以`nacos.`作为前缀的kv对，NacosProxyDynamicConfLoader会去掉前缀后，把kv对传递给nacos的sdk
* 默认是properties类型，如果是json类型，则会把json的第一层key-value转换为properties


### EtcdProxyDynamicConfLoader
这是proxy提供的集成nacos获取配置的一个loader，首先你需要额外引入maven依赖，如下：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-config-etcd</artifactId>
    <version>1.2.23</version>
</dependency>
```
随后如下配置：

* application.yml
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.config.etcd.EtcdProxyDynamicConfLoader
  config:
    "etcd.target": "ip:///etcd0:2379,etcd1:2379,etcd2:2379"
    #"etcd.endpoints": "http://etcd0:2379,http://etcd1:2379,http://etcd2:2379" #etcd.target和etcd.endpoints二选一，优先使用etcd.target
    "etcd.config.key": "/xxx/xx"
    "etcd.config.type": "properties" #也可以配置为json
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379 #target transpond redis address
```

* 默认是properties类型，如果是json类型，则会把json的第一层key-value转换为properties


### 自定义loader
你也可以自己实现loader，loader的接口定义如下：
```java
public interface ProxyDynamicConfLoader {

    /**
     * ProxyDynamicConf初始化时，会把yml文件中的kv配置通过这个接口传递给loader
     * 这个方法只会被调用一次
     * @param initConf yml中的初始配置
     */
    void init(Map<String, String> initConf);

    /**
     * ProxyDynamicConf会定期调用load方法来更新配置
     * @return 配置
     */
    Map<String, String> load();

}
```
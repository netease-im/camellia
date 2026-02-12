
## ProxyDynamicConf.java

* proxy的配置可以理解为一堆kv配置
* proxy内部通过 `ProxyDynamicConf.java` 来加载和获取配置
* 端口、密码、路由、插件等功能依赖的配置，均来自 `ProxyDynamicConf.java`
* 默认使用FileBasedProxyDynamicConfLoader这个ProxyDynamicConfLoader实现
* 你也可以自定义ProxyDynamicConf的数据源（自定义ProxyDynamicConfLoader即可）

### 一个简单的配置示例（仅配置了application.yml和camellia-redis-proxy.properties）
* application.yml
```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  # 默认为FileBasedProxyDynamicConfLoader，此时可以不写
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader
  # 这是优先级最低的配置
  config:
    "k1": "v1"
    "k2": "v2"
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

此外，proxy同时支持支持json和properties两种配置文件，注意到以下两份配置是等价的

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


### FileBasedProxyDynamicConfLoader
配置优先级如下（从高到低，高优先级的会覆盖低优先级的）：

* 系统参数中指定文件路径：-Ddynamic.conf.file.path=/xx/xxx，会自动识别json还是properties
* 指定文件路径：dynamic.conf.file.path，会自动识别json还是properties
* 指定文件名称：dynamic.conf.file.name，会自动识别json还是properties
* camellia-redis-proxy.json
* camellia-redis-proxy.properties
* application.yml中的config


### ApiBasedProxyDynamicConfLoader
这是proxy内置的另外一个loader，读取的是camellia-config服务器，具体见：[camellia-config](/docs/camellia-config/config.md)
配置方法如下：  

* application.yml
```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.ApiBasedProxyDynamicConfLoader
  config:
    "camellia.config.url": "http://127.0.0.1:8080"
    "camellia.config.namespace": "xxx"
```

该实现的优先级如下：
* 系统参数中指定文件路径：-Ddynamic.conf.file.path=/xx/xxx，会自动识别json还是properties
* 指定文件路径：dynamic.conf.file.path，会自动识别json还是properties
* camellia-config
* application.yml中的config


### NacosProxyDynamicConfLoader
这是proxy提供的集成nacos获取配置的一个loader，首先你需要额外引入maven依赖，如下：  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-config-nacos</artifactId>
    <version>1.4.0-SNAPSHOT</version>
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.config.nacos.NacosProxyDynamicConfLoader
  config:
    "nacos.serverAddr": "127.0.0.1:8848"
    "nacos.dataId": "xxx"
    "nacos.group": "xxx"
    "nacos.config.type": "properties" #也可以配置为json
```
* 如果还需其他nacos相关的配置，请在application.yml配置以`nacos.`作为前缀的kv对，NacosProxyDynamicConfLoader会去掉前缀后，把kv对传递给nacos的sdk

该实现的优先级如下：
* 系统参数中指定文件路径：-Ddynamic.conf.file.path=/xx/xxx，会自动识别json还是properties
* 指定文件路径：dynamic.conf.file.path，会自动识别json还是properties
* nacos
* application.yml中的config


### EtcdProxyDynamicConfLoader
这是proxy提供的集成nacos获取配置的一个loader，首先你需要额外引入maven依赖，如下：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-config-etcd</artifactId>
    <version>1.4.0-SNAPSHOT</version>
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
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.config.etcd.EtcdProxyDynamicConfLoader
  config:
    "etcd.target": "ip:///etcd0:2379,etcd1:2379,etcd2:2379"
    #"etcd.endpoints": "http://etcd0:2379,http://etcd1:2379,http://etcd2:2379" #etcd.target和etcd.endpoints二选一，优先使用etcd.target
    "etcd.config.key": "/xxx/xx"
    "etcd.config.type": "properties" #也可以配置为json
```

该实现的优先级如下：
* 系统参数中指定文件路径：-Ddynamic.conf.file.path=/xx/xxx，会自动识别json还是properties
* 指定文件路径：dynamic.conf.file.path，会自动识别json还是properties
* etcd
* application.yml中的config


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
自定义实现下，是否有配置优先级，由实现本身决定
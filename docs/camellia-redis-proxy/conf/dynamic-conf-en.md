## ProxyDynamicConf.java

* Proxy configuration can be understood as a bunch of kv configurations
* Proxy internally uses `ProxyDynamicConf.java` to load and obtain configuration
* Configuration for ports, passwords, routes, plugins and other functions all comes from `ProxyDynamicConf.java`
* By default uses FileBasedProxyDynamicConfLoader as the ProxyDynamicConfLoader implementation
* You can also customize the data source of ProxyDynamicConf (just customize ProxyDynamicConfLoader)

### A Simple Configuration Example (Only Configured application.yml and camellia-redis-proxy.properties)
* application.yml
```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  # Default is FileBasedProxyDynamicConfLoader, can be omitted
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader
  # This is the lowest priority configuration
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


In the above configuration example, the final configuration obtained by ProxyDynamicConf is (note that k2 was overridden):
```properties
k1=v1
k2=v22
k3=v3
k4=v4
```

In addition, proxy supports both json and properties configuration files. Note that the following two configurations are equivalent:

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
Configuration priority is as follows (from high to low, higher priority overrides lower priority):

* File path specified in system parameter: -Ddynamic.conf.file.path=/xx/xxx, automatically recognizes json or properties
* Specified file path: dynamic.conf.file.path, automatically recognizes json or properties
* Specified file name: dynamic.conf.file.name, automatically recognizes json or properties
* camellia-redis-proxy.json
* camellia-redis-proxy.properties
* config in application.yml


### ApiBasedProxyDynamicConfLoader
This is another built-in loader of proxy, reading from camellia-config server, see: [camellia-config](/docs/camellia-config/config.md)
Configuration method is as follows:

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

The priority of this implementation is as follows:
* File path specified in system parameter: -Ddynamic.conf.file.path=/xx/xxx, automatically recognizes json or properties
* Specified file path: dynamic.conf.file.path, automatically recognizes json or properties
* camellia-config
* config in application.yml


### NacosProxyDynamicConfLoader
This is a loader provided by proxy for integrating nacos to obtain configuration. First, you need to introduce additional maven dependencies as follows:
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-config-nacos</artifactId>
    <version>1.4.0-SNAPSHOT</version>
</dependency>
```
Then configure as follows:

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
    "nacos.config.type": "properties" # can also be configured as json
```
* If you need other nacos-related configurations, please configure kv pairs with the prefix `nacos.` in application.yml. NacosProxyDynamicConfLoader will remove the prefix and pass the kv pairs to nacos SDK

The priority of this implementation is as follows:
* File path specified in system parameter: -Ddynamic.conf.file.path=/xx/xxx, automatically recognizes json or properties
* Specified file path: dynamic.conf.file.path, automatically recognizes json or properties
* nacos
* config in application.yml


### EtcdProxyDynamicConfLoader
This is a loader provided by proxy for integrating etcd to obtain configuration. First, you need to introduce additional maven dependencies as follows:
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-config-etcd</artifactId>
    <version>1.4.0-SNAPSHOT</version>
</dependency>
```
Then configure as follows:

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
    #"etcd.endpoints": "http://etcd0:2379,http://etcd1:2379,http://etcd2:2379" # choose one of etcd.target or etcd.endpoints, etcd.target is preferred
    "etcd.config.key": "/xxx/xx"
    "etcd.config.type": "properties" # can also be configured as json
```

The priority of this implementation is as follows:
* File path specified in system parameter: -Ddynamic.conf.file.path=/xx/xxx, automatically recognizes json or properties
* Specified file path: dynamic.conf.file.path, automatically recognizes json or properties
* etcd
* config in application.yml


### Custom Loader
You can also implement your own loader. The interface definition of the loader is as follows:
```java
public interface ProxyDynamicConfLoader {

    /**
     * When ProxyDynamicConf is initialized, the kv configuration in the yml file will be passed to the loader through this interface
     * This method will only be called once
     * @param initConf Initial configuration in yml
     */
    void init(Map<String, String> initConf);

    /**
     * ProxyDynamicConf will periodically call the load method to update configuration
     * @return Configuration
     */
    Map<String, String> load();

}
```
For custom implementations, whether there is a configuration priority is determined by the implementation itself

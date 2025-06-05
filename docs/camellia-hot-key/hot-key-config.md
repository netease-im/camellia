
## HotKeyConfigService
* 热key配置的数据源，默认是FileBasedHotKeyConfigService，会读取本地文件
* 内置了本地配置文件、接入camellia-config、接入nacos三种方式
* 你也可以自定义实现，只要实现`HotKeyConfig get(String namespace)`方法，并且在配置变更时回调`void invokeUpdate(namespace)`即可

```java
public abstract class HotKeyConfigService {
    /**
     * 获取HotKeyConfig
     * @param namespace namespace
     * @return HotKeyConfig
     */
    public abstract HotKeyConfig get(String namespace);

    /**
     * 初始化后会调用本方法，你可以重写本方法去获取到HotKeyServerProperties中的相关配置
     * @param properties properties
     */
    public void init(HotKeyServerProperties properties) {
    }

    //回调方法
    protected final void invokeUpdate(String namespace) {
        //xxxx
    }
}
```

### 读取本地配置文件

* application.yml

```yaml
server:
  port: 7070
spring:
  application:
    name: camellia-hot-key-server

camellia-hot-key-server:
  #工作线程和工作队列
  biz-work-thread: -1 #默认使用cpu核数的一半，不建议修改
  biz-queue-capacity: 1000000 #队列容量，默认100w
  #netty部分
  netty:
    boss-thread: 1 #默认1，不建议修改
    work-thread: -1 #默认cpu核数的一半，不建议修改
  #console，一个简单的http服务器，可以做优雅上下线和监控数据暴露，也可以自定义接口
  console-port: 17070
  #公共部分
  max-namespace: 1000 #预期的最大的namespace数量，默认1000
  hot-key-config-service-class-name: com.netease.nim.camellia.hot.key.server.conf.FileBasedHotKeyConfigService #热key配置数据源，默认使用本地配置文件，业务可以自定义实现
```

* camellia-hot-key-config.properties
```properties
namespace1=namespace1.json
namespace2=namespace2.json
```

表示namespace1的配置是namespace1.json

* namespace1.json
```json
{
  "namespace": "namespace1",
  "rules":
  [
    {
      "name": "rule1",
      "type": "exact_match",
      "keyConfig": "abcdef",
      "checkMillis": 1000,
      "checkThreshold": 100,
      "expireMillis": 10000
    },
    {
      "name": "rule2",
      "type": "prefix_match",
      "keyConfig": "xyz",
      "checkMillis": 1000,
      "checkThreshold": 100,
      "expireMillis": 10000
    },
    {
      "name": "rule3",
      "type": "suffix_match",
      "keyConfig": "qwe",
      "checkMillis": 1000,
      "checkThreshold": 100
    },
    {
      "name": "rule4",
      "type": "contains",
      "keyConfig": "opq",
      "checkMillis": 1000,
      "checkThreshold": 100
    },
    {
      "name": "rule4",
      "type": "match_all",
      "checkMillis": 1000,
      "checkThreshold": 100,
      "expireMillis": 10000
    }
  ]
}
```

特别的，camellia-hot-key-config.properties和xxx.json中的namespace字段务必是一样的，否则会忽略相关配置


### 使用camellia-config

* application.yml

```yaml
server:
  port: 7070
spring:
  application:
    name: camellia-hot-key-server

camellia-hot-key-server:
  #工作线程和工作队列
  biz-work-thread: -1 #默认使用cpu核数的一半，不建议修改
  biz-queue-capacity: 1000000 #队列容量，默认100w
  #netty部分
  netty:
    boss-thread: 1 #默认1，不建议修改
    work-thread: -1 #默认cpu核数的一半，不建议修改
  #console，一个简单的http服务器，可以做优雅上下线和监控数据暴露，也可以自定义接口
  console-port: 17070
  #公共部分
  max-namespace: 1000 #预期的最大的namespace数量，默认1000
  hot-key-config-service-class-name: com.netease.nim.camellia.hot.key.server.conf.ApiBasedHotKeyConfigService #热key配置数据源，默认使用本地配置文件，业务可以自定义实现
  config:
    'camellia.config.url': https://xx.xx.xx
    'camellia.config.namespace': xxx
```

camellia-config下每个key代表一个hot-key的namespace，value代表config本身的json


### 使用nacos

需要先引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-hot-key-config-nacos</artifactId>
    <version>1.3.5</version>
</dependency>
```

随后进行如下配置：

* application.yml

```yaml
server:
  port: 7070
spring:
  application:
    name: camellia-hot-key-server

camellia-hot-key-server:
  #工作线程和工作队列
  biz-work-thread: -1 #默认使用cpu核数的一半，不建议修改
  biz-queue-capacity: 1000000 #队列容量，默认100w
  #netty部分
  netty:
    boss-thread: 1 #默认1，不建议修改
    work-thread: -1 #默认cpu核数的一半，不建议修改
  #console，一个简单的http服务器，可以做优雅上下线和监控数据暴露，也可以自定义接口
  console-port: 17070
  #公共部分
  max-namespace: 1000 #预期的最大的namespace数量，默认1000
  hot-key-config-service-class-name: com.netease.nim.camellia.hot.key.server.config.nacos.NacosHotKeyConfigService #热key配置数据源，默认使用本地配置文件，业务可以自定义实现
  config:
    "nacos.serverAddr": "10.0.0.1:8848"
    "nacos.dataId": "xxx"
    "nacos.group": "xxx"
    "nacos.config.type": "properties" #默认properties，支持properties和json
```

* 默认使用properties类型的配置文件，每一行的key=value代表一组配置，其中key代表hot-key的namespace，value代表hot-key-config的json
* 也可以使用json类型，则json的第一层key-value结构的key是namespace，value是hot-key-config的json


### 使用etcd

需要先引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-hot-key-config-etcd</artifactId>
    <version>1.3.5</version>
</dependency>
```

随后进行如下配置：

* application.yml

```yaml
server:
  port: 7070
spring:
  application:
    name: camellia-hot-key-server

camellia-hot-key-server:
  #工作线程和工作队列
  biz-work-thread: -1 #默认使用cpu核数的一半，不建议修改
  biz-queue-capacity: 1000000 #队列容量，默认100w
  #netty部分
  netty:
    boss-thread: 1 #默认1，不建议修改
    work-thread: -1 #默认cpu核数的一半，不建议修改
  #console，一个简单的http服务器，可以做优雅上下线和监控数据暴露，也可以自定义接口
  console-port: 17070
  #公共部分
  max-namespace: 1000 #预期的最大的namespace数量，默认1000
  hot-key-config-service-class-name: com.netease.nim.camellia.hot.key.server.config.etcd.EtcdHotKeyConfigService #热key配置数据源，默认使用本地配置文件，业务可以自定义实现
  config:
    "etcd.target": "ip:///etcd0:2379,etcd1:2379,etcd2:2379"
    #"etcd.endpoints": "http://etcd0:2379,http://etcd1:2379,http://etcd2:2379" #etcd.target和etcd.endpoints二选一，优先使用etcd.target
    "etcd.config.key": "/xxx/xx"
    "etcd.config.type": "json" #也可以配置为properties
```

* 默认是json类型，则json的第一层key-value结构的key是namespace，value是hot-key-config的json
* 如果是properties类型，则key=value的key是namespace，value是hot-key-config的json

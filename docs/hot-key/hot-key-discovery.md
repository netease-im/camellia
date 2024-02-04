
## 注册中心
* hot-key-server以集群方式对外服务，sdk会从注册中心获取节点列表，随后将key根据hash规则推给特定的节点
* hot-key-server支持多组注册中心，内置了zk和eureka两种，你也可以实现自己的注册发现逻辑，从而对接到其他注册中心（如nacos、etcd、consul等）
* 你也可以不使用注册中心，本地写死所有hot-key-server的地址，缺点就是不能方便的扩缩容

### zk

#### 服务器端
* hot-key-server额外引入依赖

```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-hot-key-server-zk-registry-spring-boot-starter</artifactId>
    <version>1.2.26</version>
</dependency>
```

* 配置application.yml

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

#topn统计依赖redis
camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379

camellia-hot-key-zk-registry:
  enable: true
  zkUrl: 127.0.0.1:2181
  basePath: /camellia-hot-key
```

* 重写consoleService，如下：
```java
@Component
public class MyConsoleService extends ConsoleServiceAdaptor {

    @Autowired(required = false)
    private CamelliaHotKeyServerZkRegisterBoot zkRegisterBoot;

    @Override
    public ConsoleResult online() {
        if (zkRegisterBoot != null) {
            zkRegisterBoot.register();
        }
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        if (zkRegisterBoot != null) {
            zkRegisterBoot.deregister();
        }
        return super.offline();
    }
}
```

#### sdk侧
* 额外引入依赖

```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-hot-key-discovery-zk</artifactId>
    <version>1.2.26</version>
</dependency>
```

* 编写代码如下： 
```java
public class TestZk {

    public static void main(String[] args) {
        String zkUrl = "127.0.0.1:2181";
        String basePath = "/camellia-hot-key";
        String applicationName = "camellia-hot-key-server";
        ZkHotKeyServerDiscovery discovery = new ZkHotKeyServerDiscovery(zkUrl, basePath, applicationName);

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(discovery);
        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        //设置相关参数，一般来说默认即可
        CamelliaHotKeyMonitorSdkConfig monitorSdkConfig = new CamelliaHotKeyMonitorSdkConfig();
        //初始化CamelliaHotKeyMonitorSdk，一般全局一个即可
        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, monitorSdkConfig);

        //把key的访问push给server即可
        String namespace1 = "db_cache";
        monitorSdk.push(namespace1, "key1", 1);
        monitorSdk.push(namespace1, "key2", 1);
        monitorSdk.push(namespace1, "key2", 1);

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx", 1);
        monitorSdk.push(namespace2, "/xx/xx2", 1);
    }
}

```


### eureka

#### 服务器端
* 服务器侧额外引入依赖
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

* 配置application.yml

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

#topn统计依赖redis
camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379

eureka:
  client:
    serviceUrl:
      defaultZone: http://127.0.0.1:8761/eureka/
    registryFetchIntervalSeconds: 5
  instance:
    leaseExpirationDurationInSeconds: 15
    leaseRenewalIntervalInSeconds: 5
    prefer-ip-address: true
```

* 重写consoleService，如下：
```java
@Component
public class MyConsoleService extends ConsoleServiceAdaptor {

    @Override
    public ConsoleResult online() {
        ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        ApplicationInfoManager.getInstance().setInstanceStatus(InstanceInfo.InstanceStatus.DOWN);
        return super.offline();
    }
}
```

#### sdk侧
* 额外引入依赖

```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-hot-key-discovery-eureka</artifactId>
    <version>1.2.26</version>
</dependency>
```

* 编写代码如下： 
```java
public class TestEureka {

    @Autowired
    private DiscoveryClient discoveryClient;

    public void test() {
        String applicationName = "camellia-hot-key-server";
        int refreshIntervalSeconds = 5;
        EurekaHotKeyServerDiscovery discovery = new EurekaHotKeyServerDiscovery(discoveryClient, applicationName, refreshIntervalSeconds);

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(discovery);
        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        //设置相关参数，一般来说默认即可
        CamelliaHotKeyMonitorSdkConfig monitorSdkConfig = new CamelliaHotKeyMonitorSdkConfig();
        //初始化CamelliaHotKeyMonitorSdk，一般全局一个即可
        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, monitorSdkConfig);

        //把key的访问push给server即可
        String namespace1 = "db_cache";
        monitorSdk.push(namespace1, "key1", 1);
        monitorSdk.push(namespace1, "key2", 1);
        monitorSdk.push(namespace1, "key2", 1);

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx", 1);
        monitorSdk.push(namespace2, "/xx/xx2", 1);
    }
}

```


#### 不使用注册中心
* 你也可以不使用注册中心，本地写死所有hot-key-server的地址
* 缺点就是不能方便的扩缩容

SDK侧代码如下：

```java
public class TestNoRegister {

    public static void main(String[] args) {
        String applicationName = "camellia-hot-key-server";
        List<HotKeyServerAddr> list = new ArrayList<>();
        list.add(new HotKeyServerAddr("127.0.0.1", 7070));
        list.add(new HotKeyServerAddr("127.0.0.2", 7070));
        LocalConfHotKeyServerDiscovery discovery = new LocalConfHotKeyServerDiscovery(applicationName, list);

        CamelliaHotKeySdkConfig config = new CamelliaHotKeySdkConfig();
        config.setDiscovery(discovery);
        CamelliaHotKeySdk sdk = new CamelliaHotKeySdk(config);

        //设置相关参数，一般来说默认即可
        CamelliaHotKeyMonitorSdkConfig monitorSdkConfig = new CamelliaHotKeyMonitorSdkConfig();
        //初始化CamelliaHotKeyMonitorSdk，一般全局一个即可
        CamelliaHotKeyMonitorSdk monitorSdk = new CamelliaHotKeyMonitorSdk(sdk, monitorSdkConfig);

        //把key的访问push给server即可
        String namespace1 = "db_cache";
        monitorSdk.push(namespace1, "key1", 1);
        monitorSdk.push(namespace1, "key2", 1);
        monitorSdk.push(namespace1, "key2", 1);

        String namespace2 = "api_request";
        monitorSdk.push(namespace2, "/xx/xx", 1);
        monitorSdk.push(namespace2, "/xx/xx2", 1);
    }
}
```
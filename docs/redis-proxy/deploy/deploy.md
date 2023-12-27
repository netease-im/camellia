## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的   

## 大纲
* 部署模式
* 集成Zookeeper
* 伪redis-cluster模式
* 随机端口
* 优雅上下线
* 客户端接入（java之jedis）
* 客户端接入（java之SpringRedisTemplate)
* 客户端接入（其他语言）
* 注意事项（容器环境部署）
* 部署最佳实践

### 部署模式
通常来说，有四种方式来部署多实例的架构：  
* 前置四层代理(如lvs/阿里slb), 如下:   
<img src="redis-proxy-lb.png" width="60%" height="60%">  

此时，你可以像调用单点redis一样调用redis proxy

* 注册发现模式(使用zk/eureka/consul), 如下:  
<img src="redis-proxy-zk.png" width="60%" height="60%">
   
此时，你需要在客户端侧实现一下负载均衡策略

* 伪redis-cluster模式，如下：  
<img src="redis-proxy-cluster.jpg" width="60%" height="60%">

此时，可以把proxy集群当作一个redis-cluster集群去访问，从而不需要外部服务即可组成高可用集群  

* 伪redis-sentinel模式，如下：

<img src="redis-proxy-sentinel.png" width="60%" height="60%">

此时，可以把proxy节点同时当作sentinel节点和redis节点，通过不同的端口区分，通过cport去模拟sentinel获取master节点的请求，返回的是proxy自己（多个proxy节点哈希选一个，从而不同的proxy节点返回的master是相同的）  
客户端需要同时监听多个proxy节点（当作sentinel节点），当一个proxy节点宕机，则另外一个proxy节点会通过sentinel协议去告知客户端（只会告知把master设置为宕机节点的客户端），master已经变成我了，快切过来  

* 特别的，如果应用程序是java，则还可以同进程部署，如下：
<img src="redis-proxy-in-process.png" width="40%" height="40%">  

此时，应用程序直接访问127.0.0.1:6379即可
                                                                            
### 集成Zookeeper
camellia提供了一个基于zookeeper的注册发现模式的默认实现，你可以这样来使用它：
1) 首先在redis proxy上引入maven依赖： 
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-zk-registry-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
``` 
2) 在redis proxy的application.yml添加如下配置：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379

camellia-redis-zk-registry:
  enable: true
  zk-url: 127.0.0.1:2181,127.0.0.2:2181
  base-path: /camellia
```
则启动后redis proxy会注册到zk(127.0.0.1:2181,127.0.0.2:2181)  
此时你需要自己从zk上获取proxy的地址列表，然后自己实现一下客户端侧的负载均衡策略，但是如果你客户端是java，则camellia帮你做了一个实现，参考下节

### 伪redis-cluster模式
这种模式下，可以把proxy集群当作一个redis-cluster集群去访问，从而不需要外部服务即可组成高可用集群  
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: -6379 #优先级高于server.port，如果缺失，则使用server.port，如果设置为-6379则会随机一个可用端口
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  console-port: 16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口
  cport: 16380 #cluster-mode下的心跳端口，默认是proxy端口+10000
  password: pass123
  cluster-mode-enable: true #cluster-mode，把proxy伪装成cluster，需要在camellia-redis-proxy.properties配置proxy.cluster.mode.nodes
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```     
随后你需要在camellia-redis-proxy.properties里选择若干个个proxy节点配置，如下：
```
#随机挑选几个proxy节点配置即可（都配上当然更好），格式为ip:port@cport
proxy.cluster.mode.nodes=192.168.3.218:6380@16380,192.168.3.218:6390@16390
```
依次启动所有proxy即可    
节点宕机、节点扩容，proxy集群内部会通过心跳自动感知（心跳通过cport和自定义的redis协议去实现）         

其他可以配置的参数：
```
#proxy节点间的心跳间隔，表示了心跳请求的频率
proxy.cluster.mode.heartbeat.interval.seconds=5
#proxy节点间的心跳超时，20s没有收到心跳，则会剔除该节点
proxy.cluster.mode.heartbeat.timeout.seconds=20
#proxy节点的ip，默认会自动获取本机ip，一般不需要配置
proxy.cluster.mode.current.node.host=10.1.1.1
```

伪redis-cluster模式下常见操作的逻辑如下：  
```
1、启动时
1）取配置文件中配置的地址串
2）发送心跳给地址串中的所有地址（排除自己）
3）等待所有地址响应，如果有未响应的，会一直重试
4）标识自己为ONLINE


2、下线时
1）标识自己为OFFLINE
2）发送心跳告知所有其他节点


3、重新上线时
1）标识自己为ONLINE
2）发送心跳告知所有其他节点


4、扩容（有地址串外的节点启动）
1）取地址串中的所有地址
2）发送心跳给地址串中的所有地址（里面没有自己）
3）等待所有地址响应，如果有未响应的，会一直重试
4）标识自己为ONLINE
```

通过console的/online接口和/offline接口可以完成节点的上下线  

### 伪redis-sentinel模式
这种模式下，可以把proxy集群当作一个redis-sentinel集群去访问，从而不需要外部服务即可组成高可用集群
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: -6379 #优先级高于server.port，如果缺失，则使用server.port，如果设置为-6379则会随机一个可用端口
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  console-port: 16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口
  cport: 16380 #cluster-mode下的心跳端口，默认是proxy端口+10000
  password: pass123
  sentinel-mode-enable: true #cluster-mode，把proxy伪装成cluster，需要在camellia-redis-proxy.properties配置proxy.cluster.mode.nodes
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```     
随后你需要在camellia-redis-proxy.properties里把所有proxy节点配置上（至少两个），如下：
```
#把所有proxy节点配置上，格式为ip:port@cport
proxy.sentinel.mode.nodes=192.168.3.218:6380@16380,192.168.3.218:6390@16390
```

其他可以配置的参数：
```
#proxy节点间的心跳间隔，表示了心跳请求的频率
proxy.sentinel.mode.heartbeat.interval.seconds=5
#proxy节点间的心跳超时，20s没有收到心跳，则会剔除该节点
proxy.sentinel.mode.heartbeat.timeout.seconds=20
#proxy节点的ip，默认会自动获取本机ip，一般不需要配置
proxy.sentinel.mode.current.node.host=10.1.1.1
```

```高可用原理

1、客户端把proxy当作sentinel节点，通过cport去连接
2、客户端会通过sentinel协议，尝试获取master节点，proxy会根据客户端ip，做哈希，返回其中一个proxy节点
3、客户端会通过sentinel协议，去监听master节点的变化（需要对所有proxy节点cport端口建立监听，就和真正的sentinel集群一样）
4、proxy节点间会互相发送心跳，从而感知到节点的存活和宕机，维护在线proxy节点列表
5、当有一个proxy节点宕机后，另外一个proxy节点会发现，于是会到连到自己的所有客户端（过滤出哈希结果为宕机节点的客户端），发送一个master变化的通知，让客户端切到新的proxy节点

例子：
1）假设有一个客户端c，有两个proxy，a和b
2）c连接a和b的cport端口，询问master是谁，proxy根据c的客户端ip哈希，均返回a是master
3）c连接a的port端口，开启正常业务请求（get/set等）
4）b宕机，因为业务请求在a上，业务无影响
5）a宕机，b发现到a的心跳不通，同时发现刚才告诉了c，master是a，于是通过cport那个端口建立的监听连接，发送一个通知给到c，告诉他，master变了
6）c收到通知，重新建立到b的连接，继续开启正常业务请求（get/set等）

```


### 随机端口
有一些业务场景（比如测试环境混部）容易出现端口冲突情况，你可能希望proxy启动时支持随机选择可用端口，则你可以这样配置
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: -6379 #优先级高于server.port，如果缺失，则使用server.port，如果设置为-6379则会随机一个可用端口
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  console-port: -16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```
如果你设置成特殊的-6379和-16379，则proxy以及内嵌的console就会随机选择一个可用的端口进行监听   
你可以调用ProxyInfoUtils的getPort()和getConsolePort()方法获取实际生效的端口   

### 优雅上下线
当redis proxy启动的时候，会同时启动一个http服务器console server，默认端口是16379  
我们可以用console server做一些监控指标采集、优雅上下线等操作，使用方法是自己实现一个ConsoleService（继承自ConsoleServiceAdaptor）即可，如下所示：  
```java
@Component
public class MyConsoleService extends ConsoleServiceAdaptor implements InitializingBean {

    @Autowired
    private CamelliaRedisProxyBoot redisProxyBoot;

    @Autowired
    private CamelliaRedisProxyZkRegisterBoot zkRegisterBoot;

    @Override
    public ConsoleResult online() {
        zkRegisterBoot.register();
        return super.online();
    }

    @Override
    public ConsoleResult offline() {
        zkRegisterBoot.deregister();
        return super.offline();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setServerPort(redisProxyBoot.getPort());
    }
}
```
console server包含六个http api:    
* /online
会将一个全局的内存变量status设置成ONLINE状态
* /offline
会将一个全局的内存变量status设置成OFFLINE状态    
并且如果此时proxy是idle的，则返回http.code=200，否则会返回http.code=500  
ps: 当且仅当最后一个命令执行完成已经超过10s了，才会处于idle     
* /status
如果status=ONLINE, 则返回http.code=200,    
否则返回http.code=500  
* /check
如果服务器端口可达（指的是proxy的服务端口），则返回200，否则返回500
* /monitor
获取监控数据，具体可见：[详情](../monitor/monitor-data.md)
* /reload
reload动态配置ProxyDynamicConf
* /custom
一个自定义接口，可以通过设置不同的http参数来表示不同的请求类型

在上面的例子中，MyConsoleService注入了CamelliaRedisProxyZkRegisterBoot，  
如果我们调用/online，则CamelliaRedisProxyZkRegisterBoot会注册到zk（启动后会自动去注册；此外，不用担心重复注册，内部会处理掉）      
如果我们调用/offline，则CamelliaRedisProxyZkRegisterBoot会从zk上摘除，因为如果proxy没有idle，offline会返回500，因此我们可以反复调用offline直到返回200，此时我们就可以shutdown掉proxy了而不用担心命令执行中被打断了  

### 客户端接入（java之jedis）
如果端侧是Java，并且使用的是Jedis，那么camellia提供了RedisProxyJedisPool，方便你进行改造。  
首先，在客户端侧的工程里添加如下maven依赖(如果是jedis3则引入camellia-redis-proxy-discovery-jedis3)：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-discovery-zk</artifactId>
    <version>a.b.c</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-discovery-jedis2</artifactId>
    <version>a.b.c</version>
</dependency>
``` 
然后你就可以使用RedisProxyJedisPool代替你原先使用的JedisPool，其他的操作都一样。   
RedisProxyJedisPool使用IProxySelector来定义proxy的负载均衡策略，默认使用的是RandomProxySelector，也即随机选择proxy。  
如果设置了sideCarFirst=true，则会使用SideCarFirstProxySelector，该策略下会优先选择同机部署的proxy（即side-car-proxy）   
对于其他proxy，SideCarFirstProxySelector也会优先访问相同region的proxy（从而有更小的延迟），但是需要实现RegionResolver接口，默认提供了根据ip段来设置region的IpSegmentRegionResolver      
当然，你也可以自己实现IProxySelector来自定义proxy的负载均衡策略  
此外，如果redis-proxy使用了camellia-dashboard，且使用了动态的多组配置，那么RedisProxyJedisPool需要声明一下自己的bid和bgroup  
下面是一个例子：  
```java

public class TestRedisProxyJedisPool {

    public static void main(String[] args) {
        String zkUrl = "127.0.0.1:2181,127.0.0.2:2181";
        String basePath = "/camellia";
        String applicationName = "camellia-redis-proxy-server";
        ZkProxyDiscovery zkProxyDiscovery = new ZkProxyDiscovery(zkUrl, basePath, applicationName);

        RedisProxyJedisPool jedisPool = new RedisProxyJedisPool.Builder()
                .poolConfig(new JedisPoolConfig())
//                .bid(1)
//                .bgroup("default")
                .proxyDiscovery(zkProxyDiscovery)
                .password("pass123")
                .timeout(2000)
                .build();

        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.setex("k1", 10, "v1");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}

```
RedisProxyJedisPool.Builder的所有参数及其介绍如下：  
```
    public static class Builder {

        private long bid = -1;//业务id，小于等于0表示不指定
        private String bgroup = null;//业务分组
        private IProxyDiscovery proxyDiscovery;//proxy discovery，用于获取proxy列表以及获取proxy的变更通知，默认提供了基于zk的实现，你也可以自己实现
        private GenericObjectPoolConfig poolConfig = new JedisPoolConfig();//jedis pool config
        private int timeout = defaultTimeout;//超时
        private String password;//密码
        private int refreshSeconds = defaultRefreshSeconds;//兜底的从proxyDiscovery刷新proxy列表的间隔
        private int maxRetry = defaultMaxRetry;//获取jedis时的重试次数
        //因为每个proxy都要初始化一个JedisPool，当proxy数量很多的时候，可能会引起RedisProxyJedisPool初始化很慢
        //若开启jedisPoolLazyInit，则会根据proxySelector策略优先初始化jedisPoolInitialSize个proxy，剩余proxy会延迟初始化，从而加快RedisProxyJedisPool的初始化过程
        private boolean jedisPoolLazyInit = defaultJedisPoolLazyInit;//是否需要延迟初始化jedisPool，默认true，如果延迟初始化，则一开始会初始化少量的proxy对应的jedisPool，随后兜底线程会初始化剩余的proxy对应的jedisPool
        private int jedisPoolInitialSize = defaultJedisPoolInitialSize;//延迟初始化jedisPool时，一开始初始化的proxy个数，默认16个
        //以下参数用于设置proxy的选择策略
        //当显式的指定了proxySelector
        //--则使用自定义的proxy选择策略
        //若没有显示指定：
        //--当以下参数均未设置时，则会从所有proxy里随机挑选proxy发起请求，此时，实际使用的proxySelector是RandomProxySelector
        //--当设置了sideCarFirst=true，则会优先使用同机部署的proxy，即side-car-proxy，此时实际使用的proxySelector是SideCarFirstProxySelector
        //--localhost用于判断proxy是否是side-car-proxy，若缺失该参数，则会自动获取本机ip
        //--当设置了sideCarFirst=true，但是又找不到side-car-proxy，SideCarFirstProxySelector会优先使用相同region下的proxy，用于判断proxy归属于哪个region的方法是RegionResolver
        //-----当regionResolver未设置时，默认使用DummyRegionResolver，即认为所有proxy都归属于同一个proxy
        //-----我们还提供了一个IpSegmentRegionResolver的实现，该实现用ip段的方式来划分proxy的region，当然你也可以实现一个自定义的RegionResolver
        private boolean sideCarFirst = defaultSideCarFirst;
        private String localhost = defaultLocalHost;
        private RegionResolver regionResolver;
        private IProxySelector proxySelector;
```

### 客户端接入（java之SpringRedisTemplate)
如果你使用了Spring的RedisTemplate，为了以zk注册中心的方式接入redis-proxy，可以引入如下依赖：    
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-spring-redis-zk-discovery-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
```
并且在application.yml添加如下依赖（类似的，如果redis-proxy使用了camellia-dashboard，且使用了动态的多组配置，那么需要声明一下bid和bgroup）：  
```yaml
camellia-spring-redis-zk-discovery:
  application-name: camellia-redis-proxy-server
  #bid: 1
  #bgroup: default
  password: pass123
  zk-conf:
    zk-url: 127.0.0.1:2181
    base-path: /camellia
    side-car-first: true
    region-resolve-conf: 10.189.0.0/20:region1,10.189.208.0/21:region2
    default-region: default
  redis-conf:
    min-idle: 0
    max-active: 8
    max-idle: 8
    max-wait-millis: 2000
    timeout: 2000
```
那么自动生成的SpringRedisTemplate就是访问redis-proxy了   
上述的示例代码见：[示例](/)  

### 客户端接入（其他语言)
* 如果proxy使用前置四层代理来组成一个集群，那么你可以用各自语言的标准redis客户端sdk，然后像访问单节点redis一样访问proxy集群
* 如果proxy使用了zookeeper等注册中心的模式来组成一个集群，那么需要你自己实现一套从注册中心获取所有proxy节点地址的逻辑，并且实现想要的负载均衡策略，而且当proxy节点发生上下线时，你需要自己处理来自注册中心的变更通知；除此之外，对于每个proxy节点，你都可以像访问单节点redis一样访问

### 注意事项（容器环境部署）
camellia-redis-proxy启动时默认会去读取操作系统的cpu核数，并开启对应数量的work线程  
如果部署在容器里，请务必使用高版本的jdk（jdk8u191之后），并在启动参数里添加-XX:+UseContainerSupport，确保proxy可以自动获取到正确的可用cpu核数    

如果jdk版本不支持上述启动参数，则务必设置合理的workThread数（不要超过可用cpu核数），如下：
```yaml
camellia-redis-proxy:
  password: pass123
  netty:
    boss-thread: 1 #默认1即可
    work-thread: 4 #建议等于可用cpu核数，切不可超过可用cpu核数
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```

### 部署最佳实践
* 云信线上proxy集群，使用4C8G的云主机or容器进行部署（jdk版本1.8.0_202），配置3G的堆内存，搭配G1垃圾回收器  
```
-server -Xms3072m, -Xmx3072m -XX:MetaspaceSize=128m -XX:+UseG1GC -verbose:gc -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:/xxx/logs/camellia-redis-proxy-gc-`date +%Y-%m-%d_%I-%M-%S`.log 
```
* 如果有优化建议，或者在其他环境或者机器配置下的最佳实践，欢迎补充
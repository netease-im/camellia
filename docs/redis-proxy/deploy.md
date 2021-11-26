## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的   

## 大纲
* 部署模式
* 集成Zookeeper
* 随机端口
* 优雅上下线
* 客户端接入（java之jedis）
* 客户端接入（java之SpringRedisTemplate)
* 客户端接入（其他语言）
* 注意事项（容器环境部署）

### 部署模式
通常来说，有两种方式来部署多实例的架构：  
* 前置四层代理(如lvs/阿里slb), 如下:   
<img src="redis-proxy-lb.png" width="60%" height="60%">  

此时，你可以像调用单点redis一样调用redis proxy

* 注册发现模式(使用zk/eureka/consul), 如下:  
<img src="redis-proxy-zk.png" width="60%" height="60%">
   
此时，你需要在客户端侧实现一下负载均衡策略

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

camellia-redis-zk-registry:
  enable: true
  zk-url: 127.0.0.1:2181,127.0.0.2:2181
  base-path: /camellia
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
获取监控数据，具体可见：[详情](monitor-data.md)
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
                .sideCarFirst(true)
                .regionResolver(new RegionResolver.IpSegmentRegionResolver("10.189.0.0/20:region1,10.189.208.0/21:region2", "default"))
//                .proxySelector(new CustomProxySelector())
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
上述的示例代码见：[示例](/camellia-samples/camellia-spring-redis-samples)  

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
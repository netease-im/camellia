## 注册中心模式

注册发现模式(使用zk/eureka/consul), 如下:  
  <img src="redis-proxy-zk.png" width="60%" height="60%">

此时，你需要在客户端侧实现一下负载均衡策略


### zk为例

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




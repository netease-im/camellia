
# camellia-feign
## 介绍
camellia-feign封装了feign，集成到camellia的体系中，提供一些额外的能力

## feature
join camellia-core and open-feign, so your feign-client have this feature:  
* support dynamic route
* support custom route by request param
* support custom load balance policy by request param
* support read-write-separate/double-write
* support dynamic conf, such like timeout

## 特性
* 支持动态路由，运行期间支持动态配置新的路由规则
* 支持camellia-core的通用能力，如读写分离、双写
* 支持熔断（集成了CamelliaCircuitBreaker）
* 支持fallback（支持fallback和fallbackFactory）
* 支持根据租户id配置不同的路由（使用注解的方式标识租户id，无侵入性）
* 支持动态配置（包括超时时间、熔断配置）
* 支持动态识别openFeign和spring-cloud-feign的注解
* 支持spring-boot-starter，可以一键替换spring-cloud-feign

## 快速开始
* 先定义一个接口类
```java
@CamelliaFeignClient(route = "feign#http://127.0.0.1:8080", fallback = TestFeignServiceFallback.class)
public interface ITestFeignService {

    @RequestMapping(value = "/getUser", method = RequestMethod.POST)
    UserResponse getUser(User user);
}

```
如上，@CamelliaFeignClient 注解定义了默认的路由，以及一个fallback实现

* 在启动类上启动camellia-feign
```java
@SpringBootApplication
@EnableCamelliaFeignClients(basePackages = {"com.netease.nim.camellia.feign.samples"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```

* 通过@Autowired自动注入service
```java
@RestController
public class TestFeignController {

    @Autowired
    private ITestFeignService testFeignService;
    
    //.....
}
```

## 支持的路由种类
```
#固定url
##支持http
feign#http://127.0.0.1:8080
##也支持https
feign#https://www.abc.com

#基于注册中心
##支持http
feign#http://serviceName
##也支持https
feign#https://serviceName

```
此外，路由可以按照camellia的标准写法，支持读写分离、双写

## 支持的配置项
你可以使用CamelliaFeignClientFactory来手动构造camellia-feign客户端（spring-boot-starter内部也是调用的CamelliaFeignClientFactory来自动注入相关service）  
```java
public class CamelliaFeignClientFactory {

    /**
     * 构造方法
     * @param feignEnv 一些全局参数：包括camellia-core的一些基本参数、使用注册中心时需要的discovery实现以及对应的健康检查方法、使用fallback和熔断时过滤异常类型的FallbackExceptionChecker
     * @param camelliaApi 接入camellia-dashboard时需要本参数
     * @param checkIntervalMillis 接入camellia-dashboard时的规则检查周期，默认5000ms
     * @param feignProps 构造feign客户端时的参数，包括编解码类型、异常处理类等
     * @param dynamicOptionGetter 动态配置的回调接口
     */
    public CamelliaFeignClientFactory(CamelliaFeignEnv feignEnv, CamelliaApi camelliaApi, long checkIntervalMillis,
                                      CamelliaFeignProps feignProps, CamelliaFeignDynamicOptionGetter dynamicOptionGetter) {
        this.feignProps = feignProps;
        this.feignEnv = feignEnv;
        this.camelliaApi = camelliaApi;
        this.checkIntervalMillis = checkIntervalMillis;
        this.dynamicOptionGetter = dynamicOptionGetter;
    }

    /**
     * 生成一个camellia-feign客户端
     * @param apiType 类型
     * @return 客户端实例
     */
    public <T> T getService(Class<T> apiType) {
        //...
    }
    
    /**
     * 生成一个camellia-feign客户端
     * @param apiType 类型
     * @param fallback fallback
     * @return 客户端实例
     */
    public <T> T getService(Class<T> apiType, T fallback) {
        //...
    }
    
    //.....
}
```
通过CamelliaFeignClientFactory的构造方法以及对外提供的getService方法可以了解到camellia-feign提供的基本功能  
特别的，camellia-feign提供一些动态配置的功能，通过CamelliaFeignDynamicOptionGetter来暴露，如下：  
```java
public interface CamelliaFeignDynamicOptionGetter {

    /**
     * 根据bid和bgroup获取动态配置
     * @param bid 业务bid
     * @param bgroup 业务bgroup
     * @return DynamicOption
     */
    DynamicOption getDynamicOption(long bid, String bgroup);

    /**
     * 根据bid获取DynamicRouteConfGetter
     * @param bid 业务bid
     * @return DynamicRouteConfGetter
     */
    default DynamicRouteConfGetter getDynamicRouteConfGetter(long bid) {
        return null;
    }
}
```
DynamicOption可以动态配置超时、熔断等参数，如下：
```java
public class DynamicOption {

    //连接超时配置
    private DynamicValueGetter<Long> connectTimeout;
    private DynamicValueGetter<TimeUnit> connectTimeoutUnit;

    //读超时配置
    private DynamicValueGetter<Long> readTimeout;
    private DynamicValueGetter<TimeUnit> readTimeoutUnit;

    //是否支持重定向
    private DynamicValueGetter<Boolean> followRedirects;

    //是否开启监控
    private DynamicValueGetter<Boolean> monitorEnable;

    //是否开启熔断，若circuitBreakerConfig为null，则不开启熔断
    private CircuitBreakerConfig circuitBreakerConfig;

    //动态检查feign的注解类型，当前支持默认注解和spring-mvc注解，默认支持动态监测
    private DynamicContractTypeGetter dynamicContractTypeGetter = new DynamicContractTypeGetter.Default();

    //当基于注册中心时，如何选择服务节点，默认随机，支持哈希以及其他自定义规则，功能类似于一个简单的ribbon
    private CamelliaServerSelector<FeignResource> serverSelector = new RandomCamelliaServerSelector<>();
    
    //.....
}
```

## CamelliaFeignClient注解含义
```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CamelliaFeignClient {

    long bid() default -1;//业务bid，默认-1，若路由规则委托给camellia-dashboard，则bid需要大于0

    String bgroup() default "default";//业务bgroup

    String route() default "";//如果路由没有委托给camellia-dashboard，则本字段必填

    String[] qualifiers() default {};//仅给camellia-feign-spring-boot-starter使用

    boolean primary() default true;//仅给camellia-feign-spring-boot-starter使用

    //如果使用camellia-feign-spring-boot-starter，则会优先去spring工厂取，如果取不到，则尝试使用无参构造方法建一个
    //正常使用CamelliaFeignClientFactory的话，则会优先使用传入的fallback，否则尝试使用无参构造方法建一个
    Class<?> fallback() default void.class;
    //fallbackFactory的优先级高于fallback
    Class<?> fallbackFactory() default void.class;

}
```

## 根据不同的参数设置不同的路由规则
camellia支持根据请求参数中的某个字段，映射到不同的bgroup，从而映射到不同的路由  
上诉功能使用时需要配合使用@RouteKey注解和DynamicRouteConfGetter动态接口  
```java
public class User {

    @RouteKey
    private long tenantId;

    @LoadBalanceKey
    private long uid;

    private String name;
    private String ext;
    
    //.....
}
```
以快速开始sample中的接口为例，我们把接口入参User中的tenantId字段添加了@RouteKey注解，随后可以定义如下DynamicRouteConfGetter动态接口  
```java
public class SampleDynamicRouteConfGetter implements DynamicRouteConfGetter {

    @Override
    public String bgroup(Object routeKey) {
        if (routeKey == null) return "default";
        if (String.valueOf(routeKey).equals("1")) {
            return "bgroup1";
        } else if (String.valueOf(routeKey).equals("2")) {
            return "bgroup2";
        }
        return "default";
    }
}
```
假设bid=1，则上述的DynamicRouteConfGetter动态接口表示：  
1）当User的tenantId字段是1时，使用bid=1,bgroup=bgroup1的路由，比如路由到集群1    
2）当User的tenantId字段是2时，使用bid=1,bgroup=bgroup2的路由，比如路由到集群2    
3）其他情况，使用bid=1,bgroup=default的路由，比如路由到集群3  

## 根据不同的参数设置不同的负载均衡策略
* 负载均衡策略的含义是指在使用注册中心进行节点的自动发现的模式下，在同一个路由规则下，针对多个服务节点，如何将请求分发到具体某个节点     
* 设置负载均衡器策略的方法通过设置DynamicOption下的CamelliaServerSelector参数来实现，默认是随机策略，此外还内置了哈希策略，可以按需选择    
```java
public interface CamelliaServerSelector<T> {
    /**
     *
     * @param list 待选择的节点列表
     * @param loadBalanceKey 负载均衡key
     * @return 具体选择哪个节点
     */
    T pick(List<T> list, Object loadBalanceKey);
}
```
我们同样以快速开始的sample为例，我们把接口入参User对象中的uid字段添加了@LoadBalanceKey注解，假设我们选择了哈希策略，则相同uid的请求总是会发给同一个服务节点来处理  

## 关于熔断
camellia-feign集成了CamelliaCircuitBreaker，从而拥有熔断能力  
CamelliaCircuitBreaker通过DynamicOption的CircuitBreakerConfig来配置，配置可以动态修改，并且对于不同的bid/bgroup可以进行不同的配置，具体配置包括如下：  
```java
public class CircuitBreakerConfig {

    //以下参数一经设置不可修改
    private String name = "camellia-circuit-breaker";//熔断器的别名
    private long statisticSlidingWindowTime = 10 * 1000L;//统计成功失败的滑动窗口的大小，单位ms，默认10s
    private int statisticSlidingWindowBucketSize = 10;//滑动窗口分割为多少个bucket，默认10个

    //以下参数可以动态修改
    private DynamicValueGetter<Boolean> enable = () -> true;//是否启用，若不启用，则不进行失败率统计，所有请求都允许
    private DynamicValueGetter<Boolean> forceOpen = () -> false;//强制打开，则所有请求都不允许
    private DynamicValueGetter<Double> failThresholdPercentage = () -> 0.5;//滑动窗口范围内失败比例超过多少触发熔断，默认50%
    private DynamicValueGetter<Long> requestVolumeThreshold = () -> 20L;//滑动窗口内至少多少个请求才会触发熔断，默认20个
    private DynamicValueGetter<Long> singleTestIntervalMillis = () -> 5000L;//当熔断器打开的情况下，间隔多久尝试一次探测（也就是半开）
    private DynamicValueGetter<Boolean> logEnable = () -> true;//是否打开日志（主要是打印熔断器状态变更时打印
    
    //.....
}
```

## 关于fallback
camellia-feign支持为每个service设置fallback或者fallbackFactory（所谓的fallbackFactory指可以针对不同的异常类型返回不同的fallback对象）    
我们可以在@CamelliaFeignClient注解中添加fallback和fallbackFactory，如果使用spring-boot-starter使用camellia-feign，则fallback和fallbackFactory可以通过spring自动注入，整体的优先级：  
* spring自动注入的fallbackFactory实例  
* spring自动注入的fallback实例
* 尝试使用无参构造方法去初始化注解中fallbackFactory实例
* 尝试使用无参构造方法去初始化注解中fallback实例

## 关于动态参数（如超时、熔断）
* camellia-feign使用DynamicValueGetter<T>的方式来提供动态参数，你可以自行实现（如对接到你们的配置中心）
* camellia-feign默认使用DefaultCamelliaFeignDynamicOptionGetter来获取动态参数，此时参数实际上是静态的
* 此外，camellia-feign还提供了一个FileBasedCamelliaFeignDynamicOptionGetter，支持使用本地配置文件（properties）来动态修改参数

## 关于动态路由
* 一般来说，你需要引入camellia-dashboard来配置和下发动态的路由配置
* 此外你也可以使用FileBasedCamelliaApi，从而使用本地配置文件（properties）来模拟camellia-dashboard
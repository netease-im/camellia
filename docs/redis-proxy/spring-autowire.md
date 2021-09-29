
## 通过spring自动注入自定义回调

camellia-redis-proxy默认通过在application.yml里配置全类名的方式来自定义一些功能（如监控回调、自定义动态路由等），1.0.38版本开始，支持使用spring来托管相关类的初始化

以监控回调为例，默认注入方式如下：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: 6380 #优先级高于server.port，如果缺失，则使用server.port
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  password: pass123   #proxy的密码
  monitor-enable: true  #是否开启监控
  command-spend-time-monitor-enable: true #是否开启请求耗时的监控，只有monitor-enable=true才有效
  monitor-interval-seconds: 60 #监控回调的间隔
  monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback #监控回调类
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```
通过在application.yml里配置monitor-callback-class-name的全类目，proxy在初始化的时候会自动调用该类的无参构造方法来生成MonitorCallback的实例对象  
1.0.38版本开始，你可以把自定义实现的callback托管给spring，proxy会自动优先使用spring工厂中的实例对象，如下：  
```java
@Component
public class CustomMonitorCallback implements MonitorCallback {
    
    private final LoggingMonitorCallback loggingMonitorCallback = new LoggingMonitorCallback();
    
    @Override
    public void callback(Stats stats) {
        loggingMonitorCallback.callback(stats);
    }
}
```  

目前支持通过spring依赖注入的回调类包括如下（参见CamelliaRedisProxyConfigurerSupport）：
```java
@Component
public class CamelliaRedisProxyConfigurerSupport {

    @Autowired(required = false)
    private MonitorCallback monitorCallback;

    @Autowired(required = false)
    private CommandInterceptor commandInterceptor;

    @Autowired(required = false)
    private SlowCommandMonitorCallback slowCommandMonitorCallback;

    @Autowired(required = false)
    private BigKeyMonitorCallback bigKeyMonitorCallback;

    @Autowired(required = false)
    private HotKeyMonitorCallback hotKeyMonitorCallback;

    @Autowired(required = false)
    private HotKeyCacheKeyChecker hotKeyCacheKeyChecker;

    @Autowired(required = false)
    private HotKeyCacheStatsCallback hotKeyCacheStatsCallback;

    @Autowired(required = false)
    private ClientAuthProvider clientAuthProvider;

    @Autowired(required = false)
    private KeyConverter keyConverter;

    @Autowired(required = false)
    private StringConverter stringConverter;

    @Autowired(required = false)
    private ListConverter listConverter;

    @Autowired(required = false)
    private SetConverter setConverter;

    @Autowired(required = false)
    private ZSetConverter zSetConverter;

    @Autowired(required = false)
    private HashConverter hashConverter;

    @Autowired(required = false)
    private ShadingFunc shadingFunc;

    @Autowired(required = false)
    private ProxyDynamicConfHook proxyDynamicConfHook;
    
    @Autowired(required = false)
    private ProxyRouteConfUpdater proxyRouteConfUpdater;
}
```  

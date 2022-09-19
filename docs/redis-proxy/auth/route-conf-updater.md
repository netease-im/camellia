
## 集成ProxyRouteConfUpdater自定义管理多组动态配置
集成camellia-dashboard后，proxy就有了多租户路由的能力，如果你不想使用camellia-dashboard，那么你可以自定义ProxyRouteConfUpdater来实现相关逻辑   
ProxyRouteConfUpdater是一个抽象类，你需要自己去实现一个子类，在ProxyRouteConfUpdater对象实例的内部，你至少需要实现以下方法：
```
public abstract ResourceTable getResourceTable(long bid, String bgroup);
```
proxy在启动时会默认调用该方法去获取初始的路由配置。（备注：配置都可以用前文说过的json字符串去描述，你可以用ReadableResourceTableUtil.parseTable(String conf)方法来转成ResourceTable对象）  
此外，当路由配置发生了变更，你可以调用ProxyRouteConfUpdater提供的回调方法去实时变更，回调方法如下：
```
public final void invokeUpdateResourceTable(long bid, String bgroup, ResourceTable resourceTable)
```
当需要删除一个路由时，可以调用invokeRemoveResourceTable回调方法，如下：
```
public void invokeRemoveResourceTable(long bid, String bgroup)
```
开启ProxyRouteConfUpdater的示例配置如下：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.DynamicConfProxyRouteConfUpdater
      dynamic: true #表示支持多组配置，默认就是true
      bid: 1 #默认的bid，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      bgroup: default #默认的bgroup，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      reload-interval-millis: 600000 #使用ProxyRouteConfUpdater时，配置变更会通过回调自动更新，为了防止更新出现丢失，会有一个兜底轮询机制，本配置表示兜底轮询的间隔，默认10分钟
```
上面的配置表示我们使用DynamicConfProxyRouteConfUpdater这个ProxyRouteConfUpdater的实现类，这个实现类下，配置托管给了ProxyDynamicConf(camellia-redis-proxy.properties)   
使用DynamicConfProxyRouteConfUpdater时的配置方式是以k-v的形式进行配置，如下：
```
#表示bid=1/bgroup=default的路由配置
1.default.route.conf=redis://@127.0.0.1:6379
#表示bid=2/bgroup=default的路由配置
2.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}
```
除了camellia提供的DynamicConfProxyRouteConfUpdater，你可以自己实现一个自定义的ProxyRouteConfUpdater实现，从而对接到你们的配置中心中，下面提供了一个自定义实现的例子：
```java
public class CustomProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private String url = "redis://@127.0.0.1:6379";

    public CustomProxyRouteConfUpdater() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::update, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        return ReadableResourceTableUtil.parseTable(url);
    }

    private void update() {
        String newUrl = "redis://@127.0.0.2:6379";
        if (!url.equals(newUrl)) {
            url = newUrl;
            invokeUpdateResourceTableJson(1, "default", url);
        }
    }
}
```
上述的例子中，proxy一开始的路由是redis://@127.0.0.1:6379，10s之后，被切换到了redis://@127.0.0.2:6379
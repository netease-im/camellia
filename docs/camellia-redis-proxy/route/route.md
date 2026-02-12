## 路由配置
路由配置表示了camellia-redis-proxy在收到客户端的redis命令之后的转发规则

### 最简单的示例
在`camellia-redis-proxy.properties`
```properties
port=6380
password=pass123
route.conf=redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```
上面的配置表示proxy的端口=6380，proxy的密码=pass123，代理到后端redis-cluster集群，地址串=127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377

### 支持的后端redis类型
* `route.conf` 不仅仅支持redis-cluster，也支持各种类型的redis后端
* 具体见：[redis-resources](redis-resources.md)

### 如何定义一个复杂配置（读写分离、分片等）
* `route.conf` 不仅仅可以配置一个地址串，也可以配置一个json，来描述读写分离、分片等复杂逻辑
* 具体见：[complex](complex.md)

### 底层逻辑
* 上面的最简单的示例中只有3行配置，但是通过更多的配置，你可以实现以下效果
* 多租户支持，也就是逻辑上支持配置多个 `route.conf` ，类似于nginx不同的server
* 那proxy如何判断应该走哪个 `route.conf` 呢？有两种方法，一种是建立连接时调用 `client setname` 命令来告诉proxy，一个是通过不同的 `password` 来告诉proxy
* 通过 `password` 选择路由的建议和最初实现来自于 [@yangxb2010000](https://github.com/yangxb2010000) 

### proxy的路由功能包括2个环节
* 一个是建立连接时选定路由，在proxy中，一个路由我们通过bid和bgroup两个字段来描述，前者为一个数字，后者为一个字符串（为什么是2个字段而不是1个字段，这是历史原因，不要深究）
* 选定了路由后，proxy再找到bid/bgroup对应的 `route.conf` ，可能是一个redis地址，也可能是一个复杂的json配置
* 通过这种两层的逻辑架构，客户端连接不断开的情况下，就可以动态切换后端了

### 路由选择的实现
* 具体见：`RouteConfProvider.java`
* 这是一个抽象类，有多种内置实现，你也可以自定义，类定义如下：

```java
    /**
     * 对应于auth或者hello命令
     * @param userName 账号
     * @param password 密码
     * @return 校验结果+ bid/bgroup
     */
    public abstract ClientIdentity auth(String userName, String password);
    
    /**
     * 是否需要密码
     * @return true/false
     */
    public abstract boolean isPasswordRequired();
    
    /**
     * 默认路由，当客户端没有声明bid/bgroup时的路由配置
     * @return route conf
     */
    public abstract String getRouteConfig();
    
    /**
     * 当客户端声明bid/bgroup时的路由配置
     * @param bid bid
     * @param bgroup bgroup
     * @return ResourceTable
     */
    public abstract String getRouteConfig(long bid, String bgroup);
    
    /**
     * 是否支持多租户
     * @return true/false
     */
    public abstract boolean isMultiTenantsSupport();

    protected final void invokeUpdateResourceTable(String routeConf) {}
    protected final void invokeUpdateResourceTable(long bid, String bgroup, String routeConf) {}
    protected final void invokeRemoveResourceTable(long bid, String bgroup) {}
```

* `auth` 和 `isPasswordRequired` 在建立连接时使用，会返回登录成功或者失败
* `ClientIdentity` 内部包含了bid和bgroup两个字段，proxy会把相关信息绑定到建立好的客户端连接上
* 当客户端连接建立好后，发起redis命令，如 `get` 和 `set`，proxy会取出连接上的bid和bgroup信息，去调用 `getRouteConfig` 方法，获取到路由配置，并做转发
* `isMultiTenantsSupport` 用于告诉proxy，当bid和bgroup不同时，ResourceTable是不是会不一样，如果 `isMultiTenantsSupport=true`，则代表有多条路由
* `invokeUpdateResourceTable` 和 `invokeRemoveResourceTable` 是回调方法，当某个bid和bgroup对应的 `route.conf` 发生了变化，可以通过这三个回调来告诉proxy
* 正是因为有这三个回调方法的存在，因此并不是每个 `get` 和 `set` 命令都会触发 `getRouteConfig` 方法的调用，proxy会缓存结果以便有更好的性能


### 内置的RouteConfProvider实现
* 具体见：`RouteConfProviderEnums.java` 
* 当前内置了5种实现，文档开头的最简单配置，实际上使用了 `DefaultRouteConfProvider.java` 这一默认实现


#### DefaultRouteConfProvider.java
* 这是默认实现，该实现不支持多租户，也就是 `isMultiTenantsSupport=false`
```properties
route.conf.provider=default

#### 优先看route.conf，如果没有则看route.conf.file
#### 支持单个地址，也支持复杂配置（json）
route.conf=redis://@127.0.0.1:6379
#route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}
#### 支持外置其他配置文件作为数据原，支持class_path下的文件名，也支持绝对路径下的文件地址
route.conf.file=resource-table.json
#### 配置检查更新的间隔
route.conf.check.interval.millis=3000
```

#### CamelliaDashboardRouteConfProvider.java
* 该实现下，路由配置托管给了 `camellia-dashboard`，支持配置多租户，也可以关掉多租户
* 该模式下，客户端登录后，需要使用 `client setname camellia_1_default` 命令告诉服务器，选择了bid=1和bgroup=default这组路由配置
* 这种模式内部主要是为了统一管理redis-proxy和其他配置（如camellia-feign、camellia-hbase），如果你单独使用redis-proxy，可以跳过
```properties
route.conf.provider=camellia_dashboard

#
camellia.dashboard.url=http://127.0.0.1:8080
## 当客户端没有告诉服务器bid/bgroup时，默认的bid和bgroup
camellia.dashboard.bid=1
camellia.dashboard.bgroup=default
## 设置为true，表示支持多租户
camellia.dashboard.dynamic=true
camellia.dashboard.monitor.enable=false
camellia.dashboard.check.interval.millis=5000
camellia.dashboard.connect.timeout.millis=10000
camellia.dashboard.read.timeout.millis=60000
camellia.dashboard.headers={"k1":"v1"}
```

#### MultiTenantsV1RouteConfProvider.java
* 该实现下，路由配置托管给了本地配置文件，支持多租户
* 配置方式更适合properties格式的配置文件
```properties
route.conf.provider=multi_tenants_v1

#### 表示pass123密码指向bid=1/bgroup=default，路由是redis://@127.0.0.1:6379
pass123.password.1.default.route.conf=redis://@127.0.0.1:6379
#### 表示pass456密码指向bid=2/bgroup=default，路由是一个复杂的配置
pass456.password.2.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}

```

#### MultiTenantsV2RouteConfProvider.java
* 该实现下，路由配置托管给了本地配置文件，支持多租户
* 配置方式更适合json格式的配置文件
```json
{
  "route.conf.provider": "multi_tenants_v1",
  "multi.tenant.route.config":
  [
    {
      "bid": 1,
      "bgroup": "route1",
      "password": "passwd1",
      "route": "redis://passxx@127.0.0.1:16379"
    },
    {
      "bid": 1,
      "bgroup": "route2",
      "password": "passwd2",
      "route": "redis-cluster://@127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382"
    },
    {
      "bid": 1,
      "bgroup": "route3",
      "password": "passwd3",
      "route":
      {
        "type": "simple",
        "operation":
        {
          "read": "redis://passwd123@127.0.0.1:6379",
          "type": "rw_separate",
          "write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"
        }
      }
    }
  ]
}
```

#### SimpleConfigRouteConfProvider.java
* 该实现下，路由配置托管给了外部的simple_config服务，支持多租户
* simple_config的接口规范参考 `SimpleConfigFetcher.java`
```properties
route.conf.provider=multi_tenants_simple_config

#### 外部系统（满足simple_config的接口规范即可）
simple.config.fetch.url=http://127.0.0.1:8080
simple.config.fetch.key=xxx
simple.config.fetch.secret=xxx
#### 表示pass123密码指向bid=1/bgroup=default，路由biz=biz1，路由配置通过外部simple_config系统获取
pass123.password.1.default.route.conf.biz=biz1
#### 表示pass456密码指向bid=2/bgroup=default，路由biz=biz2，路由配置通过外部simple_config系统获取
pass456.password.2.default.route.conf.biz=biz2
```

#### 当然你也可以自定义
```properties
# 配置全路径的类名即可
route.conf.provider=com.xxx.xxx.CustomRouteConfProvider
```
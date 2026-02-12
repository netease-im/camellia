## Route Configuration
Route configuration defines the forwarding rules after camellia-redis-proxy receives redis commands from clients.

### Simplest Example
In `camellia-redis-proxy.properties`
```properties
port=6380
password=pass123
route.conf=redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```
The above configuration means proxy port=6380, proxy password=pass123, proxying to backend redis-cluster cluster, address string=127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377

### Supported Backend Redis Types
* `route.conf` not only supports redis-cluster, but also supports various types of redis backends
* See: [redis-resources](redis-resources.md)

### How to Define Complex Configuration (Read-Write Separation, Sharding, etc.)
* `route.conf` can not only configure an address string, but also configure a JSON to describe complex logic like read-write separation, sharding, etc.
* See: [complex](complex.md)

### Underlying Logic
* The simplest example above has only 3 lines of configuration, but through more configuration, you can achieve the following effects
* Multi-tenant support, which logically supports configuring multiple `route.conf`, similar to different servers in nginx
* How does proxy determine which `route.conf` to use? There are two methods: one is to call the `client setname` command when establishing a connection to tell the proxy, and the other is to tell the proxy through different `password`
* The suggestion and initial implementation for route selection through `password` came from [@yangxb2010000](https://github.com/yangxb2010000)

### Proxy's Routing Function Includes Two Stages
* One is selecting a route when establishing a connection. In proxy, a route is described by two fields: bid and bgroup. The former is a number, the latter is a string (why two fields instead of one? This is for historical reasons, don't dig too deep)
* After selecting a route, proxy finds the `route.conf` corresponding to bid/bgroup, which may be a redis address or a complex JSON configuration
* Through this two-layer logical architecture, the backend can be dynamically switched without disconnecting the client connection

### Route Selection Implementation
* See: `RouteConfProvider.java`
* This is an abstract class with multiple built-in implementations, and you can also customize it. The class definition is as follows:

```java
    /**
     * Corresponds to auth or hello command
     * @param userName Account
     * @param password Password
     * @return Verification result + bid/bgroup
     */
    public abstract ClientIdentity auth(String userName, String password);

    /**
     * Whether password is required
     * @return true/false
     */
    public abstract boolean isPasswordRequired();

    /**
     * Default route, the route configuration when the client has not declared bid/bgroup
     * @return route conf
     */
    public abstract String getRouteConfig();

    /**
     * Route configuration when the client declares bid/bgroup
     * @param bid bid
     * @param bgroup bgroup
     * @return ResourceTable
     */
    public abstract String getRouteConfig(long bid, String bgroup);

    /**
     * Whether to support multi-tenancy
     * @return true/false
     */
    public abstract boolean isMultiTenantsSupport();

    protected final void invokeUpdateResourceTable(long bid, String bgroup, String routeConf) {}
    protected final void invokeRemoveResourceTable(long bid, String bgroup) {}
```

* `auth` and `isPasswordRequired` are used when establishing a connection and will return login success or failure
* `ClientIdentity` contains two fields: bid and bgroup. Proxy will bind relevant information to the established client connection
* After the client connection is established and redis commands are initiated (such as `get` and `set`), proxy will retrieve the bid and bgroup information from the connection and call the `getRouteConfig` method to obtain the route configuration and forward it
* `isMultiTenantsSupport` is used to tell proxy whether ResourceTable will be different when bid and bgroup are different. If `isMultiTenantsSupport=true`, it means there are multiple routes
* `invokeUpdateResourceTable` and `invokeRemoveResourceTable` are two callback methods. When the `route.conf` corresponding to a bid and bgroup changes, you can notify proxy through these two callbacks
* Because of these two callback methods, not every `get` and `set` command will trigger the `getRouteConfig` method call. Proxy will cache the results for better performance


### Built-in RouteConfProvider Implementations
* See: `RouteConfProviderEnums.java`
* Currently, 5 implementations are built-in. The simplest configuration at the beginning of the document actually uses the `DefaultRouteConfProvider.java` default implementation


#### DefaultRouteConfProvider.java
* This is the default implementation, which does not support multi-tenancy, i.e., `isMultiTenantsSupport=false`
```properties
route.conf.provider=default

#### Priority is given to route.conf, if not available, check route.conf.file
#### Support single address, also support complex configuration (json)
route.conf=redis://@127.0.0.1:6379
#route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}
#### Support external configuration files as data sources, support file names under class_path, also support file addresses under absolute paths
route.conf.file=resource-table.json
#### Configuration check update interval
route.conf.check.interval.millis=3000
```

#### CamelliaDashboardRouteConfProvider.java
* Under this implementation, route configuration is entrusted to `camellia-dashboard`, supporting multi-tenant configuration, or you can disable multi-tenancy
* In this mode, after the client logs in, you need to use the `client setname camellia_1_default` command to tell the server that you have selected the route configuration with bid=1 and bgroup=default
* This mode is mainly designed to uniformly manage redis-proxy and other configurations (such as camellia-feign, camellia-hbase). If you use redis-proxy alone, you can skip it
```properties
route.conf.provider=camellia_dashboard

#
camellia.dashboard.url=http://127.0.0.1:8080
## When the client has not told the server bid/bgroup, the default bid and bgroup
camellia.dashboard.bid=1
camellia.dashboard.bgroup=default
## Set to true to indicate support for multi-tenancy
camellia.dashboard.dynamic=true
camellia.dashboard.monitor.enable=false
camellia.dashboard.check.interval.millis=5000
camellia.dashboard.connect.timeout.millis=10000
camellia.dashboard.read.timeout.millis=60000
camellia.dashboard.headers={"k1":"v1"}
```

#### MultiTenantsV1RouteConfProvider.java
* Under this implementation, route configuration is entrusted to local configuration files, supporting multi-tenancy
* Configuration method is more suitable for properties format configuration files
```properties
route.conf.provider=multi_tenants_v1

#### Indicates that password pass123 points to bid=1/bgroup=default, route is redis://@127.0.0.1:6379
pass123.password.1.default.route.conf=redis://@127.0.0.1:6379
#### Indicates that password pass456 points to bid=2/bgroup=default, route is a complex configuration
pass456.password.2.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}

```

#### MultiTenantsV2RouteConfProvider.java
* Under this implementation, route configuration is entrusted to local configuration files, supporting multi-tenancy
* Configuration method is more suitable for JSON format configuration files
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
* Under this implementation, route configuration is entrusted to external simple_config service, supporting multi-tenancy
* For the interface specification of simple_config, refer to `SimpleConfigFetcher.java`
```properties
route.conf.provider=multi_tenants_simple_config

#### External system (just need to meet the interface specification of simple_config)
simple.config.fetch.url=http://127.0.0.1:8080
simple.config.fetch.key=xxx
simple.config.fetch.secret=xxx
#### Indicates that password pass123 points to bid=1/bgroup=default, route biz=biz1, route configuration is obtained through external simple_config system
pass123.password.1.default.route.conf.biz=biz1
#### Indicates that password pass456 points to bid=2/bgroup=default, route biz=biz2, route configuration is obtained through external simple_config system
pass456.password.2.default.route.conf.biz=biz2
```

#### Of course, you can also customize
```properties
# Just configure the full path class name
route.conf.provider=com.xxx.xxx.CustomRouteConfProvider
```

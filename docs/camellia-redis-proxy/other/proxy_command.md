
## PROXY命令

PROXY命令用于查询和操作proxy本身的一些配置

### 查询基本信息(proxy info)
示例： 
```shell
127.0.0.1:6380> proxy info
camellia_version:v1.3.5
upstream_client_template_factory:UpstreamRedisClientTemplateFactory
transpond_config:{"type":"local","multiTenantsSupport":false}
client_auth_provider:ClientAuthByConfigProvider
proxy_mode:standalone
proxy_dynamic_conf_loader:FileBasedProxyDynamicConfLoader
monitor_enable:false
command_spend_time_monitor_enable:false
upstream_redis_spend_time_monitor_enable:false
request_plugins:MonitorProxyPlugin,HotKeyProxyPlugin,BigKeyProxyPlugin
reply_plugins:HotKeyProxyPlugin,BigKeyProxyPlugin,MonitorProxyPlugin
127.0.0.1:6380> 
```
包括了proxy版本，是否开启监控，路由配置，插件配置等信息

### 查询proxy列表
* 有三种情况，开启伪redis-cluster模式、开启伪redis-sentinel模式、默认standalone模式
* 如果开启了伪redis-cluster模式，则返回的是伪redis-cluster模式下自动发现的所有节点
* 如果开启了伪redis-sentinel模式，则返回的是伪redis-sentinel模式下配置的所有节点
* 如果是默认standalone模式，则需要从其他途径获取节点列表，默认提供了两种方式：
* 方式一（默认）：
* 配置文件中配置，如下：  
```properties
proxy.nodes=10.221.145.235:6380@7380,10.221.145.235:6381@7381
```
* 方式二（基于redis心跳）：
* 配置文件中配置，如下：  
```properties
#表示启用RedisProxyNodesDiscovery
proxy.nodes.discovery.className=com.netease.nim.camellia.redis.proxy.command.RedisProxyNodesDiscovery
#表示RedisProxyNodesDiscovery使用的redis地址
proxy.nodes.discovery.redis.url=redis://@127.0.0.1:6379
###
#RedisProxyNodesDiscovery会定时发送心跳到redis，key为前缀+application.yml中的applicationName
#所有proxy节点会定时从redis中获取心跳结果，也就是proxy集群列表
##相关配置参数如下：
#心跳目标key的前缀
proxy.nodes.discovery.redis.heartbeat.key.prefix=camellia_redis_proxy_heartbeat
#心跳间隔，默认5s
proxy.nodes.discovery.redis.heartbeat.interval.seconds=5
#心跳过期时间，默认30s，也就是30s没有心跳代表某个proxy节点已经下线了
proxy.nodes.discovery.redis.heartbeat.timeout.seconds=30
```
* 方式三（自定义）：
* 自己实现ProxyNodesDiscovery接口即可

示例：
```shell
127.0.0.1:6380> proxy nodes
1) "10.221.145.235:6380@7380"
2) 1) "10.221.145.235:6380@7380"
   2) "10.221.145.235:6381@7381"
127.0.0.1:6380> 
```
第一行表示本节点的信息，格式为：ip:port@cport
第二行表示所有节点的信息，是一个数组（如果是伪redis-cluster模式，则只会展示在线节点）

### 查询自定义配置
```shell
127.0.0.1:6380> proxy config list
1) 1) 1) "init.config.size"
      2) "2"
   2) 1) "init.config.md5"
      2) "44797b4040c1f6ac3e048da320fd8085"
   3) 1) "special.config.size"
      2) "4"
   4) 1) "special.config.md5"
      2) "8aebd6b93c3d4d3f89fe213e610f3190"
   5) 1) "all.config.size"
      2) "6"
   6) 1) "all.config.md5"
      2) "eaeccc6375e6c75297ae65f92893241a"
2) 1) 1) "k1"
      2) "v1"
   2) 1) "k2"
      2) "v2"
   3) 1) "k3"
      2) "v3"
   4) 1) "k4"
      2) "v44444"
   5) 1) "kkkk"
      2) "vvvvv123"
   6) 1) "proxy.nodes"
      2) "10.221.145.235:6380@7380,10.221.145.235:6381@7381"
127.0.0.1:6380>
```
* 第一部分（meta，包括size和md5）： 
* init.config表示初始配置，也就是application.yml中config的配置（如下的：k1=v1,k2=v2），size表示数量，md5表示md5值
```yaml
camellia-redis-proxy:
#  port: 6380 #priority greater than server.port, if missing, use server.port; if setting -6379, proxy will choose a random port
#  application-name: camellia-redis-proxy-server  #priority greater than spring.application.name, if missing, use spring.application.name
  console-port: 16379 #console port, default 16379, if setting -16379, proxy will choose a random port, if setting 0, will disable console
  password: pass123   #password of proxy, priority less than custom client-auth-provider-class-name
  monitor-enable: false  #monitor enable/disable configure
  monitor-interval-seconds: 60 #monitor data refresh interval seconds
  config:
    "k1": "v1"
    "k2": "v2"
  transpond:
    type: local #local、remote、custom
    local:
      type: simple #simple、complex
      resource: redis://@127.0.0.1:6379
```
* special.config表示特殊配置，也就是camellia-redis-proxy.properties或者camellia-redis-proxy.json中的配置，size表示数量，md5表示md5值，会覆盖init.config的配置
```properties
k2=v222
k3=v3
```
* all.config表示合并后的配置，size表示数量，md5表示md5值，最终ProxyDynamicConf.java获取到的就是这个配置:（k1=v1,k2=v222,k3=v3）
* 
* 第二部分（详情）：
* 表示all.config的详情，是一个数组，通过key-value对的形式展示

### 查询自定义配置的meta信息
```shell
127.0.0.1:6380> proxy config listmeta
1) 1) "init.config.size"
   2) "2"
2) 1) "init.config.md5"
   2) "44797b4040c1f6ac3e048da320fd8085"
3) 1) "special.config.size"
   2) "4"
4) 1) "special.config.md5"
   2) "8aebd6b93c3d4d3f89fe213e610f3190"
5) 1) "all.config.size"
   2) "6"
6) 1) "all.config.md5"
   2) "eaeccc6375e6c75297ae65f92893241a"
127.0.0.1:6380>
```
也就是`proxy config list`的第一部分

### 查询所有节点的自定义配置的meta信息
```shell
127.0.0.1:6380> proxy config listmetaall
1) 1) "10.221.145.235:6380@7380"
   2) 1) 1) "init.config.size"
         2) "2"
      2) 1) "init.config.md5"
         2) "44797b4040c1f6ac3e048da320fd8085"
      3) 1) "special.config.size"
         2) "4"
      4) 1) "special.config.md5"
         2) "8aebd6b93c3d4d3f89fe213e610f3190"
      5) 1) "all.config.size"
         2) "6"
      6) 1) "all.config.md5"
         2) "eaeccc6375e6c75297ae65f92893241a"
2) 1) 1) "10.221.145.235:6381@7381"
      2) 1) 1) "init.config.size"
            2) "2"
         2) 1) "init.config.md5"
            2) "53edd66781a8bfe68ed9ac8fb81570ba"
         3) 1) "special.config.size"
            2) "4"
         4) 1) "special.config.md5"
            2) "8aebd6b93c3d4d3f89fe213e610f3190"
         5) 1) "all.config.size"
            2) "6"
         6) 1) "all.config.md5"
            2) "9142ddafce90ccdb3cbcbd4aaf3ce340"
127.0.0.1:6380>
```
* 第一部分为为本节点的config的meta信息
* 第二部分为一个数组，为其他节点的config的meta信息



### reload配置
```shell
127.0.0.1:6380> proxy config reload
OK
127.0.0.1:6380>
```

### 所有节点一起reload配置
```shell
127.0.0.1:6380> proxy config reloadall
1) 1) "10.221.145.235:6380@7380"
   2) OK
2) 1) 1) "10.221.145.235:6381@7381"
      2) OK
127.0.0.1:6380>
```
* 第一部分为本节点的reload结果
* 第二部分为一个数组，是其他节点的reload结果

### 把本节点的配置同步到其他节点
```shell
127.0.0.1:6380> proxy config broadcast
1) 1) "10.221.145.235:6380@7380"
   2) OK
2) 1) 1) "10.221.145.235:6381@7381"
      2) OK
127.0.0.1:6380>
```
* 第一部分为本节点的reload结果
* 第二部分为把配置发送给其他节点后并且立即reload后的结果
* 同步的配置只有special.config，不包括init.config


### 校验自定义配置的一致性
会校验所有节点的special.config.md5是否一致
```shell
127.0.0.1:6380> proxy config checkmd5all
1) "8aebd6b93c3d4d3f89fe213e610f3190"
2) 1) 1) "10.221.145.235:6381@7381"
      2) (error) ERR md5 not match
127.0.0.1:6380>
127.0.0.1:6380>
127.0.0.1:6380> proxy config broadcast
1) 1) "10.221.145.235:6380@7380"
   2) OK
2) 1) 1) "10.221.145.235:6381@7381"
      2) OK
127.0.0.1:6380>
127.0.0.1:6380>
127.0.0.1:6380> proxy config checkmd5all
1) "8aebd6b93c3d4d3f89fe213e610f3190"
2) 1) 1) "10.221.145.235:6381@7381"
      2) OK
127.0.0.1:6380>
```
* 第一部分为本节点的special.config.md5
* 第二部分为其他节点的md5校验情况，ok表示一致，error表示不一致
* 可以用于`proxy config broadcast`后，确认所有配置是否一致

### 当启动一个全新节点时，希望从指定节点同步配置
```shell
127.0.0.1:6381> proxy config syncfrom 10.221.145.235:6380@7380
OK
127.0.0.1:6381>
```
特别的，如果你希望这个同步操作是自动的，则可以在新节点上配置如下：
```properties
config.auto.sync.target.proxy.node=10.221.145.235:6380@7380
config.auto.sync.enable=true
```
则proxy启动时会自动从指定节点同步配置（只会同步一次）


### 应用场景
* 可以选定某个节点，修改配置，然后broadcast到其他节点，这样就可以实现动态批量修改配置，从而不依赖于etcd/nacos等配置中心
* 只有实现了WritableProxyDynamicConfLoader的ProxyDynamicConfLoader才可以这样操作
* 内置的FileBasedProxyDynamicConfLoader（默认，基于camellia-redis-proxy.properties文件）实现了WritableProxyDynamicConfLoader
* 内置的JsonFileBasedProxyDynamicConfLoader（基于camelia-redis-proxy.json文件）实现了WritableProxyDynamicConfLoader
* 对于proxy节点的个性化配置（和集群内其他节点不一样的配置），配置在application.yml里 
* 对于proxy节点的公共配置，则配置在camellia-redis-proxy.properties或者camellia-redis-proxy.json里

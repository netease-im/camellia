
## PROXY命令

PROXY命令用于查询和操作proxy本身的一些配置

### 查询基本信息(proxy info)
示例： 
```shell
127.0.0.1:6380> proxy info
version:v1.2.19
upstream_client_template_factory:UpstreamRedisClientTemplateFactory
transpond_config:{"type":"local","multiTenantsSupport":false}
client_auth_provider:ClientAuthByConfigProvider
cluster_mode_enable:false
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
* 有两种情况，开启或者未开启伪redis-cluster模式
* 如果开启了伪redis-cluster模式，则返回的是伪redis-cluster模式下自动发现的所以节点
* 如果没有开启伪redis-cluster模式，则返回的列表需要在配置文件中配置，如下：  
```properties
proxy.nodes=10.221.145.235:6380@7380,10.221.145.235:6381@7381
```

示例：
```shell
127.0.0.1:6380> proxy servers
1) "redis_cluster_mode_enable:false"
2) "10.221.145.235:6380@7380"
3) 1) "10.221.145.235:6380@7380"
   2) "10.221.145.235:6381@7381"
127.0.0.1:6380> 
```
第一行表示是否开启伪redis-cluster模式
第二行表示本节点的信息，格式为：ip:port@cport
第三行表示所有节点的信息，是一个数组（如果是伪redis-cluster模式，则只会展示在线节点）

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
* init.config表示初始配置，也就是application.yml中config的配置，size表示数量，md5表示md5值
* special.config表示特殊配置，也就是camellia-redis-proxy.properties或者camellia-redis-proxy.json中的配置，size表示数量，md5表示md5值，会覆盖init.config的配置
* all.config表示合并后的配置，size表示数量，md5表示md5值，最终ProxyDynamicConf.java获取到的就是这个配置
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

### 应用场景
* 可以选定某个节点，修改配置，然后broadcast到其他节点，这样就可以实现动态批量修改配置，从而不依赖于etcd/nacos等配置中心
* 只有实现了WritableProxyDynamicConfLoader的ProxyDynamicConfLoader才可以这样操作
* 内置的FileBasedProxyDynamicConfLoader（默认，基于camellia-redis-proxy.properties文件）和JsonFileBasedProxyDynamicConfLoader（基于camelia-redis-proxy.json文件）支持
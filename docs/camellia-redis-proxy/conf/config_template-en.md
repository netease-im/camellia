# Core Configuration


```properties

############# Basic Configuration ##################

# server
## see ServerConf.java
## TCP port, defaults to server.port in application.yml, this configuration has higher priority
port=6380
## Cluster name, defaults to spring.application.name, this configuration has higher priority
application.name=camellia-redis-proxy-server
## Password
password=pass123
## Whether to enable monitoring
monitor.enable=false
## Console port
console.port=16379
## HTTP port, invalid if less than 0, default -1
http.port=-1
## UDS address, invalid if omitted, default omitted
uds.path=
## Internal communication port, required in cluster/sentinel mode, default port+10000
cport=6381
## Internal communication port password
cport.password=pass456



############# Network Configuration ##################

# netty
## see NettyConf.java, generally use default
## support auto, epoll, kqueue, io_uring, nio
netty.transport.mode=auto


############# Cluster Mode ##################


# support standalone, cluster, sentinel
proxy.mode=standalone
## Declared local address, has an auto-obtained default value, can be replaced by this configuration, format is host:port@cport
proxy.node.current.host=
## Declared local port, has an auto-obtained default value, can be replaced by this configuration, must be a number greater than 0
proxy.node.current.announce.port=


## cluster mode
### Two built-in implementations: DefaultClusterModeProvider and ConsensusClusterModeProvider, can also customize ProxyClusterModeProvider.java

### DefaultClusterModeProvider, based on configuration (default)
cluster.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.cluster.provider.DefaultClusterModeProvider
#### Configured node list
cluster.mode.nodes=10.0.0.1:6380@6381,10.0.0.2:6380@6381

### ConsensusClusterModeProvider, based on redis
#cluster.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.cluster.provider.ConsensusClusterModeProvider
#### Redis address
redis.consensus.leader.selector.redis.url=redis://@127.0.0.1:6379
#### Redis-key, one cluster one key
redis.consensus.leader.selector.redis.key=xxx


## sentinel mode
### Two built-in implementations: DefaultSentinelModeProvider and RedisSentinelModeProvider, can also customize SentinelModeProvider.java
sentinel.mode.master.name=xxx
sentinel.mode.sentinel.username=xxx
sentinel.mode.sentinel.password=xxx

### DefaultSentinelModeProvider, based on configuration (default)
sentinel.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.sentinel.DefaultSentinelModeProvider
#### Configured node list
sentinel.mode.nodes=10.0.0.1:6380@6381,10.0.0.2:6380@6381

### RedisSentinelModeProvider, based on redis
#sentinel.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.sentinel.RedisSentinelModeProvider
#### Redis address
sentinel.mode.provider.redis.url=redis://@127.0.0.1:6379
#### Redis-key, one cluster one key
sentinel.mode.provider.redis.key=xxx


############# Routing Configuration ##################

# route
## Route implementation, see RouteConfProviderEnums.java for built-in route implementations
## Built-in: default (default), camellia_dashboard, multi_tenants_v1, multi_tenants_v2, multi_tenants_simple_config
## You can also implement RouteConfProvider.java, then configure a full class path
route.conf.provider=default

### default route
#### Priority is given to route.conf, if not available then route.conf.file
#### Support single address, also support complex configuration (json)
route.conf=redis://@127.0.0.1:6379
#route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}
#### Support external configuration files as data sources, support file names under class_path, also support file addresses under absolute paths
route.conf.file=resource-table.json
#### Configuration check update interval
route.conf.check.interval.millis=3000

### camellia dashboard route
camellia.dashboard.url=http://127.0.0.1:8080
camellia.dashboard.bid=1
camellia.dashboard.bgroup=default
camellia.dashboard.dynamic=true
camellia.dashboard.monitor.enable=false
camellia.dashboard.check.interval.millis=5000
camellia.dashboard.connect.timeout.millis=10000
camellia.dashboard.read.timeout.millis=60000
camellia.dashboard.headers={"k1":"v1"}

### multi_tenants_v1
#### Indicates that password pass123 points to bid=1/bgroup=default, route is redis://@127.0.0.1:6379
pass123.password.1.default.route.conf=redis://@127.0.0.1:6379
#### Indicates that password pass456 points to bid=2/bgroup=default, route is a complex configuration
pass456.password.2.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}

### multi_tenants_v2
#### Points to a json array containing 3 route groups
multi.tenant.route.config=[{"bid":1,"bgroup":"route1","password":"passwd1","route":"redis://passxx@127.0.0.1:16379"},{"bid":1,"bgroup":"route2","password":"passwd2","route":"redis-cluster://@127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382"},{"bid":1,"bgroup":"route3","password":"passwd3","route":{"type":"simple","operation":{"read":"redis://passwd123@127.0.0.1:6379","type":"rw_separate","write":"redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}}]

### multi_tenants_simple_config
#### External system (just need to meet the interface specification of simple_config)
simple.config.fetch.url=http://127.0.0.1:8080
simple.config.fetch.key=xxx
simple.config.fetch.secret=xxx
#### Indicates that password pass123 points to bid=1/bgroup=default, route biz=biz1, route configuration is obtained through external system
pass123.password.1.default.route.conf.biz=biz1
#### Indicates that password pass456 points to bid=2/bgroup=default, route biz=biz2, route configuration is obtained through external system
pass456.password.2.default.route.conf.biz=biz2


############# Plugin Configuration ##################

# Plugin list, comma separated
## See ProxyPluginEnums.java for built-in plugins
## You can also customize ProxyPlugin.java, configure a full class path
## Refer to specific plugin implementation code for each plugin configuration
proxy.plugin.list=monitorPlugin,hotKeyPlugin,bigKeyPlugin



############# Other Configuration ##################


```

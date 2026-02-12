

# camellia-redis-proxy.properties配置


```properties

#############基本配置##################

# server
## see ServerConf.java
## tcp端口，默认使用application.yml里的server.port，本配置优先级更高
port=6380
## 集群名，默认使用spring.application.name，本配置优先级更高
application.name=camellia-redis-proxy-server
## 密码
password=pass123
## 是否开启监控
monitor.enable=false
## console端口
console.port=16379
## http端口，小于0则不生效，默认-1
http.port=-1
## uds的地址，缺省则不生效，默认缺省
uds.path=
## 内部通信端口，cluster/sentinel模式下，则必须有，默认为port+10000
cport=6381
## 内部通信端口的密码
cport.password=pass456



#############网络配置##################

# netty
## see NettyConf.java，一般走默认即可
## support auto、epoll、kqueue、io_uring、nio
netty.transport.mode=auto


#############集群模式##################


# support standalone、cluster、sentinel
proxy.mode=standalone
## 声明的本机地址，有自动获取的默认值，可以通过本配置替换掉，格式为host:port@cport
proxy.node.current.host=
## 声明的本机端口，有自动获取的默认值，可以通过本配置替换掉，要大于0的数字
proxy.node.current.announce.port=


## cluster mode
### 内置了2种实现：DefaultClusterModeProvider和ConsensusClusterModeProvider，也可以自定义ProxyClusterModeProvider.java

### DefaultClusterModeProvider，基于配置（默认）
cluster.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.cluster.provider.DefaultClusterModeProvider
#### 配置的节点列表
cluster.mode.nodes=10.0.0.1:6380@6381,10.0.0.2:6380@6381

### ConsensusClusterModeProvider，基于redis
#cluster.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.cluster.provider.ConsensusClusterModeProvider
#### redis地址
redis.consensus.leader.selector.redis.url=redis://@127.0.0.1:6379
#### redis-key，一个集群一个key
redis.consensus.leader.selector.redis.key=xxx


## sentinel mode
### 内置了2种实现：DefaultSentinelModeProvider和RedisSentinelModeProvider，也可以自定义SentinelModeProvider.java
sentinel.mode.master.name=xxx
sentinel.mode.sentinel.username=xxx
sentinel.mode.sentinel.password=xxx

### DefaultSentinelModeProvider，基于配置（默认）
sentinel.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.sentinel.DefaultSentinelModeProvider
#### 配置的节点列表
sentinel.mode.nodes=10.0.0.1:6380@6381,10.0.0.2:6380@6381

### RedisSentinelModeProvider，基于redis
#sentinel.mode.provider.class.name=com.netease.nim.camellia.redis.proxy.sentinel.RedisSentinelModeProvider
#### redis地址
sentinel.mode.provider.redis.url=redis://@127.0.0.1:6379
#### redis-key，一个集群一个key
sentinel.mode.provider.redis.key=xxx


#############路由配置##################

# route
## 路由实现，内置路由实现见：RouteConfProviderEnums.java
## 内置: default（默认）、camellia_dashboard、multi_tenants_v1、multi_tenants_v2、multi_tenants_simple_config
## 也可以自定义实现RouteConfProvider.java，则配置一个类的全路径即可
route.conf.provider=default

### default route
#### 优先看route.conf，如果没有则看route.conf.file
#### 支持单个地址，也支持复杂配置（json）
route.conf=redis://@127.0.0.1:6379
#### 支持外置其他配置文件作为数据原，支持class_path下的文件名，也支持绝对路径下的文件地址
route.conf.file=resource-table.json
#### 配置检查更新的间隔
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
#### 表示pass123密码指向bid=1/bgroup=default，路由是redis://@127.0.0.1:6379
pass123.password.1.default.route.conf=redis://@127.0.0.1:6379
#### 表示pass456密码指向bid=2/bgroup=default，路由是一个复杂的配置
pass456.password.2.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}

### multi_tenants_v2
####指向了一个json数组，包含了3组路由
multi.tenant.route.config=[{"bid":1,"bgroup":"route1","password":"passwd1","route":"redis://passxx@127.0.0.1:16379"},{"bid":1,"bgroup":"route2","password":"passwd2","route":"redis-cluster://@127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382"},{"bid":1,"bgroup":"route3","password":"passwd3","route":{"type":"simple","operation":{"read":"redis://passwd123@127.0.0.1:6379","type":"rw_separate","write":"redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}}]

### multi_tenants_simple_config
#### 外部系统（满足simple_config的接口规范即可）
simple.config.fetch.url=http://127.0.0.1:8080
simple.config.fetch.key=xxx
simple.config.fetch.secret=xxx
#### 表示pass123密码指向bid=1/bgroup=default，路由biz=biz1，路由配置通过外部系统获取
pass123.password.1.default.route.conf.biz=biz1
#### 表示pass456密码指向bid=2/bgroup=default，路由biz=biz2，路由配置通过外部系统获取
pass456.password.2.default.route.conf.biz=biz2


#############插件配置##################

# 插件列表，逗号分隔
## 内置插件见：ProxyPluginEnums.java
## 也可以自定义ProxyPlugin.java，配置一个类的全路径即可
## 每个插件的配置参考具体的plugin实现代码即可
proxy.plugin.list=monitorPlugin,hotKeyPlugin,bigKeyPlugin



#############其他配置##################


```
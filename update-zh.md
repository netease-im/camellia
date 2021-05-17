[ENGLISH](update-en.md)
# 未来(TODO)
* camellia-redis-proxy支持key/value等的自定义转换，可以用于透明的数据加密/数据压缩等
* 支持基于注册中心的Lettuce的简单的接入方案
* 支持redis6.0的client-cache特性
* 支持监控数据可视化到prometheus等平台

# 1.0.25（2021/05/xx）
### 新增
* camellia-redis-proxy支持关闭到后端redis的空闲连接，默认开启
* camellia-redis-proxy支持监控到后端redis的连接数，具体见：[监控数据](/docs/redis-proxy/monitor-data.md)

### 更新
* 无

### fix
* camellia-redis-proxy代理redis-cluster时，fix在极端边界情况下可能触发的死锁问题

# 1.0.24（2021/05/11）
### 新增
* camellia-redis-proxy新增ProxyRouteConfUpdater，用户可以自定义实现基于bid/bgroup的多组动态路由配置（比如对接到自己的配置中心，这样就不用依赖camellia-dashboard了），具体见：[路由配置](/docs/redis-proxy/route.md)
* 提供了ProxyRouteConfUpdater的一个默认实现DynamicConfProxyRouteConfUpdater，该实现使用DynamicConfProxy（camellia-redis-proxy.properties）来管理多组路由配置以及配置的动态更新
* camellia-redis-proxy新增ProxyDynamicConfHook，用户可以基于hook来自定义的动态修改相关配置，具体见：[动态配置](/docs/redis-proxy/dynamic-conf.md)
* camellia-redis-proxy新增监控相关callback的DummyMonitorCallback的实现，如果不想打印相关统计日志，设置为dummy的callback实现即可
* camellia-redis-proxy监控指标里新增路由相关的监控项，包括到各个redis后端的请求数，以及当前生效的路由配置，具体见：[监控数据](/docs/redis-proxy/monitor-data.md)
* camellia-redis-proxy耗时监控增加业务级别的数据（bid/bgroup），具体见：[监控数据](/docs/redis-proxy/monitor-data.md)

### 更新
* 无

### fix
* 无


# 1.0.23（2021/04/16）
### 新增
* 无

### 更新
* 更新netty版本到4.1.63

### fix
* 修复jdk8下ConcurrentHashMap的computeIfAbsent方法的一个性能bug，修复见：CamelliaMapUtils，bug见：https://bugs.openjdk.java.net/browse/JDK-8161372

# 1.0.22（2021/04/14）
### 新增
* CamelliaRedisTemplate支持从redis-sentinel集群中的从节点读数据（会自动感知节点宕机、主从切换和节点扩容），具体见：RedisSentinelResource和JedisSentinelSlavesPool
* camellia-redis-proxy支持从redis-sentinel集群中的从节点读数据（会自动感知节点宕机、主从切换和节点扩容），具体见：[路由配置](/docs/redis-proxy/route.md)
* CamelliaRedisTemplate使用camellia-redis-spring-boot-starter接入时，在访问camellia-redis-proxy时支持设置bid/bgroup

### 更新
* camellia-redis-proxy预热失败时启动失败

### fix
* 无

# 1.0.21（2021/04/06）
### 新增
* camellia-redis-proxy在使用本地配置时，支持动态修改路由转发规则，见：[路由配置](/docs/redis-proxy/route.md)
* camellia-redis-proxy的ProxyDynamicConf(camellia-redis-proxy.properties)支持使用外部独立的配置文件进行覆盖，见[动态配置](/docs/redis-proxy/dynamic-conf.md)
* camellia-redis-proxy增加预热功能（默认开启），若开启，则proxy启动时会提前创建好到后端的连接，而不是等实际流量过来时再初始化到后端的连接
* camellia-redis-spring-boot-starter/camellia-hbase-spring-boot-starter使用本地配置文件配置路由时，也支持动态变更

### 更新
* camellia-redis-proxy通过动态配置文件关闭RT监控时同步关闭慢查询监控，和yml配置逻辑保持一致
* camellia-spring-redis-{zk,eureka}-discovery-spring-boot-starter增加开关（默认开启）
* RedisProxyJedisPool增加jedisPoolLazyInit参数用于延迟初始化jedisPool，以提高RedisProxyJedisPool的初始化速度，默认开启，默认先初始化优先级最高的16个proxy的jedisPool

### fix
* fix了RedisProxyJedisPool一个bug，概率极低，会导致异常"Could not get a resource from the pool"（1.0.14时引入）
* fix了camellia-redis-proxy使用fat-jar运行时配置文件找不到的问题

# 1.0.20（2021/02/26）
### 新增
* 无

### 更新
* 重构camellia-redis-proxy-hbase，和老版本不兼容，见 [文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)
* 优化了camellia-redis-proxy开启命令耗时监控下的性能

### fix
* 无

# 1.0.19（2021/02/07）
### 新增
* 无  

### update
* camellia-redis-proxy性能提升，见 [v1.0.19](/docs/redis-proxy/performance-report-8.md)

### fix
* 修复调用KeyParser获取xinfo/xgroup的key时的错误返回，修复使用pipeline方式调用xinfo/xgroup时可能出现的bug

# 1.0.18（2021/01/25）
### 新增
* 新增console的http-api接口/reload去重新加载ProxyDynamicConf
* 支持HSTRLEN/SMISMEMBER/LPOS/LMOVE/BLMOVE
* 支持ZMSCORE/ZDIFF/ZINTER/ZUNION/ZRANGESTORE/GEOSEARCH/GEOSEARCHSTORE
* 开放ProxyDynamicConf的动态配置功能，例子：你在camellia-redis-proxy.properties添加了"k=v"，则你可以调用ProxyDynamicConf.getString("k")获取到"v"，具体详见ProxyDynamicConf类

### 更新
* 若配置了双（多）写，阻塞式命令直接返回不支持

### fix
* 无

# 1.0.17（2021/01/15）
### 新增
* 代理到redis/redis-sentinel，且无分片/无读写分离时，支持事务命令（WATCH/UNWATCH/MULTI/EXEC/DISCARD）
* 支持ZPOPMIN/ZPOPMAX/BZPOPMIN/BZPOPMAX

### 更新
* 无

### fix
* 修复ReplyDecoder的一个bug，proxy将nil的MultiBulkReply改成了empty的MultiBulkReply返回的问题（实现事务命令时发现）
* 修复了ProxyDynamicConf初始化时的一个NPE，该报错不影响ProxyDynamicConf的功能，只是会在proxy（v1.0.16）启动时打印一次错误日志

# 1.0.16（2021/01/11）
### 新增
* 部分参数支持动态变更
* camellia-redis-zk-registry支持注册主机名

### 更新
* 优化了若干并发初始化的加锁过程

### fix
* 无

# 1.0.15（2020/12/30）
### 新增
* 无

### 更新
* HotKeyMonitor的json新增字段times/avg/max
* LRUCounter更新，使用LongAdder替换AtomicLong

### fix
* 无

# 1.0.14（2020/12/28）
### 新增
* 无

### 更新
* RedisProxyJedisPool的兜底线程在刷新proxy列表时，即使ProxySelector已经持有了该proxy，仍然调用add方法，避免偶尔的超时等异常导致proxy负载不均衡

### fix
* 无

# 1.0.13（2020/12/18）
### 新增
* 无

### 更新
* IpSegmentRegionResolver允许设置空的config，从而camellia-spring-redis-eureka-discovery-spring-boot-starter和camellia-spring-redis-zk-discovery-spring-boot-starter启动时regionResolveConf参数可以缺省

### fix
* 无

# 1.0.12（2020/12/17）
### 新增
* RedisProxyJedisPool允许设置自定义的proxy选择策略IProxySelector，默认使用RandomProxySelector，若开启side-car优先，则使用SideCarFirstProxySelector
* RedisProxyJedisPool在使用SideCarFirstProxySelector时，proxy的访问优先级：同机部署的proxy -> 相同region的proxy -> 其他proxy，声明proxy属于哪个region，需要你传入RegionResolver，默认提供了一个基于ip段划分region的IpSegmentRegionResolver
* 新增LocalConfProxyDiscovery

### 更新
* 优化了camellia-redis-proxy在代理redis-cluster时后端redis宕机时的快速失败策略
* camellia-redis-proxy刷新后端slot分布信息的操作改成异步执行

### fix
* 修复了一个redis-cluster刷新slot信息的一个bug（1.0.9时引入）

# 1.0.11（2020/12/09）
### 新增
* camellia-redis-proxy支持设置监控回调MonitorCallback
* camellia-redis-proxy支持慢查询监控，支持设置SlowCommandMonitorCallback
* camellia-redis-proxy支持热key监控，支持设置HotKeyMonitorCallback
* camellia-redis-proxy支持热key在proxy层的本地缓存（仅支持GET命令），支持设置HotKeyCacheStatsCallback
* camellia-redis-proxy支持大key监控，支持设置BigKeyMonitorCallback
* camellia-redis-proxy支持配置读写分离时设置多个读地址（随机选择一个地址读）
* CamelliaRedisTemplate支持获取原始Jedis
* RedisProxyJedisPool支持side-car模式，开启后优先访问同机部署的redis-proxy
* camellia-redis-proxy的console支持根据api（默认是http://127.0.0.1:16379/monitor）获取监控数据（包括tps/rt/慢查询/热key/大key/热key缓存等）
* 新增camellia-spring-redis-zk-discovery-spring-boot-starter，方便使用SpringRedisTemplate的客户端以注册中心模式接入proxy

### 更新
* 修改了CommandInterceptor接口的定义

### fix
* fix自定义分片时mget的NPE问题（1.0.10引入的bug）
* 修复了redis sentinel在proxy上切换时的一个bug

# 1.0.10（2020/10/16）
### 新增
* camellia-redis-proxy支持阻塞式命令，如BLPOP/BRPOP/BRPOPLPUSH等
* camellia-redis-proxy支持redis5.0的stream命令，包括阻塞式的XREAD/XREADGROUP
* camellia-redis-proxy支持pub-sub命令
* camellia-redis-proxy支持集合运算命令，如SINTER/SINTERSTORE/SUNION/SUNIONSTORE/SDIFF/SDIFFSTORE等
* camellia-redis-proxy支持设置双（多）写的模式，提供了三种方式供选择, 参考com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode以及相关文档
* camellia-redis-proxy提供了抽象类AbstractSimpleShadingFunc用于自定义分片函数
* camellia-redis-proxy-hbase支持了针对zmember到hbase的读穿透的单机频控

### 更新
* camellia-redis-proxy-hbase增加了对zset从hbase重建缓存时的保护逻辑

### fix
* 修复了CamelliaHBaseTemplate在双（多）写时执行批量删除时的bug

# 1.0.9（2020/09/08）
### 新增
* camellia-redis-proxy的async模式支持redis sentinel
* camellia-redis-proxy的async模式支持统计命令的执行时间
* camellia-redis-proxy的async模式支持CommandInterceptor，自定义拦截规则
* 新增camellia-redis-zk注册发现组件，提供一个使用注册中心模式使用camellia-redis-proxy的默认实现
* camellia-redis-proxy-hbase新增hbase读穿透的单机流控

### 更新
* 调整camellia-redis-proxy的sendbuf和rcvbuf的默认值，且在回包时不判断channel是否writable，避免超大包+pipeline场景下可能channel not writeable而回包失败
* 移除了camellia-redis-proxy的sync模式
* camellia-redis-proxy的async模式性能优化，具体可见性能报告

### fix
* 无

# 1.0.8（2020/08/04）
### 新增
* camellia-redis-proxy的async模式支持eval和evalsha指令
* CamelliaRedisTemplate支持eval/evalsha
* CamelliaRedisLock使用lua脚本来实现更严格的分布式锁

### 更新
* camellia-redis-proxy的若干优化

### fix
* 无

# 1.0.7（2020/07/16）
### 新增
* camellia-redis-proxy-hbase新增hbase读请求并发情况下的穿透保护逻辑  
* camellia-redis-proxy-hbase对hbase读写新增单次批量限制（批量GET和批量PUT）  
* camellia-redis-proxy-hbase的hbase写操作支持设置为ASYNC_WAL  
* camellia-redis-proxy-hbase的type命令支持缓存null  
* camellia-redis-proxy-hbase新增降级配置，hbase读写操作纯异步化（可能会导致数据不一致）      

### 更新
* 优化了部分监控的性能（LongAdder代替AtomicLong）
* camellia-redis-proxy-hbase的配置使用HashMap代替Properties避免锁竞争  
* camellia-redis-proxy的若干性能优化

### fix
* 无

# 1.0.6（2020/05/22）  
### 新增  
* camellia-redis-proxy-hbase提供了异步刷hbase的模式，减少端侧的响应RT（使用redis队列做缓冲），默认关闭  
### 更新  
* 优化了RedisProxyJedisPool的实现，增加自动禁用不可用Proxy的逻辑  
* camellia-hbase-spring-boot-starter在使用remote配置时，默认开启监控  
### fix  
* 修复了camellia-redis-proxy在async模式下处理pipeline请求时可能乱序返回的bug  

# 1.0.5（2020/04/27）
### 新增
* 新增camellia-redis-eureka-spring-boot-starter，方便spring boot工程以直连camellia-redis-proxy的模式接入（通过eureka自动发现proxy集群），从而不需要LVS/VIP这样的组件  
### 更新
* 优化CamelliaRedisLock的实现  
* 优化camellia-redis-proxy-hbase的实现  
* 更新了camellia-redis-proxy-hbase监控指标（见RedisHBaseMonitor类和RedisHBaseStats类）  
### fix
* 修复camellia-dashboard的swagger-ui的中文乱码问题  

# 1.0.4 (2020/04/20)
第一次发布  
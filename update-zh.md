# 未来(TODO)
* camellia-redis-proxy支持key/value等的自定义转换，可以用于透明的数据加密/数据压缩等
* 支持基于注册中心的Lettuce的简单的接入方案
* 支持redis6.0的client-cache特性
* 支持redis事务，multi/watch/exec等

# 1.0.12（2020/12/17）
### 新增
* RedisProxyJedisPool允许设置自定义的proxy选择策略IProxySelector，默认使用RandomProxySelector，若开启side-car优先，则使用SideCarFirstProxySelector
* RedisProxyJedisPool在使用SideCarFirstProxySelector时，proxy的访问优先级：同机部署的proxy -> 相同region的proxy -> 其他proxy，声明proxy属于哪个region，需要你传入RegionResolver，默认提供了一个基于ip段划分region的IpSegmentRegionResolver
* 新增LocalConfProxyDiscovery

### 更新
* 优化了camellia-redis-proxy在代理redis-cluster时后端redis宕机时的快速失败策略
* camellia-redis-proxy刷新后端slot分布信息的操作改成异步执行

### fix
* 无

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
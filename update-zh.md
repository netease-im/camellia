[ENGLISH](update-en.md)
# 未来(TODO)
* camellia-redis-proxy支持基于注册中心的Lettuce的简单的接入方案
* camellia-redis-proxy支持redis6.0的client-cache特性
* camellia-redis-proxy支持监控数据可视化到prometheus等平台

# 1.0.59（2022/06/xx）
### 新增
* camellia-core、camellia-feign调整异步双写线程模型，并新增支持MISC_ASYNC_MULTI_THREAD模式
* camellia-redis-proxy支持缓存透明双删，具体见：[interceptor](/docs/redis-proxy/interceptor.md)

### 更新
* camellia-dashboard新增几个api
* CamelliaHashedExecutor支持获取完成任务数
* 调整了ProxyConstants的默认参数，调大了双写和分片使用的内部线程池的默认线程数
* camellia-redis-proxy在统计后端redis响应时间时，跳过发布订阅命令和阻塞型命令

### fix
* 修复了后端redis有密码且开启mask的情况下，监控的后端redis响应时间为0的问题


# 1.0.58（2022/05/16）
### 新增
* camellia-redis-proxy的detect接口支持返回key总数/qps等信息，具体见：[detect](/docs/redis-proxy/detect.md)

### 更新
* CamelliaIdGenSdkConfig支持设置OkHttpClient的keepAliveSeconds配置，默认30s

### fix
* 无


# 1.0.57（2022/05/10）
### 新增
* 无

### 更新
* 无

### fix
* fix CamelliaNakedClient双写无效的问题


# 1.0.56（2022/05/10）
### 新增
* camellia-redis-proxy支持转发到其他proxy（如codis、twemproxy），且支持以注册发现模式去发现后端proxy节点列表，具体见：[路由](/docs/redis-proxy/route.md)
* camellia-core支持异步双写（基于线程池+内存队列），进程内相同线程的多次写请求会保证顺序执行
* camellia-feign提供CamelliaNakedClient，用于支持自定义的调用（非标准feign客户端）
* camellia-redis-proxy支持BloomFilter相关的命令，具体见：[redis-proxy](/docs/redis-proxy/redis-proxy-zh.md)
* camellia-redis-proxy内置一个基于ip校验客户端的拦截器（IPCheckerCommandInterceptor），具体见：[拦截器](/docs/redis-proxy/interceptor.md)

### 更新
* DynamicValueGetter类从camellia-core包移动到camellia-tools包

### fix
* 无


# 1.0.55（2022/04/07）
### 新增
* 无

### 更新
* 无

### fix
* 修复camellia-feign熔断器过滤异常类型时的一个bug（没有把原始异常从InvocationTargetException中提取出来）


# 1.0.54（2022/04/07）
### 新增
* 新增CamelliaCircuitBreaker熔断器
* camellia-feign支持熔断（接入CamelliaCircuitBreaker），支持spring-boot-starter，支持动态配置，具体见：[camellia-feign](/docs/feign/feign.md)
* camellia-redis-proxy自定义ProxyRouteConfUpdater支持删除已有路由，具体见：[路由](/docs/redis-proxy/route.md)

### 更新
* 无

### fix
* 无


# 1.0.53（2022/03/24）
### 新增
* camellia-redis-proxy的console新增detect接口，从而可以把camellia-redis-proxy作为一个监控平台使用

### 更新
* 无

### fix
* camellia-redis-proxy使用info upstream-info命令获取后端redis集群信息时，当后端是redis-cluster时抛异常，v1.0.51时引入


# 1.0.52（2022/03/16）
### 新增
* 新增camellia-feign模块，feign支持动态路由，支持双写，支持动态调整超时时间等
* camellia-core新增CamelliaDiscovery/CamelliaDiscoveryFactory系列接口，统一camellia各模块下的discovery功能
* camellia-core新增ResourceTableUpdater/MultiResourceTableUpdater系列抽象类，统一camellia各模块下基于updater实现的动态路由功能

### 更新
* camellia-redis移除了ProxyDiscovery抽象类，统一使用IProxyDiscovery接口，并继承自CamelliaDiscovery接口
* camellia-id-gen移除了AbstractIdGenServerDiscovery抽象类，统一使用IdGenServerDiscovery接口，并继承自CamelliaDiscovery接口
* 所有模块的最低jdk依赖都升级到jdk8

### fix
* 无


# 1.0.51（2022/02/28）
### 新增
* 无

### 更新
* camellia-redis-proxy的info命令回包中，换行符从\n替换为\r\n，从而适配redis-shake进行redis数据迁移，具体见：[misc](/docs/redis-proxy/misc.md)

### fix
* ZkProxyRegistry在调用了deregister方法取消注册后，如果网络异常导致proxy到zk的tcp连接重连，可能会导致camellia-redis-proxy重新注册到zk
* camellia-dashboard和camellia-redis-proxy某些情况下在日志中打印了后端redis的密码，感谢[@chanjarster](https://github.com/chanjarster)修复该问题


# 1.0.50（2022/02/17）
### 新增
* camellia-redis新增CamelliaRedisLockManager，用于管理redis分布式锁的自动续约，具体见：[toolkit](/docs/redis-template/toolkit.md)
* camellia-redis新增CamelliaRedisTemplateManager，用于管理不同bid/bgroup的多组CamelliaRedisTemplate，具体见：[dynamic-dashboard](/docs/redis-template/dynamic-dashboard.md)
* camellia-tools新增CamelliaHashedExecutor，用于执行相同hashKey的runnable/callable任务时是相同线程执行

### 更新
* 无

### fix
* camellia-dashboard的deleteResourceTable接口，应该同步更新ResourceInfo的tid引用，感谢[@chanjarster](https://github.com/chanjarster)修复该bug


# 1.0.49（2022/01/19）
### 新增
* camellia-redis-proxy支持script load/flush/exists，具体见：[misc](/docs/redis-proxy/misc.md)
* camellia-redis-proxy支持eval_ro/evalsha_ro，需要后端是redis7.0+

### 更新
* camellia-redis-proxy监控后端redis响应耗时的监控数据支持密码脱敏

### fix
* scan在监控数据里应该是一个读命令而不是写命令，不影响功能，只是监控数据有误
* camellia-dashboard的getTableRefByBidGroup/deleteTableRef接口参数应该是bid而不是tid，感谢[@chanjarster](https://github.com/chanjarster)修复该bug


# 1.0.48（2022/01/17）
### 新增
* camellia-redis-proxy在自定义分片策略下支持scan命令
* CamelliaRedisTemplate新增getReadJedisList/getWriteJedisList接口
* CamelliaRedisTemplate新增executeRead/executeWrite接口

### 更新
* 无

### fix
* 无


# 1.0.47（2022/01/05）
### 新增
* CamelliaRedisTemplate新增getJedisList接口

### 更新
* 无

### fix
* 无


# 1.0.46（2021/12/29）
### 新增
* 新增CRC16HashTagShardingFunc类和DefaultHashTagShardingFunc类，用于自定义分片时可以支持hashtag，具体见：[路由](/docs/redis-proxy/route.md)

### 更新
* 重命名shading为sharding，具体见：[路由](/docs/redis-proxy/route.md)

### fix
* 无


# 1.0.45（2021/12/24）
### 新增
* camellia-redis-proxy的KafkaMqPackConsumer支持配置批量消费和重试，具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy提供DynamicCommandInterceptorWrapper用于动态组合多个拦截器，具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy支持不开启console（设置端口为0即可），具体见：[监控](/docs/redis-proxy/monitor.md)
* camellia-redis-proxy支持读redis-cluster的从节点，具体见：[路由](/docs/redis-proxy/route.md)
* camellia-redis-proxy支持代理到多个其他无状态的proxy节点，如codis-proxy、twemproxy等，具体见：[路由](/docs/redis-proxy/route.md)

### 更新
* camellia-id-gen调整了若干参数的默认值

### fix
* 无


# 1.0.44（2021/11/29）
### 新增
* camellia-redis-proxy新增KafkaMqPackProducerConsumer，proxy可以同时作为kafka的生产者和消费者，具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy支持监控后端redis的响应时间，具体见：[监控](/docs/redis-proxy/monitor.md)
* RedisProxyJedisPool支持jedis3，具体见：[部署](/docs/redis-proxy/deploy.md)

### 更新
* 调整代码结构，新建camellia-redis-proxy-plugins模块，camellia-redis-zk/camellia-redis-proxy-mq/camellia-redis-proxy-hbase平移到camellia-redis-proxy-plugins模块下
* camellia-redis-zk重命名为camellia-redis-proxy-discovery-zk，并归属于camellia-redis-proxy-discovery，相关类包名修改    
* RedisProxyJedisPool相关类包名修改，代码从camellia-redis移动到camellia-redis-proxy-discovery
* camellia-redis-proxy的info gc命令修改返回格式，从而支持zgc等垃圾回收器，具体见：[info](/docs/redis-proxy/info.md)

### fix
* 无


# 1.0.43（2021/11/23）
### 新增
* camellia-id-gen的segment和strict模式新增更新号段起始值的update接口，具体见：[id-gen](/docs/id-gen/id-gen.md)
* camellia-id-gen的segment和strict模式，regionId字段支持设置偏移量，具体见：[id-gen](/docs/id-gen/id-gen.md)
* camellia-id-gen的segment模式支持跨单元同步，具体见：[id-gen-segment](/docs/id-gen/segment.md)
* camellia-id-gen新增解析regionId、workerId等的接口，具体见：[id-gen](/docs/id-gen/id-gen.md)
* camellia-redis-proxy支持基于消息队列（kafka等）的异地双写，具体见：[拦截器](/docs/redis-proxy/interceptor.md)

### 更新
* camellia-redis-proxy的监控数据buffer增加最大size的限制以保护proxy
* camellia-redis-proxy的自定义ClientAuthProvider抛异常时关闭客户端连接

### fix
* 修复了camellia-id-gen-strict-spring-boot-starter的cache-key-prefix配置不生效的问题


# 1.0.42（2021/10/26）
### 新增
* camellia-redis-proxy的info命令修改redis-cluster集群安全性指标的含义，具体见：[info](/docs/redis-proxy/info.md)

### 更新
* 通过console-api获取慢查询/大key监控数据时支持设置监控数据量的上限，具体见：[monitor-data](/docs/redis-proxy/monitor-data.md)

### fix
* 无


# 1.0.41（2021/10/20）
### 新增
* camellia-redis-proxy的info命令修改redis-cluster集群安全性指标的含义，具体见：[info](/docs/redis-proxy/info.md)

### 更新
* 无

### fix
* 无


# 1.0.40（2021/10/19）
### 新增
* camellia-redis-proxy支持使用http-api执行info命令并获取相关信息，具体见：[info](/docs/redis-proxy/info.md)
* camellia-redis-proxy的info命令新增返回bid/bgroup级别的客户端连接数数据数据，具体见：[info](/docs/redis-proxy/info.md)

### 更新
* 无

### fix
* 无


# 1.0.39（2021/10/18）
### 新增
* camellia-redis-proxy支持配置客户端最大连接数（总连接数限制+bid/bgroup限制），默认不限制，具体见：[客户端连接控制](/docs/redis-proxy/connectlimit.md)
* camellia-redis-proxy支持配置检测空闲客户端连接并关闭，默认不启用，具体见：[客户端连接控制](/docs/redis-proxy/connectlimit.md)
* camellia-redis-proxy提供RateLimitCommandInterceptor，可以用于控制客户端请求速率（支持全局级别，也支持bid/bgroup级别），具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* 使用/monitor获取camellia-redis-proxy的大key监控数据时，支持配置返回json的大小，具体见：[监控数据](/docs/redis-proxy/monitor-data.md)
* camellia-redis-proxy开放更多netty参数配置，具体见：[netty-conf](/docs/redis-proxy/netty-conf.md)
* camellia-redis-proxy提供camellia-redis-proxy-nacos-spring-boot-starter用于使用nacos托管proxy配置，具体见：[nacos-conf](/docs/redis-proxy/nacos-conf.md)

### 更新
* 修改CommandInterceptor所属包名

### fix
* 无


# 1.0.38（2021/10/11）
### 新增
* 新增camellia-id-gen模块，支持：snowflake策略（支持设置单元标记）、基于数据库的id生成策略（支持设置单元标记，趋势递增）、基于数据库和redis的id生成策略（支持设置单元标记，严格递增），具体见：[id-gen](/docs/id-gen/id-gen.md)
* camellia-redis-proxy支持自定义callback通过spring的@Autowired来自动注入，具体见：[spring-autowire](/docs/redis-proxy/spring-autowire.md)

### 更新
* 移除了camellia-redis-toolkit模块，其中CamelliaCounterCache/CamelliaRedisLock合并到camellia-redis
* camellia-tools模块下包名重命名

### fix
* 无


# 1.0.37（2021/09/24）
### 新增
* camellia-redis-proxy配置的后端redis支持使用账号+密码登录，具体见：[route](/docs/redis-proxy/route.md)

### 更新
* info命令获取后端redis连接数时，如果某个后端连接数是0，则不返回
* 增强ProxyDynamicConfHook，可以拦截ProxyDynamicConf的所有动态配置
* 扩大监控/日志打印时隐藏密码功能的范围
* 优化了CommandDecoder

### fix
* 修复了后端redis连接数的监控可能不准的问题（不影响核心功能）


# 1.0.36（2021/09/06）
### 新增
* 新增camellia-tools模块，提供解压缩工具类CamelliaCompressor、加解密工具类CamelliaEncryptor、本地缓存工具类CamelliaLoadingCache，具体见：[tools](/docs/tools/tools.md)
* 新增了使用camellia-tools来实现camellia-redis-proxy数据解压缩、加解密的例子，具体见：[转换](/docs/redis-proxy/converter.md)
* camellia-redis-proxy支持自定义的ClientAuthProvider来实现通过password区分路由的方法，具体见：[路由配置](/docs/redis-proxy/route.md)，感谢[@yangxb2010000](https://github.com/yangxb2010000)提供该功能
* camellia-redis-proxy支持设置使用随机端口，具体见：[部署](/docs/redis-proxy/deploy.md)
* camellia-redis-proxy支持对key的自定义转换，从而你可以将单个redis集群划分成不同的命名空间（如添加不同的前缀），具体见：[转换](/docs/redis-proxy/converter.md)
* camellia-redis-proxy新增对RANDOMKEY命令的支持
* camellia-redis-proxy新增对HELLO命令的支持，不支持RESP3，但是支持通过HELLO命令setname和auth username password（如果客户端使用Lettuce6.x，则需要升级到本版本）
* camellia-redis-proxy代理到redis-cluster时支持scan命令，感谢[@yangxb2010000](https://github.com/yangxb2010000)提供该功能

### 更新
* camellia-redis-proxy的info命令返回新增http_console_port字段，具体见：[info](/docs/redis-proxy/info.md)
* camellia-redis-proxy的info命令返回新增redis_version字段，spring actuator默认会使用info命令返回的redis_version字段来做健康检查，这里直接返回一个固定的版本号，具体见：[info](/docs/redis-proxy/info.md)
* camellia-redis-proxy的info命令中Stats部分的字段重命名（改成下划线），如：avg.commands.qps改成avg_commands_qps，具体见：[info](/docs/redis-proxy/info.md)
* camellia-redis-proxy的info命令中Stats部分的qps字段取2位小数
* camellia-redis-proxy的auth/client/quit等命令的处理从ServerHandler迁移到CommandsTransponder  

### fix
* fix工具类KeyParser对EVAL/EVALSHA/XINFO/XGROUP/ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE命令的key的解析


# 1.0.35（2021/08/13）
### 新增
* camellia-redis-proxy支持对string/set/list/hash/zset相关命令的value自定义转换（可以用于透明的实现数据压缩、数据加解密等），具体见：[转换](/docs/redis-proxy/converter.md)
* camellia-redis-proxy新增对GETEX/GETDEL/HRANDFIELD/ZRANDMEMBER命令的支持
* camellia-redis-proxy的大key检测新增对GETDEL/GETEX命令的检测，新增对GETSET回包的检测

### 更新
* 无

### fix
* 修复camellia-redis-proxy阻塞型命令不可用的问题（1.0.33引入）


# 1.0.34（2021/08/05）
### 新增
* camellia-redis-proxy-hbase重构了string相关命令的冷热分离存储设计，具体见：[文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)
* CamelliaRedisTemplate提供Jedis适配器，修改一行代码从Jedis迁移到CamelliaRedisTemplate，具体见：[文档](/docs/redis-template/redis-template.md)
* CamelliaRedisTemplate提供SpringRedisTemplate适配器，具体见：[文档](/docs/redis-template/redis-template.md)
* camellia-redis-proxy提供一个不使用spring-boot-starter启动proxy的简单封装工具类CamelliaRedisProxyStarter，具体见：[文档](/docs/redis-proxy/redis-proxy-zh.md)

### 更新
* camellia-redis-proxy移除jedis的依赖

### fix
* 无


# 1.0.33（2021/07/29）
### 新增
* camellia-redis-proxy提供TroubleTrickKeysCommandInterceptor去避免异常key导致后端redis异常（比如业务层bug导致的死循环引起后端redis被打挂，需要临时屏蔽相关请求来保护后端redis），具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy提供MultiWriteCommandInterceptor用于自定义双写策略（比如有些key需要双写，有些key不需要，有些key双写到redisA，有些key双写到redisB），具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy支持DUMP/RESTORE命令
* CamelliaRedisTemplate支持DUMP/RESTORE命令

### 更新
* 无

### fix
* camellia-redis-proxy的BITPOS应该是读命令
* CamelliaRedisTemplate的BITPOS应该是读命令


# 1.0.32（2021/07/15）
### 新增
* camellia-redis-proxy-hbase新增对string/hash相关命令的冷热分离存储的支持，具体见：[文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)

### 更新
* 无

### fix
* 无


# 1.0.31（2021/07/05）
### 新增
* info命令支持section参数，且支持获取后端redis集群的信息（内存使用率、版本、主从分布情况、slot分布情况等），具体见：[监控](/docs/redis-proxy/monitor.md)

### 更新
* 无

### fix
* 修复先调用subscribe/psubscribe，再调用unsubscribe/punsubscribe之后，对应的后端redis连接没有释放的问题


# 1.0.30（2021/06/29）
### 新增
* 无

### 更新
* 初始化和动态更新路由配置时打印的日志也需要支持隐藏密码

### fix
* 修复打开慢查询/大key监控时，使用subscribe/psubscribe命令时，收到超过一定数量消息后的NPE问题（会导致不能收到后续的消息）
* 代理到redis-cluster时：subscribe/psubscribe支持在同一个长连接内多次订阅，并且unsubscribe/punsubscribe后，客户端连接可以用于普通命令（老版本proxy只能调用subscribe/psubscribe一次，并且调用后就不能unsubscribe/punsubscribe）


# 1.0.29（2021/06/25）
### 新增
* 无

### 更新
* 无

### fix
* 修复阻塞式命令偶现的not_available问题（1.0.27引入）

# 1.0.28（2021/06/25）
### 新增
* 新增info命令获取服务器相关信息，具体见：[监控](/docs/redis-proxy/monitor.md)
* 新增monitor-data-mask-password配置，用于隐藏日志和监控数据中的密码，具体见：[监控](/docs/redis-proxy/monitor.md)

### 更新
* 无

### fix
* 修复使用pipeline一次性提交多个阻塞型命令时可能导致not_available的问题（1.0.27引入）

# 1.0.27（2021/06/22）
### 新增
* 无

### 更新
* 无

### fix
* 修复使用阻塞型命令时如果单连接tps较高时导致到后端redis连接过多的问题

# 1.0.26（2021/05/27）
### 新增
* camellia-redis-proxy支持单独配置端口和applicationName（优先级高于spring的server.port/spring.application.name）
* ProxyDynamicConf支持直接把k-v的配置项map设置进去（之前只能从指定某个文件去读取）

### 更新
* camellia-redis-proxy重命名LoggingHoyKeyMonitorCallback为LoggingHotKeyMonitorCallback
* camellia-redis-proxy删除基于Disruptor/LinkedBlockingQueue的命令转发模式，仅保留直接转发的模式
* camellia-redis-proxy统计日志的logger名字变更（增加camellia.redis.proxy.前缀），如LoggingMonitorCallback.java
* camellia-redis-proxy重命名BigKeyMonitorCallback的回调方法，callbackUpstream/callbackDownstream变更为callbackRequest/callbackReply
* camellia-redis-proxy性能优化

### fix
* 无


# 1.0.25（2021/05/17）
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
* camellia-redis-proxy提供了抽象类AbstractSimpleShardingFunc用于自定义分片函数
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
[ENGLISH](update-en.md)

# 1.2.17（2023/10/10）
### 新增
* camellia-config/camellia-console支持自定义ConfigChangeInterceptor，用于控制配置变更的流程（如审批）
* camellia-redis-proxy增强plugin的功能，允许对单个命令选择自定义的路由
* camellia-redis-proxy内置HotKeyRouteRewriteProxyPlugin，允许对热key进行自定义的路由
* camellia-redis-proxy支持unix-domain-socket，client到proxy支持，proxy到redis也支持

### 更新
* camellia-redis-proxy配置连接上限时，如果达到上限，先返回一个error再关闭连接，且支持设置延迟关闭
* 优化CamelliaStrictIdGen的peekId接口的性能
* camellia-redis-proxy后向连接的sendbuf和rcvbuf的默认配置从10M调整为6M
* camellia-redis-proxy优化MultiWriteProxyPlugin的实现

### fix
* 无


# 1.2.16（2023/09/04）
### 新增
* 无

### 更新
* 无

### fix
* camellia-delay-queue，发送消息的ttl字段，含义应该是消息延迟到达后的存活时间，而不是消息发送后的存活时间，感谢 [fuhaodev](https://github.com/fuhaodev) 发现这个问题


# 1.2.15（2023/09/01）
### 新增
* camellia-redis-proxy支持`client info`和`client list`命令
* camellia-redis-proxy在使用haproxy等四层代理服务时，支持开启`proxy_protocol`去获取真实的客户端ip和端口

### 更新
* 重构FileUtil为FileUtils
* camellia-redis-proxy代理到redis-cluster时，如果ASK/MOVED超过次数上限，避免错误信息透传回客户端
* camellia-redis-proxy支持PKCS8的SSL/TLS证书，感谢 [HelloWorld1018](https://github.com/HelloWorld1018) 提供该功能

### fix
* camellia-redis-proxy的client到proxy开启tls时，双向认证不生效的问题，感谢 [@InputOutputZ](https://github.com/InputOutputZ) 发现这个bug
* 修复camellia-redis-proxy打成fatjar运行时部分配置文件读取失败的问题


# 1.2.14（2023/08/18）
### 新增
* camellia-redis-proxy支持client到proxy的连接开启tls，感谢 [HelloWorld1018](https://github.com/HelloWorld1018) 的测试和bugfix
* camellia-redis-proxy支持proxy到redis的连接开启tls，感谢 [HelloWorld1018](https://github.com/HelloWorld1018) 的测试和bugfix
* camellia-redis-proxy使用`info upstream-info`命令获取后端信息时，`redis-sentinel`也支持使用`sentinelUserName`和`sentinelPassword`
* camellia-id-gen新增CamelliaStrictIdGen2，基于redis/ntp时间戳实现严格递增序列
* camellia-redis-proxy支持本地配置文件为json格式

### 更新
* camellia-redis-proxy的console支持关闭，设置端口为0即可
* camellia-http-accelerate-proxy使用quic作为传输通道时，使用bbr作为默认拥塞控制算法（之前是配置缺省）
* 优化camellia-redis-proxy的预热逻辑，如果预热失败，则启动失败
* camellia-redis-proxy代理到redis-sentinel时增加定时获取master节点的逻辑，作为订阅master变更的兜底补充
* redis-sentinel/redis-proxies支持自适应刷新节点列表

### fix
* camellia-redis-proxy的console的随机端口功能无效的问题，1.2.11引入
* camellia-redis-proxy使用sentinelPassword时，打印的日志也需要隐藏密码
* camellia-redis-proxy当redis-cluster出现主从切换时，修复NPE（不影响切换）
* camellia-redis-proxy代理到sentinel时如果出现`+reset-master`时没有切换的问题，感谢 [segment11](https://github.com/segment11) 发现这个问题


# 1.2.13（2023/08/04）
### 新增
* camellia-http-accelerate-proxy支持设置backupServer
* camellia-redis-proxy集成nacos作为配置中心时，新增支持json格式，默认还是之前的properties格式
* camellia-redis-proxy支持集成etcd作为配置中心，支持json/properties格式，默认properties格式
* camellia-hot-key-server支持集成etcd作为配置中心，支持json/properties格式，默认json格式
* camellia-hot-key支持`not_contains`规则类型
* 新增MultiTenantProxyRouteConfUpdater和MultiTenantClientAuthProvider，新提供一种更简便的多租户配置方案
* camellia-htt-accelerate-proxy支持设置quic的congestion.control.algorithm
* camellia-redis-proxy multi-write-mode的配置从yml迁移到ProxyDynamicConf，支持租户级别配置，并且支持动态变更

### 更新
* `ProxyDynamicConfLoader` 重命名方法 `updateInitConf` 为 `init`
* `camellia-redis-proxy-nacos` 重命名 artifactId 为 `camellia-redis-proxy-config-nacos`
* `com.netease.nim.camellia.redis.proxy.nacos.NacosProxyDynamicConfLoader` 重命名为 `com.netease.nim.camellia.redis.proxy.config.nacos.NacosProxyDynamicConfLoader`

### fix
* camellia-redis-proxy代理到redis-cluster时，select 0不应该返回error，1.2.1引入该bug


# 1.2.12（2023/07/28）
### 新增
* camellia-http-accelerate-proxy，proxy和transport-server支持设置绑定的host（默认是0.0.0.0）
* camellia-http-accelerate-proxy，transport-route和upstream-route支持关闭
* camellia-redis3(CamelliaRedisTemplate)，支持设置auth账号（redis-standalone、redis-sentinel、redis-sentinel-slaves、redis-cluster）
* camellia-redis(CamelliaRedisTemplate)，redis-sentinel、redis-sentinel-slaves支持设置sentinelPassword
* camellia-redis3(CamelliaRedisTemplate)，redis-sentinel、redis-sentinel-slaves支持设置sentinelUserName和sentinelPassword
* camellia-redis-proxy的redis-sentinel、redis-sentinel-slaves支持设置sentinelUserName和sentinelPassword
* camellia-redis3(CamelliaRedisTemplate)，支持zmscore
* camellia-http-accelerate-proxy支持使用quic作为转发层协议
* camellia-http-accelerate-proxy支持对http-content进行压缩传输
* camellia-codec新增XProps工具类，某些情况下比Props更节省内存

### 更新
* camellia-hot-key-sdk使用ConcurrentHashMapCollector时如果key满了，打印的日志从error改成info
* 优化了camellia-http-console和camellia-http-accelerate-proxy响应包的connection头的逻辑
* 使用direct-buffer优化camellia-hot-key和camellia-http-accelerate-proxy的打包
* camellia-hot-key的rule支持后缀匹配

### fix
* 无


# 1.2.11（2023/07/19）
### 新增
* camellia-tools新增CamelliaSegmentStatistics工具类
* camellia-cache新增一个全局总开关
* 新增camellia-http-console模块，一个简单的http-server
* CamelliaRedisTemplate支持设置自定义RedisInterceptor，从而可以方便的接入CamelliaHotKeyMonitorSdk
* 新增camellia-codec模块
* 新增camellia-http-accelerate-proxy模块
* camellia-redis-proxy的GlobalRedisProxyEnv新增ProxyShutdown入口，用于释放端口和连接

### 更新
* camellia-redis-proxy使用camellia-http-console代替原来的console实现
* camellia-hot-key-server使用camellia-http-console代替原来的console实现
* camellia-hot-key使用camellia-codec模块
* camellia-hot-key-sdk支持设置collector类型，包括Caffeine（默认）、ConcurrentLinkedHashMap、ConcurrentHashMap
* camellia-hot-key-sdk支持设置为异步采集，默认为同步模式

### fix
* camellia-redis-proxy打印失败的command的error日志时，日志里的resource没有隐藏password
* camellia-hot-key-sdk获取的hot-key-config的checkThreshold有误（不影响功能）
* camellia-hot-key-server修复热key探测逻辑有误的问题


# 1.2.10（2023/06/07）
### 新增
* camellia-redis-proxy支持集成camellia-hot-key，感谢[@21want28k](https://github.com/21want28k) 提供该功能
* CamelliaHotKeyCacheSdk新增几个api，感谢[@21want28k](https://github.com/21want28k) 提供该功能

### 更新
* camellia-hot-key设置ConcurrentLinkedQueue作为默认内存队列，提高一些性能
* camellia-hot-key移除了HotKeyCounterManager中Caffeine的expire策略，避免引起性能劣化
* camellia-hot-key-server支持设置每个namespace下的Caffeine实例个数，从而在某些场景下突破Caffeine单实例的性能上限

### fix
* camellia-hot-key-server修复`unknown seqId`报错的问题
* CamelliaHotKeyCacheSdk修复namespace错误的问题，感谢[@21want28k](https://github.com/21want28k) 发现这个bug
* camellia-redis-proxy-discovery-zk的1.2.8/1.2.9与1.2.7以及之前的低版本有不兼容的问题，1.2.10增加了兼容性的逻辑


# 1.2.9（2023/06/02）
### 新增
* camellia-redis-proxy在后端redis失败的情况下，支持把redis地址、command命令和关联keys打印到日志文件中

### 更新
* camellia-redis-proxy、camellia-delay-queue-server、camellia-id-gen-server新增online/offline的callback
* ZkProxyRegistry/ZkHotKeyServerRegistry注册online/offline的callback
* CamelliaHashedExecutor新增hashIndex方法，用于获取hashKey计算得到的线程index

### fix
* CamelliaHotKeyCacheSdkConfig移除namespace字段，CamelliaHotKeyCacheSdk的namespace字段应该来自方法传参
* camellia-hot-key-server优雅上下线时没有判断是否有流量
* CamelliaHotKeyCacheSdk修复keyDelete/keyUpdate方法产生的通知没有发送到其他客户端的问题
* TopNCounter每个大周期计算完成后没有清空buffer的问题
* TopNCounter修复计算maxQps不对的问题


# 1.2.8（2023/05/29）
### 新增
* 新增camellia-hot-key模块，具体见：[hot-key](/docs/hot-key/hot-key.md)
* 新增camellia-zk模块，camellia-redis-proxy-zk和camellia-hot-key-zk均引用camellia-zk，从而复用代码

### 更新
* camellia-redis-proxy面向client的连接，tcp_keepalive参数默认改成true
* camellia-config的namespace的info字段，mysql存储字段从varchar改成text

### fix
* camellia-redis-proxy的`/prometheus`接口的换行符，从`%n`改成`\n`，从而适配windows环境
* camellia-redis-proxy在客户端连接处于发布订阅状态时，如果后端redis宕机或者因为某些原因关闭了redis到proxy的连接，proxy需要同步关闭到client的连接
* camellia-redis-proxy修复连接处于订阅状态时，频繁发送ping命令或者频繁sub/unsub后，导致订阅无效的问题
* camellia-redis-proxy在由于后端redis不可用导致客户端订阅失败的情况下，强制关闭客户端连接


# 1.2.7（2023/05/04）
### 新增
* 无

### 更新
* 无

### fix
* 修复了CamelliaRedisLockManager在并发场景下，可能导致自动renew任务泄露的问题（逻辑正确，但是引起cpu多余开销），影响camellia-delay-queue-server
* 修复了camellia-redis-proxy中RedisConnection下心跳和idle检测定时任务，在使用了事务命令/发布订阅命令时，某些情况下，在连接已经关闭的情况下可能空跑的问题


# 1.2.6（2023/04/28）
### 新增
* camellia-redis-proxy支持对TRANSACTION命令的双写，具体见：[multi-write](/docs/redis-proxy/other/multi-write.md)
* camellia-tools新增CamelliaScheduleExecutor工具类
* RateLimitProxyPlugin支持对租户级别设置默认频控，具体见：[rate-limit](/docs/redis-proxy/plugin/rate-limit.md)

### 更新
* camellia-redis-proxy支持复用CommandPack，优化gc
* camellia-config配置key服务器增加trim逻辑
* camellia-config调整了`/getConfigString`接口的返回
* CamelliaLoadingCache在缓存穿透时增加最大执行时间控制
* camellia-redis-proxy细化了后端redis异常时返回给客户端的错误描述信息
* camellia-redis-proxy在伪redis-cluster模式下，`cluster nodes`命令返回的换行符应该是`\n`，而不是`\r\n`
* CamelliaRedisLockManager底层使用CamelliaScheduleExecutor代替ScheduledExecutorService
* camellia-redis-proxy的RedisConnection底层使用CamelliaScheduleExecutor代替ScheduledExecutorService执行idle检测和心跳检测
* camellia-redis-proxy优化了伪redis-cluster模式下心跳逻辑

### fix
* 修复camellia-config相关接口sql错误的问题
* camellia-redis-proxy在连接处于TRANSACTION或者SUBSCRIBE状态时，ping命令应该透给后端而不是直接返回
* camellia-redis-proxy修复了连接在SUBSCRIBE和normal状态间频繁切换后，普通命令没有响应的问题
* camellia-redis-proxy修复了连接在从SUBSCRIBE转变为normal后，又使用阻塞型命令后命令没有响应的问题
* camellia-redis-proxy修复了代理到redis-cluster时，TRANSACTION命令包裹的普通命令的key的slot计算为0时导致事务逻辑错误的问题


# 1.2.5（2023/04/07）
### 新增
* 无

### 更新
* camellia-redis-proxy内置内存队列支持使用jctools的高性能队列，从而优化性能
* camellia-redis-proxy伪redis-cluster模式下，proxy集群扩缩容时，优化MOVED指令逻辑

### fix
* camellia-redis-proxy代理redis-cluster时，优化了renew的逻辑（1.2.0引入，导致redis节点宕机后刷新路由表不及时），感谢[@saitama-24](https://github.com/saitama-24) 发现这个问题


# 1.2.4（2023/04/03）
### 新增
* 新增camellia-config模块，一个简单的kv配置中心，具体见：[camellia-config](/docs/config/config.md)
* camellia-redis-proxy新增NacosProxyDynamicConfLoader，一种新的集成nacos的方法，具体见：[dynamic-conf](/docs/redis-proxy/other/dynamic-conf.md)
* camellia-redis-proxy中内建的ProxyPlugin支持自定义执行顺序（order），具体见：[plugin](/docs/redis-proxy/plugin/plugin.md)

### 更新
* camellia-redis-proxy优化了RedisConnection的实现
* camellia-redis-proxy支持接入camellia-config
* camellia-feign支持接入camellia-config
* camellia-redis-proxy中的PUBSUB系列命令的响应也需要统计到upstream-fail里面
* camellia-redis-proxy-hbase的内存队列支持动态调整容量
* camellia-delay-queue-server定时任务增加一个单机并发控制，优化资源使用
* 优化了IPMatcher的实现，从而可以处理`10.22.23.1/24`的判断

### fix
* 修复camellia-redis-proxy使用custom自定义路由模式时，多读场景下的自动剔除异常后端功能不生效的问题
* 修复camellia-redis-proxy同时使用converterPlugin的key转换功能和hotKeyCachePlugin时，热key缓存功能不生效的问题


# 1.2.3（2023/03/15）
### 新增
* camellia-redis-proxy支持根据后端resource统计请求失败的情况，具体见：[monitor-data](/docs/redis-proxy/monitor/monitor-data.md)

### 更新
* camellia-redis-proxy细化了后端redis异常时返回给客户端的错误描述信息
* camellia-redis-proxy的/prometheus端点调整了部分metrics的type
* camellia-redis-proxy优化了RedisConnection的状态判断的实现逻辑

### fix
* 修复camellia-redis-proxy使用info upstream-info命令获取到的后端redis地址的密码没有mask的问题


# 1.2.2（2023/02/28）
### 新增
* 无

### 更新
* 重构了ProxyDynamicConf，支持自定义loader

### fix
* 修复RedisConnection心跳异常后没有关闭连接(1.2.0引入)


# 1.2.1（2023/02/22）
### 新增
* redis-proxies和redis-proxies-discovery两种redis-resource支持设置db，包括camellia-redis-proxy和CamelliaRedisTemplate
* camellia-redis-proxy支持select命令，当前仅当后端是redis-standalone/redis-sentinel/redis-proxies或者其分片/读写分离等的组合时支持设置非0的db，如果后端有redis-cluster类型的resource，则只支持select 0
* CamelliaRedisTemplate支持RedisProxiesDiscoveryResource这种资源

### 更新
* camellia-redis-proxy-hbase支持配置upstream.redis.hbase.command.execute.concurrent.enable（默认false），从而提高客户端使用pipeline批量提交命令时的执行效率，但是要求客户端是阻塞性的提交方式，否则可能导致命令乱序执行
* 重命名DefaultTenancyNamespaceKeyConverter -> DefaultMultiTenantNamespaceKeyConverter

### fix
* 修复了使用jedis3+SpringRedisTemplate+zk/eureka接入proxy的相关jar包的依赖错误问题（打包时使用了jedis2打包导致类找不到）
* 修复了使用CamelliaRedisProxyZkFactory+CamelliaRedisTemplate接入camellia-redis-proxy时，bid/bgroup不生效的问题
* CamelliaRedisTemplate里这个pipeline方法应该是读方法而不是写方法：Response<Long> zcard(String key)


# 1.2.0（2023/02/14）
### 新增
* 新增camellia-redis3模块以及相关模块，支持jedis3.x（默认使用v3.6.3)
* 把camellia-redis-client和camellia-redis-proxy的公共部分组成camellia-redis-base模块，从而camellia-redis-proxy不再依赖camellia-redis-client
* camellia-redis-proxy支持自定义upstream模块，核心接口：IUpstreamClientTemplate和IUpstreamClientTemplateFactory
* camellia-redis-proxy-hbase冷热分离存储，从自定义CommandInvoker改成自定义upstream，从而有更多代码复用，并且修改了线程模型（新增了工作线程池和netty-worker线程池隔离）
* camellia-tools新增CamelliaLinearInitializationExecutor，支持资源的异步线性初始化
* camellia-redis-proxy引入CamelliaLinearInitializationExecutor，对多租户upstream初始化逻辑进行了重构
* camellia-redis-proxy在多读场景下，支持对后端进行健康检查，自动剔除故障节点
* camellia-redis-proxy支持异步初始化到后端redis的连接
* camellia-redis-proxy支持监控秒级的qps
* camellia-redis-proxy代理redis-cluster时除了MOVED/disconnect触发renew外，新增兜底的定时renew，默认600s一次
* camellia-hbase支持url中设置userName和password以及aliyun-lindorm的标记
* camellia-redis-proxy优化了redis-cluster-slaves和redis-sentinel-slaves两种resource在有节点宕机下的failover逻辑
* camellia-redis-proxy优化了redis-proxies和redis-proxies-discovery两种resource在有节点宕机下的failover逻辑
* camellia-redis-proxy支持在application.yml中配置dynamic.conf.file.name来代替camellia-redis-proxy.properties文件

### 更新
* camellia-redis-proxy相关核心类进行了重命名（upstream部分）
* camellia-redis移除了CamelliaRedisTemplate到SpringRedisTemplate的适配器
* camellia-redis移除了CamelliaRedisTemplate到Jedis的适配器
* 新增camellia-redis-toolkit模块，把camellia-redis中的toolkit相关功能（如分布式锁等）独立出来，从而可以被camellia-redis3复用
* 使用按照包启动(redis-proxy、delay-queue、id-gen-server)时，新增camellia的banner

### fix
* camellia-redis-proxy修复了使用ProxyDynamicConf#reload(Map)方法直接设置自定义变量时（而非基于camellia-redis-proxy.properties文件），配置被清空的问题，1.1.8引入该问题


# 1.1.14（2023/02/01）(1.1.13相关jar包在maven中央仓库已损坏，因此换一个版本号重新deploy)
### 新增
* camellia-redis-proxy支持使用transport_native_epoll、transport_native_kqueue、transport_native_io_uring，默认使用jdk_nio，具体见：[netty-conf](/docs/redis-proxy/other/netty-conf.md)
* camellia-redis-proxy支持配置TCP_QUICKACK参数，当前仅当使用transport_native_epoll时支持，感谢[@tain198127](https://github.com/tain198127) ，具体见：[netty-conf](/docs/redis-proxy/other/netty-conf.md) ，关联issue: [issue-87](https://github.com/netease-im/camellia/issues/87)
* RedisProxyJedisPool新增AffinityProxySelector，支持亲和性配置，感谢[@tain198127](https://github.com/tain198127) 提供该功能

### 更新
* id-gen-sdk底层线程池默认使用共享模式，减少初始化多个sdk实例时的线程数量占用
* delay-queue-sdk底层线程池默认使用共享模式，减少初始化多个sdk实例时的线程数量占用
* RedisProxyJedisPool底层线程池默认使用共享模式，减少初始化多个实例时的线程数量占用
* id-gen-server新增bootstrap模块，提供直接运行的安装包
* delay-queue-server新增bootstrap模块，提供直接运行的安装包

### fix
* fix了CamelliaStatistics在count=0时计算avg报错的问题


# 1.1.12（2023/01/12）
### 新增
* 无

### 更新
* 回滚了1.1.8中对于BulkReply的堆外内存优化（当客户端连接在收到reply之前断开连接，可能导致BulkReply中的ByteBuf没有release，引起内存泄漏）

### fix
* 修复camellia-redis-proxy，当客户端连接在收到reply之前断开连接，可能导致BulkReply中的ByteBuf没有release，1.1.8引入


# 1.1.11（2023/01/10）
### 新增
* camellia-redis-proxy新增对prometheus/grafana的支持，感谢[@tasszz2k](https://github.com/tasszz2k) ，具体见：[prometheus-grafana](/docs/redis-proxy/monitor/prometheus-grafana.md)
* camellia-tools新增CamelliaDynamicExecutor和CamelliaDynamicIsolationExecutor工具类，以及线程池监控工具CamelliaExecutorMonitor

### 更新
* camellia-core包下面部分utils类，迁移到camellia-tools
* camellia-redis-proxy优化了默认提供的DefaultTenancyNamespaceKeyConverter在包含hashtag的key的行为，从而兼容更多场景, 感谢[@phuc1998](https://github.com/phuc1998) 和 [@tasszz2k](https://github.com/tasszz2k) 

### fix
* 修复camellia-redis-proxy在使用TRANSACTION系列命令时，如果有较高的客户端qps，导致的后端redis连接泄漏的问题, 感谢[@phuc1998](https://github.com/phuc1998) 和 [@tasszz2k](https://github.com/tasszz2k) 发现了这个bug 
* 修复camellia-redis-proxy使用PUBSUB系列命令时一个reply编解码的并发问题，1.1.8引入


# 1.1.10（2023/01/03）
### 新增
* camellia-redis-proxy提供了DynamicRateLimitProxyPlugin，支持使用camellia-dashboard进行动态配置，感谢[@tasszz2k](https://github.com/tasszz2k)

### 更新
* 调整了项目maven结构
* 重命名了camellia-redis-proxy的artifactId为camellia-redis-proxy-core，camellia-redis-proxy变成目录

### fix
* 修复CamelliaRedisTemplate使用RedisResource时select db不生效的问题 
* 修复了camellia-delay-queue的中文乱码问题，感谢[@ax3353](https://github.com/ax3353)


# 1.1.9（2022/12/21）
### 新增
* 新增camellia-cache模块，增强spring-cache，具体见：[cache](/docs/cache/cache.md)
* camellia-redis-proxy新增对LMPOP和BLMPOP命令的支持（redis7.0）

### 更新
* 优化camellia-id-gen-sdk，id-gen-server的任何异常（不只是网络异常）都要触发节点屏蔽和请求重试

### fix
* camellia-delay-queue的getMsg接口，在消息已经被消费，且消息还在缓存中的情况下，获取的结果是200，但是没有消息内容


# 1.1.8（2022/12/13）
### 新增
* camellia-redis-proxy支持通过application.yml配置ProxyDynamicConf中自定义的k-v配置项（优先级低于camellia-redis-proxy.properties）
* camellia-redis-proxy提供了DefaultTenancyNamespaceKeyConverter，可以基于租户（bid/bgroup）进行key的命名空间隔离
* camellia-redis-proxy新增对ZMPOP和BZMPOP命令的支持（redis7.0）

### 更新
* camellia-redis-proxy-samples中移除zk和nacos的依赖（如果需要，自行添加相关依赖即可）
* camellia-redis-proxy使用ConverterProxyPlugin和KeyConverter进行key命名空间隔离时，SCAN命令不带MATCH字段时也需要执行KeyConverter#convert
* camellia-redis-proxy使用堆外内存优化了BulkReply的编解码性能
* camellia-redis-proxy支持全局配置租户级别的连接数上限，感谢[@tasszz2k](https://github.com/tasszz2k)

### fix
* fix camellia-delay-queue使用长轮询时，运行一段时间后长轮询失效的问题（没有hold住连接）
* fix camellia-redis-proxy当使用ConverterProxyPlugin进行key转换时，没有处理TairZSet的EXBZPOPMAX/EXBZPOPMIN的回包中的key


# 1.1.7（2022/11/30）
### 新增
* camellia-redis-proxy新增对ZINTERCARD命令的支持
* camellia-redis-proxy新增对TairZSet、TairHash、TairString的支持
* camellia-redis-proxy新增对RedisJSON的支持
* camellia-redis-proxy新增对RedisSearch的支持
* camellia-redis-proxy代理到redis-standalone/redis-sentinel时，支持设置后端的db
* CamelliaRedisTemplate请求redis-standalone/redis-sentinel时，支持设置db

### 更新
* 调整了camellia-dashboard配置camellia-redis-proxy的ip黑白名单的配置接口，感谢[@tasszz2k](https://github.com/tasszz2k)

### fix
* 无


# 1.1.6（2022/11/23）
### 新增
* 无

### 更新
* camellia-redis-proxy优化了统计模块的实现，优化内存和GC

### fix
* 无


# 1.1.5（2022/11/21）
### 新增
* CamelliaStatistic工具类支持统计分位数（p50、p75、p90、p90、p95、p99、p999）
* camellia-redis-proxy的耗时监控支持统计分位数（p50、p75、p90、p90、p95、p99、p999），具体见：[monitor-data](/docs/redis-proxy/monitor/monitor-data.md)
* 提供FileBasedCamelliaApi，支持使用本地properties配置文件模拟camellia-dashboard
* camellia-feign支持使用本地配置文件提供动态参数的能力（如超时、熔断、路由等参数）
* camellia-core双写/分片执行线程池支持设置拒绝策略

### 更新
* camellia-feign初始化时，如果依赖的后端服务整体宕机了，当前服务不是报错而是打印warn日志
* camellia-feign使用动态路由时，如果远端（camellia-dashboard）返回404，则使用本地路由代替，而不是报错  
* camellia-feign在设置双写线程池设置为Abort策略后，如果触发RejectedExecutionException，也回调CamelliaFeignFailureListener

### fix
* 无


# 1.1.4（2022/11/08）
### 新增
* 访问camellia-dashboard的相关API时支持自定义header头，感谢[@tasszz2k](https://github.com/tasszz2k)提供该功能
* camellia-redis-proxy在配置双写时也支持PUBSUB系列命令
* 新增任务合并工具类，具体见：[toolkit](/docs/redis-client/toolkit.md)

### 更新
* camellia-redis-proxy重构并优化了ReplyDecoder的实现和性能

### fix
* 修复camellia-delay-queue使用长轮询时线程池泄漏的问题


# 1.1.3（2022/10/24）
### 新增
* CamelliaRedisTemplate支持RedisProxiesResource这种redis资源配置
* camellia-redis-proxy新增CommandDisableProxyPlugin，可以限制某些命令在proxy上的访问

### 更新
* camellia-delay-queue，deleteMsg支持立即释放redis内存（默认false）
* 优化了camellia-redis-proxy代理redis-cluster时的renew策略，优先使用地址串中的ip:port，其次使用master节点，再次使用slave节点，并且加一个随机

### fix
* camellia-delay-queue，当消息被消费或者删除后，此时相同的msgId的消息重新发送会被去重
* 修复proxy配置密码时，redis-benchmark无法工作的问题（v1.1.0引入），根因：当auth和其他命令通过pipeline一起提交时，proxy没有正确处理

# 1.1.2（2022/10/12）
### 新增
* CamelliaRedisProxyStarter支持启动console-server
* RedisProxyRedisConnectionFactory实现DisposableBean接口，支持destroy方法
* camellia-redis-proxy新增cluster-mode，从而可以把proxy集群伪装成redis-cluster
* camellia-id-gen-sdk新增DelayQueueServerDiscoveryFactory方便管理多个基于注册发现的delay-queue-server集群
* camellia-redis-proxy支持COMMAND命令，透传给后端

### 更新
* 自定义监控回调（MonitorCallback、SlowCommandMonitorCallback、HotKeyCacheStatsCallback、HotKeyMonitorCallback、BigKeyMonitorCallback）走独立的线程池执行回调，避免不合理的自定义监控回调实现阻塞proxy主流程

### fix
* camellia-redis-proxy随机端口功能没有校验端口是否被占用
* 修复camellia-redis-proxy中SpringProxyBeanFactory不生效的问题

# 1.1.1（2022/09/26）
### 新增
* camellia-redis-proxy代理redis-cluster时也支持TRANSACTION系列命令（MULTI/EXEC/DISCARD/WATCH/UNWATCH）

### 更新
* 优化了AsyncCamelliaRedisTemplate和AsyncCamelliaRedisClusterClient的代码实现
* 调整了内建ProxyPlugin的默认顺序（热key监控修改为优先于热key缓存监控）

### fix
* 无


# 1.1.0（2022/09/21）
### 新增
* 重构camellia-redis-proxy插件和监控体系，相关功能统一为新的框架下面，具体见：[redis-proxy](/docs/redis-proxy/redis-proxy-zh.md)

### 更新
* 无

### fix
* 无


# 1.0.61（2022/09/06）
### 新增
* camellia-delay-queue支持使用长轮询接口消费延迟消息，具体见：[delay-queue](/docs/delay-queue/delay-queue.md)
* 新增camellia-console模块，用于管理多组camellia-dashboard集群，感谢[@HongliangChen-963](https://github.com/HongliangChen-963)提供该模块
* 新增CamelliaStatisticsManager用于管理多个CamelliaStatistics对象

### 更新
* camellia-redis-proxy优化了AsyncCamelliaRedisTemplate的初始化逻辑

### fix
* fix了camellia-redis-proxy使用分片或者代理到redis-cluster时执行ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE命令失败的问题
* fix了camellia-feign中，当后端服务异常的情况下，调用方进程启动时，由于DiscoveryResourcePool初始化失败产生的内存泄漏问题


# 1.0.60（2022/08/16）
### 新增
* 新增camellia-delay-queue模块，可以用于实现延迟队列功能，具体见：[delay-queue](/docs/delay-queue/delay-queue.md)
* camellia-feign新增failureListener，包括CamelliaNakedClient和CamelliaFeignClient都支持，可以用于监控，也可以用于失败重试
* camellia-tools新增CamelliaStatistics工具类，可以用于计数、求和、平均值、最大值等的统计
* camellia-redis新增CamelliaFreq工具类，可以用于频控，包括单机频控和集群频控
* camellia-redis-proxy在动态路由刷新时，生效之前增加对新路由的检查
* camellia-redis-proxy新增路由时，资源初始化改成异步，提升多租户间的隔离能力

### 更新
* CamelliaRedisTemplate初始化redis-cluster时增加可用性判断（jedis/v2.9.3没有这个判断，新版本jedis有这个）
* 重命名NacosProxyDamicConfSupport为NacosProxyDynamicConfSupport
* CamelliaRedisTemplate执行eval/evalsha命令时使用超时配置（jedis2.9.3会移除超时，高版本jedis不会，和高版本保持一致）

### fix
* fix了camellia-dashboard中FeignChecker没有生效的问题（缺失了@Component注解）
* fix了RedisProxyJedisPool的SideCarFirstProxySelector下线proxy失败的问题

# 1.0.59（2022/06/21）
### 新增
* camellia-core、camellia-feign调整异步双写线程模型，并新增支持MISC_ASYNC_MULTI_THREAD模式
* camellia-redis-proxy支持缓存透明双删，具体见：[interceptor](/docs/redis-proxy/interceptor.md)
* camellia-dashboard新增几个管理api

### 更新
* CamelliaHashedExecutor支持获取完成任务数
* 调整了ProxyConstants的默认参数，调大了双写和分片使用的内部线程池的默认线程数
* camellia-redis-proxy在统计后端redis响应时间时，跳过发布订阅命令和阻塞型命令
* 升级fastjson版本，从1.2.76升级到1.2.83

### fix
* 修复了后端redis有密码且开启密码mask的情况下，监控的后端redis响应时间为0的问题


# 1.0.58（2022/05/16）
### 新增
* camellia-redis-proxy的detect接口支持返回key总数/qps等信息，具体见：[detect](/docs/redis-proxy/monitor/detect.md)

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
* camellia-redis-proxy支持转发到其他proxy（如codis、twemproxy），且支持以注册发现模式去发现后端proxy节点列表，具体见：[路由](/docs/redis-proxy/auth/route.md)
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
* camellia-redis-proxy自定义ProxyRouteConfUpdater支持删除已有路由，具体见：[路由](/docs/redis-proxy/auth/route.md)

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
* camellia-redis-proxy的info命令回包中，换行符从\n替换为\r\n，从而适配redis-shake进行redis数据迁移，具体见：[misc](/docs/redis-proxy/other/misc.md)

### fix
* ZkProxyRegistry在调用了deregister方法取消注册后，如果网络异常导致proxy到zk的tcp连接重连，可能会导致camellia-redis-proxy重新注册到zk
* camellia-dashboard和camellia-redis-proxy某些情况下在日志中打印了后端redis的密码，感谢[@chanjarster](https://github.com/chanjarster)修复该问题


# 1.0.50（2022/02/17）
### 新增
* camellia-redis新增CamelliaRedisLockManager，用于管理redis分布式锁的自动续约，具体见：[toolkit](/docs/redis-client/toolkit.md)
* camellia-redis新增CamelliaRedisTemplateManager，用于管理不同bid/bgroup的多组CamelliaRedisTemplate，具体见：[dynamic-dashboard](/docs/redis-client/dynamic-dashboard.md)
* camellia-tools新增CamelliaHashedExecutor，用于执行相同hashKey的runnable/callable任务时是相同线程执行

### 更新
* 无

### fix
* camellia-dashboard的deleteResourceTable接口，应该同步更新ResourceInfo的tid引用，感谢[@chanjarster](https://github.com/chanjarster)修复该bug


# 1.0.49（2022/01/19）
### 新增
* camellia-redis-proxy支持script load/flush/exists，具体见：[misc](/docs/redis-proxy/other/misc.md)
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
* 新增CRC16HashTagShardingFunc类和DefaultHashTagShardingFunc类，用于自定义分片时可以支持hashtag，具体见：[路由](/docs/redis-proxy/auth/route.md)

### 更新
* 重命名shading为sharding，具体见：[路由](/docs/redis-proxy/auth/route.md)

### fix
* 无


# 1.0.45（2021/12/24）
### 新增
* camellia-redis-proxy的KafkaMqPackConsumer支持配置批量消费和重试，具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy提供DynamicCommandInterceptorWrapper用于动态组合多个拦截器，具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy支持不开启console（设置端口为0即可），具体见：[监控](/docs/redis-proxy/monitor/monitor.md)
* camellia-redis-proxy支持读redis-cluster的从节点，具体见：[路由](/docs/redis-proxy/auth/route.md)
* camellia-redis-proxy支持代理到多个其他无状态的proxy节点，如codis-proxy、twemproxy等，具体见：[路由](/docs/redis-proxy/auth/route.md)

### 更新
* camellia-id-gen调整了若干参数的默认值

### fix
* 无


# 1.0.44（2021/11/29）
### 新增
* camellia-redis-proxy新增KafkaMqPackProducerConsumer，proxy可以同时作为kafka的生产者和消费者，具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* camellia-redis-proxy支持监控后端redis的响应时间，具体见：[监控](/docs/redis-proxy/monitor/monitor.md)
* RedisProxyJedisPool支持jedis3，具体见：[部署](/docs/redis-proxy/deploy/deploy.md)

### 更新
* 调整代码结构，新建camellia-redis-proxy-plugins模块，camellia-redis-zk/camellia-redis-proxy-mq/camellia-redis-proxy-hbase平移到camellia-redis-proxy-plugins模块下
* camellia-redis-zk重命名为camellia-redis-proxy-discovery-zk，并归属于camellia-redis-proxy-discovery，相关类包名修改    
* RedisProxyJedisPool相关类包名修改，代码从camellia-redis移动到camellia-redis-proxy-discovery
* camellia-redis-proxy的info gc命令修改返回格式，从而支持zgc等垃圾回收器，具体见：[info](/docs/redis-proxy/monitor/info.md)

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
* camellia-redis-proxy的info命令修改redis-cluster集群安全性指标的含义，具体见：[info](/docs/redis-proxy/monitor/info.md)

### 更新
* 通过console-api获取慢查询/大key监控数据时支持设置监控数据量的上限，具体见：[monitor-data](/docs/redis-proxy/monitor/monitor-data.md)

### fix
* 无


# 1.0.41（2021/10/20）
### 新增
* camellia-redis-proxy的info命令修改redis-cluster集群安全性指标的含义，具体见：[info](/docs/redis-proxy/monitor/info.md)

### 更新
* 无

### fix
* 无


# 1.0.40（2021/10/19）
### 新增
* camellia-redis-proxy支持使用http-api执行info命令并获取相关信息，具体见：[info](/docs/redis-proxy/monitor/info.md)
* camellia-redis-proxy的info命令新增返回bid/bgroup级别的客户端连接数数据数据，具体见：[info](/docs/redis-proxy/monitor/info.md)

### 更新
* 无

### fix
* 无


# 1.0.39（2021/10/18）
### 新增
* camellia-redis-proxy支持配置客户端最大连接数（总连接数限制+bid/bgroup限制），默认不限制，具体见：[客户端连接控制](/docs/redis-proxy/other/connectlimit.md)
* camellia-redis-proxy支持配置检测空闲客户端连接并关闭，默认不启用，具体见：[客户端连接控制](/docs/redis-proxy/other/connectlimit.md)
* camellia-redis-proxy提供RateLimitCommandInterceptor，可以用于控制客户端请求速率（支持全局级别，也支持bid/bgroup级别），具体见：[拦截器](/docs/redis-proxy/interceptor.md)
* 使用/monitor获取camellia-redis-proxy的大key监控数据时，支持配置返回json的大小，具体见：[监控数据](/docs/redis-proxy/monitor/monitor-data.md)
* camellia-redis-proxy开放更多netty参数配置，具体见：[netty-conf](/docs/redis-proxy/other/netty-conf.md)
* camellia-redis-proxy提供camellia-redis-proxy-nacos-spring-boot-starter用于使用nacos托管proxy配置，具体见：[nacos-conf](/docs/redis-proxy/other/nacos-conf.md)

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
* camellia-redis-proxy配置的后端redis支持使用账号+密码登录，具体见：[route](/docs/redis-proxy/auth/route.md)

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
* 新增了使用camellia-tools来实现camellia-redis-proxy数据解压缩、加解密的例子，具体见：[转换](/docs/redis-proxy/plugin/converter2.md)
* camellia-redis-proxy支持自定义的ClientAuthProvider来实现通过password区分路由的方法，具体见：[路由配置](/docs/redis-proxy/auth/route.md)，感谢[@yangxb2010000](https://github.com/yangxb2010000)提供该功能
* camellia-redis-proxy支持设置使用随机端口，具体见：[部署](/docs/redis-proxy/deploy/deploy.md)
* camellia-redis-proxy支持对key的自定义转换，从而你可以将单个redis集群划分成不同的命名空间（如添加不同的前缀），具体见：[转换](/docs/redis-proxy/plugin/converter2.md)
* camellia-redis-proxy新增对RANDOMKEY命令的支持
* camellia-redis-proxy新增对HELLO命令的支持，不支持RESP3，但是支持通过HELLO命令setname和auth username password（如果客户端使用Lettuce6.x，则需要升级到本版本）
* camellia-redis-proxy代理到redis-cluster时支持scan命令，感谢[@yangxb2010000](https://github.com/yangxb2010000)提供该功能

### 更新
* camellia-redis-proxy的info命令返回新增http_console_port字段，具体见：[info](/docs/redis-proxy/monitor/info.md)
* camellia-redis-proxy的info命令返回新增redis_version字段，spring actuator默认会使用info命令返回的redis_version字段来做健康检查，这里直接返回一个固定的版本号，具体见：[info](/docs/redis-proxy/monitor/info.md)
* camellia-redis-proxy的info命令中Stats部分的字段重命名（改成下划线），如：avg.commands.qps改成avg_commands_qps，具体见：[info](/docs/redis-proxy/monitor/info.md)
* camellia-redis-proxy的info命令中Stats部分的qps字段取2位小数
* camellia-redis-proxy的auth/client/quit等命令的处理从ServerHandler迁移到CommandsTransponder  

### fix
* fix工具类KeyParser对EVAL/EVALSHA/XINFO/XGROUP/ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE命令的key的解析


# 1.0.35（2021/08/13）
### 新增
* camellia-redis-proxy支持对string/set/list/hash/zset相关命令的value自定义转换（可以用于透明的实现数据压缩、数据加解密等），具体见：[转换](/docs/redis-proxy/plugin/converter2.md)
* camellia-redis-proxy新增对GETEX/GETDEL/HRANDFIELD/ZRANDMEMBER命令的支持
* camellia-redis-proxy的大key检测新增对GETDEL/GETEX命令的检测，新增对GETSET回包的检测

### 更新
* 无

### fix
* 修复camellia-redis-proxy阻塞型命令不可用的问题（1.0.33引入）


# 1.0.34（2021/08/05）
### 新增
* camellia-redis-proxy-hbase重构了string相关命令的冷热分离存储设计，具体见：[文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)
* CamelliaRedisTemplate提供Jedis适配器，修改一行代码从Jedis迁移到CamelliaRedisTemplate，具体见：[文档](/docs/redis-client/redis-client.md)
* CamelliaRedisTemplate提供SpringRedisTemplate适配器，具体见：[文档](/docs/redis-client/redis-client.md)
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
* info命令支持section参数，且支持获取后端redis集群的信息（内存使用率、版本、主从分布情况、slot分布情况等），具体见：[监控](/docs/redis-proxy/monitor/monitor.md)

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
* 新增info命令获取服务器相关信息，具体见：[监控](/docs/redis-proxy/monitor/monitor.md)
* 新增monitor-data-mask-password配置，用于隐藏日志和监控数据中的密码，具体见：[监控](/docs/redis-proxy/monitor/monitor.md)

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
* camellia-redis-proxy支持监控到后端redis的连接数，具体见：[监控数据](/docs/redis-proxy/monitor/monitor-data.md)

### 更新
* 无

### fix
* camellia-redis-proxy代理redis-cluster时，fix在极端边界情况下可能触发的死锁问题

# 1.0.24（2021/05/11）
### 新增
* camellia-redis-proxy新增ProxyRouteConfUpdater，用户可以自定义实现基于bid/bgroup的多组动态路由配置（比如对接到自己的配置中心，这样就不用依赖camellia-dashboard了），具体见：[路由配置](/docs/redis-proxy/auth/route.md)
* 提供了ProxyRouteConfUpdater的一个默认实现DynamicConfProxyRouteConfUpdater，该实现使用DynamicConfProxy（camellia-redis-proxy.properties）来管理多组路由配置以及配置的动态更新
* camellia-redis-proxy新增ProxyDynamicConfHook，用户可以基于hook来自定义的动态修改相关配置，具体见：[动态配置](/docs/redis-proxy/dynamic-conf.md)
* camellia-redis-proxy新增监控相关callback的DummyMonitorCallback的实现，如果不想打印相关统计日志，设置为dummy的callback实现即可
* camellia-redis-proxy监控指标里新增路由相关的监控项，包括到各个redis后端的请求数，以及当前生效的路由配置，具体见：[监控数据](/docs/redis-proxy/monitor/monitor-data.md)
* camellia-redis-proxy耗时监控增加业务级别的数据（bid/bgroup），具体见：[监控数据](/docs/redis-proxy/monitor/monitor-data.md)

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
* camellia-redis-proxy支持从redis-sentinel集群中的从节点读数据（会自动感知节点宕机、主从切换和节点扩容），具体见：[路由配置](/docs/redis-proxy/auth/route.md)
* CamelliaRedisTemplate使用camellia-redis-spring-boot-starter接入时，在访问camellia-redis-proxy时支持设置bid/bgroup

### 更新
* camellia-redis-proxy预热失败时启动失败

### fix
* 无

# 1.0.21（2021/04/06）
### 新增
* camellia-redis-proxy在使用本地配置时，支持动态修改路由转发规则，见：[路由配置](/docs/redis-proxy/auth/route.md)
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
[中文版](update-zh.md)


# 1.2.14（2023/08/xx）
### add
* camellia-redis-proxy support frontend tls

### update
* none

### fix
* none


# 1.2.13（2023/08/04）
### add
* camellia-http-accelerate-proxy support setting backupServer
* camellia-redis-proxy use nacos as config server, support json/properties, default properties
* camellia-redis-proxy support ectd as config server, support json/properties, default properties
* camellia-hot-key-server support ectd as config server, support json/properties, default json
* camellia-hot-key support `not_contains` rule type
* camellia-redis-proxy provide MultiTenantProxyRouteConfUpdater and MultiTenantClientAuthProvider
* camellia-htt-accelerate-proxy support setting congestion.control.algorithm of quic
* camellia-redis-proxy multi-write-mode migrate from yml to ProxyDynamicConf, support tenant level config, support dynamic config

### update
* `ProxyDynamicConfLoader` rename method `updateInitConf` to `init`
* `camellia-redis-proxy-nacos` rename artifactId to `camellia-redis-proxy-config-nacos`
* `com.netease.nim.camellia.redis.proxy.nacos.NacosProxyDynamicConfLoader` rename to `com.netease.nim.camellia.redis.proxy.config.nacos.NacosProxyDynamicConfLoader`

### fix
* camellia-redis-proxy `select 0` command should reply ok when upstream is redis-cluster


# 1.2.12（2023/07/28）
### add
* camellia-http-accelerate-proxy，proxy and transport-server support setting bind host, default 0.0.0.0
* camellia-http-accelerate-proxy，transport-route and upstream-route support disabled
* camellia-redis3(CamelliaRedisTemplate), support setting auth username（redis-standalone、redis-sentinel、redis-sentinel-slaves、redis-cluster）
* camellia-redis(CamelliaRedisTemplate), redis-sentinel、redis-sentinel-slaves support setting sentinelPassword
* camellia-redis3(CamelliaRedisTemplate), redis-sentinel、redis-sentinel-slaves support setting sentinelUserName and sentinelPassword
* camellia-redis-proxy, redis-sentinel、redis-sentinel-slaves support setting sentinelUserName and sentinelPassword
* camellia-redis3(CamelliaRedisTemplate)，support zmscore
* camellia-http-accelerate-proxy support quic as the transport protocol
* camellia-http-accelerate-proxy support compress for http-content
* camellia-codec provide XProps

### update
* camellia-hot-key-sdk use ConcurrentHashMapCollector, update collector full log from error to info
* optimize response-http-header=connection for http-console and http-accelerate-proxy
* use direct-buffer optimize pack of camellia-hot-key and camellia-http-accelerate-proxy
* camellia-hot-key support suffix match rule

### fix
* none


# 1.2.11（2023/07/19）
### add
* camellia-tools add CamelliaSegmentStatistics tools
* camellia-cache add a global on-off config
* provide camellia-http-console module, a simple http-server based on netty
* CamelliaRedisTemplate support setting custom RedisInterceptor, so you can import CamelliaHotKeyMonitorSdk
* provide camellia-codec module
* provide camellia-http-accelerate-proxy module
* camellia-redis-proxy's GlobalRedisProxyEnv provide ProxyShutdown to unbind port and release upstream redis connection

### update
* camellia-redis-proxy use camellia-http-console
* camellia-hot-key-server use camellia-http-console
* camellia-hot-key use camellia-codec
* camellia-hot-key-sdk support setting collector type, include Caffeine（default）and ConcurrentLinkedHashMap and ConcurrentHashMap
* camellia-hot-key-sdk support setting async collect, default in sync mode

### fix
* camellia-redis-proxy logging upstream failed command, the resource do not mask password
* camellia-hot-key-sdk get incorrect checkThreshold field of hot-key-config的checkThreshold（do not affect function）
* camellia-hot-key-server fix incorrect hot key calculate logic


# 1.2.10（2023/06/07）
### add
* camellia-redis-proxy support use camellia-hot-key，thanks[@21want28k](https://github.com/21want28k) provide this function
* CamelliaHotKeyCacheSdk add some new api, thanks[@21want28k](https://github.com/21want28k) provide this function

### update
* camellia-hot-key set ConcurrentLinkedQueue as default work queue, improve performance
* camellia-hot-key remove the expire policy of HotKeyCounterManager Caffeine, avoid performance degradation
* camellia-hot-key-server support setting Caffeine instance count of every namespace, it will improve performance in some case

### fix
* camellia-hot-key-server fix `unknown seqId` problem
* CamelliaHotKeyCacheSdk fix namespace wrong，thanks[@21want28k](https://github.com/21want28k) find this bug
* camellia-redis-proxy-discovery-zk 1.2.8/1.2.9 has compatibility issue with 1.2.7 or earlier, 1.2.10 fix this compatibility issue


# 1.2.9（2023/06/02）
### add
* camellia-redis-proxy support print resource/command/keys in log when upstream redis failed

### update
* camellia-redis-proxy、camellia-delay-queue-server、camellia-id-gen-server add online/offline callback
* ZkProxyRegistry/ZkHotKeyServerRegistry register online/offline callback
* CamelliaHashedExecutor add hashIndex method to get thread index by hashKey

### fix
* CamelliaHotKeyCacheSdkConfig remove namespace field, CamelliaHotKeyCacheSdk should use namespace from method
* camellia-hot-key-server grace online/offline should check traffic
* CamelliaHotKeyCacheSdk fix keyDelete/keyUpdate do not notify other sdk
* TopNCounter should clear buffer after collect
* TopNCounter fix calculate maxQps not exact issue


# 1.2.8（2023/05/29）
### add
* add camellia-hot-key module
* add camellia-zk module, camellia-redis-proxy-zk and camellia-hot-key-zk base on camellia-zk

### update
* camellia-redis-proxy set `tcp_keepalive` default true for client connect
* camellia-config, update config namespace info field from varchar to text

### fix
* camellia-redis-proxy update the console api `/prometheus`, replace `%n` to `\n`, adapate windows os
* camellia-redis-proxy when client connection in subscribe mode, if upstream redis down or proxy-redis connection disconnect for other reason, proxy should close client-proxy connection in same time
* camellia-redis-proxy fix client connection in subscribe mode, if client send ping、sub/unsub in frequency, client connection will broken 
* camellia-redis-proxy force disconnect client connection with subscribe fail for upstream redis not available


# 1.2.7（2023/05/04）
### add
* none

### update
* none

### fix
* fix lock instance leak in concurrent case of CamelliaRedisLockManager
* fix schedule task leak in some case of RedisConnection when camellia-redis-proxy proxy pass TRANSACTION/PUB-SUB/BLOCKING commands


# 1.2.6（2023/04/28）
### add
* camellia-redis-proxy support custom-write for TRANSACTION commands
* camellia-tools provide CamelliaScheduleExecutor
* RateLimitProxyPlugin support setting default config in tenant level

### update
* camellia-redis-proxy support recycler CommandPack instance, optimize gc
* camellia-config add trim logic for key
* camellia-config optimize the response of `/getConfigString`
* CamelliaLoadingCache add max load time control on cache penetration case
* camellia-redis-proxy specify ErrorReply msg of proxy upstream error
* camellia-redis-proxy run in redis-cluster mode, the line break in reply of `cluster nodes` commands should use `\n` rather than `\r\n`
* CamelliaRedisLockManager use CamelliaScheduleExecutor instead of ScheduledExecutorService
* camellia-redis-proxy RedisConnection use CamelliaScheduleExecutor instead of ScheduledExecutorService to invoke idle-check and heartbeat
* camellia-redis-proxy optimize the heartbeat logic in redis-cluster mode

### fix
* camellia-config fix sql error
* camellia-redis-proxy when client connection in TRANSACTION or SUBSCRIBE status, `ping` command should pass through
* camellia-redis-proxy fix command no reply when client connection change status between SUBSCRIBE and normal
* camellia-redis-proxy fix command no reply when client connection change status between SUBSCRIBE to normal, and send blocking command
* camellia-redis-proxy fix the TRANSACTION command logic error when route to redis-cluster, only occurs bug when in key's slot is 0


# 1.2.5（2023/04/07）
### add
* none

### update
* camellia-redis-proxy memory queue support use jctools high performance queue
* camellia-redis-proxy run in redis-cluster mode, optimize the MOVED logic

### fix
* camellia-redis-proxy optimize the renew logic when proxy to redis-cluster, may renew timely(bug from 1.2.0), thanks [@saitama-24](https://github.com/saitama-24) find this problem


# 1.2.4（2023/04/03）
### add
* add camellia-config module, a simple k-v config center
* camellia-redis-proxy provide NacosProxyDynamicConfLoader, a new method to use nacos
* camellia-redis-proxy BuildInProxyPlugins support setting custom order

### update
* camellia-redis-proxy optimize RedisConnection
* camellia-redis-proxy support use camellia-config
* camellia-feign support use camellia-config
* camellia-redis-proxy support stats upstream fails when use PUBSUB commands
* camellia-redis-proxy-hbase memory queue support dynamic capacity
* camellia-delay-queue-server scheduler add concurrent control
* optimize IPMatcher, so it can calculate `10.22.23.1/24` correct

### fix
* fix camellia-redis-proxy use custom proxy route conf, fix no-effective of automatically eliminate faulty nodes when use multi read mode
* fix camellia-redis-proxy both use converterPlugin's key converter and hotKeyCachePlugin, the hot key cache not-effective


# 1.2.3（2023/03/15）
### add
* camellia-redis-proxy support monitor upstream fail

### update
* camellia-redis-proxy specify ErrorReply msg of proxy upstream error
* camellia-redis-proxy update some metrics type of /prometheus
* camellia-redis-proxy optimize status of RedisConnection

### fix
* fix camellia-redis-proxy use info upstream-info command to get upstream route conf do not mask redis password


# 1.2.2（2023/02/28）
### add
* none

### update
* refactor ProxyDynamicConf, support custom Loader

### fix
* fix RedisConnection heartbeat error do not close connection(bug from v1.2.0)


# 1.2.1（2023/02/22）
### add
* redis-resource of redis-proxies and redis-proxies-discovery support setting db, both camellia-redis-proxy and CamelliaRedisTemplate
* camellia-redis-proxy support select command, only when upstream is redis-standalone/redis-sentinel/redis-proxies or their compose of sharding/read-write-separate, you can select no-zero db. if upstream contains redis-cluster resource, only support select 0
* CamelliaRedisTemplate support RedisProxiesDiscoveryResource

### update
* camellia-redis-proxy-hbase support setting upstream.redis.hbase.command.execute.concurrent.enable(default false), it will improve the performance of pipeline commands, you can enable it only when the client is blocking, such as jedis
* rename DefaultTenancyNamespaceKeyConverter -> DefaultMultiTenantNamespaceKeyConverter

### fix
* fix the dependency problem of jar when use jedis3+SpringRedisTemplate+zk/eureka to access redis-proxy by discovery mode(root case: compile by jedis2.x, occur class not found error)
* fix bid/bgroup params not available when use CamelliaRedisProxyZkFactory+CamelliaRedisTemplate to access camellia-redis-proxy 
* this pipeline method in CamelliaRedisTemplate should be read method: Response<Long> zcard(String key)


# 1.2.0（2023/02/14）
### add
* add camellia-redis3 module, support jedis3.x(default use v3.6.3)
* add camellia-redis-base, the common of camellia-redis-client and camellia-redis-proxy
* camellia-redis-proxy support custom upstream, core interface is IUpstreamClientTemplate and IUpstreamClientTemplateFactory
* refactor camellia-redis-proxy-hbase, replace custom CommandInvoker into custom upstream, more code reuse, refactor thread module, use business thread to isolation netty worker thread and business thread 
* camellia-tools provide CamelliaLinearInitializationExecutor to resource liner initialization
* camellia-redis-proxy refactor multi tenant init by CamelliaLinearInitializationExecutor
* camellia-redis-proxy support check upstream healthy in multi-read mode
* camellia-redis-proxy support init upstream RedisConnection in async mode
* camellia-redis-proxy support monitor qps every seconds
* camellia-redis-proxy add schedule renew when upstream is redis-cluster, default 600s
* camellia-hbase support setting userName、password、tag of aliyun-lindorm in url
* camellia-redis-proxy optimize the failover logic of redis-cluster-slaves and redis-sentinel-slaves
* camellia-redis-proxy optimize the failover logic of redis-proxies and redis-proxies-discovery
* camellia-redis-proxy support configure dynamic.conf.file.name in application.yml to replace camellia-redis-proxy.properties

### update
* camellia-redis-proxy rename core upstream service  
* camellia-redis remove the adaptor of CamelliaRedisTemplate to SpringRedisTemplate
* camellia-redis remove the adaptor of CamelliaRedisTemplate to Jedis
* add camellia-redis-toolkit module, independent the toolkits (such as CamelliaRedisLock) from camellia-redis, so the code could reuse by camellia-redis3
* add camellia banner in log file when use package to startup(redis-proxy、delay-queue、id-gen-server)

### fix
* fix camellia-redis-proxy use ProxyDynamicConf#reload(Map) direct setting custom k-v config, and config will be cleared in schedule task(bug from v1.1.8)


# 1.1.13（2023/01/30）（1.1.13 is broken in maven central repository, so re-deploy in 1.1.14）
### add
* camellia-redis-proxy support use transport_native_epoll、transport_native_kqueue、transport_native_io_uring, default use jdk_nio
* camellia-redis-proxy support configure TCP_QUICKACK option, only support in transport_native_epoll mode, thanks [@tain198127](https://github.com/tain198127) , related issue: [issue-87](https://github.com/netease-im/camellia/issues/87)
* RedisProxyJedisPool provide AffinityProxySelector, support configure affinity of proxy, thanks [@tain198127](https://github.com/tain198127) provide this feature

### update
* id-gen-sdk use shared schedule thread pool default, decrease the thread num when init multi sdk instance
* delay-queue-sdk use shared schedule thread pool default, decrease the thread num when init multi sdk instance
* RedisProxyJedisPool use shared schedule thread pool default, decrease the thread num when init multi instance
* id-gen-server add bootstrap module, provide *.tar.gz to run directly
* delay-queue-server add bootstrap module, provide *.tar.gz to run directly

### fix
* fix CamelliaStatistics calc avg error when count=0


# 1.1.12（2023/01/12）
### add
* none

### update
* rollback the performance improve for BulkReply by direct buf (from 1.1.8), the reason is memory leak in some cases, such as: client connection disconnect before Reply write, the BulkReply will not release

### fix
* camellia-redis-proxy fix memory leak when client connection disconnect before Reply write, the BulkReply will not release(bug from v1.1.8)


# 1.1.11（2023/01/10）
### add
* camellia-redis-proxy support prometheus/grafana, thanks [@tasszz2k](https://github.com/tasszz2k) 
* camellia-tools provide CamelliaDynamicExecutor and CamelliaDynamicIsolationExecutor, provide CamelliaExecutorMonitor to monitor thread-pool

### update
* some utils from camellia-core package, move to camellia-tools package
* camellia-redis-proxy optimize the DefaultTenancyNamespaceKeyConverter when redis-key contains hashtag, thanks [@phuc1998](https://github.com/phuc1998) and [@tasszz2k](https://github.com/tasszz2k)   

### fix
* fix camellia-redis-proxy upstream redis connection leak when use TRANSACTION commands in high qps, thanks [@phuc1998](https://github.com/phuc1998) and [@tasszz2k](https://github.com/tasszz2k) find this bug
* fix camellia-redis-proxy a concurrent issue of reply decode when use PUBSUB commands(bug from v1.1.8)


# 1.1.10（2023/01/03）
### add
* camellia-redis-proxy provide DynamicRateLimitProxyPlugin, which support dynamic configure by camellia-dashboard, thanks [@tasszz2k](https://github.com/tasszz2k)

### update
* refactor project maven module
* rename camellia-redis-proxy artifactId to camellia-redis-proxy-core, camellia-redis-proxy transform to a directory

### fix
* fix CamelliaRedisTemplate select db when use RedisResource
* fix Chinese disorderly code of camellia-delay-queue, thanks [@ax3353](https://github.com/ax3353)


# 1.1.9（2022/12/21）
### add
* provide camellia-cache module, enhance spring-cache
* camellia-redis-proxy support LMPOP、BLMPOP command(redis7.0)

### update
* optimize camellia-id-gen-sdk, any error from id-gen-server should trigger retry and node-ban, not only network error

### fix
* getMsg api of camellia-delay-queue, when msg is ack, getMsg return 200, but msg not return


# 1.1.8（2022/12/13）
### add
* camellia-redis-proxy support configure custom k-v config(ProxyDynamicConf.java) by application.yml, priority is lower than camellia-redis-proxy.properties  
* camellia-redis-proxy provider DefaultTenancyNamespaceKeyConverter, you can isolate the key(different key prefix) in different tenancy(bid/bgroup)
* camellia-redis-proxy support ZMPOP、BZMPOP command(redis7.0)

### update
* camellia-redis-proxy-samples remove zk/nacos dependency, if need, add related dependencies by self
* camellia-redis-proxy update SCAN command without MATCH args when use ConverterProxyPlugin and KeyConverter
* camellia-redis-proxy performance improved for BulkReply encode/decode by direct buf
* camellia-redis-proxy Support connect limit for each tenant(bid, bgroup) by only 1 global configuration, thanks [@tasszz2k](https://github.com/tasszz2k)

### fix
* fix camellia-delay-queue use long-polling mode, do not hold http request after run for a period of time
* fix camellia-redis-proxy do not handler the key in reply of EXBZPOPMAX/EXBZPOPMIN commands when use ConverterProxyPlugin


# 1.1.7（2022/11/30）
### add
* camellia-redis-proxy support ZINTERCARD command
* camellia-redis-proxy support TairZSet、TairHash、TairString commands
* camellia-redis-proxy support RedisJSON commands
* camellia-redis-proxy support RedisSearch commands
* camellia-redis-proxy support select db when upstream is redis-standalone or redis-sentinel
* CamelliaRedisTemplate support select db when upstream is redis-standalone or redis-sentinel

### update
* modify the error code of camellia-dashboard api for ip-permission, thanks [@tasszz2k](https://github.com/tasszz2k)

### fix
* none


# 1.1.6（2022/11/23）
### add
* none

### update
* camellia-redis-proxy optimize monitor function on memory/gc

### fix
* none


# 1.1.5（2022/11/21）
### add
* CamelliaStatistic support quantile stats, like p50/p75/p90/p90/p95/p99/p999
* camellia-redis-proxy rt monitor support quantile stats, like p50/p75/p90/p90/p95/p99/p999
* provide FileBasedCamelliaApi, support use local properties file to simulate camellia-dashboard
* camellia-feign support use local properties file to provide dynamic option, such as timeout\circuit\route 
* camellia-core multi-write/sharding thread pool executor support setting RejectedExecutionHandler

### update
* during camellia-feign initialization, if upstream services down, logging warn log instead of throw exception
* when use camellia-dashboard manage camellia-feign dynamic resource-table, if remote return 404, use local resource-table rather than throw exception
* when camellia-feign setting multi-write thread pool executor's RejectedExecutionHandler into Abort, the reject task will call CamelliaFeignFailureListener

### fix
* none


# 1.1.4（2022/11/08）
### add
* camellia-dashboard support custom header, thanks [@tasszz2k](https://github.com/tasszz2k) provide this function
* camellia-redis-proxy support PUB-SUB commands when configure multi-write route conf
* provide CamelliaMergeTask and CamelliaMergeTaskExecutor

### update
* camellia-redis-proxy refactor and optimize ReplyDecoder to improve performance

### fix
* fix camellia-delay-queue thread leak when use long-polling


# 1.1.3（2022/10/24）
### add
* CamelliaRedisTemplate support RedisProxiesResource
* camellia-redis-proxy provide CommandDisableProxyPlugin, you can configure in the camellia-redis-proxy.properties to disable some commands

### update
* camellia-delay-queue, deleteMsg api support release redis memory right now(default false)
* optimize camellia-redis-proxy renew logic when upstream is redis-cluster

### fix
* camellia-delay-queue, when delay msg has delete or consumer, same msgId msg send will duplicate ignore
* fix redis-benchmark do not work when proxy start with password(from v1.1.0), root case: error handler when auth and other commands submit in pipeline


# 1.1.2（2022/10/12）
### add
* CamelliaRedisProxyStarter support start console-server
* RedisProxyRedisConnectionFactory implements DisposableBean, support destroy method
* camellia-redis-proxy support cluster-mode, so proxy-cluster will be regarded as redis-cluster
* camellia-id-gen-sdk provide DelayQueueServerDiscoveryFactory to manager multi delay-queue-server clusters base on discovery
* camellia-redis-proxy support COMMAND command, transpond to upstream redis

### update
* custom monitor callback running in isolation thread pool

### fix
* camellia-redis-proxy random port mode do not check available
* fix SpringProxyBeanFactory not available in camellia-redis-proxy

# 1.1.1（2022/09/26）
### add
* camellia-redis-proxy support TRANSACTION commands(MULTI/EXEC/DISCARD/WATCH/UNWATCH) when route to redis-cluster

### update
* optimize code of AsyncCamelliaRedisTemplate and AsyncCamelliaRedisClusterClient
* modify HotKeyProxyPlugin request order greater than HotKeyCacheProxyPlugin

### fix
* none


# 1.1.0（2022/09/21）
### add
* refactor camellia-redis-proxy plugins and monitor

### update
* none

### fix
* none


# 1.0.61（2022/09/06）
### add
* camellia-delay-queue support long-polling to consume msg
* provide camellia-console module, so you can manager multi camellia-dashboard clusters
* provide CamelliaStatisticsManager to manage multi CamelliaStatistics instances

### update
* optimize camellia-redis-proxy's AsyncCamelliaRedisTemplate init logic

### fix
* fix camellia-redis-proxy invoke ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE command error when route to redis-cluster or sharding-redis
* fix camellia-feign memory leak in DiscoveryResourcePool init fail case


# 1.0.60（2022/08/16）
### add
* add camellia-delay-queue module
* camellia-feign support failureListener, include CamelliaNakedClient and CamelliaFeignClient
* camellia-tools provide CamelliaStatistics for calculate sum/count/avg/max
* camellia-redis provide CamelliaFreq for freq, include standalone/cluster mode
* camellia-redis-proxy add valid check when dynamic route conf update
* camellia-redis-proxy add route, resource init in async mode, improve multi-tenant isolation

### update
* CamelliaRedisTemplate add available check for redis-cluster init
* rename NacosProxyDamicConfSupport to NacosProxyDynamicConfSupport
* CamelliaRedisTemplate eval/evalsha with the specified timeout

### fix
* fix camellia-dashboard FeignChecker not effective
* fix SideCarFirstProxySelector of RedisProxyJedisPool offline proxy failure


# 1.0.59（2022/06/21）
### add
* camellia-core/camellia-feign adjust thread mode, provide new MultiWriteType MISC_ASYNC_MULTI_THREAD
* camellia-redis-proxy support cache double-delete
* camellia-dashboard provide some new api

### update
* CamelliaHashedExecutor support getCompletedTaskCount
* update ProxyConstants default conf, increment default sharding/multi-write threads pool size
* camellia-redis-proxy skip monitor upstream redis spend time for pub-sub commands and blocking commands
* bump fastjson from 1.2.76 to 1.2.83

### fix
* fix upstream redis spend time = 0 when upstream-redis has password


# 1.0.58（2022/05/16）
### add
* camellia-redis-proxy detect api support key count and qps

### update
* CamelliaIdGenSdkConfig support setting OkHttpClient keepAliveSeconds, default 30s

### fix
* none


# 1.0.57（2022/05/10）
### add
* none

### update
* none

### fix
* fix CamelliaNakedClient multi-write


# 1.0.56（2022/05/10）
### add
* camellia-redis-proxy support transpond to other proxy, such as codis、twemproxy, and support use discovery mode to find proxy
* camellia-core support async write, base on thread pool and memory queue
* camellia-feign provide CamelliaNakedClient
* camellia-redis-proxy support BloomFilter commands
* camellia-redis-proxy provide IPCheckerCommandInterceptor

### update
* DynamicValueGetter move from package camellia-core to camellia-tools包

### fix
* none


# 1.0.55（2022/04/07）
### add
* none

### update
* none

### fix
* fix camellia-feign circuit breaker exception checker 


# 1.0.54（2022/04/07）
### add
* provide CamelliaCircuitBreaker
* camellia-feign support circuit breaker, support spring-boot-starter, support dynamic option conf
* camellia-redis-proxy custom ProxyRouteConfUpdater support delete route conf

### update
* none

### fix
* none


# 1.0.53（2022/03/24）
### add
* camellia-redis-proxy console support /detect, so you can use camellia-redis-proxy as a monitor platform

### update
* none

### fix
* fix camellia-redis-proxy's if command with upstream-info section(bug from v1.0.51)


# 1.0.52（2022/03/16）
### add
* provide camellia-feign module, so feign support dynamic route, multi-write, dynamic timeout conf
* camellia-core provide CamelliaDiscovery/CamelliaDiscoveryFactory
* camellia-core provide ResourceTableUpdater/MultiResourceTableUpdater

### update
* camellia-redis remove ProxyDiscovery, use IProxyDiscovery which implements CamelliaDiscovery
* camellia-id-gen remove AbstractIdGenServerDiscovery, use IdGenServerDiscovery which implements CamelliaDiscovery
* all modules upgrade to jdk8

### fix
* none


# 1.0.51（2022/02/28）
### add
* none

### update
* camellia-redis-proxy info command reply, replace \n to \r\n, so you can use redis-shake to migrate redis data

### fix
* after invoke deregister method of ZkProxyRegistry, if the tcp connect of zk reset, reconnect task will trigger camellia-redis-proxy register to zk again
* camellia-dashboard and camellia-redis-proxy print redis password in some case


# 1.0.50（2022/02/17）
### add
* camellia-redis provide CamelliaRedisLockManager to manager redis-lock auto renew
* camellia-redis provide CamelliaRedisTemplateManager to manger multi-redis-template of different bid/bgroup
* camellia-tools prvodie CamelliaHashedExecutor to execute runnable/callable with same thread in same hashKey

### update
* none

### fix
* fix camellia-dashboard deleteResourceTable api, should update ResourceInfo's tid ref


# 1.0.49（2022/01/19）
### add
* camellia-redis-proxy support script load/flush/exists
* camellia-redis-proxy support eval_ro/evalsha_ro, need upstream redis7.0+

### update
* camellia-redis-proxy upstream redis spend stats support mask password

### fix
* scan should be a read command in monitor data
* fix camellia-dashboard api getTableRefByBidGroup/deleteTableRef, param should bid not tid

# 1.0.48（2022/01/17）
### add
* camellia-redis-proxy support scan command when use custom sharding
* CamelliaRedisTemplate provide getReadJedisList/getWriteJedisList method
* CamelliaRedisTemplate provide executeRead/executeWrite method

### update
* none

### fix
* none


# 1.0.47（2022/01/05）
### add
* CamelliaRedisTemplate provide getJedisList method

### update
* none

### fix
* none


# 1.0.46（2021/12/29）
### add
* provide CRC16HashTagShardingFunc/DefaultHashTagShardingFunc to support HashTag when use custom sharding route table

### update
* rename shading to sharding

### fix
* none


# 1.0.45（2021/12/24）
### add
* camellia-redis-proxy KafkaMqPackConsumer support batch/retry
* camellia-redis-proxy provide DynamicCommandInterceptorWrapper to combine multi CommandInterceptors
* camellia-redis-proxy support disable console
* camellia-redis-proxy support read from redis-cluster slave node
* camellia-redis-proxy support transpond to multi stateless redis proxies, such as codis-proxy/twemproxy

### update
* camellia-id-gen modify default conf

### fix
* none


# 1.0.44（2021/11/29）
### add
* camellia-redis-proxy provide KafkaMqPackProducerConsumer, so proxy can be producer/consumer at the same time
* camellia-redis-proxy provide monitor upstream redis spend time
* RedisProxyJedisPool support jedis3

### update
* refactor project module structure, new module camellia-redis-proxy-plugins, rename/move camellia-redis-zk/camellia-redis-proxy-mq/camellia-redis-proxy-hbase into camellia-redis-proxy-plugins
* RedisProxyJedisPool rename package, move package to camellia-redis-proxy-discovery
* camellia-redis-proxy refactor reply of info gc command

### fix
* none


# 1.0.43（2021/11/23）
### add
* camellia-id-gen of segment and strict mode provide update api to setting starting id
* camellia-id-gen of segment and strict mode support setting shifting region id
* camellia-id-gen of segment mode support id sync in multi regions
* camellia-id-gen provide api to decode regionId/workerId
* camellia-redis-proxy support multi-write based on mq(such as kafka)

### update
* camellia-redis-proxy monitor data buffer with size limit
* camellia-redis-proxy close client connection if custom ClientAuthProvider throw exception

### fix
* fix camellia-id-gen-strict-spring-boot-starter config of cache-key-prefix not effective


# 1.0.42（2021/10/26）
### add
* camellia-redis-proxy info command metrics of redis-cluster-safety redefine

### update
* camellia-redis-proxy console api of monitor support setting json max size of slow-command/big-key

### fix
* none


# 1.0.41（2021/10/20）
### add
* camellia-redis-proxy info command metrics of redis-cluster-safety redefine

### update
* none

### fix
* none


# 1.0.40（2021/10/19）
### add
* camellia-redis-proxy support info command by http-api
* camellia-redis-proxy support client connect of bid/bgroup in info command

### update
* none

### fix
* none


# 1.0.39（2021/10/18）
### add
* camellia-redis-proxy support setting max client connect limit, default no limit
* camellia-redis-proxy support setting idle client connect check and close, default disable
* camellia-redis-proxy provide RateLimitCommandInterceptor, both support proxy-level and bid-bgroup-level
* camellia-redis-proxy provide camellia-redis-proxy-nacos-spring-boot-starter  

### update
* rename package name of CommandInterceptor

### fix
* none


# 1.0.38（2021/10/11）
### add
* add camellia-id-gen mode, support snowflake, support db-base id-gen(growth tread), support db/redis-base id-gen(strict growth)
* support setting custom callback by spring @Autowired

### update
* remove camellia-redis-toolkit module, CamelliaCounterCache/CamelliaRedisLock merge to camellia-redis module
* rename package of camellia-tools module

### fix
* none


# 1.0.37（2021/09/24）
### add
* camellia-redis-proxy support setting upstream redis auth with userName/password

### update
* info command get upstream redis connect, will not return if connect is 0
* enhance ProxyDynamicConfHook, so you can intercept all dynamic conf of ProxyDynamicConf
* extend the boundary of password-mask in monitor-data/log  
* refactor CommandDecoder

### fix
* fix monitor data not exact of upstream redis connect, no effect the core function  


# 1.0.36（2021/09/06）
### add
* add camellia-tools module, provide compress utils CamelliaCompressor, encrypt utils CamelliaEncryptor, local cache utils CamelliaLoadingCache  
* provide samples for camellia-redis-proxy implements data-encryption/data-compress by use camellia-tools
* camellia-redis-proxy support custom ClientAuthProvider to route different bid/bgroup route conf by different password  
* camellia-redis-proxy support setting random port/consolePort
* camellia-redis-proxy support key converter
* camellia-redis-proxy support RANDOMKEY command  
* camellia-redis-proxy support HELLO command, do not support RESP3, but support setname and auth username password by HELLO command(if redis-client is Lettuce6.x, proxy should upgrade to this version)
* camellia-redis-proxy support scan command when route to redis-cluster  

### update
* camellia-redis-proxy info command reply add http_console_port field
* camellia-redis-proxy info command reply add redis_version field
* camellia-redis-proxy info command reply of Stats rename field, such as avg.commands.qps rename to avg_commands_qps  
* camellia-redis-proxy info command reply of Stats qps field format to %.2f
* auth/client/quit commands migrate from ServerHandler to CommandsTransponder  

### fix
* fix KeyParser of EVAL/EVALSHA/XINFO/XGROUP/ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE


# 1.0.35（2021/08/13）
### add
* camellia-redis-proxy support convert value of string/hash/list/set/zset commands, you can use this feature to data-encryption/data-compress
* camellia-redis-proxy support GETEX/GETDEL/HRANDFIELD/ZRANDMEMBER commands
* camellia-redis-proxy's BigKeyHunter support check of GETEX/GETDEL, support check reply of GETSET

### update
* none

### fix
* fix camellia-redis-proxy blocking commands not available(bug from v1.0.33)

# 1.0.34（2021/08/05）
### add
* camellia-redis-proxy-hbase refactor string commands implements
* CamelliaRedisTemplate provide Jedis Adaptor to migrate from Jedis
* CamelliaRedisTemplate provide SpringRedisTemplate Adaptor
* camellia-redis-proxy provide util class CamelliaRedisProxyStarter to start proxy without spring-boot-starter

### update
* camellia-redis-proxy remove jedis dependency

### fix
* none


# 1.0.33（2021/07/29）
### add
* camellia-redis-proxy provide TroubleTrickKeysCommandInterceptor to avoid trouble-trick-keys attack upstream redis
* camellia-redis-proxy provide MultiWriteCommandInterceptor to setting custom multi-write-policy(such as some key need multi-write, others no need)
* camellia-redis-proxy support DUMP/RESTORE commands
* CamelliaRedisTemplate support DUMP/RESTORE commands

### update
* none

### fix
* camellia-redis-proxy BITPOS should be READ command
* CamelliaRedisTemplate BITPOS should be READ command


# 1.0.32（2021/07/15）
### add
* camellia-redis-proxy-hbase support string/hash commands to hot-cold separate store

### update
* none

### fix
* none

# 1.0.31（2021/07/05）
### add
* info commands support section param, support get upstream-info(such like memory/version/master-slave/slot)

### update
* none

### fix
* fix after request subscribe/psubscribe and unsubscribe/punsubscribe, the bind pub-sub upstream redis-client do not release connection


# 1.0.30（2021/06/29）
### add
* none

### update
* support mask password when init and reload route conf

### fix
* fix NPE when open slow-command-monitor/big-key-monitor and use pub-sub commands
* when proxy route to redis-cluster, support subscribe/psubscribe multiple times, and support unsubscribe/punsubscribe


# 1.0.29（2021/06/25）
### add
* none

### update
* none

### fix
* fix occasional not_available when use blocking commands


# 1.0.28（2021/06/25）
### add
* support info command to get server info
* support setting monitor-data-mask-password conf, you can mask password in log and monitor data

### update
* none

### fix
* fix not_available when use pipeline submit batch blocking commands

# 1.0.27（2021/06/22）
### add
* none

### update
* none

### fix
* fix too many connections when use blocking commands frequency

# 1.0.26（2021/05/27）
### add
* camellia-redis-proxy support setting port/applicationName instead of server.port/spring.application.name
* ProxyDynamicConf support setting k-v map instead of read from file

### update
* rename LoggingHoyKeyMonitorCallback to LoggingHotKeyMonitorCallback
* camellia-redis-proxy delete transpond mode of Disruptor/LinkedBlockingQueue, only support direct transpond mode
* camellia-redis-proxy stats log rename logger, add prefix of camellia.redis.proxy., e.g LoggingMonitorCallback.java
* camellia-redis-proxy rename BigKeyMonitorCallback callback method, callbackUpstream/callbackDownstream rename to callbackRequest/callbackReply
* camellia-redis-proxy performance update

### fix
* none

# 1.0.25（2021/05/17）
### add
* camellia-redis-proxy support close idle upstream redis connection, default setting true
* camellia-redis-proxy support monitor connect count of upstream redis

### update
* none

### fix
* when camellia-redis-proxy proxy to redis-cluster, fix a deadlock bug in some extreme case

# 1.0.24（2021/05/11）
### add
* camellia-redis-proxy support ProxyRouteConfUpdater, so you can use multi-route-conf exclude camellia-dashboard
* support a default implements of ProxyRouteConfUpdater, named DynamicConfProxyRouteConfUpdater, it uses DynamicConfProxy(camellia-redis-proxy.properties) to manager multi-route-conf
* camellia-redis-proxy support ProxyDynamicConfHook，so you can dynamic update conf by hook
* camellia-redis-proxy add dummy callback implements of monitor
* camellia-redis-proxy monitor add route metrics: request of upstream redis, current route-conf
* camellia-redis-proxy add spend stats metric of bid/bgroup

### update
* none

### fix
* none


# 1.0.23（2021/04/16）
### add
* none

### update
* update netty version to 4.1.63

### fix
* fix jdk8 ConcurrentHashMap's computeIfAbsent performance bug，fix see: CamelliaMapUtils，bug see: https://bugs.openjdk.java.net/browse/JDK-8161372

# 1.0.22（2021/04/xx）
### add
* CamelliaRedisTemplate support read from slaves in redis-sentinel cluster(will automatic process node-down/master-switch/node-expansion)
* camellia-redis-proxy support read from slaves in redis-sentinel cluster(will automatic process node-down/master-switch/node-expansion)
* CamelliaRedisTemplate use camellia-redis-spring-boot-starter, support setting bid/bgroup when call camellia-redis-proxy

### update
* camellia-redis-proxy do not start if preheat fail

### fix
* none

# 1.0.21（2021/04/14）
### add
* camellia-redis-proxy support dynamic reload of redis address route conf when use local conf
* camellia-redis-proxy's ProxyDynamicConf(camellia-redis-proxy.properties) support use standalone absolute-path conf file to merge classpath:camellia-redis-proxy.properties
* camellia-redis-proxy support preheat(default true), if true, proxy will connect to upstream redis when proxy start, rather than connect to upstream redis until command from redis-cli arrive proxy
* camellia-redis-spring-boot-starter/camellia-hbase-spring-boot-starter support dynamic local json complex conf 

### update
* when camellia-redis-proxy close RT monitor by DynamicConf, slow-command-monitor will close either, same logic to yml
* camellia-spring-redis-{zk,eureka}-discovery-spring-boot-starter add open-off config, default open
* RedisProxyJedisPool add param of jedisPoolLazyInit to lazy init jedisPool of proxy, to reduce initial time of RedisProxyJedisPool, default open, default init 16 jedisPool of proxy first

### fix
* fix a bug of RedisProxyJedisPool may cause 'Could not get a resource from the pool', very low probability(from 1.0.14) 
* fix conf not found error when camellia-redis-proxy build/run in fat-jar 

# 1.0.20（2021/02/26）
### add
* none

### update
* refactor camellia-redis-proxy-hbase, inconsistent to the old version, see [doc](/docs/redis-proxy-hbase/redis-proxy-hbase.md)
* optimize camellia-redis-proxy when open command spend time monitor

### fix
* none


# 1.0.19（2021/02/07）
### add
* none  

### update
* performance update of camellia-redis-proxy, see [v1.0.19](/docs/redis-proxy/performance-report-8.md)

### fix
* fix xinfo/xgroup in KeyParser/pipeline

# 1.0.18（2021/01/25）
### add
* add console http api of /reload, so you can reload ProxyDynamicConf by 'curl http://127.0.0.1:16379/reload'
* support HSTRLEN/SMISMEMBER/LPOS/LMOVE/BLMOVE
* support ZMSCORE/ZDIFF/ZINTER/ZUNION/ZRANGESTORE/GEOSEARCH/GEOSEARCHSTORE
* open the dynamic conf function of ProxyDynamicConf, if you setting 'k=v' in file camellia-redis-proxy.properties, then you can call ProxyDynamicConf.getString("k") to get 'v'  

### update
* if proxy setting multi-write, then blocking command will return not support

### fix
* none

# 1.0.17（2021/01/15）
### add
* camellia-redis-proxy support transaction command, only when proxy route to redis/redis-sentinel with no-sharding/no-read-write-separate
* support ZPOPMIN/ZPOPMAX/BZPOPMIN/BZPOPMAX

### update
* none

### fix
* fix ReplyDecoder bug of camellia-redis-proxy，proxy will modify nil-MultiBulkReply to empty-MultiBulkReply, find this bug when realize transaction command's support
* fix NPE when ProxyDynamicConf init, this bug does not affect the use of ProxyDynamicConf, only print the error log once when proxy start 

# 1.0.16（2021/01/11）
### add
* some conf properties support dynamic reload
* camellia-redis-zk-registry support register hostname

### update
* optimize lock of some concurrent initializer

### fix
* none

# 1.0.15（2020/12/30）
### add
* none

### update
* HotKeyMonitor json add fields times/avg/max
* LRUCounter update, use LongAdder instead of AtomicLong

### fix
* none

# 1.0.14（2020/12/28）
### add
* none

### update
* when RedisProxyJedisPool's RefreshThread refresh proxy set, event ProxySelector hold this proxy, RefreshThread still call add method, avoid some times' timeout of proxy cause proxy not load balance

### fix
* fix a bug of RedisProxyJedisPool, low probability, may cause error of 'Could not get a resource from the pool'(from v1.0.14)

# 1.0.13（2020/12/18）
### add
* none

### update
* IpSegmentRegionResolver allow null/empty config，so camellia-spring-redis-eureka-discovery-spring-boot-starter and camellia-spring-redis-zk-discovery-spring-boot-starter can ignore configure of regionResolveConf

### fix
* none

# 1.0.12（2020/12/17）
### add
* RedisProxyJedisPool allow setting custom policy of proxy choose: IProxySelector. default use RandomProxySelector，if you enable side-car-first, then use SideCarFirstProxySelector
* if RedisProxyJedisPool use SideCarFirstProxySelector，proxy priority is: side-car-proxy -> same-region-proxy -> other-proxy, for setting a proxy belongs to which region, you need define RegionResolver(provider IpSegmentRegionResolver which divide region by ip-segment)
* provider LocalConfProxyDiscovery

### update
* optimize the fast fail policy when redis-cluster nodes down in camellia-redis-proxy
* camellia-redis-proxy renew slot-node in async

### fix
* fix a bug when redis-cluster renew slot-node (from 1.0.9)

# 1.0.11（2020/12/09）
### add
* camellia-redis-proxy support setting MonitorCallback
* camellia-redis-proxy support monitor slow command, support setting SlowCommandMonitorCallback
* camellia-redis-proxy support monitor hot key, support setting HotKeyMonitorCallback
* camellia-redis-proxy support hot key local cache(only support GET command), support setting HotKeyCacheStatsCallback 
* camellia-redis-proxy support monitor big key, support setting BigKeyMonitorCallback 
* camellia-redis-proxy support multi-read-resources while rw_separate(will random choose a redis to read)
* CamelliaRedisTemplate support get original Jedis
* RedisProxyJedisPool support side-car mode, if setting true, RedisProxyJedisPool will use side-car-proxy first
* camellia-redis-proxy console support http api(default http://127.0.0.1:16379/monitor) to get metrics（tps、rt、slow command、hot key、big key、hot key cache）
* provide camellia-spring-redis-zk-discovery-spring-boot-starter，so you can use proxy in discovery way easily when your client is SpringRedisTemplate

### update
* update CommandInterceptor define

### fix
* fix NPE for mget when use custom sharding(from 1.0.10)
* fix bug of redis sentinel master switch in proxy

# 1.0.10（2020/10/16）
### add
* camellia-redis-proxy support blocking commands, such as BLPOP/BRPOP/BRPOPLPUSH and so on
* camellia-redis-proxy support stream commands of redis5.0，include blocking XREAD/XREADGROUP
* camellia-redis-proxy support pub-sub commands
* camellia-redis-proxy support set calc commands, such as SINTER/SINTERSTORE/SUNION/SUNIONSTORE/SDIFF/SDIFFSTORE and so on
* camellia-redis-proxy support setting multi-write-mode, provider three options, see com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode
* camellia-redis-proxy provider AbstractSimpleShardingFunc to easily define custom sharding func
* camellia-redis-proxy-hbase support standalone freq of hbase get hit of zmemeber

### update
* camellia-redis-proxy-hbase add prevent when rebuild zset from hbase

### fix
* fix CamelliaHBaseTemplate multi-write bug of batch-delete

# 1.0.9（2020/09/08）
### add
* camellia-redis-proxy-async support redis sentinel
* camellia-redis-proxy-async support monitor command spend time
* camellia-redis-proxy-async support custom CommandInterceptor
* add camellia-redis-zk，provider a default register/discovery implements for camellia-redis-proxy
* camellia-redis-proxy-hbase add standalone freq policy for hbase-get

### update
* modify camellia-redis-proxy's netty default conf sendbuf/rcvbuf，only check channel.isActive() instead of channel.isWritable()
* remove camellia-redis-proxy-sync
* camellia-redis-proxy-async performance improve

### fix
* none

# 1.0.8（2020/08/04）
### add
* camellia-redis-proxy-async support eval/evalsha command
* CamelliaRedisTemplate support eval/evalsha
* CamelliaRedisLock use lua script, more strict lock

### update
* some camellia-redis-proxy optimize

### fix
* none

# 1.0.7（2020/07/16）
### add
* camellia-redis-proxy-hbase add freq policy for hbase-get
* camellia-redis-proxy-hbase add batch restrict for hbase-get/hbase-put     
* camellia-redis-proxy-hbase hbase-write support set ASYNC_WAL  
* camellia-redis-proxy-hbase support null-cache for type command  
* camellia-redis-proxy-hbase add degraded conf, add pure async mode(data may be inconsistency)      

### update
* optimize monitor（use LongAdder instead of AtomicLong）
* camellia-redis-proxy-hbase conf use HashMap instead of Properties(reduce lock competition)  
* some camellia-redis-proxy optimize

### fix
* none

# 1.0.6（2020/05/22）  
### add  
* camellia-redis-proxy-hbase support async write mode, default not enable  
### update  
* optimize RedisProxyJedisPool, add auto-ban for bad proxy address  
* camellia-hbase-spring-boot-starter open remote monitor  
### fix  
* fix camellia-redis-proxy-async commands' reply out-of-order in pipeline

# 1.0.5（2020/04/27）
### add
* add camellia-redis-eureka-spring-boot-starter  
### update
* optimize CamelliaRedisLock  
* optimize camellia-redis-proxy-hbase  
* update camellia-redis-proxy-hbase monitor  
### fix
* fix chinese garbled code in swagger-ui on camellia-dashboard  

# 1.0.4 (2020/04/20)
first deploy  
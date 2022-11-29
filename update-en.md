[中文版](update-zh.md)
# future(TODO)  
* camellia-redis-proxy support a way for Lettuce to use camellia-redis-proxy depends on register-discovery mode easily
* camellia-redis-proxy support client-cache feature of redis6.0
* camellia-redis-proxy support monitor data visualization in prometheus

# 1.1.6（2022/11/23）
### add
* camellia-redis-proxy support ZINTERCARD command
* camellia-redis-proxy support TairZSet、TairHash、TairString commands
* camellia-redis-proxy support RedisJSON commands

### update
* camellia-redis-proxy optimize monitor function on memory/gc
* modify the error code of camellia-dashboard api for ip-permission, thanks [@tasszz2k](https://github.com/tasszz2k)

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
* camellia-redis-proxy add route, resource init in async mode, improve multi-tenancy isolation

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
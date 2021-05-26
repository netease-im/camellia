[中文版](update-zh.md)
# future(TODO)  
* camellia-redis-proxy support key/value custom transfer, you can use this feature in data-encryption/data-compress
* support a way for Lettuce to use camellia-redis-proxy depends on register-discovery mode easily
* support client-cache feature of redis6.0
* support monitor data visualization in prometheus

# 1.0.26（2021/05/xxx）
### add
* camellia-redis-proxy support setting port/applicationName instead of server.port/spring.application.name
* ProxyDynamicConf support setting k-v map instead of read from file

### update
* rename LoggingHoyKeyMonitorCallback to LoggingHotKeyMonitorCallback
* camellia-redis-proxy delete transpond mode of Disruptor/LinkedBlockingQueue, only support direct transpond mode
* camellia-redis-proxy stats log rename logger, add prefix of camellia.redis.proxy., e.g LoggingMonitorCallback.java
* camellia-redis-proxy rename BigKeyMonitorCallback callback method, callbackUpstream/callbackDownstream rename to callbackRequest/callbackReply

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
* camellia-redis-proxy support transaction command, only when proxy route to redis/redis-sentinel with no-shading/no-read-write-separate
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
* fix NPE for mget when use custom shading(from 1.0.10)
* fix bug of redis sentinel master switch in proxy

# 1.0.10（2020/10/16）
### add
* camellia-redis-proxy support blocking commands, such as BLPOP/BRPOP/BRPOPLPUSH and so on
* camellia-redis-proxy support stream commands of redis5.0，include blocking XREAD/XREADGROUP
* camellia-redis-proxy support pub-sub commands
* camellia-redis-proxy support set calc commands, such as SINTER/SINTERSTORE/SUNION/SUNIONSTORE/SDIFF/SDIFFSTORE and so on
* camellia-redis-proxy support setting multi-write-mode, provider three options, see com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode
* camellia-redis-proxy provider AbstractSimpleShadingFunc to easily define custom shading func
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
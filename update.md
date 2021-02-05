[中文版](update-zh.md)
# future(TODO)  
* camellia-redis-proxy support key/value custom transfer, you can use this feature in data-encryption/data-compress
* support a way for Lettuce to use camellia-redis-proxy depends on register-discovery mode easily
* support client-cache feature of redis6.0
* support monitor data visualization in prometheus

# 1.0.19（2020/02/05）
### add
* none  

### update
* performance update of camellia-redis-proxy, see [v1.0.19](/docs/redis-proxy/performance-report-8.md)

### fix
* fix xinfo/xgroup in KeyParser/pipeline

# 1.0.18（2020/01/25）
### add
* add console http api of /reload, so you can reload ProxyDynamicConf by 'curl http://127.0.0.1:16379/reload'
* support HSTRLEN/SMISMEMBER/LPOS/LMOVE/BLMOVE
* support ZMSCORE/ZDIFF/ZINTER/ZUNION/ZRANGESTORE/GEOSEARCH/GEOSEARCHSTORE
* open the dynamic conf function of ProxyDynamicConf, if you setting 'k=v' in file camellia-redis-proxy.properties, then you can call ProxyDynamicConf.getString("k") to get 'v'  

### update
* if proxy setting multi-write, then blocking command will return not support

### fix
* none

# 1.0.17（2020/01/15）
### add
* camellia-redis-proxy support transaction command, only when proxy route to redis/redis-sentinel with no-shading/no-read-write-separate
* support ZPOPMIN/ZPOPMAX/BZPOPMIN/BZPOPMAX

### update
* none

### fix
* fix ReplyDecoder bug of camellia-redis-proxy，proxy will modify nil-MultiBulkReply to empty-MultiBulkReply, find this bug when realize transaction command's support
* fix NPE when ProxyDynamicConf init, this bug does not affect the use of ProxyDynamicConf, only print the error log once when proxy start 

# 1.0.16（2020/01/11）
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
* none

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
* none

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
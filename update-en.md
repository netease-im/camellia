[中文版](update-zh.md)


# 1.4.0 (2026/02/27)
### New Features
* camellia-redis-proxy supports FT._LIST command
* Added camellia-naming module, supports zk and nacos
* Added camellia-bom module
* camellia-cache supports configuring class names for null-cache to facilitate backward compatibility when migrating from other cache frameworks
* HttpClientUtils supports put and delete methods
* hot-key-server supports configuring io_uring, epoll, and kqueue, defaults to auto-selection
* camellia-discovery supports multiple data sources with online switching
* camellia-feign-client supports setting log level during invoke

### Updates
* Upgraded to netty4.2, affects camellia-redis-proxy and camellia-hot-key-server
* hot-key-sdk modified default configuration to reduce default sampling rate and lower resource overhead
* Optimized discovery-client protection logic
* ReloadableLocalFileCamelliaApi no longer depends on file last_modified
* hbase-client version upgraded to 2.4.18
* Using new Java21 syntax, including camellia-redis-proxy, etc.
* camellia-redis-proxy supports custom configuration of redis_version field returned by info command
* Upgraded spring-boot to 3.5.9, startup class changed (breaking change)
* Upgraded bouncycastle to jdk18on series 1.83
* SSLContextUtil migrated from camellia-tools to camellia-redis-proxy-core
* camellia-redis-proxy refactored configuration methods (breaking change)
* Refactored camellia-discovery logic to simplify code (breaking change)
* Streamlined spring-boot-starters to improve code maintainability, some jars deleted (breaking change)
* camellia-redis-proxy routing configuration methods refactored, unified under the same framework (breaking change)
* ResourceTableUpdater renamed to ResourceTableProvider (breaking change)
* The cluster nodes command returns the real cport.

### Fixes
* Fixed issue where camellia-dashboard dirty cache during updates prevented configuration from taking effect immediately


# 1.3.7 (2025/09/28)
### New Features
* camellia-redis-client, pipeline operations support multiKey commands, currently supports mget
* camellia-redis-client, added support for jedis5
* camellia-tools, supports `SimpleConfigFetcher` for easy definition of simple external configuration centers
* camellia-tools, provides a simple HTTP call utility class `HttpClientUtils`
* camellia-redis-client, supports using `SimpleConfigFetcher` for dynamic configuration management
* camellia-hbase-client, supports using `SimpleConfigFetcher` for dynamic configuration management
* camellia-redis-proxy, supports using `SimpleConfigFetcher` for dynamic configuration management
* camellia-hbase-client, supports using custom kv in url

### Updates
* camellia-redis-proxy, kv, optimized GC logic, supports configuring concurrent threads
* camellia-redis-client, optimized logging for redis-cluster-slave

### Fixes
* camellia-delay-queue-sdk-spring-boot, optimized initialization logic to avoid missing CamelliaDelayMsgListener registration when beans depend on each other, thanks [@logan2013](https://github.com/logan2013)
* camellia-redis-proxy/camellia-redis-client, ltrim command should be a write command, thanks [@shenyujia2512](https://github.com/shenyujia2512)
* camellia-redis-client, fixed redis-cluster-slave initialization issue
* camellia-redis-proxy, fixed ConcurrentModificationException in ProxyDynamicConf when executing remove callback in callback
* camellia-redis-client, redis-cluster-slave initialization, username was configured but not used


# 1.3.6 (2025/06/23)
### New Features
* camellia-redis-proxy, kv, supports setting maximum lru-cache capacity based on namespace and name
* camellia-hbase-client, supports custom mode for getting configuration

### Updates
* camellia-redis-proxy, kv, optimized obkv-client batchGet and batchExists performance
* camellia-redis-proxy, optimized /detect implementation under console

### Fixes
* camellia-redis-proxy, fixed scan method logic under multi-read configuration


# 1.3.5 (2025/04/14)
### New Features
* camellia-redis-proxy, supports configuring thread pool for info command
* camellia-redis-proxy, added utility `IpAffinityServerSelector` for IP affinity and anti-affinity during load balancing

### Updates
* camellia-redis-proxy, kv, optimized obkv-client performance for large batch operations
* camellia-redis-proxy, removed camellia-redis-proxy-hbase module

### Fixes
* camellia-redis-proxy, fixed issue where `script load` and other commands didn't take effect on all nodes in sharded mode, thanks [@Ak1yama-mio](https://github.com/Ak1yama-mio)


# 1.3.4 (2025/03/19)
### New Features
* camellia-redis-proxy, kv, added monitoring metrics for hot-key lru-cache load count
* camellia-redis-proxy, added utility method RedisClusterPhysicsNodeTopologyUtils to export node distribution when redis-cluster nodes and cluster are co-deployed

### Updates
* camellia-redis-proxy, optimized error message descriptions for some command parameter errors, thanks [@jzhao20230918](https://github.com/jzhao20230918)
* camellia-redis-proxy, optimized master-slave node update logic when backend is redis-sentinel, thanks [@masteroogway123](https://github.com/masteroogway123)
* camellia-redis-proxy, kv, updated obkv-table-client version to 1.4.2
* camellia-redis-proxy, kv, adjusted some default configurations (effective when zset's encode-version=1)

### Fixes
* camellia-redis-proxy, fixed array out of bounds issue in ClusterModeCommandMoveInvoker#checkSlotInProxyNode method under single slot
* camellia-redis-proxy, kv, fixed issue where zset's score field returned in scientific notation in some cases
* camellia-delay-queue, fixed calculation error in CamelliaDelayQueueServer#getTopicInfo when counting messages, thanks [@kalencaya](https://github.com/kalencaya)


# 1.3.3 (2025/03/05)
### New Features
* camellia-redis-proxy, kv, command-executor enables run-in-completion mode by default
* camellia-redis-proxy, kv, obkv-client supports setting runtimeBatchExecutor and slowQueryMonitorThreshold
* camellia-redis-proxy, kv, supports configuring lru-cache-size based on memory usage, enabled by default (automatically adjusts based on max heap memory)
* camellia-redis-proxy, ReadOnlyProxyPlugin supports enabling/disabling per tenant (enabled by default)

### Updates
* camellia-redis-proxy, optimized pipeline request response logic, improved performance
* camellia-redis-proxy, defaults to netty-native epoll/kqueue when system supports it
* camellia-redis-proxy, MultiWriteProxyPlugin reduced thread switching, optimized performance
* camellia-redis-proxy, kv, added protection logic when data is inconsistent under zset version=1
* camellia-redis-proxy, kv, optimized lru-cache logging
* camellia-redis-proxy, kv, under zset version=1, index write is async by default
* camellia-redis-proxy, kv, obkv-client upgraded to version 1.4.1
* camellia-redis-proxy, kv, added monitoring for gc scan key count
* camellia-redis-proxy, kv, during gc scan, meta-key and sub-key execute concurrently
* camellia-redis-proxy, kv, during gc scan, sub-key deletion adds key version check

### Fixes
* camellia-redis-proxy, when using ConsensusProxyClusterModeProvider deployed as redis-cluster mode, fixed slot calculation error in edge cases


# 1.3.2 (2025/01/15)
### New Features
* camellia-redis-proxy, kv, supports kv-client read/write degradation

### Updates
* None

### Fixes
* camellia-redis-proxy, kv, fixed zset range by rank sorting error


# 1.3.1 (2024/12/23)
### New Features
* camellia-hbase-client, supports obkv-hbase

### Updates
* camellia-redis-proxy, cluster-mode-2, kv scenarios (with strict command redirection enabled), supports smoother node online/offline
* camellia-redis-proxy, executes custom offlineCallback when process exits
* camellia-hot-key-server, executes custom offlineCallback when process exits
* camellia-delay-queue-server, executes custom offlineCallback when process exits
* camellia-id-gen-servers, executes custom offlineCallback when process exits
* Upgraded netty version to `4.1.116`
* Upgraded jctools version to `4.0.5`
* Upgraded obkv-table version to `1.3.0`

### Fixes
* camellia-redis-proxy, kv, fixed initialization failure in some scenarios


# 1.3.0 (2024/12/06)
### New Features
* camellia-redis-proxy, minimum Java version upgraded to Java21, using spring-boot3 as launcher, related SDKs remain at Java8
* camellia-delay-queue, minimum Java version upgraded to Java21, using spring-boot3 as launcher, related SDKs remain at Java8
* camellia-hot-key-server, minimum Java version upgraded to Java21, using spring-boot3 as launcher, related SDKs remain at Java8
* camellia-id-gen-server, minimum Java version upgraded to Java21, using spring-boot3 as launcher, related SDKs remain at Java8
* camellia-redis-client, supports transparent reading of redis-cluster slave nodes
* camellia-redis-proxy, kv module, when zset uses encode-version1 encoding, supports async write to kv
* camellia-redis-proxy, supports adding password to cport port, so pseudo-redis-sentinel and pseudo-redis-cluster node heartbeats can add authentication
* camellia-redis-proxy, health check during read-write separation supports automatically removing inactive backends
* camellia-redis-proxy, kv module, supports disabling hot-key calculation logic
* camellia-redis-proxy, kv module, supports different hot-key configurations for different namespaces

### Updates
* camellia-redis-proxy, adjusted custom implementation class configuration names, supports both `xxx.xxx.className` and `xxx.xxx.class.name`
* camellia-redis-proxy, when using `proxy config broadcast` command to broadcast configuration changes, supports not overwriting specific configuration files
* camellia-redis-proxy, kv module, optimized custom configuration read cache performance
* camellia-redis-proxy, kv module, when zset uses encode-version1 encoding, optimized zadd command performance
* camellia-redis-proxy, `proxy` limited to access via cport only
* camellia-mq-isolation, when topic and namespace don't match, logs but continues task execution instead of throwing exception
* camellia-mq-isolation, optimized controller select mq info logic to match better results even when dependent redis is abnormal
* camellia-dashboard/camellia-console/camellia-config, removed swagger
* camellia-redis-proxy, kv module, modified obkv table structure, no longer uses ttl table for better performance (breaking change)
* camellia-core, optimized ReloadableProxyFactory reload logic

### Fixes
* camellia-redis-proxy, kv module, incorrect return format when key doesn't exist in zrem method
* camellia-redis-proxy, logic error when publish command wrapped in transaction (only affects when backend is redis-cluster)
* camellia-redis-client, incorrect db parameter when using CamelliaRedisProxyResource resource type
* camellia-redis-proxy, incorrect initialization logic when backend is `redis-proxies-discovery://username:passwd@proxyName`
* camellia-redis-proxy, pubsub timeout issue in some scenarios

# 1.2.30 (2024/09/14)
### New Features
* camellia-redis-proxy, plugin module, added built-in KeyPrefixMultiWriteFunc for MultiWriteProxyPlugin, as default
* camellia-redis-proxy, kv module, supports using run-to-completion to optimize read command performance

### Updates
* camellia-redis-proxy, plugin module, MultiWriteProxyPlugin, config item name changed from `multi.write.func.className` to `multi.write.func.class.name`
* camellia-redis-proxy, kv module, optimized lru cache cleanup logic when nodes go online/offline
* camellia-redis-proxy, kv module, optimized ZSetIndexLRUCache slot calculation
* camellia-http-accelerate-proxy, removed this module

### Fixes
* camellia-redis-proxy, cluster module, optimized and fixed some edge cases in cluster-mode-2


# 1.2.29 (2024/09/06)
### New Features
* camellia-redis-proxy, kv module, refactored KVClient interface definition, added slot parameter, updated underlying storage encoding (breaking, incompatible with 1.2.28), to simultaneously support range partition and hash partition kv storage
* camellia-redis-proxy, kv module, added support for set-related commands including: `sadd`, `srem`, `smembers`, `spop`, `srandmember`, `sismember`, `smismember`, `scard`
* camellia-redis-proxy, kv module, added support for scan command
* camellia-redis-proxy, kv module, supports configuring different kv backends for different namespaces
* camellia-redis-proxy, kv module, supports electing one node through redis to execute gc, no need to start a separate node for gc
* camellia-redis-proxy, added support for `client kill id xxx`, `client kill addr xxx`, `client kill laddr xxx` commands
* camellia-redis-client, added support for smismember method (camellia-redis3)
* camellia-redis-client, eval command and executeWrite command support different MultiWriteType

### Updates
* camellia-redis-proxy, kv module, removed encode-version 2 and 3 implementations for hash and zset, simplified code structure
* camellia-redis-proxy, kv module, obkv directly uses ObTableClient instead of OHTableClient
* camellia-redis-proxy, kv module, adjusted some configuration keys like encode-version
* camellia-redis-proxy, kv module, enhanced monitoring features
* camellia-redis-proxy, kv module, optimized zset lru cache performance and cache rebuild logic
* camellia-redis-proxy, kv module, updated kv-client's scanByStartEnd and countByStartEnd method definitions, added prefix parameter
* camellia-redis-proxy, kv module, optimized HBaseKVClient's scanByPrefix/countByPrefix/countByStartEnd methods, reduced invalid data scanning
* camellia-redis-proxy, kv module, upgraded tikv-client dependency (old version had bugs)
* camellia-redis-proxy, optimized response when command not supported
* camellia-redis-proxy-bootstrap supports using maven profile to compile different features
* Refactored multi-write related code, affects camellia-redis-client, camellia-hbase-client, camellia-feign-client

### Fixes
* camellia-redis-proxy, kv module, issue where zset's score field returned in scientific notation
* camellia-redis-proxy, kv module, zset's zrevrangebyscore method infinite loop in some cases
* camellia-redis-proxy, cluster-mode-2, in some cases after leader re-election, new leader didn't refresh slot-map to storage


# 1.2.28 (2024/07/29)
### New Features
* camellia-redis-proxy, supports using **distributed kv storage to simulate redis protocol**, such as hbase, tikv, obkv, etc.
* camellia-redis-proxy, provides new pseudo-redis-cluster mode, recommended when using **distributed kv storage to simulate redis protocol** for smoother scaling, new mode also supports deployment without pre-determined IPs, suitable for k8s
* camellia-redis-proxy, supports custom ProxySentinelModeNodesProvider for pseudo-redis-sentinel mode deployment without pre-determined IPs, suitable for k8s
* camellia-redis-proxy, added support for time command
* camellia-redis-proxy, when using nacos and etcd as configuration centers, supports using local config files for node-level special configs
* camellia-redis-proxy, supports keys and randomkey commands under read-write separation when write address is redis-standalone or redis-sentinel
* camellia-redis-proxy, supports transaction series commands under read-write separation, all requests go to write address
* camellia-redis-proxy, supports plugin command redirection at reply stage
* camellia-core, supports setting failure queues for failed methods for custom upper-layer handling
* camellia-mq-isolation, optimized related features including performance and auto-isolation mechanism
* camellia-config, supports 64k configuration items
* camellia-feign, supports custom retry strategy
* camellia-redis-toolkit, added CamelliaRedisReadWriteLock utility class
* camellia-tools, CamelliaHashedExecutor supports configuring initialization callback for custom logic during worker thread initialization
* Supports configuring prometheus metrics prefix, including redis-proxy, hot-key, id-gen, delay-queue

### Updates
* Upgraded netty version to 4.1.108.Final
* Upgraded netty-incubator-transport-native-io_uring version to 0.0.25.Final
* Upgraded netty-incubator-codec-native-quic version to 0.0.62.Final
* camellia-redis-proxy, optimized logging for easier troubleshooting
* camellia-redis-proxy, optimized lazy loading feature
* camellia-redis-proxy, refactored and optimized pseudo-cluster and pseudo-sentinel mode code structure
* camellia-redis-toolkit, CamelliaRedisLock uses ReentrantLock instead of synchronized
* Optimized health check api, including id-gen, delay-queue, mq-isolation modules
* Optimized spring-boot-starter startup console-server flow, including redis-proxy and hot-key
* Refactored InetUtils#findFirstNonLoopbackAddress to adapt to more general configuration scenarios
* Supports using unified config to identify local IP, including pseudo-cluster mode, pseudo-sentinel mode, proxy command, upstream address replacement, etc.

### Fixes
* camellia-cache, fixed issue with spring-boot3 not taking effect


# 1.2.27 (2024/03/13)
### New Features
* camellia-redis-proxy added support for `MOVE`, `CF.RESERVE`, `BF.CARD`, `BF.RESERVE` commands
* camellia-redis-proxy added support for `GEORADIUSBYMEMBER_RO`, `GEORADIUS_RO` commands, thanks [@wozaizhe55](https://github.com/wozaizhe55)
* camellia-redis-proxy added support for `CLUSTER KEYSLOT` command, thanks [@ttiantian006](https://github.com/ttiantian006)
* camellia-tools added `CamelliaCircuitBreakerManager` utility class, thanks [@hkj-07](https://github.com/hkj-07)
* Added mq-isolation module (alpha preview)

### Updates
* camellia-redis-proxy added docker-compose/k8s related documentation, thanks [@48N6E](https://github.com/48N6E)

### Fixes
* camellia-redis-proxy fixed issue with shard pubsub commands not working
* camellia-redis-proxy fixed memory leak


# 1.2.26 (2024/02/04)
### New Features
* camellia-redis-proxy added functionality to execute commands via HTTP
* camellia-redis-proxy added ReadOnlyProxyPlugin plugin, intercepts write commands when enabled
* camellia-redis-proxy added `READONLY` command, directly returns `ok`

### Updates
* camellia-id-gen modified metrics return, interface monitoring granularity no longer at tag level

### Fixes
* None


# 1.2.25 (2024/01/11) (1.2.24 related jars corrupted in maven central, redeployed with new version)
### New Features
* None

### Updates
* camellia-redis-proxy `quit` command, replies `ok` before closing connection instead of directly closing
* camellia-redis-proxy, cport supports `quit` command when pseudo-redis-sentinel mode enabled

### Fixes
* camellia-redis-proxy, `proxy` command not working in pseudo-redis-sentinel mode


# 1.2.23 (2024/01/02)
### New Features
* camellia-redis-proxy supports lazy loading of backend redis connections

### Updates
* camellia-redis-proxy optimized connection management logic under sentinel-mode

### Fixes
* None


# 1.2.22 (2023/12/29)
### New Features
* camellia-redis-proxy enhanced prometheus/grafana monitoring capabilities
* camellia-hot-key enhanced prometheus/grafana monitoring capabilities
* camellia-delay-queue added prometheus/grafana monitoring capabilities
* camellia-id-gen added prometheus/grafana monitoring capabilities
* camellia-redis-proxy supports pseudo-redis-sentinel mode to simulate sentinel master-slave failover for HA clusters
* camellia-redis-proxy supports using -D startup parameter to specify custom config files

### Updates
* camellia-redis-proxy info command, added redis_mode field return
* camellia-redis-proxy info command, camellia_redis_proxy_version field renamed to camellia_version
* camellia-redis-proxy hello command, added version, redis_version and other fields for compatibility with some redis clients (e.g. higher version lettuce)
* camellia-delay-queue-sdk, modified logging to avoid initialization errors under jdk17

### Fixes
* camellia-redis-proxy info command, collection_time field result was incorrect
* CamelliaRedisTemplate, when using redis-sentinel/redis-sentinel-slaves, if sentinel has password, may cause master-slave failover failure


# 1.2.21 (2023/12/19)
### New Features
* camellia-redis-spring-boot-starters, supports spring-boot3
* camellia-cache-spring-boot-starters, supports spring-boot3
* camellia-delay-queue-spring-boot-starters, supports spring-boot3
* camellia-feign-spring-boot-starters, supports spring-boot3
* camellia-hbase-spring-boot-starters, supports spring-boot3
* camellia-id-gen-spring-boot-starters, supports spring-boot3
* camellia-hot-key-spring-boot-starters, supports spring-boot3
* camellia-config-spring-boot-starters, supports spring-boot3
* camellia-redis-proxy-spring-boot-starters, supports spring-boot3

### Updates
* None

### Fixes
* None


# 1.2.20 (2023/12/05)
### New Features
* camellia-redis-proxy added `PROXY` command to manage all other nodes' dynamic configurations through any proxy node, eliminating dependence on centralized config centers like etcd/nacos
* camellia-redis-proxy added support for CuckooFilter series commands

### Updates
* camellia-redis-proxy, when using shard-pubsub commands, backend may return `MOVED XXX` error, proxy should rewrite error message to avoid exposing upstream IP to client
* camellia-redis-proxy, when client connection idle detection enabled and it's a blocking command like blpop, supports configuring whether to close connection
* camellia-redis-proxy, when connecting to camellia-dashboard, changed from HttpURLConnection to OkHttpClient as underlying http-client
* camellia-redis-proxy, when connecting to camellia-dashboard, supports using v2-api to reduce http-body size
* camellia-redis-proxy, when using local/complex config, added trim logic when reading config files
* camellia-redis-proxy, `info memory` added netty_direct_memory field

### Fixes
* camellia-redis-proxy, fixed memory leak issue, only occurs when proxy_protocol_v2 protocol enabled to get real client IP
* camellia-redis-proxy, fixed MonitorProxyPlugin NPE issue, only occurs when unsupported commands appear


# 1.2.19 (2023/11/07)
### New Features
* Added camellia-redis-proxy-nacos-bootstrap for easy operation management without java development when using nacos
* Added camellia-redis-proxy-etcd-bootstrap for easy operation management without java development when using etcd
* camelia-feign, added GlobalCamelliaFeignEnv class for load balancing calculation in advance when using hash strategy, enabling service instance distribution visibility before invocation for request merging
* camellia-redis-proxy console added `/shutdownUpstreamClient?url=redis://@127.0.0.1:6379` interface for unloading connections to redis cluster
* camellia-redis-proxy supports shard-pubsub, i.e. supports `SSUBSCRIBE/SUNSUBSCRIBE/SPUBLISH` commands
* camellia-redis-proxy supports `FUNCTION/TFUNCTION` related commands
* camellia-redis-proxy supports `SCRIPT KILL` command
* camellia-redis-proxy supports `EXPIRETIME/PEXPIRETIME/LCS/SINTERCARD/SORT_RO/BITFIELD_RO` commands

### Updates
* None

### Fixes
* camellia-redis-proxy, eval_ro and evalsha_ro commands not routed correctly when backend is custom sharding or redis-cluster
* camellia-redis-proxy, when using eval/eval_sha/eval_ro/evasha_ro with key count 0 and backend is redis-cluster, returns slot error


# 1.2.18 (2023/10/25)
### New Features
* camellia-redis-proxy supports custom UpstreamAddrConverter to modify backend addresses (IP or UDS path), typical use case: when proxy and redis co-located on same machines, use UDS or 127.0.0.1 for local node access, LAN IP for remote nodes

### Updates
* camellia-feign, DynamicCamelliaFeignDynamicOptionGetter supports setting load balancing strategy

### Fixes
* None


# 1.2.17 (2023/10/10)
### New Features
* camellia-config/camellia-console supports custom ConfigChangeInterceptor for controlling configuration change flow (e.g. approval)
* camellia-redis-proxy enhanced plugin functionality, allows selecting custom routing for individual commands
* camellia-redis-proxy built-in HotKeyRouteRewriteProxyPlugin allows custom routing for hot keys
* camellia-redis-proxy supports unix-domain-socket, both client-to-proxy and proxy-to-redis

### Updates
* camellia-redis-proxy when configuring connection limit, returns error before closing connection when limit reached, supports delayed close
* Optimized CamelliaStrictIdGen peekId interface performance
* camellia-redis-proxy backend connection sendbuf and rcvbuf default config adjusted from 10M to 6M
* camellia-redis-proxy optimized MultiWriteProxyPlugin implementation

### Fixes
* None


# 1.2.16 (2023/09/04)
### New Features
* None

### Updates
* None

### Fixes
* camellia-delay-queue, message send ttl field should mean survival time after message delay arrival, not survival time after message send, thanks [fuhaodev](https://github.com/fuhaodev) for discovering this issue


# 1.2.15 (2023/09/01)
### New Features
* camellia-redis-proxy supports `client info` and `client list` commands
* camellia-redis-proxy supports enabling `proxy_protocol` to get real client IP and port when using L4 proxies like haproxy

### Updates
* Refactored FileUtil to FileUtils
* camellia-redis-proxy when proxying to redis-cluster, avoid passing error message back to client when ASK/MOVED exceeds limit
* camellia-redis-proxy supports PKCS8 SSL/TLS certificates, thanks [HelloWorld1018](https://github.com/HelloWorld1018) for this feature

### Fixes
* camellia-redis-proxy, mutual authentication not working when client-to-proxy TLS enabled, thanks [@InputOutputZ](https://github.com/InputOutputZ) for discovering this bug
* Fixed issue where some config files failed to read when camellia-redis-proxy packaged as fatjar


# 1.2.14 (2023/08/18)
### New Features
* camellia-redis-proxy supports TLS for client-to-proxy connections, thanks [HelloWorld1018](https://github.com/HelloWorld1018) for testing and bugfix
* camellia-redis-proxy supports TLS for proxy-to-redis connections, thanks [HelloWorld1018](https://github.com/HelloWorld1018) for testing and bugfix
* camellia-redis-proxy when using `info upstream-info` command to get backend info, `redis-sentinel` also supports using `sentinelUserName` and `sentinelPassword`
* camellia-id-gen added CamelliaStrictIdGen2, implements strictly increasing sequence based on redis/ntp timestamp
* camellia-redis-proxy supports local config files in json format

### Updates
* camellia-redis-proxy console supports disabling, set port to 0
* camellia-http-accelerate-proxy uses bbr as default congestion control algorithm when using quic (previously config default)
* Optimized camellia-redis-proxy warmup logic, startup fails if warmup fails
* camellia-redis-proxy when proxying to redis-sentinel, added logic to periodically get master node as fallback for master change subscription
* redis-sentinel/redis-proxies support adaptive node list refresh

### Fixes
* camellia-redis-proxy console random port feature not working, introduced in 1.2.11
* camellia-redis-proxy when using sentinelPassword, printed logs should also mask password
* camellia-redis-proxy fixed NPE when redis-cluster master-slave failover (doesn't affect failover)
* camellia-redis-proxy when proxying to sentinel and `+reset-master` appears, didn't switch, thanks [segment11](https://github.com/segment11) for discovering this issue


# 1.2.13 (2023/08/04)
### New Features
* camellia-http-accelerate-proxy supports setting backupServer
* camellia-redis-proxy when integrating nacos as config center, added support for json format, default still previous properties format
* camellia-redis-proxy supports integrating etcd as config center, supports json/properties formats, default properties format
* camellia-hot-key-server supports integrating etcd as config center, supports json/properties formats, default json format
* camellia-hot-key supports `not_contains` rule type
* Added MultiTenantProxyRouteConfProvider and MultiTenantClientAuthProvider, providing simpler multi-tenant configuration solution
* camellia-htt-accelerate-proxy supports setting quic congestion.control.algorithm
* camellia-redis-proxy multi-write-mode config migrated from yml to ProxyDynamicConf, supports tenant-level configuration and dynamic changes

### Updates
* `ProxyDynamicConfLoader` renamed method `updateInitConf` to `init`
* `camellia-redis-proxy-nacos` renamed artifactId to `camellia-redis-proxy-config-nacos`
* `com.netease.nim.camellia.redis.proxy.nacos.NacosProxyDynamicConfLoader` renamed to `com.netease.nim.camellia.redis.proxy.config.nacos.NacosProxyDynamicConfLoader`

### Fixes
* camellia-redis-proxy when proxying to redis-cluster, select 0 shouldn't return error, bug introduced in 1.2.1


# 1.2.12 (2023/07/28)
### New Features
* camellia-http-accelerate-proxy, proxy and transport-server support setting bound host (default 0.0.0.0)
* camellia-http-accelerate-proxy, transport-route and upstream-route support disabling
* camellia-redis3(CamelliaRedisTemplate), supports setting auth account (redis-standalone, redis-sentinel, redis-sentinel-slaves, redis-cluster)
* camellia-redis(CamelliaRedisTemplate), redis-sentinel, redis-sentinel-slaves support setting sentinelPassword
* camellia-redis3(CamelliaRedisTemplate), redis-sentinel, redis-sentinel-slaves support setting sentinelUserName and sentinelPassword
* camellia-redis-proxy's redis-sentinel, redis-sentinel-slaves support setting sentinelUserName and sentinelPassword
* camellia-redis3(CamelliaRedisTemplate), supports zmscore
* camellia-http-accelerate-proxy supports using quic as forwarding layer protocol
* camellia-http-accelerate-proxy supports compressing http-content transmission
* camellia-codec added XProps utility class, more memory efficient than Props in some cases

### Updates
* camellia-hot-key-sdk when using ConcurrentHashMapCollector and key is full, log changed from error to info
* Optimized camellia-http-console and camellia-http-accelerate-proxy response packet connection header logic
* Used direct-buffer to optimize camellia-hot-key and camellia-http-accelerate-proxy packaging
* camellia-hot-key rule supports suffix matching

### Fixes
* None


# 1.2.11 (2023/07/19)
### New Features
* camellia-tools added CamelliaSegmentStatistics utility class
* camellia-cache added global on/off switch
* Added camellia-http-console module, a simple http-server
* CamelliaRedisTemplate supports setting custom RedisInterceptor for easy integration with CamelliaHotKeyMonitorSdk
* Added camellia-codec module
* Added camellia-http-accelerate-proxy module
* camellia-redis-proxy GlobalRedisProxyEnv added ProxyShutdown entry for releasing ports and connections

### Updates
* camellia-redis-proxy uses camellia-http-console instead of original console implementation
* camellia-hot-key-server uses camellia-http-console instead of original console implementation
* camellia-hot-key uses camellia-codec module
* camellia-hot-key-sdk supports setting collector type, including Caffeine (default), ConcurrentLinkedHashMap, ConcurrentHashMap
* camellia-hot-key-sdk supports setting as async collection, default is sync mode

### Fixes
* camellia-redis-proxy when printing error logs for failed commands, resource in log didn't mask password
* camellia-hot-key-sdk get hot-key-config checkThreshold incorrect (doesn't affect functionality)
* camellia-hot-key-server fixed hot key detection logic error


# 1.2.10 (2023/06/07)
### New Features
* camellia-redis-proxy supports integrating camellia-hot-key, thanks[@21want28k](https://github.com/21want28k) for this feature
* CamelliaHotKeyCacheSdk added several APIs, thanks[@21want28k](https://github.com/21want28k) for this feature

### Updates
* camellia-hot-key set ConcurrentLinkedQueue as default memory queue, improved performance
* camellia-hot-key removed Caffeine expire strategy in HotKeyCounterManager to avoid performance degradation
* camellia-hot-key-server supports setting number of Caffeine instances per namespace, breaking through Caffeine single instance performance limit in some scenarios

### Fixes
* camellia-hot-key-server fixed `unknown seqId` error
* CamelliaHotKeyCacheSdk fixed namespace error, thanks[@21want28k](https://github.com/21want28k) for discovering this bug
* camellia-redis-proxy-discovery-zk 1.2.8/1.2.9 incompatible with 1.2.7 and earlier versions, 1.2.10 added compatibility logic


# 1.2.9 (2023/06/02)
### New Features
* camellia-redis-proxy when backend redis fails, supports printing redis address, command and related keys to log file

### Updates
* camellia-redis-proxy, camellia-delay-queue-server, camellia-id-gen-server added online/offline callbacks
* ZkProxyRegistry/ZkHotKeyServerRegistry register online/offline callbacks
* CamelliaHashedExecutor added hashIndex method to get thread index calculated from hashKey

### Fixes
* CamelliaHotKeyCacheSdkConfig removed namespace field, CamelliaHotKeyCacheSdk namespace field should come from method parameter
* camellia-hot-key-server graceful online/offline didn't check for traffic
* CamelliaHotKeyCacheSdk fixed issue where notifications from keyDelete/keyUpdate weren't sent to other clients
* TopNCounter buffer not cleared after each large cycle calculation
* TopNCounter fixed incorrect maxQps calculation


# 1.2.8 (2023/05/29)
### New Features
* Added camellia-hot-key module, see: [hot-key](/docs/camellia-hot-key/hot-key.md)
* Added camellia-zk module, both camellia-redis-proxy-zk and camellia-hot-key-zk reference camellia-zk for code reuse

### Updates
* camellia-redis-proxy client-facing connection, tcp_keepalive parameter defaults to true
* camellia-config namespace info field, mysql storage field changed from varchar to text

### Fixes
* camellia-redis-proxy `/prometheus` interface line break changed from `%n` to `\n` for windows compatibility
* camellia-redis-proxy when client connection in pubsub state and backend redis down or closed connection to proxy for some reason, proxy should synchronously close connection to client
* camellia-redis-proxy fixed issue where frequent ping commands or sub/unsub caused subscription invalidation when connection in subscribed state
* camellia-redis-proxy force close client connection when backend redis unavailable causes client subscription failure


# 1.2.7 (2023/05/04)
### New Features
* None

### Updates
* None

### Fixes
* Fixed CamelliaRedisLockManager potential auto-renew task leak in concurrent scenarios (logic correct but caused extra CPU overhead), affects camellia-delay-queue-server
* Fixed camellia-redis-proxy RedisConnection heartbeat and idle detection scheduled tasks, in some cases when using transaction/pubsub commands, tasks might run unnecessarily when connection already closed


# 1.2.6 (2023/04/28)
### New Features
* camellia-redis-proxy supports dual-write for TRANSACTION commands, see: [multi-write](/docs/camellia-redis-proxy/other/multi-write.md)
* camellia-tools added CamelliaScheduleExecutor utility class
* RateLimitProxyPlugin supports setting default rate limit per tenant, see: [rate-limit](/docs/camellia-redis-proxy/plugin/rate-limit.md)

### Updates
* camellia-redis-proxy supports reusing CommandPack, optimized GC
* camellia-config config key server added trim logic
* camellia-config adjusted `/getConfigString` interface return
* CamelliaLoadingCache added max execution time control during cache penetration
* camellia-redis-proxy refined error description returned to client when backend redis abnormal
* camellia-redis-proxy in pseudo-redis-cluster mode, `cluster nodes` command return line break should be `\n` not `\r\n`
* CamelliaRedisLockManager uses CamelliaScheduleExecutor instead of ScheduledExecutorService
* camellia-redis-proxy RedisConnection uses CamelliaScheduleExecutor instead of ScheduledExecutorService for idle detection and heartbeat detection
* camellia-redis-proxy optimized heartbeat logic in pseudo-redis-cluster mode

### Fixes
* Fixed camellia-config related interface SQL error
* camellia-redis-proxy when connection in TRANSACTION or SUBSCRIBE state, ping command should pass through to backend not respond directly
* camellia-redis-proxy fixed issue where normal commands had no response after frequent SUBSCRIBE and normal state switching
* camellia-redis-proxy fixed issue where commands had no response after connection changed from SUBSCRIBE to normal then used blocking commands
* camellia-redis-proxy fixed issue where transaction logic error occurred when TRANSACTION command wrapped normal command with key slot calculated as 0 when proxying to redis-cluster


# 1.2.5 (2023/04/07)
### New Features
* None

### Updates
* camellia-redis-proxy built-in memory queue supports using jctools high-performance queue for performance optimization
* camellia-redis-proxy in pseudo-redis-cluster mode, optimized MOVED instruction logic during proxy cluster scaling

### Fixes
* camellia-redis-proxy optimized renew logic when proxying to redis-cluster (introduced in 1.2.0, caused delayed route table refresh after redis node failure), thanks[@saitama-24](https://github.com/saitama-24) for discovering this issue


# 1.2.4 (2023/04/03)
### New Features
* Added camellia-config module, a simple kv config center, see: [camellia-config](/docs/camellia-config/config.md)
* camellia-redis-proxy added NacosProxyDynamicConfLoader, a new method for integrating nacos, see: [dynamic-conf](/docs/camellia-redis-proxy/conf/dynamic-conf.md)
* camellia-redis-proxy built-in ProxyPlugin supports custom execution order (order), see: [plugin](/docs/camellia-redis-proxy/plugin/plugin.md)

### Updates
* camellia-redis-proxy optimized RedisConnection implementation
* camellia-redis-proxy supports integrating camellia-config
* camellia-feign supports integrating camellia-config
* camellia-redis-proxy PUBSUB series command responses also need to be counted in upstream-fail
* camellia-redis-proxy-hbase memory queue supports dynamic capacity adjustment
* camellia-delay-queue-server scheduled tasks added single-machine concurrency control to optimize resource usage
* Optimized IPMatcher implementation to handle `10.22.23.1/24` judgment

### Fixes
* Fixed camellia-redis-proxy when using custom custom routing mode, automatic removal of abnormal backends in multi-read scenarios not working
* Fixed camellia-redis-proxy when using both converterPlugin key conversion and hotKeyCachePlugin, hot key cache not working


# 1.2.3 (2023/03/15)
### New Features
* camellia-redis-proxy supports statistics of request failures by backend resource, see: [monitor-data](/docs/camellia-redis-proxy/monitor/monitor-data.md)

### Updates
* camellia-redis-proxy refined error description returned to client when backend redis abnormal
* camellia-redis-proxy /prometheus endpoint adjusted type of some metrics
* camellia-redis-proxy optimized RedisConnection state judgment implementation logic

### Fixes
* Fixed camellia-redis-proxy when using info upstream-info command, backend redis address password not masked


# 1.2.2 (2023/02/28)
### New Features
* None

### Updates
* Refactored ProxyDynamicConf, supports custom loader

### Fixes
* Fixed RedisConnection not closing connection after heartbeat exception (introduced in 1.2.0)


# 1.2.1 (2023/02/22)
### New Features
* redis-proxies and redis-proxies-discovery two redis-resource types support setting db, including camellia-redis-proxy and CamelliaRedisTemplate
* camellia-redis-proxy supports select command, currently only supports setting non-0 db when backend is redis-standalone/redis-sentinel/redis-proxies or their sharding/read-write separation combinations, if backend has redis-cluster type resource, only supports select 0
* CamelliaRedisTemplate supports RedisProxiesDiscoveryResource resource

### Updates
* camellia-redis-proxy-hbase supports configuring upstream.redis.hbase.command.execute.concurrent.enable (default false) to improve client pipeline batch command execution efficiency, but requires client to use blocking submission mode, otherwise may cause out-of-order command execution
* Renamed DefaultTenancyNamespaceKeyConverter -> DefaultMultiTenantNamespaceKeyConverter

### Fixes
* Fixed dependency error when using jedis3+SpringRedisTemplate+zk/eureka to access proxy (packaged with jedis2 causing class not found)
* Fixed bid/bgroup not working when using CamelliaRedisProxyZkFactory+CamelliaRedisTemplate to access camellia-redis-proxy
* CamelliaRedisTemplate pipeline method should be read method not write method: Response<Long> zcard(String key)


# 1.2.0 (2023/02/14)
### New Features
* Added camellia-redis3 module and related modules, supports jedis3.x (default v3.6.3)
* Combined common parts of camellia-redis-client and camellia-redis-proxy into camellia-redis-base module, so camellia-redis-proxy no longer depends on camellia-redis-client
* camellia-redis-proxy supports custom upstream module, core interfaces: IUpstreamClientTemplate and IUpstreamClientTemplateFactory
* camellia-redis-proxy-hbase cold-hot separation storage, changed from custom CommandInvoker to custom upstream, for more code reuse and modified thread model (added worker thread pool and netty-worker thread pool isolation)
* camellia-tools added CamelliaLinearInitializationExecutor, supports asynchronous linear initialization of resources
* camellia-redis-proxy introduced CamelliaLinearInitializationExecutor, refactored multi-tenant upstream initialization logic
* camellia-redis-proxy in multi-read scenarios, supports health check of backends and automatically removes failed nodes
* camellia-redis-proxy supports asynchronous initialization of backend redis connections
* camellia-redis-proxy supports monitoring second-level qps
* camellia-redis-proxy when proxying to redis-cluster, added fallback scheduled renew in addition to MOVED/disconnect triggered renew, default 600s once
* camellia-hbase supports setting userName and password in URL and aliyun-lindorm marker
* camellia-redis-proxy optimized failover logic for redis-cluster-slaves and redis-sentinel-slaves resources when nodes fail
* camellia-redis-proxy optimized failover logic for redis-proxies and redis-proxies-discovery resources when nodes fail
* camellia-redis-proxy supports configuring dynamic.conf.file.name in application.yml to replace camellia-redis-proxy.properties file

### Updates
* camellia-redis-proxy related core classes renamed (upstream part)
* camellia-redis removed CamelliaRedisTemplate to SpringRedisTemplate adapter
* camellia-redis removed CamelliaRedisTemplate to Jedis adapter
* Added camellia-redis-toolkit module, separated toolkit related features from camellia-redis (like distributed locks) for reuse by camellia-redis3
* Using package startup (redis-proxy, delay-queue, id-gen-server) added camellia banner

### Fixes
* camellia-redis-proxy fixed issue where configuration cleared when using ProxyDynamicConf#reload(Map) method to directly set custom variables (instead of based on camellia-redis-proxy.properties file), introduced in 1.1.8


# 1.1.14 (2023/02/01) (1.1.13 related jars corrupted in maven central, redeployed with new version)
### New Features
* camellia-redis-proxy supports using transport_native_epoll, transport_native_kqueue, transport_native_io_uring, defaults to jdk_nio, see: [netty-conf](/docs/camellia-redis-proxy/other/netty-conf.md)
* camellia-redis-proxy supports configuring TCP_QUICKACK parameter, currently only when using transport_native_epoll, thanks[@tain198127](https://github.com/tain198127), see: [netty-conf](/docs/camellia-redis-proxy/other/netty-conf.md), related issue: [issue-87](https://github.com/netease-im/camellia/issues/87)
* RedisProxyJedisPool added AffinityProxySelector, supports affinity configuration, thanks[@tain198127](https://github.com/tain198127) for this feature

### Updates
* id-gen-sdk underlying thread pool defaults to shared mode, reducing thread count when initializing multiple sdk instances
* delay-queue-sdk underlying thread pool defaults to shared mode, reducing thread count when initializing multiple sdk instances
* RedisProxyJedisPool underlying thread pool defaults to shared mode, reducing thread count when initializing multiple instances
* id-gen-server added bootstrap module, providing directly runnable installation package
* delay-queue-server added bootstrap module, providing directly runnable installation package

### Fixes
* Fixed CamelliaStatistics calculation error when count=0


# 1.1.12 (2023/01/12)
### New Features
* None

### Updates
* Rolled back BulkReply off-heap memory optimization from 1.1.8 (when client connection disconnects before receiving reply, may cause BulkReply's ByteBuf not to be released, causing memory leak)

### Fixes
* Fixed camellia-redis-proxy, when client connection disconnects before receiving reply, may cause BulkReply's ByteBuf not to be released, introduced in 1.1.8


# 1.1.11 (2023/01/10)
### New Features
* camellia-redis-proxy added support for prometheus/grafana, thanks[@tasszz2k](https://github.com/tasszz2k), see: [prometheus-grafana](/docs/camellia-redis-proxy/monitor/prometheus-grafana.md)
* camellia-tools added CamelliaDynamicExecutor and CamelliaDynamicIsolationExecutor utility classes, and thread pool monitoring utility CamelliaExecutorMonitor

### Updates
* Some utils classes under camellia-core moved to camellia-tools
* camellia-redis-proxy optimized DefaultTenancyNamespaceKeyConverter behavior with hashtag keys to accommodate more scenarios, thanks[@phuc1998](https://github.com/phuc1998) and [@tasszz2k](https://github.com/tasszz2k)

### Fixes
* Fixed camellia-redis-proxy when using TRANSACTION series commands with high client qps, causing backend redis connection leak, thanks[@phuc1998](https://github.com/phuc1998) and [@tasszz2k](https://github.com/tasszz2k) for discovering this bug
* Fixed camellia-redis-proxy reply encoding/decoding concurrency issue when using PUBSUB series commands, introduced in 1.1.8


# 1.1.10 (2023/01/03)
### New Features
* camellia-redis-proxy provides DynamicRateLimitProxyPlugin, supports dynamic configuration via camellia-dashboard, thanks[@tasszz2k](https://github.com/tasszz2k)

### Updates
* Adjusted project maven structure
* Renamed camellia-redis-proxy artifactId to camellia-redis-proxy-core, camellia-redis-proxy became directory

### Fixes
* Fixed CamelliaRedisTemplate select db not working when using RedisResource
* Fixed camellia-delay-queue Chinese character encoding issue, thanks[@ax3353](https://github.com/ax3353)


# 1.1.9 (2022/12/21)
### New Features
* Added camellia-cache module, enhances spring-cache, see: [cache](/docs/camellia-cache/cache.md)
* camellia-redis-proxy added support for LMPOP and BLMPOP commands (redis7.0)

### Updates
* Optimized camellia-id-gen-sdk, any exception in id-gen-server (not just network exceptions) triggers node masking and request retry

### Fixes
* camellia-delay-queue getMsg interface, when message already consumed and still in cache, result is 200 but no message content


# 1.1.8 (2022/12/13)
### New Features
* camellia-redis-proxy supports configuring ProxyDynamicConf custom k-v config items via application.yml (lower priority than camellia-redis-proxy.properties)
* camellia-redis-proxy provides DefaultTenancyNamespaceKeyConverter, supports key namespace isolation based on tenant (bid/bgroup)
* camellia-redis-proxy added support for ZMPOP and BZMPOP commands (redis7.0)

### Updates
* camellia-redis-proxy-samples removed zk and nacos dependencies (add yourself if needed)
* camellia-redis-proxy when using ConverterProxyPlugin and KeyConverter for key namespace isolation, SCAN command without MATCH field also needs to execute KeyConverter#convert
* camellia-redis-proxy uses off-heap memory to optimize BulkReply encoding/decoding performance
* camellia-redis-proxy supports global configuration of tenant-level connection limit, thanks[@tasszz2k](https://github.com/tasszz2k)

### Fixes
* Fixed camellia-delay-queue long polling issue after running for some time (not holding connection)
* Fixed camellia-redis-proxy when using ConverterProxyPlugin for key conversion, didn't handle key in TairZSet EXBZPOPMAX/EXBZPOPMIN reply


# 1.1.7 (2022/11/30)
### New Features
* camellia-redis-proxy added support for ZINTERCARD command
* camellia-redis-proxy added support for TairZSet, TairHash, TairString
* camellia-redis-proxy added support for RedisJSON
* camellia-redis-proxy added support for RedisSearch
* camellia-redis-proxy when proxying to redis-standalone/redis-sentinel, supports setting backend db
* CamelliaRedisTemplate when requesting redis-standalone/redis-sentinel, supports setting db

### Updates
* Adjusted camellia-dashboard config interface for camellia-redis-proxy IP whitelist/blacklist, thanks[@tasszz2k](https://github.com/tasszz2k)

### Fixes
* None


# 1.1.6 (2022/11/23)
### New Features
* None

### Updates
* camellia-redis-proxy optimized statistics module implementation, improved memory and GC

### Fixes
* None


# 1.1.5 (2022/11/21)
### New Features
* CamelliaStatistic utility class supports statistics quantiles (p50, p75, p90, p90, p95, p99, p999)
* camellia-redis-proxy latency monitoring supports statistics quantiles (p50, p75, p90, p90, p95, p99, p999), see: [monitor-data](/docs/camellia-redis-proxy/monitor/monitor-data.md)
* Provided FileBasedCamelliaApi, supports using local properties config file to simulate camellia-dashboard
* camellia-feign supports using local config files for dynamic parameters (like timeout, circuit breaker, routing, etc.)
* camellia-core dual-write/sharding execution thread pool supports setting rejection policy

### Updates
* camellia-feign when initializing, if dependent backend service entirely down, logs warn instead of error
* camellia-feign when using dynamic routing, if remote (camellia-dashboard) returns 404, uses local routing instead of error
* camellia-feign when setting dual-write thread pool to Abort policy, if RejectedExecutionException triggered, also callback CamelliaFeignFailureListener

### Fixes
* None


# 1.1.4 (2022/11/08)
### New Features
* Accessing camellia-dashboard related APIs supports custom headers, thanks[@tasszz2k](https://github.com/tasszz2k) for this feature
* camellia-redis-proxy when configuring dual-write, also supports PUBSUB series commands
* Added task merging utility class, see: [toolkit](/docs/camellia-tools/tools.md)

### Updates
* camellia-redis-proxy refactored and optimized ReplyDecoder implementation and performance

### Fixes
* Fixed camellia-delay-queue thread pool leak when using long polling


# 1.1.3 (2022/10/24)
### New Features
* CamelliaRedisTemplate supports RedisProxiesResource redis resource configuration
* camellia-redis-proxy added CommandDisableProxyPlugin, can restrict certain commands access on proxy

### Updates
* camellia-delay-queue, deleteMsg supports immediate release of redis memory (default false)
* Optimized camellia-redis-proxy renew strategy when proxying to redis-cluster, prioritizes address string IP:port, then master node, then slave node, plus random

### Fixes
* camellia-delay-queue, when message consumed or deleted, resending same msgId message will be deduplicated
* Fixed proxy when password configured, redis-benchmark not working (introduced in v1.1.0), root cause: when auth and other commands submitted together via pipeline, proxy didn't handle correctly


# 1.1.2 (2022/10/12)
### New Features
* CamelliaRedisProxyStarter supports starting console-server
* RedisProxyRedisConnectionFactory implements DisposableBean interface, supports destroy method
* camellia-redis-proxy added cluster-mode, can disguise proxy cluster as redis-cluster
* camellia-id-gen-sdk added DelayQueueServerDiscoveryFactory for easy management of multiple delay-queue-server clusters based on registry discovery
* camellia-redis-proxy supports COMMAND command, passes through to backend

### Updates
* Custom monitoring callbacks (MonitorCallback, SlowCommandMonitorCallback, HotKeyCacheStatsCallback, HotKeyMonitorCallback, BigKeyMonitorCallback) execute in independent thread pool to avoid unreasonable custom callback implementations blocking proxy main flow

### Fixes
* camellia-redis-proxy random port feature didn't check if port occupied
* Fixed camellia-redis-proxy SpringProxyBeanFactory not working


# 1.1.1 (2022/09/26)
### New Features
* camellia-redis-proxy when proxying to redis-cluster also supports TRANSACTION series commands (MULTI/EXEC/DISCARD/WATCH/UNWATCH)

### Updates
* Optimized AsyncCamelliaRedisTemplate and AsyncCamelliaRedisClusterClient code implementation
* Adjusted built-in ProxyPlugin default order (hot key monitoring changed to priority over hot key cache monitoring)

### Fixes
* None


# 1.1.0 (2022/09/21)
### New Features
* Refactored camellia-redis-proxy plugin and monitoring system, unified features under new framework, see: [redis-proxy](/docs/camellia-redis-proxy/redis-proxy-zh.md)

### Updates
* None

### Fixes
* None


# 1.0.61 (2022/09/06)
### New Features
* camellia-delay-queue supports consuming delayed messages via long polling interface, see: [delay-queue](/docs/camellia-delay-queue/delay-queue.md)
* Added camellia-console module for managing multiple camellia-dashboard clusters, thanks[@HongliangChen-963](https://github.com/HongliangChen-963) for this module
* Added CamelliaStatisticsManager for managing multiple CamelliaStatistics objects

### Updates
* camellia-redis-proxy optimized AsyncCamelliaRedisTemplate initialization logic

### Fixes
* Fixed camellia-redis-proxy when using sharding or proxying to redis-cluster, ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE commands failed
* Fixed camellia-feign when backend service abnormal, caller process startup DiscoveryResourcePool initialization failure caused memory leak


# 1.0.60 (2022/08/16)
### New Features
* Added camellia-delay-queue module for implementing delay queue functionality, see: [delay-queue](/docs/camellia-delay-queue/delay-queue.md)
* camellia-feign added failureListener, both CamelliaNakedClient and CamelliaFeignClient support, can be used for monitoring or failure retry
* camellia-tools added CamelliaStatistics utility class for counting, sum, average, max, etc. statistics
* camellia-redis added CamelliaFreq utility class for rate limiting, including single-machine and cluster rate limiting
* camellia-redis-proxy when dynamic routing refreshes, adds check of new route before taking effect
* camellia-redis-proxy when adding route, resource initialization changed to async, improved multi-tenant isolation

### Updates
* CamelliaRedisTemplate when initializing redis-cluster added availability judgment (jedis/v2.9.3 doesn't have this, newer versions do)
* Renamed NacosProxyDamicConfSupport to NacosProxyDynamicConfSupport
* CamelliaRedisTemplate when executing eval/evalsha commands uses timeout config (jedis2.9.3 removes timeout, higher versions don't, consistent with higher versions)

### Fixes
* Fixed camellia-dashboard FeignChecker not working (missing @Component annotation)
* Fixed RedisProxyJedisPool SideCarFirstProxySelector offline proxy failure


# 1.0.59 (2022/06/21)
### New Features
* camellia-core, camellia-feign adjusted async dual-write thread model, added support for MISC_ASYNC_MULTI_THREAD mode
* camellia-redis-proxy supports cache transparent double deletion, see: [interceptor](/docs/camellia-redis-proxy/interceptor.md)
* camellia-dashboard added several management APIs

### Updates
* CamelliaHashedExecutor supports getting completed task count
* Adjusted ProxyConstants default parameters, increased default thread count for internal thread pools used for dual-write and sharding
* camellia-redis-proxy when monitoring backend redis response time, skips pubsub commands and blocking commands
* Upgraded fastjson version from 1.2.76 to 1.2.83

### Fixes
* Fixed issue where backend redis response time was 0 when backend redis had password and password masking enabled


# 1.0.58 (2022/05/16)
### New Features
* camellia-redis-proxy detect interface supports returning key count/qps etc. info, see: [detect](/docs/camellia-redis-proxy/monitor/detect.md)

### Updates
* CamelliaIdGenSdkConfig supports setting OkHttpClient keepAliveSeconds config, default 30s

### Fixes
* None


# 1.0.57 (2022/05/10)
### New Features
* None

### Updates
* None

### Fixes
* Fixed CamelliaNakedClient dual-write not working


# 1.0.56 (2022/05/10)
### New Features
* camellia-redis-proxy supports forwarding to other proxies (like codis, twemproxy), and supports discovering backend proxy node lists via registry mode, see: [路由](/docs/camellia-redis-proxy/route/route.md)
* camellia-core supports async dual-write (based on thread pool + memory queue), same thread multiple write requests within process guaranteed to execute in order
* camellia-feign provides CamelliaNakedClient for custom invocations (non-standard feign client)
* camellia-redis-proxy supports BloomFilter related commands, see: [redis-proxy](/docs/camellia-redis-proxy/redis-proxy-zh.md)
* camellia-redis-proxy built-in IP-based client validator interceptor (IPCheckerCommandInterceptor), see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)

### Updates
* DynamicValueGetter class moved from camellia-core package to camellia-tools package

### Fixes
* None


# 1.0.55 (2022/04/07)
### New Features
* None

### Updates
* None

### Fixes
* Fixed camellia-feign circuit breaker filter exception type bug (didn't extract original exception from InvocationTargetException)


# 1.0.54 (2022/04/07)
### New Features
* Added CamelliaCircuitBreaker circuit breaker
* camellia-feign supports circuit breaker (integrate CamelliaCircuitBreaker), supports spring-boot-starter, supports dynamic configuration, see: [camellia-feign](/docs/camellia-feign/feign.md)
* camellia-redis-proxy custom ProxyRouteConfUpdater supports deleting existing routes, see: [路由](/docs/camellia-redis-proxy/route/route.md)

### Updates
* None

### Fixes
* None


# 1.0.53 (2022/03/24)
### New Features
* camellia-redis-proxy console added detect interface, can use camellia-redis-proxy as monitoring platform

### Updates
* None

### Fixes
* camellia-redis-proxy when using info upstream-info command to get backend redis cluster info, throws exception when backend is redis-cluster, introduced in v1.0.51


# 1.0.52 (2022/03/16)
### New Features
* Added camellia-feign module, feign supports dynamic routing, dual-write, dynamic timeout adjustment, etc.
* camellia-core added CamelliaDiscovery/CamelliaDiscoveryFactory series interfaces, unified discovery functionality across camellia modules
* camellia-core added ResourceTableUpdater/MultiResourceTableUpdater series abstract classes, unified dynamic routing functionality based on updater across camellia modules

### Updates
* camellia-redis removed ProxyDiscovery abstract class, unified to use IProxyDiscovery interface, inherits from CamelliaDiscovery interface
* camellia-id-gen removed AbstractIdGenServerDiscovery abstract class, unified to use IdGenServerDiscovery interface, inherits from CamelliaDiscovery interface
* All modules minimum JDK dependency upgraded to JDK8

### Fixes
* None


# 1.0.51 (2022/02/28)
### New Features
* None

### Updates
* camellia-redis-proxy info command response, line break changed from \n to \r\n for redis-shake redis data migration compatibility, see: [misc](/docs/camellia-redis-proxy/other/misc.md)

### Fixes
* ZkProxyRegistry after deregister method called, if network exception causes proxy-zk tcp connection reconnect, may cause camellia-redis-proxy to re-register to zk
* camellia-dashboard and camellia-redis-proxy in some cases printed backend redis password in logs, thanks[@chanjarster](https://github.com/chanjarster) for fixing this issue


# 1.0.50 (2022/02/17)
### New Features
* camellia-redis added CamelliaRedisLockManager for managing redis distributed lock auto-renewal, see: [toolkit](/docs/camellia-tools/tools.md)
* camellia-redis added CamelliaRedisTemplateManager for managing multiple CamelliaRedisTemplate for different bid/bgroup, see: [dynamic-dashboard](/docs/camellia-redis-client/dynamic-dashboard.md)
* camellia-tools added CamelliaHashedExecutor, when executing runnable/callable tasks with same hashKey, same thread executes

### Updates
* None

### Fixes
* camellia-dashboard deleteResourceTable interface should synchronously update ResourceInfo's tid reference, thanks[@chanjarster](https://github.com/chanjarster) for fixing this bug


# 1.0.49 (2022/01/19)
### New Features
* camellia-redis-proxy supports script load/flush/exists, see: [misc](/docs/camellia-redis-proxy/other/misc.md)
* camellia-redis-proxy supports eval_ro/evalsha_ro, requires backend redis7.0+

### Updates
* camellia-redis-proxy monitoring backend redis response latency data supports password masking

### Fixes
* scan in monitoring data should be read command not write command, doesn't affect functionality, only monitoring data incorrect
* camellia-dashboard getTableRefByBidGroup/deleteTableRef interface parameter should be bid not tid, thanks[@chanjarster](https://github.com/chanjarster) for fixing this bug


# 1.0.48 (2022/01/17)
### New Features
* camellia-redis-proxy supports scan command under custom sharding strategy
* CamelliaRedisTemplate added getReadJedisList/getWriteJedisList interfaces
* CamelliaRedisTemplate added executeRead/executeWrite interfaces

### Updates
* None

### Fixes
* None


# 1.0.47 (2022/01/05)
### New Features
* CamelliaRedisTemplate added getJedisList interface

### Updates
* None

### Fixes
* None


# 1.0.46 (2021/12/29)
### New Features
* Added CRC16HashTagShardingFunc and DefaultHashTagShardingFunc classes for custom sharding to support hashtag, see: [路由](/docs/camellia-redis-proxy/route/route.md)

### Updates
* Renamed shading to sharding, see: [路由](/docs/camellia-redis-proxy/route/route.md)

### Fixes
* None


# 1.0.45 (2021/12/24)
### New Features
* camellia-redis-proxy KafkaMqPackConsumer supports configuring batch consumption and retry, see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)
* camellia-redis-proxy provides DynamicCommandInterceptorWrapper for dynamic composition of multiple interceptors, see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)
* camellia-redis-proxy supports disabling console (set port to 0), see: [监控](/docs/camellia-redis-proxy/monitor/monitor.md)
* camellia-redis-proxy supports reading redis-cluster slave nodes, see: [路由](/docs/camellia-redis-proxy/route/route.md)
* camellia-redis-proxy supports proxying to multiple other stateless proxy nodes like codis-proxy, twemproxy, etc., see: [路由](/docs/camellia-redis-proxy/route/route.md)

### Updates
* camellia-id-gen adjusted some parameter defaults

### Fixes
* None


# 1.0.44 (2021/11/29)
### New Features
* camellia-redis-proxy added KafkaMqPackProducerConsumer, proxy can act as both kafka producer and consumer, see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)
* camellia-redis-proxy supports monitoring backend redis response time, see: [监控](/docs/camellia-redis-proxy/monitor/monitor.md)
* RedisProxyJedisPool supports jedis3, see: [部署](/docs/camellia-redis-proxy/deploy/deploy.md)

### Updates
* Adjusted code structure, created camellia-redis-proxy-plugins module, camellia-redis-zk/camellia-redis-proxy-mq/camellia-redis-proxy-hbase moved under camellia-redis-proxy-plugins module
* camellia-redis-zk renamed to camellia-redis-proxy-discovery-zk,归属于camellia-redis-proxy-discovery, related class package names modified
* RedisProxyJedisPool related class package names modified, code moved from camellia-redis to camellia-redis-proxy-discovery
* camellia-redis-proxy info gc command modified return format to support zgc and other garbage collectors, see: [info](/docs/camellia-redis-proxy/monitor/info.md)

### Fixes
* None


# 1.0.43 (2021/11/23)
### New Features
* camellia-id-gen segment and strict modes added update interface for updating segment start value, see: [id-gen](/docs/camellia-id-gen/id-gen.md)
* camellia-id-gen segment and strict modes, regionId field supports setting offset, see: [id-gen](/docs/camellia-id-gen/id-gen.md)
* camellia-id-gen segment mode supports cross-unit synchronization, see: [id-gen-segment](/docs/camellia-id-gen/segment.md)
* camellia-id-gen added interfaces for parsing regionId, workerId, etc., see: [id-gen](/docs/camellia-id-gen/id-gen.md)
* camellia-redis-proxy supports cross-datacenter dual-write based on message queue (kafka, etc.), see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)

### Updates
* camellia-redis-proxy monitoring data buffer added max size limit to protect proxy
* camellia-redis-proxy custom ClientAuthProvider throws exception closes client connection

### Fixes
* Fixed camellia-id-gen-strict-spring-boot-starter cache-key-prefix configuration not working


# 1.0.42 (2021/10/26)
### New Features
* camellia-redis-proxy info command modified redis-cluster cluster security metrics meaning, see: [info](/docs/camellia-redis-proxy/monitor/info.md)

### Updates
* Getting slow query/big key monitoring data via console-api supports setting monitoring data amount limit, see: [monitor-data](/docs/camellia-redis-proxy/monitor/monitor-data.md)

### Fixes
* None


# 1.0.41 (2021/10/20)
### New Features
* camellia-redis-proxy info command modified redis-cluster cluster security metrics meaning, see: [info](/docs/camellia-redis-proxy/monitor/info.md)

### Updates
* None

### Fixes
* None


# 1.0.40 (2021/10/19)
### New Features
* camellia-redis-proxy supports using http-api to execute info command and get related info, see: [info](/docs/camellia-redis-proxy/monitor/info.md)
* camellia-redis-proxy info command added return of bid/bgroup level client connection count data, see: [info](/docs/camellia-redis-proxy/monitor/info.md)

### Updates
* None

### Fixes
* None


# 1.0.39 (2021/10/18)
### New Features
* camellia-redis-proxy supports configuring max client connections (total connection limit + bid/bgroup limit), default no limit, see: [客户端连接控制](/docs/camellia-redis-proxy/other/connectlimit.md)
* camellia-redis-proxy supports configuring detection of idle client connections and closing them, default disabled, see: [客户端连接控制](/docs/camellia-redis-proxy/other/connectlimit.md)
* camellia-redis-proxy provides RateLimitCommandInterceptor for controlling client request rate (supports global level, also bid/bgroup level), see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)
* When using /monitor to get camellia-redis-proxy big key monitoring data, supports configuring returned json size, see: [监控数据](/docs/camellia-redis-proxy/monitor/monitor-data.md)
* camellia-redis-proxy exposed more netty parameter configurations, see: [netty-conf](/docs/camellia-redis-proxy/other/netty-conf.md)
* camellia-redis-proxy provides camellia-redis-proxy-nacos-spring-boot-starter for using nacos to host proxy config, see: [nacos-conf](/docs/camellia-redis-proxy/other/nacos-conf.md)

### Updates
* Modified CommandInterceptor package name

### Fixes
* None


# 1.0.38 (2021/10/11)
### New Features
* Added camellia-id-gen module, supports: snowflake strategy (supports setting unit marker), database-based ID generation strategy (supports setting unit marker, trend increasing), database and redis-based ID generation strategy (supports setting unit marker, strictly increasing), see: [id-gen](/docs/camellia-id-gen/id-gen.md)
* camellia-redis-proxy supports custom callback injection via spring's @Autowired, see: [spring-autowire](/docs/camellia-redis-proxy/spring-autowire.md)

### Updates
* Removed camellia-redis-toolkit module, where CamelliaCounterCache/CamelliaRedisLock merged into camellia-redis
* camellia-tools module package renamed

### Fixes
* None


# 1.0.37 (2021/09/24)
### New Features
* camellia-redis-proxy configured backend redis supports account+password login, see: [route](/docs/camellia-redis-proxy/route/route.md)

### Updates
* info command when getting backend redis connection count, if certain backend connection count is 0, doesn't return
* Enhanced ProxyDynamicConfHook, can intercept all dynamic configurations of ProxyDynamicConf
* Expanded monitoring/log printing password masking scope
* Optimized CommandDecoder

### Fixes
* Fixed backend redis connection count monitoring possibly inaccurate (doesn't affect core functionality)


# 1.0.36 (2021/09/06)
### New Features
* Added camellia-tools module, provides compression utility CamelliaCompressor, encryption utility CamelliaEncryptor, local cache utility CamelliaLoadingCache, see: [tools](/docs/camellia-tools/tools.md)
* Added examples using camellia-tools for camellia-redis-proxy data decompression, encryption/decryption, see: [转换](/docs/camellia-redis-proxy/plugin/converter2.md)
* camellia-redis-proxy supports custom ClientAuthProvider to implement routing by password, see: [路由配置](/docs/camellia-redis-proxy/route/route.md), thanks[@yangxb2010000](https://github.com/yangxb2010000) for this feature
* camellia-redis-proxy supports setting random port, see: [部署](/docs/camellia-redis-proxy/deploy/deploy.md)
* camellia-redis-proxy supports custom key conversion, can divide single redis cluster into different namespaces (like adding different prefixes), see: [转换](/docs/camellia-redis-proxy/plugin/converter2.md)
* camellia-redis-proxy added support for RANDOMKEY command
* camellia-redis-proxy added support for HELLO command, doesn't support RESP3, but supports setting name and auth username password via HELLO command (if client uses Lettuce6.x, needs upgrade to this version)
* camellia-redis-proxy when proxying to redis-cluster supports scan command, thanks[@yangxb2010000](https://github.com/yangxb2010000) for this feature

### Updates
* camellia-redis-proxy info command return added http_console_port field, see: [info](/docs/camellia-redis-proxy/monitor/info.md)
* camellia-redis-proxy info command return added redis_version field, spring actuator defaults to using redis_version field from info command for health check, here directly returns a fixed version number, see: [info](/docs/camellia-redis-proxy/monitor/info.md)
* camellia-redis-proxy info command Stats section field renamed (to underscore), like: avg.commands.qps changed to avg_commands_qps, see: [info](/docs/camellia-redis-proxy/monitor/info.md)
* camellia-redis-proxy info command Stats section qps field takes 2 decimal places
* camellia-redis-proxy auth/client/quit command handling moved from ServerHandler to CommandsTransponder

### Fixes
* Fixed KeyParser utility class parsing EVAL/EVALSHA/XINFO/XGROUP/ZINTERSTORE/ZUNIONSTORE/ZDIFFSTORE command keys


# 1.0.35 (2021/08/13)
### New Features
* camellia-redis-proxy supports custom value conversion for string/set/list/hash/zset related commands (can be used for transparent data compression, encryption/decryption, etc.), see: [转换](/docs/camellia-redis-proxy/plugin/converter2.md)
* camellia-redis-proxy added support for GETEX/GETDEL/HRANDFIELD/ZRANDMEMBER commands
* camellia-redis-proxy big key detection added detection for GETDEL/GETEX commands, added detection for GETSET response

### Updates
* None

### Fixes
* Fixed camellia-redis-proxy blocking commands not working (introduced in 1.0.33)


# 1.0.34 (2021/08/05)
### New Features
* camellia-redis-proxy-hbase refactored string related commands cold-hot separation storage design, see: [文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)
* CamelliaRedisTemplate provides Jedis adapter, one-line change from Jedis to CamelliaRedisTemplate, see: [文档](/docs/camellia-redis-client/redis-client.md)
* CamelliaRedisTemplate provides SpringRedisTemplate adapter, see: [文档](/docs/camellia-redis-client/redis-client.md)
* camellia-redis-proxy provides simple wrapper utility CamelliaRedisProxyStarter for starting proxy without spring-boot-starter, see: [文档](/docs/camellia-redis-proxy/redis-proxy-zh.md)

### Updates
* camellia-redis-proxy removed jedis dependency

### Fixes
* None


# 1.0.33 (2021/07/29)
### New Features
* camellia-redis-proxy provides TroubleTrickKeysCommandInterceptor to avoid abnormal keys causing backend redis exceptions (like business layer bug causing infinite loop and beating down backend redis, need to temporarily shield related requests to protect backend redis), see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)
* camellia-redis-proxy provides MultiWriteCommandInterceptor for custom dual-write strategy (some keys need dual-write, some don't, some dual-write to redisA, some to redisB), see: [拦截器](/docs/camellia-redis-proxy/interceptor.md)
* camellia-redis-proxy supports DUMP/RESTORE commands
* CamelliaRedisTemplate supports DUMP/RESTORE commands

### Updates
* None

### Fixes
* camellia-redis-proxy BITPOS should be read command
* CamelliaRedisTemplate BITPOS should be read command


# 1.0.32 (2021/07/15)
### New Features
* camellia-redis-proxy-hbase added support for string/hash related commands cold-hot separation storage, see: [文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)

### Updates
* None

### Fixes
* None


# 1.0.31 (2021/07/05)
### New Features
* info command supports section parameter, and supports getting backend redis cluster info (memory usage, version, master-slave distribution, slot distribution, etc.), see: [监控](/docs/camellia-redis-proxy/monitor/monitor.md)

### Updates
* None

### Fixes
* Fixed issue where after calling subscribe/psubscribe then unsubscribe/punsubscribe, corresponding backend redis connection not released


# 1.0.30 (2021/06/29)
### New Features
* None

### Updates
* Initializing and dynamically updating routing config logs should also support password masking

### Fixes
* Fixed NPE when opening slow query/big key monitoring and using subscribe/psubscribe commands, after receiving certain number of messages (couldn't receive subsequent messages)
* When proxying to redis-cluster: subscribe/psubscribe supports multiple subscriptions in same connection, and after unsubscribe/punsubscribe, client connection can be used for normal commands (old version proxy could only call subscribe/psubscribe once, and couldn't unsubscribe/punsubscribe after calling)


# 1.0.29 (2021/06/25)
### New Features
* None

### Updates
* None

### Fixes
* Fixed blocking commands occasional not_available issue (introduced in 1.0.27)


# 1.0.28 (2021/06/25)
### New Features
* Added info command to get server related info, see: [监控](/docs/camellia-redis-proxy/monitor/monitor.md)
* Added monitor-data-mask-password config for hiding passwords in logs and monitoring data, see: [监控](/docs/camellia-redis-proxy/monitor/monitor.md)

### Updates
* None

### Fixes
* Fixed issue where using pipeline to submit multiple blocking commands at once could cause not_available (introduced in 1.0.27)


# 1.0.27 (2021/06/22)
### New Features
* None

### Updates
* None

### Fixes
* Fixed issue where using blocking commands with high single connection TPS caused too many backend redis connections


# 1.0.26 (2021/05/27)
### New Features
* camellia-redis-proxy supports configuring port and applicationName separately (higher priority than spring's server.port/spring.application.name)
* ProxyDynamicConf supports directly setting k-v config map (previously only read from specified file)

### Updates
* camellia-redis-proxy renamed LoggingHoyKeyMonitorCallback to LoggingHotKeyMonitorCallback
* camellia-redis-proxy removed Disruptor/LinkedBlockingQueue based command forwarding mode, only kept direct forwarding mode
* camellia-redis-proxy statistics log logger name changed (added camellia.redis.proxy. prefix), like LoggingMonitorCallback.java
* camellia-redis-proxy renamed BigKeyMonitorCallback callback methods, callbackUpstream/callbackDownstream changed to callbackRequest/callbackReply
* camellia-redis-proxy performance optimization

### Fixes
* None


# 1.0.25 (2021/05/17)
### New Features
* camellia-redis-proxy supports closing idle backend redis connections, enabled by default
* camellia-redis-proxy supports monitoring backend redis connection count, see: [监控数据](/docs/camellia-redis-proxy/monitor/monitor-data.md)

### Updates
* None

### Fixes
* camellia-redis-proxy when proxying to redis-cluster, fixed possible deadlock in extreme edge cases


# 1.0.24 (2021/05/11)
### New Features
* camellia-redis-proxy added ProxyRouteConfUpdater, users can custom implement multi-group dynamic routing configuration based on bid/bgroup (like connecting to their config center, no need to depend on camellia-dashboard), see: [路由配置](/docs/camellia-redis-proxy/route/route.md)
* Provided ProxyRouteConfUpdater default implementation DynamicConfProxyRouteConfProvider, uses DynamicConfProxy (camellia-redis-proxy.properties) to manage multi-group routing configs and dynamic updates
* camellia-redis-proxy added ProxyDynamicConfHook, users can custom modify related configs based on hook, see: [动态配置](/docs/camellia-redis-proxy/dynamic-conf.md)
* camellia-redis-proxy added monitoring related callback DummyMonitorCallback implementation, if don't want to print related statistics logs, set to dummy callback implementation
* camellia-redis-proxy monitoring metrics added routing related items, including request count to each redis backend, and currently effective routing config, see: [监控数据](/docs/camellia-redis-proxy/monitor/monitor-data.md)
* camellia-redis-proxy latency monitoring added business level data (bid/bgroup), see: [监控数据](/docs/camellia-redis-proxy/monitor/monitor-data.md)

### Updates
* None

### Fixes
* None


# 1.0.23 (2021/04/16)
### New Features
* None

### Updates
* Updated netty version to 4.1.63

### Fixes
* Fixed JDK8 ConcurrentHashMap computeIfAbsent method performance bug, see: CamelliaMapUtils, bug: https://bugs.openjdk.java.net/browse/JDK-8161372


# 1.0.22 (2021/04/14)
### New Features
* CamelliaRedisTemplate supports reading data from redis-sentinel cluster slave nodes (automatically senses node failure, master-slave failover and node scaling), see: RedisSentinelResource and JedisSentinelSlavesPool
* camellia-redis-proxy supports reading data from redis-sentinel cluster slave nodes (automatically senses node failure, master-slave failover and node scaling), see: [路由配置](/docs/camellia-redis-proxy/route/route.md)
* CamelliaRedisTemplate when using camellia-redis-spring-boot-starter to access camellia-redis-proxy, supports setting bid/bgroup

### Updates
* camellia-redis-proxy startup fails when warmup fails

### Fixes
* None


# 1.0.21 (2021/04/06)
### New Features
* camellia-redis-proxy when using local config, supports dynamically modifying routing forwarding rules, see: [路由配置](/docs/camellia-redis-proxy/route/route.md)
* camellia-redis-proxy ProxyDynamicConf(camellia-redis-proxy.properties) supports using external independent config file for override, see [动态配置](/docs/camellia-redis-proxy/dynamic-conf.md)
* camellia-redis-proxy added warmup feature (enabled by default), when enabled, proxy creates connections to backends at startup instead of waiting for actual traffic
* camellia-redis-spring-boot-starter/camellia-hbase-spring-boot-starter when using local config files for routing, also support dynamic changes

### Updates
* camellia-redis-proxy when disabling RT monitoring via dynamic config, synchronously disables slow query monitoring, consistent with yml config logic
* camellia-spring-redis-{zk,eureka}-discovery-spring-boot-starter added switch (enabled by default)
* RedisProxyJedisPool added jedisPoolLazyInit parameter for delayed jedisPool initialization to improve RedisProxyJedisPool initialization speed, enabled by default, defaults to initializing highest priority 16 proxy jedisPools first

### Fixes
* Fixed RedisProxyJedisPool bug, very low probability, could cause "Could not get a resource from the pool" exception (introduced in 1.0.14)
* Fixed camellia-redis-proxy config files not found when running as fat-jar


# 1.0.20 (2021/02/26)
### New Features
* None

### Updates
* Refactored camellia-redis-proxy-hbase, incompatible with old version, see [文档](/docs/redis-proxy-hbase/redis-proxy-hbase.md)
* Optimized camellia-redis-proxy performance when command latency monitoring enabled

### Fixes
* None


# 1.0.19 (2021/02/07)
### New Features
* None

### update
* camellia-redis-proxy performance improvement, see [v1.0.19](/docs/camellia-redis-proxy/performance-report-8.md)

### Fixes
* Fixed calling KeyParser to get xinfo/xgroup keys error return, fixed possible bug when using pipeline to call xinfo/xgroup


# 1.0.18 (2021/01/25)
### New Features
* Added console http-api interface /reload to reload ProxyDynamicConf
* Support HSTRLEN/SMISMEMBER/LPOS/LMOVE/BLMOVE
* Support ZMSCORE/ZDIFF/ZINTER/ZUNION/ZRANGESTORE/GEOSEARCH/GEOSEARCHSTORE
* Opened ProxyDynamicConf dynamic config feature, example: you add "k=v" in camellia-redis-proxy.properties, then you can call ProxyDynamicConf.getString("k") to get "v", see ProxyDynamicConf class for details

### Updates
* If dual-write (multi-write) configured, blocking commands directly return not supported

### Fixes
* None


# 1.0.17 (2021/01/15)
### New Features
* When proxying to redis/redis-sentinel, and no sharding/no read-write separation, supports transaction commands (WATCH/UNWATCH/MULTI/EXEC/DISCARD)
* Support ZPOPMIN/ZPOPMAX/BZPOPMIN/BZPOPMAX

### Updates
* None

### Fixes
* Fixed ReplyDecoder bug, proxy changed nil MultiBulkReply to empty MultiBulkReply return (discovered when implementing transaction commands)
* Fixed ProxyDynamicConf initialization NPE, didn't affect ProxyDynamicConf functionality, just printed error log once at proxy (v1.0.16) startup


# 1.0.16 (2021/01/11)
### New Features
* Some parameters support dynamic changes
* camellia-redis-zk-registry supports registering hostname

### Updates
* Optimized locking process for several concurrent initializations

### Fixes
* None


# 1.0.15 (2020/12/30)
### New Features
* None

### Updates
* HotKeyMonitor json added fields times/avg/max
* LRUCounter updated, uses LongAdder instead of AtomicLong

### Fixes
* None


# 1.0.14 (2020/12/28)
### New Features
* None

### Updates
* RedisProxyJedisPool fallback thread when refreshing proxy list, even if ProxySelector already holds that proxy, still calls add method to avoid load imbalance from occasional timeout exceptions

### Fixes
* None


# 1.0.13 (2020/12/18)
### New Features
* None

### Updates
* IpSegmentRegionResolver allows setting empty config, so camellia-spring-redis-eureka-discovery-spring-boot-starter and camellia-spring-redis-zk-discovery-spring-boot-starter startup regionResolveConf parameter can be omitted

### Fixes
* None


# 1.0.12 (2020/12/17)
### New Features
* RedisProxyJedisPool allows setting custom proxy selection strategy IProxySelector, default uses RandomProxySelector, if side-car priority enabled, uses SideCarFirstProxySelector
* RedisProxyJedisPool when using SideCarFirstProxySelector, proxy access priority: co-located proxy -> same region proxy -> other proxies, declaring proxy region requires passing RegionResolver, default provides IpSegmentRegionResolver based on IP segment region division
* Added LocalConfProxyDiscovery

### Updates
* Optimized camellia-redis-proxy fast failure strategy when backend redis fails when proxying to redis-cluster
* camellia-redis-proxy refresh backend slot distribution info changed to async execution

### Fixes
* Fixed redis-cluster refresh slot info bug (introduced in 1.0.9)


# 1.0.11 (2020/12/09)
### New Features
* camellia-redis-proxy supports setting monitoring callback MonitorCallback
* camellia-redis-proxy supports slow query monitoring, supports setting SlowCommandMonitorCallback
* camellia-redis-proxy supports hot key monitoring, supports setting HotKeyMonitorCallback
* camellia-redis-proxy supports hot key local cache at proxy layer (only supports GET command), supports setting HotKeyCacheStatsCallback
* camellia-redis-proxy supports big key monitoring, supports setting BigKeyMonitorCallback
* camellia-redis-proxy when configuring read-write separation supports setting multiple read addresses (randomly select one to read)
* CamelliaRedisTemplate supports getting original Jedis
* RedisProxyJedisPool supports side-car mode, when enabled prioritizes accessing co-located redis-proxy
* camellia-redis-proxy console supports getting monitoring data via api (default http://127.0.0.1:16379/monitor) (including tps/rt/slow query/hot key/big key/hot key cache, etc.)
* Added camellia-spring-redis-zk-discovery-spring-boot-starter for SpringRedisTemplate clients to access proxy via registry mode

### Updates
* Modified CommandInterceptor interface definition

### Fixes
* Fixed custom sharding mget NPE issue (bug introduced in 1.0.10)
* Fixed redis sentinel failover bug on proxy


# 1.0.10 (2020/10/16)
### New Features
* camellia-redis-proxy supports blocking commands like BLPOP/BRPOP/BRPOPLPUSH, etc.
* camellia-redis-proxy supports redis5.0 stream commands including blocking XREAD/XREADGROUP
* camellia-redis-proxy supports pub-sub commands
* camellia-redis-proxy supports set operation commands like SINTER/SINTERSTORE/SUNION/SUNIONSTORE/SDIFF/SDIFFSTORE, etc.
* camellia-redis-proxy supports setting dual-write (multi-write) modes, provides three options, see com.netease.nim.camellia.redis.proxy.conf.MultiWriteMode and related docs
* camellia-redis-proxy provides abstract class AbstractSimpleShardingFunc for custom sharding functions
* camellia-redis-proxy-hbase supports single-machine rate limit for zmember hbase read penetration

### Updates
* camellia-redis-proxy-hbase added protection logic for zset cache rebuild from hbase

### Fixes
* Fixed CamelliaHBaseTemplate bug when executing batch delete during dual-write (multi-write)


# 1.0.9 (2020/09/08)
### New Features
* camellia-redis-proxy async mode supports redis sentinel
* camellia-redis-proxy async mode supports command execution time statistics
* camellia-redis-proxy async mode supports CommandInterceptor, custom interception rules
* Added camellia-redis-zk registry discovery component, provides default implementation for using camellia-redis-proxy via registry mode
* camellia-redis-proxy-hbase added hbase read penetration single-machine rate limit

### Updates
* Adjusted camellia-redis-proxy sendbuf and rcvbuf defaults, and when responding doesn't judge channel writable, avoiding response failure in super large packet + pipeline scenarios when channel not writable
* Removed camellia-redis-proxy sync mode
* camellia-redis-proxy async mode performance optimization, see performance report

### Fixes
* None


# 1.0.8 (2020/08/04)
### New Features
* camellia-redis-proxy async mode supports eval and evalsha commands
* CamelliaRedisTemplate supports eval/evalsha
* CamelliaRedisLock uses lua script to implement stricter distributed lock

### Updates
* camellia-redis-proxy several optimizations

### Fixes
* None


# 1.0.7 (2020/07/16)
### New Features
* camellia-redis-proxy-hbase added hbase read request concurrent penetration protection logic
* camellia-redis-proxy-hbase added single batch limit for hbase read/write (batch GET and batch PUT)
* camellia-redis-proxy-hbase hbase write operation supports setting to ASYNC_WAL
* camellia-redis-proxy-hbase type command supports caching null
* camellia-redis-proxy-hbase added degradation config, hbase read/write operations fully async (may cause data inconsistency)

### Updates
* Optimized some monitoring performance (LongAdder instead of AtomicLong)
* camellia-redis-proxy-hbase config uses HashMap instead of Properties to avoid lock contention
* camellia-redis-proxy several performance optimizations

### Fixes
* None


# 1.0.6 (2020/05/22)
### New Features
* camellia-redis-proxy-hbase provides async flush hbase mode, reduces end-side response RT (uses redis queue as buffer), disabled by default

### Updates
* Optimized RedisProxyJedisPool implementation, added automatic disable unavailable proxy logic
* camellia-hbase-spring-boot-starter when using remote config, monitoring enabled by default

### Fixes
* Fixed camellia-redis-proxy in async mode pipeline requests may return out of order bug


# 1.0.5 (2020/04/27)
### New Features
* Added camellia-redis-eureka-spring-boot-starter for spring boot projects to access camellia-redis-proxy directly (auto-discover proxy cluster via eureka), no need for LVS/VIP components

### Updates
* Optimized CamelliaRedisLock implementation
* Optimized camellia-redis-proxy-hbase implementation
* Updated camellia-redis-proxy-hbase monitoring metrics (see RedisHBaseMonitor class and RedisHBaseStats class)

### Fixes
* Fixed camellia-dashboard swagger-ui Chinese encoding issue


# 1.0.4 (2020/04/20)
* First release

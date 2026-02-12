
# camellia-redis-proxy([中文版](redis-proxy-zh.md))
## Introduction
camellia-redis-proxy is a high-performance redis proxy developed with netty4, requiring a minimum of Java 21.

## Features
* Supports proxying to redis-standalone, redis-sentinel, redis-cluster
* Supports using other proxies as backends (such as in dual-write migration scenarios), like [twemproxy](https://github.com/twitter/twemproxy), [codis](https://github.com/CodisLabs/codis), etc.
* Supports [kvrocks](https://github.com/apache/kvrocks), [pika](https://github.com/OpenAtomFoundation/pika), [tendis](https://github.com/Tencent/Tendis) as backends
* Supports using hbase/obkv/tikv and others as underlying storage to build a redis-like system, see: [kv](./kv/kv.md)
* Supports common commands like GET/SET/EVAL, as well as MGET/MSET, blocking commands like BLPOP, PUBSUB and TRANSACTION, STREAMS/JSON/SEARCH, and TAIR_HASH/TAIR_ZSET/TAIR_STRING
* For the list of supported commands, see: [supported_commands](supported_commands.md)
* Supports custom sharding
* Supports read-write separation
* Supports dual (multiple) writes - can directly write to two proxies, or use MQ (like kafka) for dual-write, or customize dual-write rules based on the plugin system
* Supports dual (multiple) reads
* Supports password authentication
* Supports SELECT command (currently only when backend redis does not include redis-cluster, in which case only SELECT 0 is supported); can be redis-standalone/redis-sentinel/redis-proxies or their combinations (sharding/read-write separation)
* Supports blocking commands like BLPOP/BRPOP/BRPOPLPUSH/BZPOPMIN/BZPOPMAX, etc.
* Supports PUBSUB series commands (supported when proxying to redis-standalone/redis-sentinel/redis-cluster)
* Supports transaction commands (MULTI/EXEC/DISCARD/WATCH/UNWATCH) (supported when proxying to redis-standalone/redis-sentinel/redis-cluster)
* Supports Redis 5.0 STREAMS series commands
* Supports SCAN command (supported when proxying to redis-standalone/redis-sentinel/redis-cluster, and also supported with custom sharding)
* Supports Alibaba TairZSet, TairHash, TairString series commands
* Supports RedisJSON and RedisSearch series commands
* Supports reading from slaves (both redis-sentinel and redis-cluster support configuring read from slave nodes)
* Supports SSL/TLS (both proxy-to-client and proxy-to-redis supported), see: [ssl/tls](/docs/camellia-redis-proxy/other/tls.md)
* Supports unix-domain-socket (both client-to-proxy and proxy-to-redis supported), see: [uds](/docs/camellia-redis-proxy/other/uds.md)
* Supports accessing proxy via HTTP protocol, similar to [webdis](https://github.com/nicolasff/webdis) but with different interface definitions, see: [redis_over_http](/docs/camellia-redis-proxy/other/redis_over_http.md)
* Supports multi-tenancy, i.e., tenant A routes to redis1, tenant B routes to redis2 (can be distinguished by different clientname or different password)
* Supports multi-tenant dynamic routing, supports custom dynamic routing data sources (built-in: local config file, nacos, etcd, etc., can also be customized)
* Supports custom plugins, with many built-in plugins available for use as needed (including: large key monitoring, hot key monitoring, hot key caching, key namespace, IP blacklist/whitelist, rate limiting, etc.)
* Supports rich monitoring, can monitor client connections, call volume, method latency, large keys, hot keys, backend redis connections and latency, and supports obtaining monitoring data via HTTP interface
* Supports using prometheus/grafana to monitor proxy clusters, see: [prometheus-grafana](prometheus/prometheus-grafana.md)
* Supports info command to get server related information (including backend redis cluster information)
* Provides a spring-boot-starter for quickly building a proxy cluster
* High availability, can form clusters based on LB, or based on registry, or can masquerade as redis-cluster to form a cluster, or can masquerade as redis-sentinel to form a cluster
* Provides a default registry discovery implementation component (depends on zookeeper); if the client side is Java, you can simply replace JedisPool with RedisProxyJedisPool to access redis proxy
* Provides a spring-boot-starter for SpringRedisTemplate to access proxy in registry discovery mode


## Quick Start 1 (Based on installation package)
See: [quick-start-package](quickstart/quick-start-package.md)

## Quick Start 2 (Based on source code compilation, personalized configuration)
See: [camellia-redis-proxy-bootstrap](other/camellia-redis-proxy-bootstrap.md)

## Quick Start 3 (Suitable for Java developers, using spring-boot-starter)
See: [quick-start-spring-boot](quickstart/quick-start-spring-boot.md)

## Quick Start 4 (Suitable for Java developers, without using spring-boot-stater)
See: [quick-start-no-spring-boot](quickstart/quick-start-no-spring-boot.md)

## Quick Start 5 (docker)
See: [quick-start-docker](deploy/quick-start-docker.md)

## Source Code Interpretation
See: [Code Structure](code/proxy-code.md)

## Configuration
* Configuration is the foundation of everything else. camellia-redis-proxy reads local configuration files by default. You can also customize configuration data sources like etcd, nacos, etc.
* For configuration principles, see: [Configuration Description](conf/dynamic-conf-en.md)
* For core configuration, see: [Core Configuration](conf/config_template-en.md)

## Routing Configuration
Routing configuration defines the forwarding rules after camellia-redis-proxy receives redis commands from clients, including:
* Simplest example
* Supported backend redis types
* Complex configurations (read-write separation, sharding, etc.)
* Built-in routing configuration solutions
* Custom routing configuration data sources

See: [Routing Configuration](route/route-en.md)

## Plugin System
* Plugins use a unified interface to intercept and control requests and responses
* proxy has many built-in plugins that can be used directly with simple configuration, choose as needed
* You can also implement custom plugins

See: [Plugins](plugin/plugin-en.md)

## Deployment and Access
In production environments, you need to deploy at least 2 proxy instances to ensure high availability, and proxy can be horizontally expanded, including:
* Cluster based on LB (like LVS, or service in k8s, etc.)
* Cluster based on registry
* redis-cluster mode
* redis-sentinel mode
* jvm-in-sidecar mode
* Graceful online/offline

See: [Deployment and Access](deploy/deploy-en.md)

## Monitoring
camellia-redis-proxy provides rich monitoring functions, including:
* Provided monitoring items
* Monitoring data acquisition methods
* Get server related information through info command
* Use proxy as a platform to monitor redis cluster status (exposed via HTTP interface)
* Use prometheus and grafana to monitor proxy clusters

See: [Monitoring](monitor/monitor-en.md)

## Others
* How to control client connections, see [Client Connection Control](other/connectlimit.md)
* Questions about dual (multiple) writes, see: [multi-write](other/multi-write.md)
* About scan and related instructions, see: [scan](other/scan.md)
* About lua/function/tfunction related instructions, see: [lua](other/lua_function.md)
* Instructions for using redis-shake for data migration, see: [redis-shake](other/redis-shake.md)
* About custom sharding functions, see: [sharding](other/sharding.md)
* How to use spring for bean generation, see: [spring](other/spring.md)
* Automatically remove failed read nodes in multi-read scenarios, see: [multi-read](other/multi-read.md)
* How redis-proxy gets real client addresses when using 4-layer load balancers like haproxy/nginx, see: [proxy_protocol](other/proxy_protocol.md)
* A complete example of using custom forwarding routing for hot keys, see: [hot-key-route-rewrite-sample](other/hot-key-route-rewrite-sample.md)
* An example of using UpstreamAddrConverter to improve service performance when redis and proxy are deployed in a mixed manner, see: [upstream-addr-converter](other/upstream-addr-converter.md)
* An idea for adjusting the number of shards when using custom sharding, see: [custom_resharding](other/custom_resharding.md)
* Instructions for using proxy commands to batch manage proxy cluster configurations, see: [proxy_command](other/proxy_command.md)
* About redis_proxy initialization and warm-up, see: [init](other/init.md)

## Application Scenarios
* Business initially used redis-standalone or redis-sentinel, now needs to switch to redis-cluster, but the client needs modification (for example, jedis accessing redis-sentinel and redis-cluster is different). In this case, you can use proxy to achieve no modification (using 4-layer proxy LB) or minimal modification (using registry)
* Use dual-write functionality for cluster migration or disaster recovery
* Use sharding functionality to deal with insufficient single-cluster capacity (a single redis-cluster cluster has node and capacity limits)
* Use built-in or custom plugins to monitor and control client access (hot keys, large keys, IP blacklist/whitelist, rate limiting, key namespace, data encryption/decryption, etc.)
* Use rich monitoring functions to control system operation
* And more

## Performance Test Report
[Performance Test Report based on v1.2.10](performance/performance.md)
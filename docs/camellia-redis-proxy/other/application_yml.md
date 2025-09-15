
* application.yaml

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  application-name: xxx
  port: -1 
  console-port: 16379 # for health check、get metrics data、online/offline
  tls-port: -1 #tls port if tls enabled
  proxy-frontend-tls-provider-class-name: com.netease.nim.camellia.redis.proxy.tls.frontend.DefaultProxyFrontendTlsProvider # tls provider
  http-port: -1 #http port for resp-over-http
  cport: -1
  cport-password: xxx
  uds-path: "/tmp/xxx.sock" #unix-domain-socket file path
  proxy-protocol-enable: false #enabled proxy protocol
  proxy-protocol-ports: "" #enabled proxy protocol ports, default all
  netty-transport-mode: auto #kqueue\epoll\io_uring\nio\auto，default auto
  cluster-mode-enable: false 
  sentinel-mode-enable: false
  netty:
    boss-thread: 1
    work-thread: -1
    tcp-no-delay: true
    tcp-quick-ack: false
    so-backlog: 1024
    so-sndbuf: 6291456
    so-rcvbuf: 6291456
    so-keepalive: true
    write-buffer-water-mark-low: 131072
    write-buffer-water-mark-high: 524288
    reader-idle-time-seconds: -1
    writer-idle-time-seconds: -1
    all-idle-time-seconds: -1
    command-decode-max-batch-size: 256
    command-decode-buffer-initializer-size: 32
  password: pass123
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.ClientAuthByConfigProvider
  monitor-enable: false
  monitor-interval-seconds: 60
  monitor-callback-className: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback
  plugins:
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyPlugin
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.conf.FileBasedProxyDynamicConfLoader
  config:
    "k1": "v1"
    "k2": "v2"
  upstream-client-template-factory-class-name: com.netease.nim.camellia.redis.proxy.upstream.UpstreamRedisClientTemplateFactory
  transpond:
    type: local
    local:
      type: simple
      resource: redis://@127.0.0.1:6379
    redis-conf:
      sharding-func: com.netease.nim.camellia.core.client.env.DefaultShardingFunc
      redis-cluster-max-attempts: 5
      proxy-upstream-tls-provider-class-name: com.netease.nim.camellia.redis.proxy.tls.upstream.DefaultProxyUpstreamTlsProvider
      close-idle-connection: true
      check-idle-connection-threshold-seconds: 600
      close-idle-connection-delay-seconds: 60
      preheat: true
      heartbeat-interval-seconds: 60 
      heartbeat-timeout-millis: 10000
      connect-timeout-millis: 1000
      fail-count-threshold: 5
      fail-ban-millis: 5000
      proxy-discovery-factory-class-name: "xx"
      upstream-addr-converter-class-name: com.netease.nim.camellia.redis.proxy.upstream.connection.DefaultUpstreamAddrConverter
```

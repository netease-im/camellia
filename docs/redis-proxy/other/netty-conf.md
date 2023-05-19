
## netty配置
一般来说，使用默认配置即可，请谨慎修改  

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  netty-transport-mode: nio #support：epoll、kqueue、io_uring、nio, default use nio
  #tcp connect between client and proxy
  netty:
    boss-thread: 1 #default 1
    work-thread: -1 #default -1, means work thread equals to cpu-num, generally, it is optimal
    so-backlog: 1024 #default 1024
    so-sndbuf: 10485760 #default 10M
    so-rcvbuf: 10485760 #default 10M
    write-buffer-water-mark-low: 131072 #default 128k
    write-buffer-water-mark-high: 524288 #default 512k
    #you should set reader-idle-time-seconds/writer-idle-time-seconds/all-idle-time-seconds >= 0 to enable idle check
    reader-idle-time-seconds: -1 #default -1, skip idle check; 
    writer-idle-time-seconds: -1 #default -1, skip idle check; 
    all-idle-time-seconds: -1 #default -1, skip idle check; 
    so-keepalive: true #default true
    tcp-no-delay: true #default true
    tcp-quick-ack: false #default false, only support when in epoll mode
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
    #tcp connect between proxy and redis
    netty:
      so-keepalive: true #default true
      so-sndbuf: 10485760 #default 10M
      so-rcvbuf: 10485760 #default 10M
      write-buffer-water-mark-low: 131072 #default 128k
      write-buffer-water-mark-high: 524288 #default 512k
      tcp-no-delay: true #default true
      tcp-quick-ack: false #default false, only support when in epoll mode
```

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
  #表示client到proxy的netty参数
  netty:
    boss-thread: 1 #默认1即可
    work-thread: -1 #表示工作线程数，默认-1，表示自动获取cpu核数，建议不要修改
    so-backlog: 1024 #默认1024
    so-sndbuf: 10485760 #默认10M
    so-rcvbuf: 10485760 #默认10M
    write-buffer-water-mark-low: 131072 #默认128k
    write-buffer-water-mark-high: 524288 #默认512k
    reader-idle-time-seconds: -1 #默认-1，表示不开启检测，如果要开启检测，三个配置均需要大于等于0
    writer-idle-time-seconds: -1 #默认-1，表示不开启检测，如果要开启检测，三个配置均需要大于等于0
    all-idle-time-seconds: -1 #默认-1，表示不开启检测，如果要开启检测，三个配置均需要大于等于0
    so-keepalive: false #默认false
    tcp-no-delay: true #默认true
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
    #表示proxy到redis的netty参数
    netty:
      so-keepalive: true #默认true
      so-sndbuf: 10485760 #默认10M
      so-rcvbuf: 10485760 #默认10M
      write-buffer-water-mark-low: 131072 #默认128k
      write-buffer-water-mark-high: 524288 #默认512k
      tcp-no-delay: true #默认true
```
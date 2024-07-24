
## HotKeyProxyPlugin

### 说明
* 一个用于监控热key的Plugin，支持动态设置阈值
* 因为属于监控类插件，因此还受monitor-enable总开关控制

### 启用方式
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口，如果设置为0，则不启动console
  password: pass123   #proxy的密码，如果设置了自定义的client-auth-provider-class-name，则密码参数无效
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  plugins: #使用yml配置插件，内置插件可以直接使用别名启用，自定义插件需要配置全类名
    - hotKeyPlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties

#开关
hot.key.monitor.enable=true
#热key监控LRU计数器的容量，一般不需要配置
hot.key.monitor.cache.max.capacity=100000
#热key监控统计的时间窗口，默认1000ms
hot.key.monitor.counter.check.millis=1000
#热key监控统计在时间窗口内超过多少阈值，判定为热key，默认500
hot.key.monitor.counter.check.threshold=500
#单个周期内最多上报多少个热key，默认32（取top）
hot.key.monitor.max.hot.key.count=32

###租户级别配置（bid=1，bgroup=default）
#开关
1.default.hot.key.monitor.enable=true
#热key监控LRU计数器的容量，一般不需要配置
1.default.hot.key.monitor.cache.max.capacity=100000
#热key监控统计的时间窗口，默认1000ms
1.default.hot.key.check.counter.check.millis=1000
#热key监控统计在时间窗口内超过多少阈值，判定为热key，默认500
1.default.hot.key.monitor.counter.check.threshold=500
#单个周期内最多上报多少个热key，默认32（取top）
1.default.hot.key.monitor.monitor.max.hot.key.count=32

##监控数据默认通过/monitor进行对外暴露（默认60s刷新一次数据），如果需要实时推送，可以设置callback（实现HotKeyMonitorCallback接口即可）
###默认的callback不做任何处理
hot.key.monitor.callback.className=com.netease.nim.camellia.redis.proxy.plugin.hotkey.DummyHotKeyMonitorCallback
```

## HotKeyCacheProxyPlugin

### 说明
* 一个用于支持热key缓存的Plugin
* 只支持GET请求，proxy会监控GET请求的tps，如果超过阈值，会把结果缓存，下次请求时直接返回
* 在缓存期间，proxy会定时穿透一个请求给后端，用于更新缓存值
* 对于GET命令，支持根据key的前缀来判断是否要启用缓存机制，也可以自定义实现规则

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
    - hotKeyCachePlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
#哪些key需要热key缓存功能，默认实现是PrefixMatchHotKeyCacheKeyChecker，可以基于key的前缀去配置，你可以自定义实现（实现HotKeyCacheKeyChecker接口即可）
hot.key.cache.key.checker.className=com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.PrefixMatchHotKeyCacheKeyChecker

#使用PrefixMatchHotKeyCacheKeyChecker时的前缀配置方法，如果要配置所有key都启用热key缓存功能，设置空字符串即可，默认所有key都不生效
hot.key.cache.key.prefix=["dao_c", "kkk"]
#使用PrefixMatchHotKeyCacheKeyChecker时的前缀配置方法（租户级别）
1.default.hot.key.cache.key.prefix=["dao_c", "kkk"]

##热key缓存相关的配置
#热key缓存功能的开关，默认true
hot.key.cache.enable=true
#用于判断是否是热key的LRU计数器的容量
hot.key.cache.counter.capacity=100000
#用于判断是否是热key的LRU计数器的时间窗口，默认1000ms
hot.key.cache.counter.check.millis=1000
#判定为热key的阈值，默认100
hot.key.cache.check.threshold=100
#是否缓存null的value，默认true
hot.key.cache.null=true
#热key缓存的时长，默认10s，过期一半的时候会穿透一个GET请求到后端
hot.key.cache.expire.millis=10000
#最多多少个缓存的热key，默认1000
hot.key.cache.max.capacity=1000

##热key缓存相关的配置（租户级别，bid=1，bgroup=default）
#热key缓存功能的开关，默认true
1.default.hot.key.cache.enable=true
#用于判断是否是热key的LRU计数器的容量
1.default.hot.key.cache.counter.capacity=100000
#用于判断是否是热key的LRU计数器的时间窗口，默认1000ms
1.default.hot.key.cache.counter.check.millis=1000
#判定为热key的阈值，默认100
1.default.hot.key.cache.check.threshold=100
#是否缓存null的value，默认true
1.default.hot.key.cache.null=true
#热key缓存的时长，默认10s，过期一半的时候会穿透一个GET请求到后端
1.default.hot.key.cache.expire.millis=10000
#最多多少个缓存的热key，默认1000
1.default.hot.key.cache.max.capacity=1000


##监控数据默认通过/monitor进行对外暴露（默认60s刷新一次数据），如果需要实时推送，可以设置callback（实现HotKeyCacheStatsCallback接口即可）
###默认的callback不做任何处理
hot.key.cache.stats.callback.className=com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.DummyHotKeyCacheStatsCallback
#热key缓存命中情况实时推送的间隔，默认10s
hot.key.cache.stats.callback.interval.seconds=10
```
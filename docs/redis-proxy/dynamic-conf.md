## 配置示例
所有配置均可在application.yml进行配置
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123   #proxy的密码
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback #监控回调类
  command-spend-time-monitor-enable: true #是否开启请求耗时的监控，只有monitor-enable=true才有效
  slow-command-threshold-millis-time: 1000 #慢查询的阈值，单位毫秒，只有command-spend-time-monitor-enable=true才有效
  slow-command-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.spendtime.LoggingSlowCommandMonitorCallback #慢查询的回调类
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomCommandInterceptor #方法拦截器
  hot-key-monitor-enable: true #是否监控热key
  hot-key-monitor-config:
    check-millis: 1000 #热key的检查周期
    check-threshold: 10 #热key的阈值，检查周期内请求次数超过该阈值被判定为热key
    check-cache-max-capacity: 1000 #检查的计数器集合的size，本身是LRU的
    max-hot-key-count: 100 #每次回调的热key个数的最大值（前N个）
    hot-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkey.LoggingHoyKeyMonitorCallback #热key的回调类
  hot-key-cache-enable: true #热key缓存开关
  hot-key-cache-config:
    counter-check-millis: 1000 #检查周期，单位毫秒
    counter-check-threshold: 5 #检查阈值，超过才算热key，才触发热key的缓存
    counter-max-capacity: 1000 #检查计数器集合的size，本身是LRU的
    need-cache-null: true #是否缓存null
    cache-max-capacity: 1000 #缓存集合的size，本身是LRU的
    cache-expire-millis: 5000 #缓存时间，单位毫秒
    hot-key-cache-stats-callback-interval-seconds: 20 #热key缓存的统计数据回调周期
    hot-key-cache-stats-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.LoggingHotKeyCacheStatsCallback #热key缓存的回调类
    hot-key-cache-key-checker-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.DummyHotKeyCacheKeyChecker #判断这个key是否需要缓存的接口
  big-key-monitor-enable: true #大key检测
  big-key-monitor-config:
    string-size-threshold: 10 #字符串类型，value大小超过多少认为是大key
    hash-size-threshold: 10 #hash类型，集合大小超过多少认为是大key
    zset-size-threshold: 10 #zset类型，集合大小超过多少认为是大key
    list-size-threshold: 10 #list类型，集合大小超过多少认为是大key
    set-size-threshold: 10 #set类型，集合大小超过多少认为是大key
    big-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.bigkey.LoggingBigKeyMonitorCallback #大key的回调类
  transpond:
    type: local #使用本地配置
    local:
      resource: redis://@127.0.0.1:6379 #转发的redis地址
    redis-conf:
      multi-write-mode: first_resource_only #双写的模式，默认第一个地址返回就返回
      shading-func: com.netease.nim.camellia.redis.proxy.samples.CustomShadingFunc #分片函数

camellia-redis-zk-registry: #需要引入相关依赖才有效
  enable: false #是否注册到zk
  zk-url: 127.0.0.1:2181 #zk地址
  base-path: /camellia #注册到zk的base-path
```

## 支持的动态配置及其含义
* application.yml的配置在服务启动后加载，且加载后不可变；对于其中部分配置，可以使用动态配置的方式进行复写，从而做到配置的动态变更
* 动态配置文件camellia-redis-proxy.properties，所有参数参见ProxyDynamicConf.java
* 修改配置文件后，默认10分钟reload一次，或者你可以调用console接口去reload，console默认端口是16379，接口是http://127.0.0.1:16379/reload
* 或者你也可以调用ProxyDynamicConf.reload()方法来reload配置
* 此外你可以使用ProxyDynamicConf来设置和获取自定义的其他配置，例子：你在camellia-redis-proxy.properties添加了"k=v"，则你可以调用ProxyDynamicConf.getString("k")获取到"v"，具体详见ProxyDynamicConf类
* 因为camellia-redis-proxy.properties必须在classpath下，因此如果你想使用另外的配置文件，则可以在camellia-redis-proxy.properties中配置dynamic.conf.file.path=xxx，xxx表示的是目标文件的绝对路径，则proxy会优先使用xxx的配置
```

#外部配置文件的绝对路径，优先级高于camellia-redis-proxy.properties
#ProxyDynamicConf会将xxx文件里的k-v配置和camellia-redis-proxy.properties里的k-v配置进行merge，xxx优先级更高
#dynamic.conf.file.path=xxx

#自动reload的间隔，默认600s，服务启动时会读取该配置
dynamic.conf.reload.interval.seconds=600


##熔断相关动态配置
#当某个redis后端（ip:port）连续几次异常后触发熔断
redis.client.fail.count.threshold=5
#当触发熔断后，多少ms内，指向该后端的所有请求直接失败
redis.client.fail.ban.millis=5000


#监控开关，当application.yml对应开关配置了true，启动后，你可以通过本配置进行动态开启和关闭监控（包括请求数量、qps、请求失败情况等监控），但是如果application.yml一开始是关闭的，则进程启动后无法通过本配置开启监控
monitor.enable=false
#是否监控命令耗时，和monitor.enable类型，只有application.yml对应配置打开，本配置才能动态开启和关闭
command.spend.time.monitor.enable=false

#慢查询的阈值
slow.command.threshold.millis=1000


##热key监控相关动态配置
#热key监控的开关，只有application.yml对应配置打开，本配置才能动态开启和关闭
hot.key.monitor.enable=false
#表示bid=1/bgroup=default的热key监控开关，优先级高于hot.key.monitor.enable
1.default.hot.key.monitor.enable=false
#热key的阈值，注意：热key的检查周期/缓存size等参数无法动态修改
hot.key.monitor.threshold=100
#表示来自bid=1/bgroup=default的请求的热key阈值，优先级高于hot.key.monitor.threshold
1.default.hot.key.monitor.threshold=100


##热key缓存相关的动态配置
#热key缓存的开关，只有application.yml对应配置打开，本配置才能动态开启和关闭
hot.key.cache.enable=false
#表示bid=1/bgroup=default的热key缓存的开关，优先级高于hot.key.cache.enable
1.default.hot.key.cache.enable=false

#热key缓存是否缓存null值
hot.key.cache.need.cache.null=true
#表示bid=1/bgroup=default的热key缓存是否缓存null值，优先级高于hot.key.cache.need.cache.null
1.default.hot.key.cache.need.cache.null=true

#热key缓存的阈值，注意：热key缓存相关的检查周期/缓存size等参数无法动态修改
hot.key.cache.threshold=100
#表示bid=1/bgroup=default的热key缓存的阈值
1.default.hot.key.cache.threshold=100

#表示bid=1/bgroup=default的哪些前缀的热key可以被缓存，当使用PrefixMatchHotKeyCacheKeyChecker时有效；默认是DummyHotKeyCacheKeyChecker，表示所有key都能被缓存
#配置的是一个json数组，每一项表示一个前缀，如果想所有key都能被缓存，则设置一个空串到数组里即可，如：[""]
1.default.hot.key.cache.key.prefix=["kkk","assa"]
#如果没有使用camellia-dashboard，只使用了local模式，即只使用一种路由规则，那么可以这样设置：
default.default.hot.key.cache.key.prefix=["kkk","assa"]


##大key监控相关
#大key监控开关，只有application.yml对应配置打开，本配置才能动态开启和关闭
big.key.monitor.enable=false
#表示bid=1/bgroup=default的大key监控开关，优先级高于big.key.monitor.enable
1.default.big.key.monitor.enable=false

#hash类型的key，集合大小超过多少算大key
big.key.monitor.hash.threshold=10000
#表示bid=1/bgroup=default的请求，hash类型的key，集合大小超过多少算大key，优先级高于big.key.monitor.hash.threshold
1.default.big.key.monitor.hash.threshold=10000

#string类型的key，value大小超过多少算大key
big.key.monitor.string.threshold=10000
#表示bid=1/bgroup=default的请求，string类型的key，value大小超过多少算大key，优先级高于big.key.monitor.string.threshold
1.default.big.key.monitor.string.threshold=10000

#set类型的key，集合大小超过多少算大key
big.key.monitor.set.threshold=10000
#表示bid=1/bgroup=default的请求，set类型的key，集合大小超过多少算大key，优先级高于big.key.monitor.set.threshold
1.default.big.key.monitor.set.threshold=10000

#zset类型的key，集合大小超过多少算大key
big.key.zset.string.threshold=10000
#表示bid=1/bgroup=default的请求，zset类型的key，集合大小超过多少算大key，优先级高于big.key.monitor.zset.threshold
1.default.big.key.monitor.zset.threshold=10000

#list类型的key，集合大小超过多少算大key
big.key.list.string.threshold=10000
#表示bid=1/bgroup=default的请求，list类型的key，集合大小超过多少算大key，优先级高于big.key.monitor.list.threshold
1.default.big.key.monitor.list.threshold=10000


```

## 通过ProxyDynamicConfHook进行配置的动态修改
如果你不想通过camellia-redis-proxy.properties这样的本地配置文件的方式进行配置的动态修改，那么你可以使用ProxyDynamicConfHook   
你需要自己实现一个ProxyDynamicConfHook的子类，并在启动时设置进去，示例如下：  
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123   #proxy的密码
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback #监控回调类
  command-spend-time-monitor-enable: true #是否开启请求耗时的监控，只有monitor-enable=true才有效
  slow-command-threshold-millis-time: 1000 #慢查询的阈值，单位毫秒，只有command-spend-time-monitor-enable=true才有效
  slow-command-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.spendtime.LoggingSlowCommandMonitorCallback #慢查询的回调类
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomCommandInterceptor #方法拦截器
  hot-key-monitor-enable: true #是否监控热key
  hot-key-monitor-config:
    check-millis: 1000 #热key的检查周期
    check-threshold: 10 #热key的阈值，检查周期内请求次数超过该阈值被判定为热key
    check-cache-max-capacity: 1000 #检查的计数器集合的size，本身是LRU的
    max-hot-key-count: 100 #每次回调的热key个数的最大值（前N个）
    hot-key-monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.command.async.hotkey.LoggingHoyKeyMonitorCallback #热key的回调类
  proxy-dynamic-conf-hook-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomProxyDynamicConfHook #设置动态配置变更的hook
  transpond:
    type: local #使用本地配置
    local:
      resource: redis://@127.0.0.1:6379 #转发的redis地址  

```
如上，设置了CustomProxyDynamicConfHook作为动态配置的hook类  
CustomProxyDynamicConfHook类继承自ProxyDynamicConfHook，并在里面重写了相关方法，每个方法代表了某个配置，如热key监控的开关、热key监控的阈值等等   
如果有些方法你没有重写，或者方法返回了null，那么proxy仍然会尝试去camellia-redis-proxy.properties中获取该配置，如果还是获取不到，则以application.yml里的配置为准    
特别的，因为proxy可能会缓存某一些配置项，当你的某些配置项发生了变更，务必调用ProxyDynamicConfHook.reload()方法去告诉proxy重新来获取相关配置   

## 配置优先级
优先级从高到低依次是：  
```
ProxyDynamicConfHook -> camellia-redis-proxy.properties中设置的filePath -> camellia-redis-proxy.properties -> application.yml
```  
特别的，对于开关类型的配置，比如热key监控开关，只有application.yml里设置为true，对应的热key监控相关的动态配置才能生效  
也就是说，你可以在application.yml把hot-key-monitor-enable设置成true，然后在ProxyDynamicConfHook或者camellia-redis-proxy.properties设置为false，那么应用启动时，热key监控功能是关闭的，但是你可以在运行期间动态打开  
相反，如果application.yml把hot-key-monitor-enable设置成false，那么不管ProxyDynamicConfHook还是camellia-redis-proxy.properties，不管设置成true还是false，都不能开启热key监控相关的功能了  
一言以蔽之，application.yml的开关配置控制了proxy启动时相关功能模块的加载，如果没有加载，那么动态配置不管怎么设置都无法生效了    
   
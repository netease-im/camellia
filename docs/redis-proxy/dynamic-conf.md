## 支持的动态配置及其含义
* 动态配置文件camellia-redis-proxy.properties，所有参数参见ProxyDynamicConf
* 修改配置文件后，默认10分钟reload一次，或者你可以调用console接口去reload，console默认端口是16379，接口是http://127.0.0.1:16379/reload
* 或者你也可以调用ProxyDynamicConf.reload()方法来reload配置
* 此外你可以使用ProxyDynamicConf来设置和获取自定义的其他配置，例子：你在camellia-redis-proxy.properties添加了"k=v"，则你可以调用ProxyDynamicConf.getString("k")获取到"v"，具体详见ProxyDynamicConf类
```
#自动reload的间隔，默认600s，服务启动时会读取改配置
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
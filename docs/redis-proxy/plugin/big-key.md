
## BigKeyProxyPlugin

### 说明
* 一个用于监控大key的Plugin，支持动态设置阈值
* 对于STRING，会监控value的字节数
* 对于SET/HASH/ZSET/LIST，会监控集合的size
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
    - bigKeyPlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
#开关
big.key.monitor.enable=true
#租户级别开关（bid=1，bgroup=default）
1.default.big.key.monitor.enable=true

#阈值
##默认2M
string.big.key.size.threshold=2097152
##默认5000
hash.big.key.size.threshold=5000
set.big.key.size.threshold=5000
zset.big.key.size.threshold=5000
list.big.key.size.threshold=5000

#阈值（租户级别）
##默认2M
1.default.string.big.key.size.threshold=2097152
##默认5000
1.default.hash.big.key.size.threshold=5000
1.default.set.big.key.size.threshold=5000
1.default.zset.big.key.size.threshold=5000
1.default.list.big.key.size.threshold=5000

##监控数据默认通过/monitor进行对外暴露（默认60s刷新一次数据），如果需要实时推送，可以设置callback（实现BigKeyMonitorCallback接口即可）
###默认的callback不做任何处理
big.key.monitor.callback.className=com.netease.nim.camellia.redis.proxy.plugin.bigkey.DummyBigKeyMonitorCallback
```
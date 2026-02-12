
## BigKeyProxyPlugin

### 说明
* 一个用于监控大key的Plugin，支持动态设置阈值
* 对于STRING，会监控value的字节数
* 对于SET/HASH/ZSET/LIST，会监控集合的size
* 因为属于监控类插件，因此还受monitor-enable总开关控制

### 配置
```properties
proxy.plugin.list=bigKeyPlugin

#开关
big.key.monitor.enable=true
#租户级别开关（bid=1，bgroup=default）
1.default.big.key.monitor.enable=true

#阈值
##默认2M
big.key.monitor.string.threshold=2097152
##默认5000
big.key.monitor.hash.threshold=5000
big.key.monitor.set.threshold=5000
big.key.monitor.zset.threshold=5000
big.key.monitor.list.threshold=5000

#阈值（租户级别）
##默认2M
1.default.big.key.monitor.string.threshold=2097152
##默认5000
1.default.big.key.monitor.hash.threshold=5000
1.default.big.key.monitor.set.threshold=5000
1.default.big.key.monitor.zset.threshold=5000
1.default.big.key.monitor.list.threshold=5000

##监控数据默认通过/monitor进行对外暴露（默认60s刷新一次数据），如果需要实时推送，可以设置callback（实现BigKeyMonitorCallback接口即可）
###默认的callback不做任何处理
big.key.monitor.callback.class.name=com.netease.nim.camellia.redis.proxy.plugin.bigkey.DummyBigKeyMonitorCallback
```
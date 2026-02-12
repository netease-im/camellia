
## DelayDoubleDeleteProxyPlugin

### 说明
* 一个用于透明的进行延迟缓存双删来保证db/缓存一致性的plugin
* 只会拦截DEL命令进行延迟的二次删除

### 配置
```properties
proxy.plugin.list=delayDoubleDeletePlugin

###配置
#首先要开启，默认是false
delay.double.del.enable=true
#其次要配置延迟双删的秒数，如果<=0，则不生效，默认-1
double.del.delay.seconds=5
#最后还要配置匹配哪些key去做延迟删除，是一个json array，如果不配置也不生效
##如果所有DEL命令中的key都要延迟双删，则配置前缀为空串
double.del.key.prefix=[""]
##如果只是部分命令，如只有dao_cache和cache前缀的key才延迟双删，则可以如下配置
#double.del.key.prefix=["dao_cache", "cache"]

###配置（租户级别）
#首先要开启，默认是false
1.default.delay.double.del.enable=true
#其次要配置延迟双删的秒数，如果<=0，则不生效，默认-1
1.default.double.del.delay.seconds=5
#最后还要配置匹配哪些key去做延迟删除，是一个json array，如果不配置也不生效
##如果所有DEL命令中的key都要延迟双删，则配置前缀为空串
1.default.double.del.key.prefix=[""]
##如果只是部分命令，如只有dao_cache和cache前缀的key才延迟双删，则可以如下配置
#1.default.double.del.key.prefix=["dao_cache", "cache"]

```
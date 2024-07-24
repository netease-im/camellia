
## DelayDoubleDeleteProxyPlugin

### 说明
* 一个用于透明的进行延迟缓存双删来保证db/缓存一致性的plugin
* 只会拦截DEL命令进行延迟的二次删除

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
    - delayDoubleDeletePlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
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
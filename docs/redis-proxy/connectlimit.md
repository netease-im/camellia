
## 控制客户端连接数
* camellia-redis-proxy支持配置客户端的连接数上限（支持全局的连接数，也支持bid/bgroup级别的）
* camellia-redis-proxy支持配置关闭空闲的客户端连接（该功能可能导致请求数较少的客户端请求异常，慎重配置） 

## 配置客户端最大连接数
### 原理
* proxy提供了ConnectLimiter来配置客户端最大连接数，你可以自定义实现ConnectLimiter接口（比如从业务的配置中心动态获取最大连接数的配置），并通过在application.yaml里配置全类名或者使用spring自动注入的方式启用  
* proxy默认使用的ConnectLimiter实现是DynamicConfConnectLimiter，该实现下，最大连接数的配置通过读取camellia-redis-proxy.properties相关配置来获取，支持动态变更  
* 当触发全局的最大连接数时，新的客户端连接会被直接关闭；当触发bid/bgroup级别的最大连接数时，会在新连接执行AUTH/CLIENT/HELLO等命令进行bid/bgroup绑定时返回错误信息并关闭连接    

### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  connect-limiter-class-name: com.netease.nim.camellia.redis.proxy.command.async.connectlimit.DynamicConfConnectLimiter
```
随后在camellia-redis-proxy.properties配置如下（默认不限制）：
```
#配置全局的最大连接数限制，如果小于0，则表示不限制
max.client.connect=100000

#配置某个bid/bgroup的最大连接数限制：
#表示归属于bid=1,bgroup=default的最大连接数限制，如果小于0，则表示不限制
1.default.max.client.connect=10000
```

## 配置检测/关闭空闲的客户端连接
### 原理
使用了netty提供的IdleStateHandler来检测空闲的客户端连接，如果配置了空闲时关闭，则空闲的客户端连接会被服务器强制关闭  
该功能可能导致请求数较少的客户端请求异常，请谨慎使用  

### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  netty:
    reader-idle-time-seconds: 600 #只有三个参数都大于等于0，才会开启空闲连接检测
    writer-idle-time-seconds: 0 #只有三个参数都大于等于0，才会开启空闲连接检测
    all-idle-time-seconds: 0 #只有三个参数都大于等于0，才会开启空闲连接检测
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```
上述例子表示开启空闲连接检测，当一个连接600s内没有任何可读的数据，则判定为空闲连接，此时会打印一条日志，如果需要关闭空闲连接，则需要在camellia-redis-proxy.properties里配置，配置支持动态变更：  
```
#触发reader-idle事件时是否关闭空闲连接，默认false
##全局配置
reader.idle.client.connection.force.close.enable=true
##bid/bgroup级别的配置，优先级高于全局配置
1.default.reader.idle.client.connection.force.close.enable=true

#触发writer-idle事件时是否关闭空闲连接，默认false
##全局配置
writer.idle.client.connection.force.close.enable=true
##bid/bgroup级别的配置，优先级高于全局配置
1.default.writer.idle.client.connection.force.close.enable=true

#触发all-idle事件时是否关闭空闲连接，默认false
all.idle.client.connection.force.close.enable=true
##bid/bgroup级别的配置，优先级高于全局配置
1.default.all.idle.client.connection.force.close.enable=true
```

## RateLimitProxyPlugin

### 说明
* 用于控制客户端的请求tps，超过了会直接返回错误，而不是穿透到后端redis

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
    - rateLimitPlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
##检查周期
rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
rate.limit.max.count=100000

#bid/bgroup级别的速率控制（下面的例子表示bid=1，bgroup=default的请求，最多允许1000ms内10w次请求，超过会返回错误）
##检查周期
1.default.rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
1.default.rate.limit.max.count=100000
```

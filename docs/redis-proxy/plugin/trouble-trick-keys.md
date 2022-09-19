
## TroubleTrickKeysProxyPlugin

### 说明
* 用于临时屏蔽某些命令的指定方法的请求，会直接返回错误信息，而不是穿透到后端redis 

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
    - troubleTrickKeys
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
#配置
#表示：针对key1和key2的ZREVRANGEBYSCORE方法，针对key3和key4的GET方法，会被拦截（直接返回错误信息）
trouble.trick.keys=ZREVRANGEBYSCORE:["key1","key2"];GET:["key3","key4"]

#配置（租户级别）
#表示：bid=2/bgroup=default路由配置下，针对key1和key2的ZRANGE方法，针对key3和key4的SMEMBERS方法，会被拦截（直接返回错误信息）
2.default.trouble.trick.keys=ZRANGE:["key1","key2"];SMEMBERS:["key3","key4"]
```
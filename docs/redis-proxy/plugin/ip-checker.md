
## IPCheckProxyPlugin

### 说明
* 一个用于对访问proxy的客户端进行ip黑白名单限制的plugin
* 支持黑名单模式，也支持白名单模式，配置支持动态变更

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
    - ipCheckerPlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
#黑名单示例（支持ip，也支持网段，逗号分隔）：
#ip.check.mode=1
#ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16

#白名单示例（支持ip，也支持网段，逗号分隔）：
#ip.check.mode=2
#ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16

#根据bid/bgroup设置不同的策略：
#黑名单示例（表示bid=1,bgroup=default的黑名单配置）：
#1.default.ip.check.mode=1
#1.default.ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16

#白名单示例（表示bid=1,bgroup=default的白名单配置）：
#1.default.ip.check.mode=2
#1.default.ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
```

## MonitorProxyPlugin

### 说明
* 一个用于统计访问proxy的客户端命令分布情况和响应耗时的plugin
* 特别的，可以监控慢查询
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
    - monitorPlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
#慢查询监控的阈值，默认2000ms
slow.command.threshold.millis=2000
##慢查询监控数据默认通过/monitor进行对外暴露（默认60s刷新一次数据），如果需要实时推送，可以设置callback（实现SlowCommandMonitorCallback接口即可）
slow.command.monitor.callback.className=com.netease.nim.camellia.redis.proxy.plugin.monitor.DummySlowCommandMonitorCallback

#其他监控数据（如请求数、rt等，统一通过/monitor接口对外暴露）
#特别的，对于rt的监控，有一个子开关，默认开启，如果关闭，则只统计tps，不统计rt
command.spend.time.monitor.enable=true
```
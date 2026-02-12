
## MonitorProxyPlugin

### 说明
* 一个用于统计访问proxy的客户端命令分布情况和响应耗时的plugin
* 特别的，可以监控慢查询
* 因为属于监控类插件，因此还受monitor-enable总开关控制

### 配置
```properties
proxy.plugin.list=monitorPlugin

#慢查询监控的阈值，默认2000ms
slow.command.threshold.millis=2000
##慢查询监控数据默认通过/monitor进行对外暴露（默认60s刷新一次数据），如果需要实时推送，可以设置callback（实现SlowCommandMonitorCallback接口即可）
slow.command.monitor.callback.class.name=com.netease.nim.camellia.redis.proxy.plugin.monitor.DummySlowCommandMonitorCallback

#其他监控数据（如请求数、rt等，统一通过/monitor接口对外暴露）
#特别的，对于rt的监控，有一个子开关，默认开启，如果关闭，则只统计tps，不统计rt
command.spend.time.monitor.enable=true
```
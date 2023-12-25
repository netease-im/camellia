## 监控
camellia-redis-proxy提供了丰富的监控功能，包括：
* 提供的监控项
* 监控数据获取方式
* 通过info命令获取服务器相关信息
* 把proxy当做一个监控redis集群状态的平台（通过http接口暴露）
* 使用prometheus和grafana监控proxy集群

## 监控项
### 大key监控
使用BigKeyProxyPlugin实现，具体见：[big-key](../plugin/big-key.md)

### 热key监控
使用HotKeyProxyPlugin实现，具体见：[hot-key](../plugin/hot-key.md)

### 热key缓存监控
主要是监控热key缓存的命中情况，具体见：[hot-key-cache](../plugin/hot-key-cache.md)

### 请求数/rt/慢查询
使用MonitorProxyPlugin实现，具体见：[hot-key](../plugin/monitor-plugin.md)

### 其他监控数据
* 客户端连接数
* 后端redis连接数
* 后端redis响应时间
* 路由信息
* ....

## 监控数据获取
### 自定义回调
你可以在application.yml里配置自定义回调，默认的回调实现是打印日志，如下：
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
  monitor-callback-class-name: com.netease.nim.camellia.redis.proxy.monitor.LoggingMonitorCallback #监控回调类
  plugins: #使用yml配置插件，内置插件可以直接使用别名启用，自定义插件需要配置全类名
    - hotKeyPlugin
    - monitorPlugin
    - bigKeyPlugin
    - hotKeyCachePlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```
回调类可以获取到所有的监控数据，参考com.netease.nim.camellia.redis.proxy.monitor.model.Stats类的定义

## 通过httpAPI获取监控数据
除了通过回调来获取监控数据，还可以通过http-api来直接获取（json格式），具体可见：[监控数据](monitor-data.md)

### 通过info命令获取服务器相关信息
proxy实现了info命令，支持返回如下信息：Server/Clients/Route/Upstream/Memory/GC/Stats/Upstream-Info  
详见[info命令](info.md)

### 把proxy当做一个监控redis集群状态的平台（通过http接口暴露）
你可以使用http接口去请求proxy，并把需要探测的redis地址传递给proxy，proxy会以json格式返回目标redis集群的信息  
详见[detect](detect.md)

### 使用prometheus和grafana监控proxy集群  
详见[prometheus-grafana](.././prometheus/prometheus-grafana.md)
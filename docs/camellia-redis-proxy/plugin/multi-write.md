
## MultiWriteProxyPlugin

### 说明
* 用于另外的进行自定义双写策略（key）的plugin，如：某些key需要双写，某些key不需要双写，某些key双写到redisA，某些key双写到redisB  
* 备注一：只有proxy完整支持的命令集合中的写命令支持本模式，对于那些限制性支持的命令（如阻塞型命令、发布订阅命令等）是不支持使用MultiWriteProxyPlugin来双写的  
* 备注二：redis事务包裹的写命令使用MultiWriteCommandInterceptor双写时可能主路由执行失败而双写成功

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
    - multiWritePlugin
  transpond:
    type: local #使用本地配置
    local:
      type: simple
      resource: redis://@127.0.0.1:6379 #转发的redis地址
```

### 动态配置开关（camellia-redis-proxy.properties）
```properties
#一个用于判断双写策略的函数方法（实现MultiWriteFunc接口即可）
multi.write.func.class.name=com.xxx.xxx.CustomMultiWriteFunc
```
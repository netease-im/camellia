
## 使用nacos托管proxy的配置

* 这是一种基于本地文件的nacos接入方法，可以托管任意的本地配置文件给nacos
* 如果你只是想托管ProxyDynamicConf（camellia-redis-proxy.properties），还有另外一种方法，参考：[dynamic-conf](dynamic-conf.md)

### maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-nacos-spring-boot-starter</artifactId>
    <version>1.2.20</version>
</dependency>
```

### 配置
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: -6379 #优先级高于server.port，如果缺失，则使用server.port，如果设置为-6379则会随机一个可用端口
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379

camellia-redis-proxy-nacos:
  enable: false #是否从nacos获取配置文件
  server-addr: 127.0.0.1:8848 #nacos地址
  nacos-conf: #其他nacos配置项
    k1: v1
    k2: v2
  conf-file-list:
    - file-name: camellia-redis-proxy.properties #文件名
      data-id: camellia-redis-proxy.properties #nacos的dataId
      group: camellia #nacos的group
    - file-name: logback.xml #文件名
      data-id: logback.xml
      group: camellia
```
上述配置表示把proxy的camellia-redis-proxy.properties和logback.xml这两份配置文件托管到nacos

### nacos安装
参见：https://nacos.io/zh-cn/docs/quick-start.html  
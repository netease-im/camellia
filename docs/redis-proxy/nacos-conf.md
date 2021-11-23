
## 使用nacos托管proxy的配置

### maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-nacos-spring-boot-starter</artifactId>
    <version>1.0.43</version>
</dependency>
```

### 配置
```yaml
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
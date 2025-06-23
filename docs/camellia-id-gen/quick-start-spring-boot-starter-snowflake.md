
* 引入maven依赖
```
<dependencies>
    <dependency>
        <groupId>com.netease.nim</groupId>
        <artifactId>camellia-id-gen-snowflake-spring-boot-starter</artifactId>
        <version>1.3.6</version>
    </dependency>
    <dependency>
        <groupId>com.netease.nim</groupId>
        <artifactId>camellia-redis-spring-boot3-starter</artifactId>
        <version>1.3.6</version>
    </dependency>
</dependencies>
```

* 编写启动类
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.id.gen.springboot.snowflake"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```

* 配置application.yml
```yaml
server:
  port: 8081
spring:
  application:
    name: camellia-id-gen-snowflake
camellia-id-gen-snowflake:
  sequence-bits: 12 #序列号所占比特位数
  worker-id-bits: 10 #workerId所占的比特位数
  region-bits: 0 #单元id所占的比特位数，0表示不区分单元
  region-id: 0 #regionId，如果regionBits为0，则regionId必须为0
  worker-id: -1 #-1表示使用redis生成workerId
  redis-worker-id-gen-conf:
    namespace: camellia #使用redis生成workerId时不同的命名空间下，workerId生成互不干扰
    lock-expire-millis: 3000 #使用redis生成workerId时获取分布式锁时的超时时间
    renew-interval-millis: 1000 #使用redis生成workerId时续约workerId的间隔
    exit-if-renew-fail: false #如果续约失败（可能redis超时了，或者gc导致workerId被其他进程抢走了，概率较低），是否进程退出，默认false


camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379
  redis-conf:
    jedis:
      timeout: 2000
      min-idle: 0
      max-idle: 32
      max-active: 32
      max-wait-millis: 2000
    jedis-cluster:
      max-wait-millis: 2000
      min-idle: 0
      max-idle: 8
      max-active: 16
      max-attempts: 5
      timeout: 2000
```
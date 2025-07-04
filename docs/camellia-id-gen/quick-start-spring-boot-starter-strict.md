
* 引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-strict-spring-boot-starter</artifactId>
    <version>1.3.6</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-id-loader</artifactId>
    <version>1.3.6</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-spring-boot3-starter</artifactId>
    <version>1.3.6</version>
</dependency>
```

* 编写启动类：
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.id.gen.springboot.segment", "com.netease.nim.camellia.id.gen.springboot.idloader"})
@MapperScan("com.netease.nim.camellia.id.gen.springboot.idloader")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}

```

* 配置application.yml
```yaml
server:
  port: 8082
spring:
  application:
    name: camellia-id-gen-segment
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/test?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=false
    username: root
    password: root
    hikari:
      minimum-idle: 5
      idle-timeout: 180000
      maximum-pool-size: 10
      connection-timeout: 30000
      connection-test-query: SELECT 1


camellia-id-gen-strict:
  region-bits: 0 #单元id所占的比特位数，0表示不区分单元
  region-id: 0 #regionId，如果regionBits为0，则regionId必须为0
  region-id-shifting-bits: 0 #regionId左移多少位
  cache-key-prefix: strict #redis key的前缀
  lock-expire-millis: 3000 #redis缓存里id耗尽时需要穿透到db重新获取，为了控制并发需要一个分布式锁，这是分布式锁的超时时间
  cache-expire-seconds: 86400 #id缓存在redis里，redis key的过期时间，默认1天
  cache-hold-seconds: 10 #缓存里的id如果在短时间内被消耗完，则下次获取id时需要多获取一些，本配置是触发step调整的阈值
  max-retry: 1000 #缓存中id耗尽时穿透到db，其他线程等待重试的最大次数
  retry-interval-millis: 5 #缓存中id耗尽时穿透到db，其他线程等待重试的间隔
  default-step: 10 #默认每次从db获取的id个数，也是最小的个数
  max-step: 100 #根据id的消耗速率动态调整每次从db获取id的个数，这个是上限值



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

* 引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-segment-spring-boot-starter</artifactId>
    <version>1.3.0</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-id-loader</artifactId>
    <version>1.3.0</version>
</dependency>
```

* 编写启动类
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
  port: 8083
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


camellia-id-gen-segment:
  region-bits: 0 #region比特位，0表示不区分单元
  region-id: 0 #regionId，如果regionBits为0，则regionId必须为0
  region-id-shifting-bits: 0 #regionId左移多少位
  tag-count: 1000 #服务包括的tag数量，会缓存在本地内存，如果实际tag数超过本配置，会导致本地内存被驱逐，进而丢失部分id段，丢失后会穿透到数据库）
  step: 1000 #每次从数据库获取一批id时的批次大小
  max-retry: 500 #当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，本配置表示重试的次数
  retry-interval-millis: 10 #当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，表示重试间隔
```
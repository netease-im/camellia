

## 严格递增的id生成算法（基于数据库+redis）
### 特性
* id全局严格递增
* 支持根据tag维护多条序列，彼此独立
* 支持设置region标记（比特位数可以自定义），从而可以在单元化部署中保证不同单元之间id不冲突（每个单元内严格递增）
* 支持peek操作（获取当前最新id，但是不使用）
* 提供了一个spring-boot-starter，快速搭建一个基于数据库和redis的严格递增的发号器集群

### 原理
* 数据库记录每个tag当前分配到的id
* 每个发号器节点会从数据库中取一段id后塞到redis的list中（不同节点会通过分布式锁保证id不会乱序）
* 每个发号器节点先从redis中取id，如果取不到则穿透到数据库进行load
* redis中的id即将耗尽时会提前从db中load最新一批的id
* 发号器节点会统计每个批次分配完毕消耗的时间来动态调整批次大小
* 核心源码参见CamelliaStrictIdGen

### 用法(直接使用)
引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-core</artifactId>
    <version>a.b.c</version>
</dependency>
```
示例如下：
```java
public class CamelliaStrictIdGenTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) {
        CamelliaStrictIdGenConfig config = new CamelliaStrictIdGenConfig();

        config.setCacheKeyPrefix("strict");//redis key的前缀
        config.setDefaultStep(10);//默认每次从db获取的id个数，也是最小的个数
        config.setMaxStep(100);//根据id的消耗速率动态调整每次从db获取id的个数，这个是上限值
        config.setLockExpireMillis(3000);//redis缓存里id耗尽时需要穿透到db重新获取，为了控制并发需要一个分布式锁，这是分布式锁的超时时间
        config.setCacheExpireSeconds(3600*24);//id缓存在redis里，redis key的过期时间，默认1天
        config.setCacheHoldSeconds(10);//缓存里的id如果在短时间内被消耗完，则下次获取id时需要多获取一些，本配置是触发step调整的阈值
        config.setRegionBits(0);//单元id所占的比特位数，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
        config.setMaxRetry(10);//缓存中id耗尽时穿透到db，其他线程等待重试的最大次数
        config.setRetryIntervalMillis(5);//缓存中id耗尽时穿透到db，其他线程等待重试的间隔
        
        //设置redis template
        config.setTemplate(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        //设置IdLoader，可以使用数据库实现
        config.setIdLoader((tag, step) -> {
            IDRange idRange = new IDRange(id.get() + 1, id.addAndGet(step));
            System.out.println("load [" + idRange.getStart() + "," + idRange.getEnd() + "] in " + Thread.currentThread().getName());
            return idRange;
        });

        CamelliaStrictIdGen idGen = new CamelliaStrictIdGen(config);
        int i=2000;
        while (i -- > 0) {
            //可以获取最新的id，但是不使用
            System.out.println("peek, id = " + idGen.peekId("tag"));
            //获取最新的id
            System.out.println("get, id = " + idGen.genId("tag"));
        }
    }
}
```

### 用法（使用spring-boot-starter)
使用spring-boot-starter方式下，默认使用数据库来生成id  
引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-strict-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-id-loader</artifactId>
    <version>a.b.c</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
```
编写启动类：
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
配置application.yml
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
  regionBits: 0 #单元id所占的比特位数，0表示不区分单元
  regionId: 0 #regionId，如果regionBits为0，则regionId必须为0
  cache-key-prefix: strict #redis key的前缀
  lock-expire-millis: 3000 #redis缓存里id耗尽时需要穿透到db重新获取，为了控制并发需要一个分布式锁，这是分布式锁的超时时间
  cache-expire-seconds: 86400 #id缓存在redis里，redis key的过期时间，默认1天
  cache-hold-seconds: 10 #缓存里的id如果在短时间内被消耗完，则下次获取id时需要多获取一些，本配置是触发step调整的阈值
  max-retry: 10 #缓存中id耗尽时穿透到db，其他线程等待重试的最大次数
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
数据库建表语句：
```sql
CREATE TABLE `camellia_id_info` (
  `tag` varchar(512) NOT NULL COMMENT 'tag',
  `id` bigint(9) DEFAULT NULL COMMENT 'id',
  `createTime` varchar(2000) DEFAULT NULL COMMENT '创建时间',
  `updateTime` varchar(64) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='id生成表';
```
启动后访问相关接口：   
返回一个id：  
http://127.0.0.1:8082/camellia/id/gen/strict/genId?tag=a  
返回示例：  
```json
{
  "code": 200,
  "data": 5071,
  "msg": "success"
}
```
返回最新的id，但是不使用：  
http://127.0.0.1:8082/camellia/id/gen/strict/peekId?tag=a    
返回示例：  
```json
{
    "code": 200,
    "data": 5023,
    "msg": "success"
}
```

当使用spring-boot-starter部署了独立的发号器服务后，为了方便使用http方法访问相关接口，我们提供了一个简易的封装    
先引入maven依赖：  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-sdk</artifactId>
    <version>a.b.c</version>
</dependency>
```
示例代码如下：  
```java
public class CamelliaStrictIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8082");
        CamelliaStrictIdGenSdk idGenSdk = new CamelliaStrictIdGenSdk(config);
        System.out.println(idGenSdk.peekId("a"));
        System.out.println(idGenSdk.genId("a"));
    }
}
```

### 示例源码
[源码](/camellia-samples/camellia-id-gen-strict-samples)


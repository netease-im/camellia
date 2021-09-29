
## 趋势递增的id生成算法（基于数据库）  
### 特性
* 单节点递增，全局趋势递增，保证全局唯一
* 支持根据tag维护多条序列，彼此独立
* 支持设置region标记（比特位数可以自定义），从而可以在单元化部署中保证不同单元之间id不冲突
* 基于数据库保证id唯一，基于内存保证分配速度，基于预留buffer确保rt稳定
* 提供了一个spring-boot-starter，快速搭建一个基于数据库的发号器集群

### 原理
* 数据库表记录每个tag当前分配到的id，每个发号器节点每次从数据库取一段id保留在内存中作为buffer提升性能
* 内存buffer快用完时，提前从数据库load一批新id，避免内存buffer耗尽时引起rt周期性上升
* 如果设置region标记，则regionId会作为id的后几位，确保不同单元间的id整体是保持相同的趋势递增规律的
* 核心源码参见CamelliaSegmentIdGen

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
public class CamelliaSegmentIdGenTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) throws Exception {
        CamelliaSegmentIdGenConfig config = new CamelliaSegmentIdGenConfig();
        config.setStep(1000);//每次从数据库获取一批id时的批次大小
        config.setTagCount(1000);//服务包括的tag数量，会缓存在本地内存，如果实际tag数超过本配置，会导致本地内存被驱逐，进而丢失部分id段，丢失后会穿透到数据库）
        config.setMaxRetry(10);//当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，本配置表示重试的次数
        config.setRetryIntervalMillis(10);//当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，表示重试间隔
        config.setRegionBits(0);//region比特位，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
                
        //设置IdLoader，可以使用数据库实现        
        config.setIdLoader((tag, step) -> {
            IDRange idRange = new IDRange(id.get() + 1, id.addAndGet(step));
            System.out.println("load [" + idRange.getStart() + "-" + idRange.getEnd() + "] in " + Thread.currentThread().getName());
            return idRange;
        });
        CamelliaSegmentIdGen idGen = new CamelliaSegmentIdGen(config);
        int i=2000;
        while (i -- > 0) {
            //可以获取一批
            System.out.println(idGen.genIds("tag", 3));
//            Thread.sleep(1000);
            //也可以获取一个
            System.out.println(idGen.genId("tag"));
//            Thread.sleep(1000);
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
    <artifactId>camellia-id-gen-segment-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-id-loader</artifactId>
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
  regionBits: 0 #region比特位，0表示不区分单元
  regionId: 0 #regionId，如果regionBits为0，则regionId必须为0
  tag-count: 1000 #服务包括的tag数量，会缓存在本地内存，如果实际tag数超过本配置，会导致本地内存被驱逐，进而丢失部分id段，丢失后会穿透到数据库）
  step: 1000 #每次从数据库获取一批id时的批次大小
  max-retry: 10 #当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，本配置表示重试的次数
  retry-interval-millis: 10 #当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，表示重试间隔
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
http://127.0.0.1:8083/camellia/id/gen/segment/genId?tag=a  
返回示例：  
```json
{
  "code": 200,
  "data": 5071,
  "msg": "success"
}
```
返回多个id：  
http://127.0.0.1:8083/camellia/id/gen/segment/genIds?tag=a&count=3   
返回示例：  
```json
{
    "code": 200,
    "data": [
      5072,
      5073,
      5074
    ],
    "msg": "success"
}
```

当使用spring-boot-starter部署了独立的发号器服务后，为了方便使用http方法访问相关接口，我们提供了一个封装sdk（sdk支持缓存id用于提高性能）      
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
public class CamelliaSegmentIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8083");
        config.setMaxRetry(2);//重试次数
        config.getSegmentIdGenSdkConfig().setCacheEnable(true);//表示sdk是否缓存id
        config.getSegmentIdGenSdkConfig().setStep(200);//sdk缓存的id数
        CamelliaSegmentIdGenSdk idGenSdk = new CamelliaSegmentIdGenSdk(config);

        System.out.println(idGenSdk.genId("a"));
        System.out.println(idGenSdk.genIds("a", 3));

        long target = 10*10000;
        int i = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGenSdk.genId("a");
            i++;
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
            if (i >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //###idea里直接运行的简单测试结果：
        //开启sdk缓存并设置缓存step=200：
        //QPS=17286.084701815038
        //开启sdk缓存并设置缓存step=100：
        //QPS=8647.526807333103
        //不开启sdk缓存
        //QPS=4657.878801993572
    }
}

```

### 示例源码
[源码](/camellia-samples/camellia-id-gen-segment-samples)


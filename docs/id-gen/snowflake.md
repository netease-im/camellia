
## 雪花算法
### 特性
* 单节点递增，全局趋势递增，保证全局唯一
* 支持设置region标记，从而可以在单元化部署中保证不同单元之间id不冲突
* 默认提供了一种基于redis的workerId生成策略，避免手动设置workerId的繁琐  
* regionId、workerId、sequence的比特位数支持自定义配置
* 提供一个spring-boot-starter，快速搭建一个基于雪花算法的发号器集群

### 原理
* 生成的id是一个64位的数字：首位保留，41位表示时间戳，剩余22位可以灵活配置regionId、workerId、sequence的比特位分配比例（22位可以不用完，以便减少id的长度）  
* 每个region的每个发号器节点的workerId都不同，确保id不重复  
* id前缀是时间戳，确保趋势递增
* 每个ms内使用递增sequence确保唯一    
* 核心源码参见CamelliaSnowflakeIdGen  

### 用法（直接使用）
引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen</artifactId>
    <version>a.b.c</version>
</dependency>
```
示例如下：
```java
public class CamelliaSnowflakeIdGenTest {

    public static void main(String[] args) throws Exception {
        CamelliaSnowflakeConfig config = new CamelliaSnowflakeConfig();
        config.setRegionBits(0);//单元id所占的比特位数，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
        config.setWorkerIdBits(10);//workerId所占的比特位数
        config.setSequenceBits(12);//序列号所占比特位数
        //使用redis生成workerId
        config.setWorkerIdGen(new RedisWorkerIdGen(new CamelliaRedisTemplate("redis://@127.0.0.1:6379")));
        
        CamelliaSnowflakeIdGen idGen = new CamelliaSnowflakeIdGen(config);

        int i=2000;
        while (i -- > 0) {
            long id = idGen.genId();
            System.out.println(id);
            System.out.println(Long.toBinaryString(id));
            System.out.println(Long.toBinaryString(id).length());
//            TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(3));
        }
    }
}
```

### 用法（使用spring-boot-starter)
引入maven依赖
```
<dependencies>
    <dependency>
        <groupId>com.netease.nim</groupId>
        <artifactId>camellia-id-gen-snowflake-spring-boot-starter</artifactId>
        <version>a.b.c</version>
    </dependency>
    <dependency>
        <groupId>com.netease.nim</groupId>
        <artifactId>camellia-redis-spring-boot-starter</artifactId>
        <version>a.b.c</version>
    </dependency>
</dependencies>
```
编写启动类：
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.id.gen.springboot.snowflake"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```
配置application.yml
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
启动后访问：  
http://127.0.0.1:8081/camellia/id/gen/snowflake/genId  
返回示例：  
```json
{
  "code": 200,
  "data": 6393964107649080,
  "msg": "success"
}
```

### 示例源码
[源码](/camellia-samples/camellia-id-gen-snowflake-samples)
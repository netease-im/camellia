
### 适配SpringRedisTemplate
如果你本来使用的是SpringRedisTemplate，但是也想拥有CamelliaRedisTemplate的分片、读写分离、双写等能力，camellia提供了CamelliaRedisTemplateRedisConnectionFactory来做适配

#### 手动适配
首先引入以下依赖：  
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-spring-redis-base</artifactId>
  <version>a.b.c</version>
</dependency>
```

示例代码：  
```java
public class TestSpringRedisTemplate {

    public static void main(String[] args) {
        //首先，你需要初始化一个CamelliaRedisTemplate
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
        //其次，再初始化一个CamelliaRedisTemplateRedisConnectionFactory
        CamelliaRedisTemplateRedisConnectionFactory connectionFactory = new CamelliaRedisTemplateRedisConnectionFactory(template);
        //然后，初始化Spring的RedisTemplate即可（这里使用了StringRedisTemplate），大部分情况下，spring会自动帮你装配好了
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();
        //然后，你就可以使用了
        redisTemplate.opsForValue().set("k1", "v2");
        String k1 = redisTemplate.opsForValue().get("k1");
        System.out.println(k1);
    }
}
```

#### 自动适配
你可以使用spring-boot-starter来自动适配Spring的RedisTemplate到CamelliaRedisTemplate，首先需要引入以下依赖：

```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-spring-temaplate-adaptor-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
```

其次，在application.yml配置如下：

```yaml
#
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

#默认就是true
camellia-redis-spring-template-adaptor:
  enable: true
```

则spring-boot-starter会自动注入CamelliaRedisTemplate实例和CamelliaRedisTemplateRedisConnectionFactory实例，从而自动注入的SpringRedisTemplate就是一个使用了CamelliaRedisTemplate适配器的实例了  

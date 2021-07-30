
### 适配SpringRedisTemplate
如果你本来使用的是SpringRedisTemplate，但是也想拥有CamelliaRedisTemplate的分片、读写分离、双写等能力，camellia提供了CamelliaRedisTemplateRedisConnectionFactory来做适配

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
        //首先你需要初始化一个CamelliaRedisTemplate
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
        //其次，再初始化一个CamelliaRedisTemplateRedisConnectionFactory
        CamelliaRedisTemplateRedisConnectionFactory connectionFactory = new CamelliaRedisTemplateRedisConnectionFactory(template);
        //然后初始化Spring的RedisTemplate即可（这里使用了StringRedisTemplate），大部分情况下，spring会自动帮你装配好了
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();
        //然后你就可以使用了
        redisTemplate.opsForValue().set("k1", "v2");
        String k1 = redisTemplate.opsForValue().get("k1");
        System.out.println(k1);
    }
}
```

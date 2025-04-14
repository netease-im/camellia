
## 使用spring-boot-starter来使用CamelliaRedisTemplate

### maven依赖
* 底层依赖jedis-2.9.3
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-spring-boot-starter</artifactId>
  <version>1.3.5</version>
</dependency>
```
* 底层依赖jedis-3.6.3
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis3-spring-boot-starter</artifactId>
  <version>1.3.5</version>
</dependency>
```

### 自动注入
使用camellia-redis-spring-boot-starter后，在application.yml里做好相关配置，则spring会自动注入一个CamelliaRedisTemplate实例，你可以直接使用@Autowired去使用，如下：
```java
@SpringBootApplication
public class Application {

    @Autowired
    private CamelliaRedisTemplate template;

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

}
```

### yml配置示例

#### 单点redis
```yaml
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
### redis-cluster
```yaml
camellia-redis:
  type: local
  local:
    resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
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
### 使用json自定义配置（需要单独的一个json文件）  
```yaml
camellia-redis:
  type: local
  local:
    type: complex
    json-file: resource-table.json   #默认去classpath下寻找文件，也可以设置一个绝对路径的文件地址
    dynamic: true  #设置为true则会动态检查文件resource-table.json是否有变更，并自动reload
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
```yaml
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "4": {
        "read": "redis://password1@127.0.0.1:6379",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381",
      "1-3-5": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
### 使用dashboard动态配置
```yaml
camellia-redis:
  type: remote
  remote:
    bid: 1
    bgroup: default
    url: http://127.0.0.1:8080
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
### 使用自定义动态配置
```yaml
camellia-redis:
  type: custom
  custom:
    resource-table-updater-class-name: com.netease.nim.camellia.redis.samples.CustomRedisTemplateResourceTableUpdater
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
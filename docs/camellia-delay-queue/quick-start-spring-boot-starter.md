
* 建立一个spring-boot工程，引入maven依赖(java21)
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-server-spring-boot-starter</artifactId>
    <version>1.3.3</version>
</dependency>
```

* 编写main方法
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.delayqueue.server"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

* 增加application.yml文件，主要是设置redis地址
```yaml

server:
  port: 8080
spring:
  application:
    name: camellia-delay-queue-server

camellia-delay-queue-server:
  ttl-millis: 3600000 #消息延迟时间到达转为可消费状态后，多久没有被成功消费后被删除，默认1h，提交消息时可以对每条消息都设置，如果不设置则走这个默认值
  max-retry: 10 #消息延迟时间到达转为可消费状态后，最多被消费几次后还未成功ack后，也会标记为删除，默认10次，提交消息时可以对每条消息都设置，如果不设置则走这个默认值
  ack-timeout-millis: 30000 #每次消息被消费后的ack超时时间，消费者来拉取时可以设置，如果没有设置，则使用本默认值
#  monitorIntervalSeconds: 60 #监控数据刷新周期，默认60s
#  namespace: default #命名空间，默认default
#  schedule-thread-num: 4 #定时器的线程池大小，默认是cpu数，一般不需要特殊配置
#  msg-schedule-millis: 100 #定时器的轮询间隔，代表了延迟消息的时间精确度，默认100ms，一般不需要特殊配置
#  topic-schedule-seconds: 600 #扫描topic是否活跃的间隔，默认600s，一般不需要特殊配置
#  check-trigger-thread-num: 32 #扫描消息是否可消费的线程池大小，默认是cpu数*4，一般不需要特殊配置
#  check-timeout-thread-num: 32 #扫描消息是否消息超时的线程池大小，默认是cpu数*4，一般不需要特殊配置
#  end-life-msg-expire-millis: 3000000 #消息到达终态（成功消息or过期or重试次数超限等），消息继续保留用于查询的缓存时间，默认5分钟
#  topic-active-tag-timeout-millis: 1800000 #一个topic多久不活跃（没有待消费的消息，也没有针对该topic的增删改查操作）会被回收相关资源，默认30分钟


camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379  #redis-standalone
  #    resource: redis://passwd@127.0.0.1:6379  #redis-standalone with password
  #    resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381 #redis-cluster
  #    resource: redis-cluster://passwd@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381 #redis-cluster with password
  #    resource: redis-sentinel://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381/masterName  #redis-sentinel
  #    resource: redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:16380,127.0.0.1:16381/masterName  #redis-sentinel with password
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


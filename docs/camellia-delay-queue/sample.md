
## producer示例
建立一个spring-boot工程，引入maven依赖：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-sdk-spring-boot-starter</artifactId>
    <version>1.3.7</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
增加application.yml文件，主要是配置delay-queue-server的地址（可以基于nginx配置一个域名，也可以基于注册中心）
```yaml
server:
  port: 8081
spring:
  application:
    name: camellia-delay-queue-producer


camellia-delay-queue-sdk:
  url: http://127.0.0.1:8080
  listener-config:
    ack-timeout-millis: 30000 #消费时告知服务器的消费ack超时时间，默认30s，添加listener时可以单独设置，如果未设置，则走本默认配置
    pull-batch: 1 #每次pullMsg时的批量大小，默认1，添加listener时可以单独设置，如果未设置，则走本默认配置，需要特别注意pull-batch和ack-timeout-millis的关系，避免未及时ack被服务器判断超时导致重复消费
    pull-interval-time-millis: 100 #pullMsg的轮询间隔，默认100ms，添加listener时可以单独设置，如果未设置，则走本默认配置
    pull-threads: 1 #每个listener的默认pullMsg线程数量，默认1，添加listener时可以单独设置，如果未设置，则走本默认配置
    consume-threads: 1 #每个listener的消息消费线程数量，默认1，添加listener时可以单独设置，如果未设置，则走本默认配置
  http-config:
    connect-timeout-millis: 5000 #到server的http超时配置，默认5000，一般不需要特殊配置
    read-timeout-millis: 5000 #到server的http超时配置，默认5000，一般不需要特殊配置
    write-timeout-millis: 500 #到server的http超时配置，默认5000，一般不需要特殊配置
    max-requests: 4096 #到server的http配置，一般不需要特殊配置
    max-requests-per-host: 1024 #到server的http配置，一般不需要特殊配置
    max-idle-connections: 1024 #到server的http配置，一般不需要特殊配置
    keep-alive-seconds: 30 #到server的http配置，一般不需要特殊配置
```
编写生产入口代码，spring会自动注入CamelliaDelayQueueSdk
```java
@RestController
public class ProducerController {

    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    @Autowired
    private CamelliaDelayQueueSdk delayQueueSdk;

    @RequestMapping("/sendDelayMsg")
    public CamelliaDelayMsg sendDelayMsg(@RequestParam("topic") String topic,
                                         @RequestParam("msg") String msg,
                                         @RequestParam("delaySeconds") long delaySeconds,
                                         @RequestParam(value = "ttlSeconds", required = false, defaultValue = "30") long ttlSeconds,
                                         @RequestParam(value = "maxRetry", required = false, defaultValue = "3") int maxRetry) {
        logger.info("sendDelayMsg, topic = {}, msg = {}, delaySeconds = {}, ttlSeconds = {}, maxRetry = {}", topic, msg, delaySeconds, ttlSeconds, maxRetry);
        return delayQueueSdk.sendMsg(topic, msg, delaySeconds, TimeUnit.SECONDS, ttlSeconds, TimeUnit.SECONDS, maxRetry);
    }
}
```
增加主类，启动即可

## consumer示例
建立一个spring-boot工程，引入maven依赖：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-sdk-spring-boot-starter</artifactId>
    <version>1.3.7</version>
</dependency>
```
增加application.yml文件，主要是配置delay-queue-server的地址（可以基于nginx配置一个域名，也可以基于注册中心）
```yaml
server:
  port: 8081
spring:
  application:
    name: camellia-delay-queue-consumer


camellia-delay-queue-sdk:
  url: http://127.0.0.1:8080
  listener-config:
    ack-timeout-millis: 30000 #消费时告知服务器的消费ack超时时间，默认30s，添加listener时可以单独设置，如果未设置，则走本默认配置
    pull-batch: 1 #每次pullMsg时的批量大小，默认1，添加listener时可以单独设置，如果未设置，则走本默认配置，需要特别注意pull-batch和ack-timeout-millis的关系，避免未及时ack被服务器判断超时导致重复消费
    pull-interval-time-millis: 100 #pullMsg的轮询间隔，默认100ms，添加listener时可以单独设置，如果未设置，则走本默认配置，短轮询时本配置生效
    pull-threads: 1 #每个listener的默认pullMsg线程数量，默认1，添加listener时可以单独设置，如果未设置，则走本默认配置
    consume-threads: 1 #每个listener的消息消费线程数量，默认1，添加listener时可以单独设置，如果未设置，则走本默认配置
    long-polling-enable: true #是否开启长轮询，默认true
    long-polling-timeout-millis: 10000 #长轮询的超时时间，默认10s
  http-config:
    connect-timeout-millis: 5000 #到server的http超时配置，默认5000，一般不需要特殊配置
    read-timeout-millis: 5000 #到server的http超时配置，默认5000，一般不需要特殊配置
    write-timeout-millis: 500 #到server的http超时配置，默认5000，一般不需要特殊配置
    max-requests: 4096 #到server的http配置，一般不需要特殊配置
    max-requests-per-host: 1024 #到server的http配置，一般不需要特殊配置
    max-idle-connections: 1024 #到server的http配置，一般不需要特殊配置
    keep-alive-seconds: 30 #到server的http配置，一般不需要特殊配置
```
编写CamelliaDelayMsgListener：
```java
/**
 * 使用spring托管
 * 此时需要service实现CamelliaDelayMsgListener接口，并且添加@CamelliaDelayMsgListenerConfig注解设置topic以及其他参数
 * Created by caojiajun on 2022/7/21
 */
@Component
@CamelliaDelayMsgListenerConfig(topic = "topic1", pullThreads = 1, consumeThreads = 3)
public class ConsumerService1 implements CamelliaDelayMsgListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService1.class);

    @Override
    public boolean onMsg(CamelliaDelayMsg delayMsg) {
        try {
            logger.info("onMsg, time-gap = {}, delayMsg = {}", System.currentTimeMillis() - delayMsg.getTriggerTime(), JSONObject.toJSONString(delayMsg));
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;//返回false，则delay-queue-server会重试
        }
    }
}
```
编写主类，启动即可，则consumer会消费对应的topic的消息

直接依次启动server、producer、consumer即可，随后你可以通过producer往delay-queue-server发送一条延迟消息：
```
#表示给topic1发送了一条消息，内容是abc，延迟10s执行
curl 'http://127.0.0.1:8081/sendDelayMsg?topic=topic1&msg=abc&delaySeconds=10'
```
随后，你可以观察delay-queue-server和consumer的日志

对于producer和consumer，除了引入spring-boot-starter，也可以引入裸的sdk包，自己去new一个CamelliaDelayQueueSdk实例，随后进行消息的发送和消费监听：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-sdk</artifactId>
    <version>1.3.7</version>
</dependency>
```
示例代码：
```java
public class TestMain {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService1.class);

    public static void main(String[] args) {
        CamelliaDelayQueueSdkConfig config = new CamelliaDelayQueueSdkConfig();
        config.setUrl("http://127.0.0.1:8080");
        CamelliaDelayQueueSdk sdk = new CamelliaDelayQueueSdk(config);
        
        //发送消息
        sdk.sendMsg("topic1", "abc", 10, TimeUnit.SECONDS);
        
        //消费消息
        sdk.addMsgListener("topic1", delayMsg -> {
            try {
                logger.info("onMsg, time-gap = {}, delayMsg = {}", 
                        System.currentTimeMillis() - delayMsg.getTriggerTime(), JSONObject.toJSONString(delayMsg));
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        });
    }
}
```

## 一个简单的性能测试
* 台式机（cpu=i5-10500），使用idea直接跑（没有调启动参数），先启动camellia-delay-queue-server-samples，再启动PerformanceTest.java
* 100个topic，每个topic1000条消息，每个topic设置10个消费线程，每条消息延迟10s-70s不等（随机），10w条消息在41s内发送完毕
* 消费端的延迟（实际消费时间和预期消费时间的GAP），平均43ms，最大798ms，可以看到没有delay不稳定的情况  
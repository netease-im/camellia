
# camellia-delay-queue
## 介绍
基于redis实现的一款延迟队列服务

## 特性
* 基于redis实现，底层使用CamelliaRedisTemplate，支持redis-standalone、redis-sentinel、redis-cluster
* 对外以http接口方式暴露服务，语言无关，对于消费端当前基于pull模型，未来会提供push模型
* 提供了camellia-delay-queue-server-spring-boot-starter，快速部署delay-queue-server集群
* 支持节点水平扩展，支持多topic
* 提供丰富的监控数据
* 提供了一个java-sdk，也提供了camellia-delay-queue-sdk-spring-boot-starter，方便快速接入

## 服务架构
<img src="camellia-delay-queue.jpg" width="60%" height="60%">

## 架构说明
### 名词解释
* topic，delay-queue-server通过topic来区分不同的延迟队列
* namespace，每个delay-queue-server都会有一个命名空间，不同的server可以通过不同的redis地址来隔离，也可以通过不同的namespace来隔离
* msg，提交给delay-queue-server的消息，是一个字符串，不建议太长，避免占用过多的redis内存资源
* msgId，当msg提交给delay-queue-server后，服务器会返回一个msgId作为唯一标识，你可以基于这个msgId去进行查询和删除msg
* delayMillis，消息的延迟时间，单位ms，表示提交msg之后多久可以被消费
* ttlMillis，表示消息的过期时间，指的是消息状态转变为可消费后，多久之内如果未被成功消费，则会被删除
* maxRetry，表示消费时的最大重试次数，最多消费次数是maxRetry+1，ttlMillis和maxRetry任一一个满足，消息将不再被投递给消费者

### 原理解析
* 基本原理基于redis+定时器实现
* 服务器主要包括五个数据结构    

|数据结构|类型|redis结构说明|功能|
|:---:|:---:|:---:|:---:|
|waitingQueue|ZSET|key=前缀+namespace+topic<br>value=msgId<br>score=消息触发时间戳（如果是延迟10s，则为服务器收到消息时的当前时间戳+10s）|服务器会启动定时器定时扫描score大于当前时间戳的msgId，移动到readyQueue|
|readyQueue|LIST|key=前缀+namespace+topic<br>value=msgId<br>先进先出|当客户端来pullMsg时，会检查readyQueue中是否有待消费的消息，如果有，则把msgId从readyQueue移动到ackQueue|
|ackQueue|ZSET|key=前缀+namespace+topic<br>value=msgId<br>score=消息消费的超时时间（如果超时时间是30s，则为消息被pull时的服务器当前时间戳+30s）|当消息被pull后，msgId位于ackQueue中，服务器会启动定时器扫描消费超时的消息，移动回readyQueue中重试，如果已经超过最大重试次数，则直接删除|
|topicQueue|ZSET|key=前缀+namespace<br>value是topic<br>score=topic最近一次操作时间|所有的定时器的根节点来自于本队列中维护的topic，此外会检测非活跃的topic，并进行资源回收|
|msg|STRING|key=前缀+namespace+topic+msgId<br>value=msg|其他数据结构只记录msgId，此外，当消息被消费成功了或者过期了，消息不会立即删除，而是会再保存一小段时间用于一些查询请求|

* 服务器会启动多个扫描线程，扫描topic和消息的状态，当部署多节点时，各个节点会使用分布式锁来避免并发操作，同时也能提高效率
* 多个数据结构之间的状态转换，使用了redis的lua脚本来保证原子性
* 对外暴露的核心接口包括：sendMsg、pullMsg、ackMsg、getMsg、deleteMsg
* 此外还提供了getMonitorData、getTopicInfo、getTopicInfoList这样的监控接口用于暴露数据


## 快速开始

### 搭建delay-queue-server
建立一个spring-boot工程，引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-server-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
```
编写main方法
```java
@SpringBootApplication
@ComponentScan(basePackages = {"com.netease.nim.camellia.delayqueue.server"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
增加application.yml文件，主要是设置redis地址
```yaml
server:
  port: 8080
spring:
  application:
    name: camellia-delay-queue-server

camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379
  redis-conf:
    jedis:
      max-idle: 8
      min-idle: 0
      max-active: 8
      max-wait-millis: 2000
      timeout: 2000
```
随后，启动即可

### producer示例
建立一个spring-boot工程，引入maven依赖：  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-sdk-spring-boot-starter</artifactId>
    <version>a.b.c</version>
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

### consumer示例
建立一个spring-boot工程，引入maven依赖：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-delay-queue-sdk-spring-boot-starter</artifactId>
    <version>a.b.c</version>
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
```
编写CamelliaDelayMsgListener：
```java
/**
 * 使用spring托管
 * 此时需要service实现CamelliaDelayMsgListener接口，并且添加@CamelliaDelayMsgListenerConfig注解设置topic以及其他参数
 * Created by caojiajun on 2022/7/21
 */
@Component
@CamelliaDelayMsgListenerConfig(topic = "topic1")
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

上述示例代码参见：（see [sample-code](/camellia-samples/camellia-delay-queue-samples)）  
直接依次启动server、producer、consumer即可，随后你可以通过producer往delay-queue-server发送一条延迟消息：
```
#表示给topic1发送了一条消息，内容是abc，延迟10s执行
curl 'http://127.0.0.1:8081/sendDelayMsg?topic=topic1&msg=abc&delaySeconds=10'
```
随后，你可以观察delay-queue-server和consumer的日志

## 接口文档
* 对于java客户端，使用sdk基本满足了需求，如果是其他语言，可以基于delay-queue-server的服务器api自行封装sdk
* 服务器接口文档如下：

### 发送消息
POST /camellia/delayQueue/sendMsg HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8  

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|topic|string|是|topic|
|msg|string|是|消息内容|
|delayMillis|number|是|延迟时间，单位ms|
|ttlMillis|number|否|过期时间，单位ms，若不填或者小于等于0，则使用服务器默认配置（可以在服务器的application.yml里配置）|
|maxRetry|number|否|消费最大重试次数，若不填或者小于0，则使用服务器默认配置（可以在服务器的application.yml里配置）|

响应  
```json
{
  "code": 200,
  "msg": "success",
  "delayMsg":
  {
    "topic": "topic1",
    "msgId": "6faa7316bc504f97aa6dd03ae12a2170",
    "msg": "abc",
    "produceTime": 1658492212132,
    "triggerTime": 1658492222132,
    "expireTime": 1658492242132,
    "maxRetry": 3,
    "retry": 0,
    "status": 1
  }
}
```

### 删除消息
POST /camellia/delayQueue/deleteMsg HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|topic|string|是|topic|
|msgId|string|是|消息id|

响应
```json
{
  "code": 200,
  "msg": "success"
}
```

### pull消息用于消费
POST /camellia/delayQueue/pullMsg HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|topic|string|是|topic|
|ackTimeoutMillis|number|否|拉到的消息，多久之内ack，如果超时未ack，服务器将重试，如果不填或者小于0则使用服务器默认配置（可以在服务器的application.yml里配置）|
|batch|number|否|最多拉多少条，如果不填或者小于0则使用服务器默认配置（可以在服务器的application.yml里配置）|

响应
```json
{
  "code": 200,
  "msg": "success",
  "delayMsgList":
  [
    {
      "topic": "topic1",
      "msgId": "6faa7316bc504f97aa6dd03ae12a2170",
      "msg": "abc",
      "produceTime": 1658492212132,
      "triggerTime": 1658492222132,
      "expireTime": 1658492242132,
      "maxRetry": 3,
      "retry": 0,
      "status": 1
    },
    {
      "topic": "topic1",
      "msgId": "6faa7316bc504f97aa6dd03ae12a2171",
      "msg": "def",
      "produceTime": 1658492212132,
      "triggerTime": 1658492222132,
      "expireTime": 1658492242132,
      "maxRetry": 3,
      "retry": 0,
      "status": 1
    }
  ]
}
```

### ack消息
POST /camellia/delayQueue/ackMsg HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|topic|string|是|topic|
|msgId|string|是|消息id|

响应
```json
{
  "code": 200,
  "msg": "success"
}
```

### 获取消息
POST /camellia/delayQueue/getMsg HTTP/1.1  
Content-Type:application/x-www-form-urlencoded;charset=utf-8

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|topic|string|是|topic|
|msgId|string|是|消息id|

响应
```json
{
  "code": 200,
  "msg": "success",
  "delayMsg":
  {
    "topic": "topic1",
    "msgId": "6faa7316bc504f97aa6dd03ae12a2170",
    "msg": "abc",
    "produceTime": 1658492212132,
    "triggerTime": 1658492222132,
    "expireTime": 1658492242132,
    "maxRetry": 3,
    "retry": 0,
    "status": 1
  }
}
```

### 获取监控数据
GET /camellia/delayQueue/getMonitorData HTTP/1.1

响应
```json
{
    "code": 200,
    "data":
    {
        "requestStatsList":
        [
            {
                "topic": "topic1",
                "sendMsg": 35,
                "pullMsg": 12,
                "deleteMsg": 0,
                "ackMsg": 12,
                "getMsg": 0,
                "triggerMsgReady": 24,
                "triggerMsgEndLife": 0,
                "triggerMsgTimeout": 0
            },
            {
                "topic": "topic",
                "sendMsg": 34,
                "pullMsg": 0,
                "deleteMsg": 0,
                "ackMsg": 0,
                "getMsg": 0,
                "triggerMsgReady": 0,
                "triggerMsgEndLife": 0,
                "triggerMsgTimeout": 0
            }
        ],
        "pullMsgTimeGapStatsList":
        [
            {
                "topic": "topic1",
                "count": 12,
                "avg": 35.25,
                "max": 84
            }
        ],
        "readyQueueTimeGapStatsList":
        [
            {
                "topic": "topic1",
                "count": 12,
                "avg": 52.5,
                "max": 87
            }
        ]
    }
}
```

### 获取topic信息
GET /camellia/delayQueue/getTopicInfo HTTP/1.1

|参数|类型|是否必填|说明|
|:---:|:---:|:---:|:---:|
|topic|string|是|topic|

响应
```json
{
  "code": 200,
  "data": {
    "topic": "topic1",
    "waitingQueueSize": 11,
    "readyQueueSize": 0,
    "ackQueueSize": 0
  }
}
```

### 获取topic信息列表
GET /camellia/delayQueue/getTopicInfoList HTTP/1.1

响应
```json
{
  "code": 200,
  "data": [
    {
      "topic": "topic",
      "waitingQueueSize": 0,
      "readyQueueSize": 0,
      "ackQueueSize": 0
    },
    {
      "topic": "topic1",
      "waitingQueueSize": 0,
      "readyQueueSize": 0,
      "ackQueueSize": 0
    }
  ]
}
```


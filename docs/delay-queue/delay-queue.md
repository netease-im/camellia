
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

|数据结构|redis结构|redis结构说明|功能|
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


## 接口文档


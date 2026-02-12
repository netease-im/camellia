
## MqMultiWriteProxyPlugin和KafkaMqPackConsumerProxyPlugin

### 说明
* 基于mq（当前支持kafka）的异步双写plugin
* 这个plugin不是内置的（因为要引入kafka-sdk），因此需要依赖引入，并且配置全类名
* 需要跨机房或者异地机房的redis数据双写同步，可以用于数据的迁移或者容灾
* 备注一：只有proxy完整支持的命令集合中的写命令支持本模式，对于那些限制性支持的命令（如阻塞型命令、发布订阅命令等）是不支持使用MultiWriteProxyPlugin来双写的
* 备注二：redis事务包裹的写命令使用MqMultiWriteProxyPlugin双写时可能主路由执行失败而双写成功  

### 架构简图
<img src="redis-proxy-mq-multi-write.png" width="50%" height="50%">

* kafka包括生产和消费，因此会包括2个ProxyPlugin，分别是MqMultiWriteProducerProxyPlugin和KafkaMqPackConsumerProxyPlugin
* 其中生产端会拦截request，并投递给kafka
* 消费端只是单纯启动plugin（内部会启动消费线程），plugin本身不会拦截任何请求

### 引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-mq-common</artifactId>
    <version>a.b.c</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-mq-kafka</artifactId>
    <version>a.b.c</version>
</dependency>
```

### 启用方法（生产端）
```properties
proxy.plugin.list=com.netease.nim.camellia.redis.proxy.mq.common.MqMultiWriteProducerProxyPlugin

#生产端kafka的地址和topic，反斜杠分隔
mq.multi.write.producer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka
#使用KafkaMqPackSender来进行mq的异步写入，你也可以自己实现一个走其他mq的
mq.multi.write.sender.class.name=com.netease.nim.camellia.redis.proxy.mq.kafka.KafkaMqPackSender
```

### 启用方式（消费端）
```properties
proxy.plugin.list=com.netease.nim.camellia.redis.proxy.mq.kafka.KafkaMqPackConsumerProxyPlugin

#消费端kafka的地址和topic，反斜杠分隔
mq.multi.write.consumer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka
```

你也可以同时配置生产者和消费者，从而实现如下效果：  
<img src="redis-proxy-mq-multi-write2.png" width="50%" height="50%">


此外，如果proxy启用了bid/bgroup，则该上下文信息也会随着kafka一起同步过来；proxy也支持同时写入/消费多组kafka，如下：
```
#生产端，竖线分隔可以表示多组kafka和topic
mq.multi.write.producer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka|127.0.0.2:9092,127.0.0.2:9093/camellia_multi_write_kafka2
#生产端还支持对不同的bid/bgroup设置不同的kafka写入地址，如下表示bid=1,bgroup=default的写入地址
1.default.mq.multi.write.producer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka

#消费端，竖线分隔可以表示多组kafka和topic
mq.multi.write.consumer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka|127.0.0.2:9092,127.0.0.2:9093/camellia_multi_write_kafka2
```


其他几个可配参数：
* 消费kafka的双写任务时，默认情况下，consumer会直接把任务发送给后端redis（异步的），如果连续的几个命令归属于相同的bid/bgroup下，则consumer会批量投递，单次批量默认最大是200，可以通过mq.multi.write.commands.max.batch=200来修改
* 如果希望双写异常时进行重试，则需要先开启mq.multi.write.kafka.consumer.sync.enable=true，随后通过mq.multi.write.kafka.consume.retry=3参数来配置重试次数，此时如果后端redis连接不可用时consumer会进行重试，重试间隔1s/2s/3s/4s/...依次增加
* 在开启mq.multi.write.kafka.consumer.sync.enable=true时，因为要支持重试，为了避免kafka的consumer触发rebalance，consumer会使用pause/commitSync来手动控制消费的速度，并且会使用一个内存队列来为缓冲，缓冲队列的容量可以通过mq.multi.write.kafka.consume.queue.size=100来配置
* 在开启mq.multi.write.kafka.consumer.sync.enable=true时，因为要支持重试，同时为了保证命令执行顺序，所有命令是依次执行的，不支持批量
* 相关参数的含义以及其他参数，可见源码KafkaMqPackConsumer.java

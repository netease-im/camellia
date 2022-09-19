## 控制
camellia-redis-proxy提供了自定义命令拦截器来达到控制客户端访问的目的，此外也针对某些业务场景提供了若干具体实现，你可以按需使用：  
* 自定义CommandInterceptor示例  
* TroubleTrickKeysCommandInterceptor 用于临时屏蔽某些key的访问    
* MultiWriteCommandInterceptor 用于自定义配置双写策略(key级别)   
* RateLimitCommandInterceptor 用于控制客户端请求速率（支持全局速率控制，也支持bid/bgroup级别的速率控制）
* MqMultiWriteCommandInterceptor 用于基于mq（如kafka等）的异步双写
* DynamicCommandInterceptorWrapper 可以组合多个CommandInterceptor的一个包装类
* IPCheckerCommandInterceptor 可以根据客户端ip进行权限校验，支持黑名单模式和白名单模式
* DelayDoubleDeleteCommandInterceptor 透明的支持缓存key的延迟双删

### 自定义CommandInterceptor
如果你想添加一个自定义的方法拦截器，则应该实现CommandInterceptor接口，类似于这样：
```java
package com.netease.nim.camellia.redis.proxy.samples;

public class CustomCommandInterceptor implements CommandInterceptor {

    private static final CommandInterceptResponse KEY_TOO_LONG = new CommandInterceptResponse(false, "key too long");
    private static final CommandInterceptResponse VALUE_TOO_LONG = new CommandInterceptResponse(false, "value too long");
    private static final CommandInterceptResponse FORBIDDEN = new CommandInterceptResponse(false, "forbidden");

    @Override
    public CommandInterceptResponse check(Command command) {
        CommandContext commandContext = command.getCommandContext();
        Long bid = commandContext.getBid();
        String bgroup = commandContext.getBgroup();
        if (bid == null || bgroup == null || bid != 100000 || !bgroup.equals("default")) {
            return CommandInterceptResponse.SUCCESS;
        }
        SocketAddress clientSocketAddress = commandContext.getClientSocketAddress();
        if (clientSocketAddress instanceof InetSocketAddress) {
            String hostAddress = ((InetSocketAddress) clientSocketAddress).getAddress().getHostAddress();
            if (hostAddress != null && hostAddress.equals("10.128.1.1")) {
                return FORBIDDEN;
            }
        }
        List<byte[]> keys = command.getKeys();
        if (keys != null && !keys.isEmpty()) {
            for (byte[] key : keys) {
                if (key.length > 256) {
                    return KEY_TOO_LONG;
                }
            }
        }
        if (command.getRedisCommand() == RedisCommand.SET) {
            byte[][] objects = command.getObjects();
            if (objects.length > 3) {
                byte[] value = objects[2];
                if (value.length > 1024 * 1024 * 5) {
                    return VALUE_TOO_LONG;
                }
            }
        }
        return CommandInterceptResponse.SUCCESS;
    }
}



```
随后，在application.yml里这样配置：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomCommandInterceptor
```
上面的配置表示：  
* 限制特定ip不得访问  
* key的长度不得超过256  
* 如果是一个set命令过来，value的长度不得超过5M  

### TroubleTrickKeysCommandInterceptor
#### 用途
用于临时屏蔽某些key的访问

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.command.async.interceptor.TroubleTrickKeysCommandInterceptor
```
随后可以动态的在camellia-redis-proxy.properties里配置需要拦截的key以及方法，如下：
```
#表示：针对key1和key2的ZREVRANGEBYSCORE方法，针对key3和key4的GET方法，会被拦截（直接返回错误信息）
trouble.trick.keys=ZREVRANGEBYSCORE:["key1","key2"];GET:["key3","key4"]

#表示：bid=2/bgroup=default路由配置下，针对key1和key2的ZRANGE方法，针对key3和key4的SMEMBERS方法，会被拦截（直接返回错误信息）
2.default.trouble.trick.keys=ZRANGE:["key1","key2"];SMEMBERS:["key3","key4"]
```

#### 使用场景
由于业务侧bug或者其他一些原因，某些key成为热点key（比如死循环调用），为了避免持续的请求导致后端redis服务异常（比如cpu持续高负荷运转或者被打挂），可以通过TroubleTrickKeysCommandInterceptor来临时屏蔽相关key的访问


### MultiWriteCommandInterceptor
#### 用途
用于自定义双写策略  
可以自定义key级别的双写策略，如：某些key需要双写，某些key不需要双写，某些key双写到redisA，某些key双写到redisB  
备注一：只有proxy完整支持的命令集合中的写命令支持本模式，对于那些限制性支持的命令（如阻塞型命令、发布订阅命令等）是不支持使用MultiWriteCommandInterceptor来双写的  
备注二：redis事务包裹的写命令使用MultiWriteCommandInterceptor双写时可能主路由执行失败而双写成功  

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.command.async.interceptor.MultiWriteCommandInterceptor
```
随后，你需要实现MultiWriteFunc接口，表示自定义的双写策略，并将实现类的全类名配置在camellia-redis-proxy.properties，如下：
```
#表示MultiWriteCommandInterceptor使用自定义双写策略
multi.write.func.class.name=com.netease.nim.camellia.redis.proxy.samples.CustomMultiWriteFunc
```
CustomMultiWriteFunc的一个例子，如下：
```java
public class CustomMultiWriteFunc implements MultiWriteCommandInterceptor.MultiWriteFunc {

    @Override
    public MultiWriteCommandInterceptor.MultiWriteInfo multiWriteInfo(MultiWriteCommandInterceptor.KeyInfo keyInfo) {
        byte[] key = keyInfo.getKey();
        if (Utils.bytesToString(key).equals("k1")) {
            return new MultiWriteCommandInterceptor.MultiWriteInfo(true, "redis://abc@127.0.0.1:6390");
        }
        return MultiWriteCommandInterceptor.MultiWriteInfo.SKIP_MULTI_WRITE;
    }
}
```
上面的例子中表示k1这个key需要双写，且双写到redis://abc@127.0.0.1:6390，其他key不需要双写

#### 使用场景
某些业务场景中（如金融合规要求），不需要全量双写，则可以使用MultiWriteCommandInterceptor来自定义key级别的双写策略  

### RateLimitCommandInterceptor
#### 用途
可以用于控制客户端的请求速率，支持全局级别的速率控制，也支持bid/bgroup级别

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.command.async.interceptor.RateLimitCommandInterceptor
```
随后你可以在camellia-redis-proxy.properties配置速率上限（默认不限制）：
```
#全局级别的速率控制（下面的例子表示proxy全局最多允许1000ms内10w次请求，超过会返回错误）
##检查周期
rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
rate.limit.max.count=100000

#bid/bgroup级别的速率控制（下面的例子表示bid=1，bgroup=default的请求，最多允许1000ms内10w次请求，超过会返回错误）
##检查周期
1.default.rate.limit.check.millis=1000
##最大请求次数，如果小于0，则不限制，如果等于0，则会拦截所有请求
1.default.rate.limit.max.count=100000
```

#### 使用场景
* 如果业务侧bug或者滥用导致不符合预期的高tps，为了保护后端redis或者保护proxy，可以临时配置速率限制
* 当使用bid/bgroup代理多组路由配置时，使用bid/bgroup级别的速率控制，避免某个业务异常引起全局异常


### MqMultiWriteCommandInterceptor
#### 用途
可以用于基于mq（如kafka等）的异步双写，如跨机房、异地场景下redis的数据双写同步

#### 架构简图
<img src="redis-proxy-mq-multi-write.png" width="50%" height="50%">

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```
MqMultiWriteCommandInterceptor依赖MqPackSender接口，camellia提供了一个默认实现（KafkaMqPackSender）  
随后你需要在camellia-redis-proxy.properties里配置启用KafkaMqPackSender，并配置好kafka的地址，如下：  
```
#生产端kafka的地址和topic，反斜杠分隔
mq.multi.write.producer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka
#使用KafkaMqPackSender来进行mq的异步写入
mq.multi.write.sender.className=com.netease.nim.camellia.redis.proxy.mq.kafka.KafkaMqPackSender
```
上面的例子表示所有到proxy的写请求（限制性命令不支持，如阻塞型命令、发布订阅命令等），均会写入kafka  
随后，你需要再启动一个proxy作为kafka的消费者，如下：  
```yaml
server:
  port: 6381
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6378
```
上面的例子表示启用KafkaMqPackConsumer作为方法拦截器，该拦截器会从kafka消费信息，并根据上下文写入到后端的redis中  
为了正确工作，你还需要在camellia-redis-proxy.properties配置kafka的地址和topic，如下  
```
#消费端kafka的地址和topic，反斜杠分隔
mq.multi.write.consumer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka
```
最后依次启动两个proxy，当你连接6380端口的proxy写入一个set命令，命令在写入127.0.0.1:6379这个redis的同时   
还会通过kafka=127.0.0.1:9092,127.0.0.1:9093的camellia_multi_write_kafka这个topic，进入到6381这个proxy，并写入到127.0.0.1:6378这个redis  

此外，如果proxy启用了bid/bgroup，则该上下文信息也会随着kafka一起同步过来；proxy也支持同时写入/消费多组kafka，如下：  
```
#生产端，竖线分隔可以表示多组kafka和topic
mq.multi.write.producer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka|127.0.0.2:9092,127.0.0.2:9093/camellia_multi_write_kafka2
#生产端还支持对不同的bid/bgroup设置不同的kafka写入地址，如下表示bid=1,bgroup=default的写入地址
1.default.mq.multi.write.producer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka

#消费端，竖线分隔可以表示多组kafka和topic
mq.multi.write.consumer.kafka.urls=127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka|127.0.0.2:9092,127.0.0.2:9093/camellia_multi_write_kafka2
```

如果你想proxy同时是生产者和消费者（kafkaUrl1的生产者、kafkaUrl2的消费者），则可以配置以下CommandInterceptor：  
```
com.netease.nim.camellia.redis.proxy.mq.kafka.KafkaMqPackProducerConsumer
```
通过对2个机房的proxy配置不同的kafka生产消费地址，搭配使用，可以实现如下效果：  
<img src="redis-proxy-mq-multi-write2.png" width="50%" height="50%">

使用KafkaMqPackProducerConsumer/KafkaMqPackConsumer的其他几个可配参数：  
* 消费kafka的双写任务时，默认情况下，consumer会直接把任务发送给后端redis（异步的），如果连续的几个命令归属于相同的bid/bgroup下，则consumer会批量投递，单次批量默认最大是200，可以通过mq.multi.write.commands.max.batch=200来修改
* 如果希望双写异常时进行重试，则需要先开启mq.multi.write.kafka.consumer.sync.enable=true，随后通过mq.multi.write.kafka.consume.retry=3参数来配置重试次数，此时如果后端redis连接不可用时consumer会进行重试，重试间隔1s/2s/3s/4s/...依次增加
* 在开启mq.multi.write.kafka.consumer.sync.enable=true时，因为要支持重试，为了避免kafka的consumer触发rebalance，consumer会使用pause/commitSync来手动控制消费的速度，并且会使用一个内存队列来为缓冲，缓冲队列的容量可以通过mq.multi.write.kafka.consume.queue.size=100来配置
* 在开启mq.multi.write.kafka.consumer.sync.enable=true时，因为要支持重试，同时为了保证命令执行顺序，所有命令是依次执行的，不支持批量
* 相关参数的含义以及其他参数，可见源码KafkaMqPackConsumer.java

#### 使用场景
* 需要跨机房或者异地机房的redis数据双写同步，可以用于数据的迁移或者容灾

### DynamicCommandInterceptorWrapper
#### 用途
当需要同时使用多个CommandInterceptor来实现相关业务功能时，可以通过组合的方式来实现，camellia默认提供了DynamicCommandInterceptorWrapper来实现组合的效果  
DynamicCommandInterceptorWrapper会依次执行各个拦截器，如果中间出现一个拦截器返回不通过，则执行链会被打断而直接返回   

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.command.async.interceptor.DynamicCommandInterceptorWrapper
```
随后你可以在camellia-redis-proxy.properties里配置如下：
```
##竖线分隔多个CommandInterceptor实现的全类名
dynamic.command.interceptor.class.names=com.netease.nim.camellia.samples.CustomCommandInterceptor1|com.netease.nim.camellia.samples.CustomCommandInterceptor2
```
上述配置表示DynamicCommandInterceptorWrapper组合了CustomCommandInterceptor1和CustomCommandInterceptor2两个拦截器的功能   
特别的，DynamicCommandInterceptorWrapper支持动态修改配置的方式来动态调整组合的拦截器列表


### IPCheckerCommandInterceptor
#### 用途
当需要通过识别客户端ip来控制proxy的访问权限时，可以使用本拦截器，支持黑名单模式和白名单模式  
你还可以根据bid/bgroup设置不同的ip拦截策略

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.command.async.interceptor.IPCheckerCommandInterceptor
```
随后你可以在camellia-redis-proxy.properties里配置如下：
```
#黑名单示例（支持ip，也支持网段，逗号分隔）：
ip.check.mode=1
ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
```
```
#白名单示例（支持ip，也支持网段，逗号分隔）：
ip.check.mode=2
ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
```
```
#根据bid/bgroup设置不同的策略：
#黑名单示例（表示bid=1,bgroup=default的黑名单配置）：
1.default.ip.check.mode=1
1.default.ip.black.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
```
```
#白名单示例（表示bid=1,bgroup=default的白名单配置）：
1.default.ip.check.mode=2
1.default.ip.white.list=2.2.2.2,5.5.5.5,3.3.3.0/24,6.6.0.0/16
```


### DelayDoubleDeleteCommandInterceptor
#### 用途
为了确保缓存和数据库的一致性，有一种策略是缓存双删，可以在业务上实现，也可以在proxy上透明的实现  
如果配置了DelayDoubleDeleteCommandInterceptor，则proxy会拦截所有DEL命令，如果key前缀匹配，则会触发一个延迟的DEL任务，表现在redis-server层就是会收到2次DEL命令

#### 配置示例
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
  command-interceptor-class-name: com.netease.nim.camellia.redis.proxy.command.async.interceptor.DelayDoubleDeleteCommandInterceptor
```

随后你可以在camellia-redis-proxy.properties里配置如下：
```
#首先要开启，默认是false
delay.double.del.enable=true

#其次要配置延迟双删的秒数，如果<=0，则不生效，默认-1
double.del.delay.seconds=5

#最后还要配置匹配哪些key去做延迟删除，是一个json array，如果不配置也不生效
##如果所有DEL命令中的key都要延迟双删，则配置前缀为空串
#double.del.key.prefix=[""]
##如果只是部分命令，如只有dao_cache和cache前缀的key才延迟双删，则可以如下配置
#double.del.key.prefix=["dao_cache", "cache"]

```

你也可以根据bid和bgroup分别配置，如下：
```
#首先要开启，默认是false
1.default.delay.double.del.enable=true

#其次要配置延迟双删的秒数，如果<=0，则不生效，默认-1
1.default.double.del.delay.seconds=5

#最后还要配置匹配哪些key去做延迟删除，是一个json array，如果不配置也不生效
##如果所有DEL命令中的key都要延迟双删，则配置前缀为空串
#1.default.double.del.key.prefix=[""]
##如果只是部分命令，如只有dao_cache和cache前缀的key才延迟双删，则可以如下配置
#1.default.double.del.key.prefix=["dao_cache", "cache"]

```
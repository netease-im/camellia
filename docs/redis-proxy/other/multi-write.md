
## 关于双写的若干问题

### 支持的双写方案
* 通过路由配置可以由proxy直接把写命令同时发给redisA和redisB，或者更多的redis集群，我们称之为直接双写模式
* 可以通过MqMultiWriteCommandInterceptor拦截器把写命令发送给mq（默认支持kafka，其他类型的mq可以自行扩展），并部署消费模式的proxy进行异步双写，我们称之为基于MQ的双写模式
* 如果有部分key双写，部分key双写的需求，可以通过MultiWriteCommandInterceptor拦截器，搭配自定义MultiWriteFunc实现，我们称之为key级别的自定义双写模式

备注，直接双写的三种模式：
#### first_resource_only
表示如果配置的第一个写地址返回了，则立即返回给客户端，这是默认的模式
#### all_resources_no_check
表示需要配置的所有写地址都返回了，才返回给给客户端，返回的是第一个地址的返回结果，你可以这样配置来生效这种模式：
```yaml
camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      json-file: resource-table.json
    redis-conf:
      multi-write-mode: all_resources_no_check
```
#### all_resources_check_error
表示需要配置的所有写地址都返回了，才返回给客户端，并且会校验是否所有地址都是返回的非error结果，如果是，则返回第一个地址的返回结果；否则返回第一个错误结果，你可以这样配置来生效这种模式：
```yaml
camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      json-file: resource-table.json
    redis-conf:
      multi-write-mode: all_resources_check_error
```  

### 双写延迟问题
#### 直接双写模式
* proxy同时和多个redis后端建立长链接，写命令同时写入（proxy是非阻塞模型）
* 直接双写模式下，默认第一个redis后端返回成功即返回客户端写命令执行成功，此外还可以支持配置多个后端同时返回成功后再回复客户端写入成功，此时可以认为双写几乎没有延迟
* 此外，1.0.44版本开始，支持单独监控每个后端redis的响应时间，业务可以使用该指标来判断双写模式的延迟（netty队列和tcp队列的延迟会反应在该指标上）

#### 基于MQ的双写模式
* proxy只和其中一个redis后端建立长链接，写命令发给该redis后端同时，会一起发给MQ，跨机房的proxy会从MQ消费数据进行写命令的异步写入
* 该模式下，如果需要监控双写的延迟问题，可以通过监控MQ本身的消费延迟情况来判断

#### 监控双写延迟的另外一个方法
* 比如要通过proxy双写redisA和redisB，不管是直接双写模式还是基于MQ的双写模式，你可以通过一个监控脚本，定时给proxy发送一个写命令（如setex）
* 随后立即直连redisA和redisB去get该key，如果get到了说明没有延迟，如果有一个没有get到，则触发等待和重试，直到获取到为止，从而计算出双写延迟的大小

### 双写可靠性
* 因为redis本身不支持分布式事务，因此无法保证两个redis同时写入成功或者写入失败
* 但是在直连双写模式下，因为和后端redis建立长链接，一般情况下不会出现部分成功，除非网络故障或者后端redis异常，或者proxy本身宕机
* 在直连双写模式下，你还可以支持配置两个redis同时返回成功后再返回客户端成功，在这种模式下，对于客户端来说可以认为，如果写命令返回了成功，可以确保两个redis都写入成功
* 在基于MQ的双写模式下，通过MQ来保证写命令的不丢失

### 双写一致性
* 你可以通过一些工具，来判断多个redis集群的数据一致性，如redis-full-check，见：https://developer.aliyun.com/article/690463    

### 事务命令的双写（TRANSACTION）
* 要求读地址只能配置一个，且必须和写地址的第一个是一样的
* proxy会把MULTI和EXEC之间的命令缓存在proxy内存中，并且监听EXEC的返回，如果返回为成功，则把缓存的命令刷新给双写的后端redis中（也就是写地址串列表中除第一个地址之外的其他地址），否则会丢弃缓存的的命令，从而尽可能保证双写数据的一致性

### 发布订阅命令的双写（PUBSUB）
* proxy会订阅双写地址中的第一个地址
* proxy会发布消息给双写地址中的所有地址

### 阻塞性命令不支持双写
* 如BLPOP等
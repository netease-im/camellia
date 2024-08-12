
## camellia-redis-proxy-kv

基于camellia-redis-proxy的可插拔架构设计，支持对接外部kv存储，模拟redis协议    
可以参考：[article](article.md)  

## 基本架构

![img.png](img.png)

* proxy基于伪redis-cluster模式运行，因此相同key会路由到同一个proxy节点
* proxy内部多work-thread运行，每个命令根据key哈希到同一个work-thread运行
* proxy本身弱状态
* proxy依赖的服务逻辑上包括三组：key-meta-server、sub-key-server、redis-cache-server（可选）
* key-meta-server，用于维护key的meta信息，包括key的类型、版本、ttl等，可以基于hbase/tikv/obkv实现
* sub-key-server，用于存储hash中的field等subkey，可以基于hbase/tikv/obkv实现
* 部分场景下（当前仅zset），可以在sub-key-server层，混合使用redis作为storage，更适合某些业务场景
* 对于hbase/tikv/obkv的访问有一个抽象层，也可以替换为其他kv存储
* 参考了 [pika](https://github.com/OpenAtomFoundation/pika) 、 [kvrocks](https://github.com/apache/kvrocks) 、 [tidis](https://github.com/yongman/tidis)、 [titan](https://github.com/distributedio/titan)、 [titea](https://github.com/distributedio/titan) 的设计
* 使用gc机制来回收kv存储层的过期数据，具体见: [gc](gc.md)
* 如何从kv数据结构映射到redis的复杂数据结构，具体见：[key-encode](key-encode.md)

## 编译

请参考：[camellia-redis-proxy-bootstrap](../other/camellia-redis-proxy-bootstrap)

## 配置列表

### 通用配置
具体见：[kv-conf](kv-conf.md)

### hbase配置
具体见：[kv-hbase-conf](kv-hbase-conf.md)

### tikv配置
具体见：[kv-tikv-conf](kv-tikv-conf.md)

### obkv配置
具体见：[kv-tikv-conf](kv-obkv-conf.md)

## 支持的command列表

|     command      |                                                             info |
|:----------------:|-----------------------------------------------------------------:|
|       del        |                                              `DEL key [key ...]` |
|      exists      |                                           `EXISTS key [key ...]` | 
|      expire      |                      `EXPIRE key seconds [NX \| XX \| GT \| LT]` |
|     pexpire      |                `PEXPIRE key milliseconds [NX \| XX \| GT \| LT]` |
|     expireat     |          `EXPIREAT key unix-time-seconds [NX \| XX \| GT \| LT]` |
|    pexpireat     |    `PEXPIREAT key unix-time-milliseconds [NX \| XX \| GT \| LT]` |
|      unlink      |                                           `UNLINK key [key ...]` |
|       type       |                                                       `TYPE key` |
|       ttl        |                                                        `TTL key` |
|       pttl       |                                                       `PTTL key` |
|    expiretime    |                                                 `EXPIRETIME key` |
|   pexpiretime    |                                                `PEXPIRETIME key` |
|       hset       |                         `HSET key field value [field value ...]` |
|      hmset       |                        `HMSET key field value [field value ...]` |
|       hget       |                                                 `HGET key field` |
|      hmget       |                                    `HMGET key field [field ...]` |
|       hdel       |                                     `HDEL key field [field ...]` |
|     hgetall      |                                                    `HGETALL key` |
|       hlen       |                                                       `HLEN key` |
|      hkeys       |                                                      `HKEYS key` |
|      hvals       |                                                      `HVALS key` |
|     hexists      |                                              `HEXISTS key field` |
|      hsetnx      |                                         `HSETNX key field value` |
|     hstrlen      |                                              `HSTRLEN key field` |
|       zadd       |                     `ZADD key score member [score member   ...]` |
|      zcard       |                                                      `ZCARD key` |
|      zrange      |                             `ZRANGE key start stop [WITHSCORES]` |
|  zrangebyscore   |    `ZRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]` |
|   zrangebylex    |                   `ZRANGEBYLEX key min max [LIMIT offset count]` |
|    zrevrange     |                          `ZREVRANGE key start stop [WITHSCORES]` |
| zrevrangebyscore | `ZREVRANGEBYSCORE key min max [WITHSCORES] [LIMIT offset count]` |
|  zrevrangebylex  |                `ZREVRANGEBYLEX key min max [LIMIT offset count]` |
| zremrangebyrank  |                                 `ZREMRANGEBYRANK key start stop` |
| zremrangebyscore |                                   `ZREMRANGEBYSCORE key min max` |
|  zremrangebylex  |                                     `ZREMRANGEBYLEX key min max` |
|       zrem       |                                   `ZREM key member [member ...]` |
|      zscore      |                                              `ZSCORE key member` |
|      zcount      |                                             `ZCOUNT key min max` |
|    zlexcount     |                                          `ZLEXCOUNT key min max` |
|      zrank       |                                   `ZRANK key member [WITHSCORE]` |
|     zrevrank     |                                `ZREVRANK key member [WITHSCORE]` |
|     zmscore      |                                `ZMSCORE key member [member ...]` |
|       sadd       |                                   `SADD key member [member ...]` |
|     smembers     |                                                   `SMEMBERS key` |
|       srem       |                                   `SREM key member [member ...]` |
|   srandmember    |                                        `SRANDMEMBER key [count]` |
|       spop       |                                               `SPOP key [count]` |
|    sismember     |                                           `SISMEMBER key member` |
|    smismember    |                             `SMISMEMBER key member [member ...]` |
|      scard       |                                                      `SCARD key` |
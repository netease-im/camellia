
## camellia-redis-proxy-kv

基于camellia-redis-proxy的可插拔架构设计，支持对接外部kv存储，模拟redis协议

## 基本架构

![img.png](img.png)

* proxy基于redis-cluster模式运行，因此相同key会路由到同一个proxy节点（proxy节点扩缩容时需要精细化处理，todo）
* proxy内部多work-thread运行，每个命令根据key哈希到同一个work-thread运行
* proxy本身弱状态
* proxy依赖的服务逻辑上包括三组：key-meta-server、subkey-server、cache-server
* key-meta-server，用于维护key的meta信息，包括key的类型、版本、ttl等，可以基于hbase/tikv/obkv实现
* subkey-server，用于存储hash中的field等subkey，可以基于hbase/tikv/obkv实现
* cache-server，可选，基于redis，特点是key的ttl很短，并且允许换出，支持对key-meta-server和subkey-server分别增加cache层
* kv-storage层，抽象了简单的put/get/delete/scan接口，从而可以自由的选择hbase/tikv/obkv去实现key-meta-server和subkey-server，也可以基于其他kv存储
* 参考了 [pika](https://github.com/OpenAtomFoundation/pika) 、 [kvrocks](https://github.com/apache/kvrocks) 、 [tidis](https://github.com/yongman/tidis)、 [titan](https://github.com/distributedio/titan)、 [titea](https://github.com/distributedio/titan) 的设计
* 使用gc机制来回收kv存储层的过期数据（todo）

## key-meta结构

|           key            |                           value                           |
|:------------------------:|:---------------------------------------------------------:|
|   m# + namespace + key   |           1-bit + 1-bit + 8-bit + 8-bit + N-bit           |
| prefix + namespace + key | encode-version + type + key-version + expire-time + extra |

```java
public enum KeyType {
    string((byte) 1),
    hash((byte) 2),
    zset((byte) 3),
    list((byte) 4),
    set((byte) 5),
}
```

## db commands

| command |                                            info |
|:-------:|------------------------------------------------:|
|   del   |                             `DEL key [key ...]` |
| exists  |                          `EXISTS key [key ...]` | 
| expire  |                            `EXPIRE key seconds` |
| pexpire |                      `PEXPIRE key milliseconds` |
| unlink  |                          `UNLINK key [key ...]` |

## string数据结构

|           key            |                   value                   |
|:------------------------:|:-----------------------------------------:|
|   m# + namespace + key   |   1-bit + 1-bit + 8-bit + 8-bit + N-bit   |
| prefix + namespace + key | 0 + 1 + key-version + expire-time + value |

* 只有key-meta，没有subkey
* 没有专门的缓存结构，依赖于key-meta本身的缓存

| command |                                                                                                                                  info |
|:-------:|--------------------------------------------------------------------------------------------------------------------------------------:|
|  setex  |                                                                                                             `SETEX key seconds value` |
| psetex  |                                                                                                       `PSETEX key milliseconds value` | 
|   set   |  `SET key value [NX \| XX] [GET] [EX seconds \| PX milliseconds \| EXAT unix-time-seconds \| PXAT unix-time-milliseconds \| KEEPTTL]` |
|   get   |                                                                                                                             `GET key` |
|  mget   |                                                                                                                  `MGET key [key ...]` |

## hash数据结构

hash数据有两种编码模式，区别在于key-meta中是否记录field-count字段，也就是哈希中元素个数

### key-meta

#### version-0

|           key            |                      value                      |
|:------------------------:|:-----------------------------------------------:|
|   m# + namespace + key   |      1-bit + 1-bit + 8-bit + 8-bit + 4-bit      |
| prefix + namespace + key | 0 + 2 + key-version + expire-time + field-count |

* 优点：hlen快，hset/hdel返回结果准确
* 缺点：写操作的读放大多

#### version-1

|           key            |               value               |
|:------------------------:|:---------------------------------:|
|   m# + namespace + key   |   1-bit + 1-bit + 8-bit + 8-bit   |
| prefix + namespace + key | 0 + 2 + key-version + expire-time |

* 优点：写入快
* 缺点：hlen慢，hset/hdel等操作返回结果不准确

### hget-cache-key

|                 redis-key                  |            redis-type             | redis-value |
|:------------------------------------------:|:---------------------------------:|------------:|
| c# + namespace + key + key-version + field |              string               | field-value |

### hgetall-cache-key

|                 redis-key                 | redis-type |     redis-value |
|:-----------------------------------------:|:----------:|----------------:|
|    c# + namespace + key + key-version     |    hash    | full-hash-value |

### hash-field-store-key

|                      redis-key                       |    value    |
|:----------------------------------------------------:|:-----------:|
| s# + namespace + key.len + key + key-version + field | field-value |

### commands

| command |                                     info |          version-0           |          version-1           |
|:-------:|-----------------------------------------:|:----------------------------:|:----------------------------:|
|  hset   | `HSET key field value [field value ...]` | [hset-v0](./hash/hset-v0.md) | [hset-v1](./hash/hset-v1.md) |
|  hget   |                         `HGET key field` |    [hget](./hash/hget.md)    |          同version-0          |
|  hdel   |             `HDEL key field [field ...]` | [hdel-v0](./hash/hdel-v0.md) | [hdel-v1](./hash/hdel-v1.md) |
| hgetall |                            `HGETALL key` | [hgetall](./hash/hgetall.md) |          同version-0          |
|  hlen   |                               `HLEN key` | [hlen-v0](./hash/hlen-v0.md) | [hlen-v1](./hash/hlen-v1.md) |


## zset数据结构

todo

## list数据结构

todo

## set数据结构

todo
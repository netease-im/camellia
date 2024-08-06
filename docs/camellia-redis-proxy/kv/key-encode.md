
# key-encode

如何从kv数据结构映射到redis的复杂数据结构


## key-meta结构

|                  key                  |                             value                             |
|:-------------------------------------:|:-------------------------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |             1-bit + 1-bit + 8-bit + 8-bit + N-bit             |
| prefix + namespace + md5_prefix + key | encode-version + key-type + key-version + expire-time + extra |

```java
public enum KeyType {
    string((byte) 1),
    hash((byte) 2),
    zset((byte) 3),
    list((byte) 4),
    set((byte) 5),
}
```

* key-meta本身支持配置redis-cache-server，从而加快读写（可换出）
* key-version使用创建key时的时间戳表示
* expire-time记录key的过期时间戳，如果key没有ttl，则为-1
* extra取决于encode-version和type，可选


## string数据结构

|                  key                  |                   value                   |
|:-------------------------------------:|:-----------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |   1-bit + 1-bit + 8-bit + 8-bit + N-bit   |
| prefix + namespace + md5_prefix + key | 0 + 1 + key-version + expire-time + value |

* 只有一种编码结构，encode-version固定为0
* key-type固定为1
* 只有key-meta，没有sub-key
* 没有专门的缓存结构，依赖于key-meta本身的缓存

| command |                                                                                                                                 info |
|:-------:|-------------------------------------------------------------------------------------------------------------------------------------:|
|  setex  |                                                                                                            `SETEX key seconds value` |
| psetex  |                                                                                                      `PSETEX key milliseconds value` | 
|   set   | `SET key value [NX \| XX] [GET] [EX seconds \| PX milliseconds \| EXAT unix-time-seconds \| PXAT unix-time-milliseconds \| KEEPTTL]` |
|   get   |                                                                                                                            `GET key` |
|  mget   |                                                                                                                 `MGET key [key ...]` |
|  mset   |                                                                                                     `MSET key value [key value ...]` |
|  setnx  |                                                                                                                    `SETNX key value` |
| strlen  |                                                                                                                         `STRLEN key` |

## hash数据结构

hash数据有四种编码模式

### version-0

#### key-meta

|                  key                  |                      value                      |
|:-------------------------------------:|:-----------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |      1-bit + 1-bit + 8-bit + 8-bit + 4-bit      |
| prefix + namespace + md5_prefix + key | 0 + 2 + key-version + expire-time + field-count |

#### sub-key

|                         key                          |    value    |
|:----------------------------------------------------:|:-----------:|
| s# + namespace + key.len + key + key-version + field | field-value |

* encode-version固定为0，key-type固定为2
* 因为key-meta中记录了field-count，因此hlen快
* hset/hdel返回结果准确
* 写操作的读放大多，因为每次写入都需要读一下是否是已经存在的field还是新的field，如hset操作，前者返回0，后者返回1

### version-1

#### key-meta

|                  key                  |               value               |
|:-------------------------------------:|:---------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |   1-bit + 1-bit + 8-bit + 8-bit   |
| prefix + namespace + md5_prefix + key | 1 + 2 + key-version + expire-time |

#### sub-key

|                                 key                                  |    value    |
|:--------------------------------------------------------------------:|:-----------:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + field | field-value |

* encode-version固定为1，key-type固定为2
* 因为不在key-meta中记录field-count，纯覆盖写，写入快，但是导致了hlen慢
* hset/hdel等操作返回结果不准确，比如hset操作，不管写入的是已存在的field还是新的field，都返回1

### version-2和version-3

#### key-meta

* version-2同version-0，但是encode-version固定为2
* version-3同version-1，但是encode-version固定为3

#### sub-key

* version-2同version-0
* version-3同version-1

相比version-0和version-1，新增了redis缓存层

#### hget-cache-key

|                 redis-key                  | redis-type | redis-value |
|:------------------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version + field |   string   | field-value |

#### hgetall-cache-key

|             redis-key              | redis-type | redis-value |
|:----------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version |    hash    |   full-hash |



## zset数据结构

zset有5种编码结构

### version-0

#### key-meta

|                  key                  |                      value                       |
|:-------------------------------------:|:------------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |      1-bit + 1-bit + 8-bit + 8-bit + 4-bit       |
| prefix + namespace + md5_prefix + key | 0 + 3 + key-version + expire-time + member-count |

#### sub-key

|                                  key                                  | value |
|:---------------------------------------------------------------------:|:-----:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + member | score |

|                                      key                                      | value |
|:-----------------------------------------------------------------------------:|:-----:|
| k# + namespace + md5(key)[0-8] + key.len + key + key-version + score + member | null  |

* encode-version固定为0，key-type固定为3
* 不依赖任何redis
* key-meta中记录了member-count，zcard快
* 写操作的读放大多，因为每次写入都需要读一下是否是已经存在的member还是新的member，如zadd操作，前者返回0，后者返回1

### version-1

#### key-meta

|                  key                  |                      value                       |
|:-------------------------------------:|:------------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |      1-bit + 1-bit + 8-bit + 8-bit + 4-bit       |
| prefix + namespace + md5_prefix + key | 1 + 3 + key-version + expire-time + member-count |

#### sub-key

|                                  key                                  | value |
|:---------------------------------------------------------------------:|:-----:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + member | score |

#### redis-cache-key

|                redis-key                | redis-type | redis-value |
|:---------------------------------------:|:----------:|------------:|
|   c# + namespace + key + key-version    |    zset    |    full-zet |

* encode-version固定为1，key-type固定为3
* 依赖redis做复杂的zset操作
* 当redis过期时，需要从kv中全量导出所有数据重建cache

### version-2

#### key-meta

|                  key                  |                      value                       |
|:-------------------------------------:|:------------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |      1-bit + 1-bit + 8-bit + 8-bit + 4-bit       |
| prefix + namespace + md5_prefix + key | 0 + 3 + key-version + expire-time + member-count |

#### index

index=member.len < 15 ? (prefix1+member) : (prefix2+md5(member))

#### sub-key

|                                  key                                  | value |
|:---------------------------------------------------------------------:|:-----:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + member | score |

|                                 key                                  | value  |
|:--------------------------------------------------------------------:|:------:|
| i# + namespace + md5(key)[0-8] + key.len + key + key-version + index | member |

* 如果member很小，则只有第一个sub-key

#### redis-index-zset-cache-key

|             redis-key              | redis-type | redis-value |
|:----------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version |    zset    |   index-zet |

#### redis-index-member-cache-key

|                 redis-key                  | redis-type | redis-value |
|:------------------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version + index |   string   |      member |

* encode-version固定为2，key-type固定为3
* 依赖redis做复杂的zset操作
* redis纯缓存使用，可以换出，且redis里可以只存部分数据
* 当redis过期时，需要从kv中导出数据重建cache（可以只导出index）

### version-3

#### key-meta

|                  key                  |               value               |
|:-------------------------------------:|:---------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |   1-bit + 1-bit + 8-bit + 8-bit   |
| prefix + namespace + md5_prefix + key | 3 + 3 + key-version + expire-time |

#### index

index=member.len < 15 ? (prefix1+member) : (prefix2+md5(member))

#### sub-key

|                                 key                                  | value  |
|:--------------------------------------------------------------------:|:------:|
| i# + namespace + md5(key)[0-8] + key.len + key + key-version + index | member |

* 如果member很小，则不会产生二级的index，只会在redis中写入，不会写入kv

#### redis-index-zset-store-key

|             redis-key              | redis-type | redis-value |
|:----------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version |    zset    |  index-zset |

* index-zset中可能存的是index，也可能不是index，通过前缀的第一个字节来判断
* 这部分redis数据不允许换出

#### redis-index-member-cache-key

|                 redis-key                  | redis-type | redis-value |
|:------------------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version + index |   string   |      member |

* encode-version固定为3，key-type固定为3
* 依赖redis做复杂的zset操作
* redis里可以只存部分数据，对于kv只有get/put/delete，没有scan操作
* redis-index-zset-store-key不属于cache，属于storage，需要确保storage部分redis内存足够，否则可能被驱逐


## set数据结构

set数据有四种编码模式

### version-0

#### key-meta

|                  key                  |                      value                       |
|:-------------------------------------:|:------------------------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |      1-bit + 1-bit + 8-bit + 8-bit + 4-bit       |
| prefix + namespace + md5_prefix + key | 0 + 2 + key-version + expire-time + member-count |

#### sub-key

|                          key                          | value |
|:-----------------------------------------------------:|:-----:|
| s# + namespace + key.len + key + key-version + member |  nil  |

* encode-version固定为0，key-type固定为2
* 因为key-meta中记录了member-count，因此scard快
* sadd/srem返回结果准确
* 写操作的读放大多，因为每次写入都需要读一下是否是已经存在的member还是新的member，如sadd操作，前者返回0，后者返回1

### version-1

#### key-meta

|                  key                  |               value               |
|:-------------------------------------:|:---------------------------------:|
| m# + namespace + md5(key)[0-8] + key  |   1-bit + 1-bit + 8-bit + 8-bit   |
| prefix + namespace + md5_prefix + key | 1 + 2 + key-version + expire-time |

#### sub-key

|                                  key                                  |    value    |
|:---------------------------------------------------------------------:|:-----------:|
| s# + namespace + md5(key)[0-8] + key.len + key + key-version + member | field-value |

* encode-version固定为1，key-type固定为2
* 因为不在key-meta中记录member-count，纯覆盖写，写入快，但是导致了scard慢
* sadd/srem等操作返回结果不准确，比如sadd操作，不管写入的是已存在的member还是新的member，都返回1

### version-2和version-3

#### key-meta

* version-2同version-0，但是encode-version固定为2
* version-3同version-1，但是encode-version固定为3

#### sub-key

* version-2同version-0
* version-3同version-1

相比version-0和version-1，新增了redis缓存层

#### smembers-cache-key

|             redis-key              | redis-type | redis-value |
|:----------------------------------:|:----------:|------------:|
| c# + namespace + key + key-version |    set     |    full-set |



## list数据结构

todo
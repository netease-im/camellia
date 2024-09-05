
# key-encode

* 如何从kv数据结构映射到redis的复杂数据结构
* 如果底层是tikv和hbase，则在key前面增加slot前缀（range分区）
* 如果底层是obkv，则slot作为obkv-table的hash分区的分区字段，key前面不需要加slot前缀


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


## hash数据结构

hash数据有2种编码模式

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


## zset数据结构

zset有2种编码结构

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

* encode-version固定为1，key-type固定为3
* 依赖redis做复杂的zset操作
* redis里可以只存部分数据，对于kv只有get/put/delete，没有scan操作
* redis-index-zset-store-key不属于cache，属于storage，需要确保storage部分redis内存足够，否则可能被驱逐
* 在这个编码下，lex相关的命令（如zlexcount、zrangebylex等），需要先把所有元素load到本地内存中，再执行操作


## set数据结构

set数据有2种编码模式

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


## list数据结构

todo
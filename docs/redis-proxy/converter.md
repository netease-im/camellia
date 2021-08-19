
## key/value的自定义转换
camellia-redis-proxy提供了key/value的自定义转换功能，从而你可以自定义的实现数据的解压缩、加解密等功能
* 当前支持大部分命令的key转换  
* 当前支持string/set/list/hash/zset相关命令的value自定义转换  

### 原理
当一个命令请求到达proxy，proxy会根据命令的类型解析出特定的参数，并过一遍Converters的正向转换接口  
在该接口下，用户可以自己实现从originalValue/originalKey到convertedValue/convertedKey的变换逻辑，proxy会将转换后的命令发往后端redis节点    
当后端redis返回之后，proxy会解析回包，并过一遍Converters反向转换接口   
在该接口下，用户可以自己实现从convertedValue/convertedKey到originalValue/originalKey的变换逻辑，proxy会将转换后的originalValue/originalKey回给客户端    

### 当前支持对key转换的命令
带key的命令都支持，特别的，对于KEYS和SCAN命令，回调给KeyConverter接口的是pattern，而不是实际的key

### 当前支持value转换的数据结构和命令
以下命令会回调Converter接口进行value转换：
```
#String
SET,GETSET,SETNX,SETEX,PSETEX,MSET,MSETNX,GET,MGET,GETDEL,GETEX,
#List
RPUSH,LPUSH,LLEN,LRANGE,LINDEX,LSET,LREM,LPOP,RPOP,LINSERT,LPUSHX,RPUSHX,LPOS,BRPOP,BLPOP,
#Set
SADD,SMEMBERS,SREM,SPOP,SCARD,SISMEMBER,SRANDMEMBER,SSCAN,SMISMEMBER,
#Hash
HSET,HGET,HSETNX,HMSET,HMGET,HINCRBY,HEXISTS,HDEL,HKEYS,
HVALS,HGETALL,HINCRBYFLOAT,HSCAN,HSTRLEN,HRANDFIELD,
#ZSet
ZADD,ZINCRBY,ZRANK,ZSCORE,ZMSCORE,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZSCAN,
ZPOPMAX,ZPOPMIN,BZPOPMAX,BZPOPMIN,ZRANDMEMBER,
```
### 如何使用
你需要实现相关的Converter接口，包括：
* KeyConverter 可以对key进行转换
* StringConverter 可以对value进行转换
* HashConverter 可以对hash中的field和value进行转换
* ListConverter 可以对list中的每个元素进行转换
* SetConverter 可以对set中的每个成员进行转换
* ZSetConverter 可以对zset中的每个member进行转换

以StringConverter为例，接口定义如下：  
```java
public interface StringConverter {

    /**
     * 将原始value进行转换
     * @param commandContext command上下文
     * @param key 所属key
     * @param originalValue 原始value
     * @return 转换后的value
     */
    byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue);

    /**
     * 将转换后的value逆向恢复成原始value
     * @param commandContext command上下文
     * @param key 所属key
     * @param convertedValue 转换后的value
     * @return 原始value
     */
    byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue);

}
```
一个简单的示例：
```java
public class CustomStringConverter implements StringConverter {

    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        String keyStr = Utils.bytesToString(key);
        if (keyStr.equals("k1")) {
            if (originalValue == null) return null;
            String str = Utils.bytesToString(originalValue);
            return Utils.stringToBytes(str.replaceAll("abc", "***"));
        }
        return originalValue;
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        String keyStr = Utils.bytesToString(key);
        if (keyStr.equals("k1")) {
            if (convertedValue == null) return null;
            String str = Utils.bytesToString(convertedValue);
            return Utils.stringToBytes(str.replaceAll("\\*\\*\\*", "abc"));
        }
        return convertedValue;
    }
}

```
随后，你需要在application.yml里如下配置：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123   #proxy的密码
  converter-enable: true
  converter-config:
    #key-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomKeyConverter
    string-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomStringConverter
    #list-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomListConverter
    #hash-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomHashConverter
    #set-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomSetConverter
    #zset-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomZSetConverter
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```    

上述示例中，如果key是k1，则value里面的abc会被转换为***再存储到redis里面；当你get时，***会被转换回abc后再返回给客户端，整个过程对于客户端是透明的

key转换的例子：  
```java
public class CustomKeyConverter implements KeyConverter {

    private static final byte[] prefix = Utils.stringToBytes("v1");

    @Override
    public byte[] convert(CommandContext commandContext, RedisCommand redisCommand, byte[] originalKey) {
        byte[] result = new byte[prefix.length + originalKey.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(originalKey, 0, result, prefix.length, originalKey.length);
        return result;
    }

    @Override
    public byte[] reverseConvert(CommandContext commandContext, RedisCommand redisCommand, byte[] convertedKey) {
        if (convertedKey.length > prefix.length) {
            for (int i=0; i<prefix.length; i++) {
                if (prefix[i] != convertedKey[i]) {
                    return convertedKey;
                }
            }
            byte[] result = new byte[convertedKey.length - prefix.length];
            System.arraycopy(convertedKey, prefix.length, result, 0, result.length);
            return result;
        }
        return convertedKey;
    }
}
```
上述例子中表示key会增加前缀v1再转发给后端，后端的回包如果包含key信息（如RANDOMKEY、BLPOP等），也会反向转换后再回给调用方


### 使用CamelliaCompressor/CamelliaEncryptor来做压缩或者加解密
首先，你需要引入：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.0.36</version>
</dependency>
```

压缩的例子
```java
public class CompressStringConverter implements StringConverter {

    private final CamelliaCompressor compressor = new CamelliaCompressor();

    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        return compressor.compress(originalValue);
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        return compressor.decompress(convertedValue);
    }
}
```

加密的例子：
```java
public class EncryptStringConverter implements StringConverter {
    
    private final CamelliaEncryptor encryptor = new CamelliaEncryptor(new CamelliaEncryptAesConfig("abc"));
    
    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        return encryptor.encrypt(originalValue);
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        return encryptor.decrypt(convertedValue);
    }
}
```

进一步的，其实你可以先压缩，再加密，如下：
```java
public class CompressEncryptStringConverter implements StringConverter {

    private final CamelliaCompressor compressor = new CamelliaCompressor();
    private final CamelliaEncryptor encryptor = new CamelliaEncryptor(new CamelliaEncryptAesConfig("abc"));
    
    @Override
    public byte[] valueConvert(CommandContext commandContext, byte[] key, byte[] originalValue) {
        return encryptor.encrypt(compressor.compress(originalValue));
    }

    @Override
    public byte[] valueReverseConvert(CommandContext commandContext, byte[] key, byte[] convertedValue) {
        return compressor.decompress(encryptor.decrypt(convertedValue));
    }
}
```


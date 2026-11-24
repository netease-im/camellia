
## ConverterProxyPlugin

### 说明
* 一个用于透明的转换key/value的plugin，可以用于进行透明的数据加解密/解压缩，也可以用于
* 当前支持大部分命令的key转换
* 当前支持string/set/list/hash/zset相关命令的value自定义转换

### 原理
当一个命令请求到达proxy，proxy会根据命令的类型解析出特定的参数，并过一遍Converters的正向转换接口  
在该接口下，用户可以自己实现从originalValue/originalKey到convertedValue/convertedKey的变换逻辑，proxy会将转换后的命令发往后端redis节点    
当后端redis返回之后，proxy会解析回包，并过一遍Converters反向转换接口   
在该接口下，用户可以自己实现从convertedValue/convertedKey到originalValue/originalKey的变换逻辑，proxy会将转换后的originalValue/originalKey回给客户端

### 当前支持对key转换的命令
大部分带 key 的命令支持，特别的：
* 对于KEYS和SCAN命令，回调给KeyConverter接口的是pattern，而不是实际的key
* 对于PUBSUB系列命令，回调给KeyConverter接口的是pattern或者channel，而不是实际的key
* 对于 `FT.SUGADD/SUGGET/SUGDEL/SUGLEN`，只转换词典 key，建议词、查询前缀、score 和 payload 保持原样
* `FT.ALIASLIST/FT.HYBRID` 的索引名不属于数据 key，不调用 KeyConverter；查询参数和响应原样透传

`PUBSUB CHANNELS/SHARDCHANNELS` 未指定 pattern 时，会补充经过 KeyConverter 转换的 `*`，避免查询整个后端的频道列表；显式 pattern 也会转换，返回的频道名会逆转换；`NUMSUB/SHARDNUMSUB` 的请求频道和返回频道也会转换，订阅数保持原样。和 `KEYS/SCAN` 一样，自定义 converter 需要支持 pattern 转换。

`RANDOMKEY` 无法按命名空间筛选，配置 key converter 时默认在请求阶段拒绝，返回 `ERR RANDOMKEY is not supported by the active key converter`，不会转发到后端。
`DefaultMultiTenantNamespaceKeyConverter` 在当前租户转换关闭、bid/bgroup 缺失或 bid 不大于 0 时允许透传。未配置 key converter 时行为保持不变。
自定义 KeyConverter 只有在确认可以安全地从整个后端数据库采样时，才应重写 `isRandomKeySupported(CommandContext)` 返回 `true`；启用租户隔离时必须保持 `false`。

**TimeSeries 标签查询的兼容性变化：** 配置 key converter 时，`TS.QUERYINDEX/QUERYLABELS/MGET/MRANGE/MREVRANGE` 默认在请求阶段拒绝，返回 `ERR TimeSeries label queries are not supported by the active key converter`。这些命令按标签搜索整个后端，无法通用地按 key 命名空间限制，分组聚合也无法在返回阶段剔除其他租户的数据。
未配置 key converter 时保持原行为；内置 namespace converter 在租户转换关闭或租户上下文无效时允许透传。自定义 converter 只有在确认全库标签查询安全（例如后端按租户独立部署）时，才可重写 `isTimeSeriesLabelQuerySupported(CommandContext)` 返回 `true`。

TimeSeries 的单 key、多 key（含 `TS.MADD`、`TS.NRANGE/NREVRANGE`）和规则源/目标 key 均支持转换。
`TS.QUERYINDEX`、`TS.MGET`、未分组的 `TS.MRANGE/MREVRANGE` 返回的 key，以及 `TS.INFO` 的 `sourceKey`、`rules` 和 `keySelfName` 会逆转换。
时间戳、数值、标签和过滤表达式原样透传，不调用 String/Hash 等 value converter。
`GROUPBY` 的结果名是 `label=value`，不按 key 逆转换；分组响应内的标签（包括 `__source__`）也保持上游原样。

`SORT` 支持主 key 和 `STORE` 目标 key 的转换；`GEORADIUS/GEORADIUSBYMEMBER` 支持主 key 和 `STORE/STOREDIST` 目标 key 的转换。源和目标必须位于同一分片资源，Redis Cluster 下必须位于同一 slot。
这些可能写入数据的命令按写命令路由；纯读场景可使用 `SORT_RO`、`GEORADIUS_RO`、`GEORADIUSBYMEMBER_RO`。

**SORT 的限制（也适用于 SORT_RO）：** 启用 key converter 后，不支持通过 `BY/GET` 模式引用外部 key（包括 `pattern->field`）。Redis 在内部用元素值替换 `*` 后查找实际 key，proxy 无法对这些实际 key 调用任意 KeyConverter；模式参数会原样透传，不保证命名空间隔离。`BY nosort` 和 `GET #` 不引用外部 key，可以使用。
`SORT/SORT_RO` 不调用 List/Set/ZSet value converter，也不转换排序结果；启用元素编码后，不保证排序语义。`STORE` 直接写入 Redis 中的元素，要求源和目标的编码规则兼容。

`SET ... GET` 会逆转换返回的旧值；`HRANDFIELD ... WITHVALUES` 分别逆转换 field 和 value；`HSCAN ... NOVALUES` 只逆转换 field。
`HINCRBY/HINCRBYFLOAT` 仅转换 field，增量保持原样，存储值必须仍是 Redis 可识别的数字；`HSTRLEN` 转换 field 后返回存储值的字节长度，不保证等于编码前长度。
`ZRANK/ZREVRANK ... WITHSCORE` 只转换 member，选项、rank 和 score 保持原样。

感谢[@yangxb2010000](https://github.com/yangxb2010000)对key转换功能的增强

### 当前支持value转换的数据结构和命令
以下命令会回调Converter接口进行value转换：
```
#String
SET,GETSET,SETNX,SETEX,PSETEX,MSET,MSETNX,GET,MGET,GETDEL,GETEX,
#List
RPUSH,LPUSH,LLEN,LRANGE,LINDEX,LSET,LREM,LPOP,RPOP,LINSERT,LPUSHX,RPUSHX,LPOS,BRPOP,BLPOP,
LMOVEM,BLMOVEM,LMOVE,BLMOVE,RPOPLPUSH,BRPOPLPUSH,LMPOP,BLMPOP,
#Set
SADD,SMEMBERS,SREM,SPOP,SCARD,SISMEMBER,SRANDMEMBER,SSCAN,SMISMEMBER,SMOVE,SUNION,SINTER,SDIFF,
#Hash
HSET,HGET,HSETNX,HMSET,HMGET,HINCRBY,HEXISTS,HDEL,HKEYS,
HVALS,HGETALL,HINCRBYFLOAT,HSCAN,HSTRLEN,HRANDFIELD,
#ZSet
ZADD,ZINCRBY,ZRANK,ZSCORE,ZMSCORE,ZRANGE,ZRANGEBYSCORE,ZRANGEBYLEX,
ZREVRANK,ZREVRANGE,ZREVRANGEBYSCORE,ZREVRANGEBYLEX,ZREM,
ZREMRANGEBYRANK,ZREMRANGEBYSCORE,ZREMRANGEBYLEX,ZSCAN,
ZPOPMAX,ZPOPMIN,BZPOPMAX,BZPOPMIN,ZRANDMEMBER,
ZMOVE,BZMOVE,ZMOVEMEMBER,ZMPOP,BZMPOP,ZUNION,ZINTER,ZDIFF,
```

`LMOVEM`/`BLMOVEM` 和 `ZMOVE`/`BZMOVE`/`ZMOVEMEMBER` 会转换源 key 和目标 key，返回的元素/member 使用源 key 进行逆转换，score 保持原样。
`ZMOVEMEMBER` 还会转换 `MEMBERS` 数量指定的 member 参数，保留 `NX/XX` 等选项。
这些命令在 Redis 内部直接搬移数据，源和目标必须使用兼容的 value/member 编码规则；proxy 不会在搬移时重新编码目标数据。

`SET IFEQ/IFNE` 会转换比较值，过期参数以及 `IFDEQ/IFDNE` 的 digest 参数保持原样。比较条件要求 value 编码具有确定性；digest 对应 Redis 中的编码后值，可通过 `DIGEST` 获取。
`SMOVE` 按源 key 转换 member；`SUNION/SINTER/SDIFF` 按第一个源 key 逆转换结果，参与集合必须使用兼容的成员编码，proxy 不会替这些运算重新编码成员。
`LMOVE/BLMOVE/RPOPLPUSH/BRPOPLPUSH` 按源 key 逆转换返回元素，要求源/目标编码兼容。
`LMPOP/BLMPOP/ZMPOP/BZMPOP` 按响应中实际弹出的 key 逆转换嵌套元素/member，再逆转换该 key；ZSet score 保持原样。
缺值和超时的 nil 响应直接保留，不调用 value converter，避免修改共享的 nil 对象。

`ZUNION/ZINTER/ZDIFF` 按第一个源 key 逆转换 member，score 保持原样；参与的 ZSet 必须使用兼容的成员编码。
`XREAD/XREADGROUP` 返回的嵌套 key 会逆转换，消息 ID、field/value、group 和 consumer 保持原样，不调用 Hash/String value converter。

### 异常处理的兼容性变化

converter 抛出异常时，不再记录异常后继续透传。请求转换失败返回 `ERR request conversion failed`，不会转发尚未转换或部分转换的请求；响应转换失败返回 `ERR reply conversion failed`，不会将原始或部分解码的数据返回客户端，Pub/Sub 异步响应同样适用。
如果自定义 converter 曾用抛异常表示“保持原样”，需要改为返回输入值。错误回复不携带异常详情；响应转换失败发生在 Redis 执行之后，写入可能已经完成，不应据此盲目重试写命令。
空订阅确认中的 null 频道保持原样，不调用 KeyConverter。外部写入的、已带租户前缀但 hashtag 未带前缀的 key，逆转换会保留原 hashtag，包括短 hashtag。

### 匹配和范围语义的限制

`HSCAN/SSCAN/ZSCAN` 的 `MATCH` 模式原样透传，不调用 field/member converter；启用 field/member 编码后，不保证模式匹配仍具有原始语义。
ZSet 的字典序范围参数同样原样透传，任意成员加密或压缩无法保证原始字典序。使用这些选项前需确认编码保留匹配/顺序语义，否则不应使用；proxy 不会自动解码整个数据结构后在本地执行查询。

### 如何使用
你需要实现相关的Converter接口，包括：
* KeyConverter 可以对key进行转换
* StringConverter 可以对value进行转换
* HashConverter 可以对hash中的field和value进行转换
* ListConverter 可以对list中的每个元素进行转换
* SetConverter 可以对set中的每个成员进行转换
* ZSetConverter 可以对zset中的每个member进行转换

### 启用方式
```properties
proxy.plugin.list=converterPlugin
```

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

随后，你需要在camellia-redis-proxy.properties里配置：
```
#string的value的转换器
converter.string.class.name=com.xxxx.CustomStringConverter

##其他转换器
#converter.key.class.name=com.xxxx.CustomXXX
#converter.hash.class.name=com.xxxx.CustomXXX
#converter.set.class.name=com.xxxx.CustomXXX
#converter.zset.class.name=com.xxxx.CustomXXX
#converter.list.class.name=com.xxxx.CustomXXX
```

上述示例中，如果key是k1，则value里面的abc会被转换为***再存储到redis里面；当你get时，***会被转换回abc后再返回给客户端，整个过程对于客户端是透明的

此外，camellia默认提供了com.netease.nim.camellia.redis.proxy.plugin.converter.DefaultMultiTenantNamespaceKeyConverter这样的key转换器，你可以通过配置来生效。  
这个转换器会对不同的租户设置不同的key前缀，从而进行key的命名空间隔离  
```
#以bid=1,bgroup=default为例，key=abc，会被转换为key=1|default|abc
converter.key.class.name=com.netease.nim.camellia.redis.proxy.plugin.converter.DefaultMultiTenantNamespaceKeyConverter
```
如果需要关闭某个bid/bgroup的key前缀转换，可以配置`bid.bgroup.key.converter.disable=true`；该开关只支持租户粒度，如果所有租户都不需要转换，直接不启用该converter即可

### 使用CamelliaCompressor/CamelliaEncryptor来做透明的解压缩或者加解密
首先，你需要引入：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>a.b.c</version>
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


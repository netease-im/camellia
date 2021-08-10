
## value的自定义转换
camellia-redis-proxy提供了value的自定义转换功能，从而你可以自定义的实现数据的解压缩、加解密等功能  
当前支持string相关命令的value自定义转换  

### 原理
* 当一个读命令请求到达proxy，proxy会根据命令的类型解析出特定的参数，并过一遍Converters接口  
在该接口下，用户可以自己实现从originalValue到convertedValue的变换逻辑，proxy会将转换后的命令发往后端redis节点  
* 当一个写命令到达proxy，proxy会直接将该命令路由到后端redis节点，后端redis返回之后，proxy会解析回包，并过一遍Converters接口  
在该接口下，用户可以自己实现从convertedValue到originalValue的变换逻辑，proxy会将转换后的originalValue回给客户端  

### 当前支持value转换的数据结构和命令
#### string
支持的命令：
```
#写命令
SET,GETSET,SETNX,SETEX,PSETEX,MSET,MSETNX,
#读命令
GET,MGET,
```
为了达到拦截string相关命令并实现value转换，你需要实现以下接口：  
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
    string-converter-class-name: com.netease.nim.camellia.redis.proxy.samples.CustomStringConverter
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```    

上述示例中，如果key是k1，则value里面的abc会被转换为***再存储到redis里面；当你get时，***会被转换回abc后再返回给客户端，整个过程对于客户端是透明的
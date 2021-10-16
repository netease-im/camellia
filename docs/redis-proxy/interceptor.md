## 控制
camellia-redis-proxy提供了自定义命令拦截器来达到控制客户端访问的目的，此外也针对某些业务场景提供了若干具体实现，你可以按需使用：  
* 自定义CommandInterceptor示例  
* TroubleTrickKeysCommandInterceptor 用于临时屏蔽某些key的访问    
* MultiWriteCommandInterceptor 用于自定义配置双写策略(key级别)   
* RateLimitCommandInterceptor 用于控制客户端请求速率（支持全局速率控制，也支持bid/bgroup级别的速率控制）

备注：相关拦截器可以通过组合的方式一起使用  
  
### 自定义CommandInterceptor
如果你想添加一个自定义的方法拦截器，则应该实现CommandInterceptor接口，类似于这样：
```java
package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

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
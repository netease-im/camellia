
## 多租户
camellia-redis-proxy支持多租户，租户通过bid（数字）和bgroup（字符串）两个字段来唯一确认，对于不同的租户，你可以配置不同的后端redis，以及不同的监控参数（如大key的阈值）

### 怎么识别一个客户端连接归属于哪个租户

#### 默认使用clientname来标识
这个例子表示使用bid=10以及bgroup=default这个租户
```
➜ ~ ./redis-cli -h 127.0.0.1 -p 6380 -a pass123
127.0.0.1:6379> client setname camellia_10_default
OK
127.0.0.1:6380> set k1 v1
OK
127.0.0.1:6380> get k1
"v1"
127.0.0.1:6380> mget k1 k2 k3
1) "v1"
2) (nil)
3) (nil)
```
客户端示例：
如果端侧是Java，且使用了Jedis，则可以这样调用：
```java
public class Test {
    public static void main(String[] args) {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6380,
                2000, "pass123", 0, "camellia_10_default");
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.setex("k1", 10, "v1");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
```

#### 使用ClientAuthProvider接口自定义实现租户选择逻辑
ClientAuthProvider使用登录密码来区分不同的租户，camellia内置了一个DynamicConfClientAuthProvider实现，通过camellia-redis-proxy.properties来配置多租户映射关系  
启用方式： 
```yaml
camellia-redis-proxy:
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.DynamicConfClientAuthProvider
  transpond:
    type: remote
    remote:
      bid: 1
      bgroup: default
      url: http://xxx:8080
      monitor: true
      check-interval-millis: 5000    
```
随后你可以在camellia-redis-proxy.properties里这里配置：  
```
pass123.auth.conf=1|default
pass456.auth.conf=2|default
```
上述例子表示：
* 当使用密码pass123登录proxy时，使用bid=1,bgroup=default的路由
* 当使用密码pass456登录proxy时，使用bid=2,bgroup=default的路由


当然你也可以自己实现一个ClientAuthProvider，如下：
一个简单的例子：
```java
public class MockClientAuthProvider implements ClientAuthProvider {

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = new ClientIdentity();
        if (password.equals("pass1")) {
            clientIdentity.setPass(true);
            clientIdentity.setBid(1L);
            clientIdentity.setBgroup("default");
        } else if (password.equals("pass2")) {
            clientIdentity.setPass(true);
            clientIdentity.setBid(2L);
            clientIdentity.setBgroup("default");
        } else if (password.equals("pass3")) {
            clientIdentity.setPass(true);
        }
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }
}
```  
上面的例子表示：
* 当使用密码pass1登录proxy时，使用bid=1,bgroup=default的路由
* 当使用密码pass2登录proxy时，使用bid=2,bgroup=default的路由
* 当使用密码pass3登录proxy时，使用默认路由，也就是bid=1,bgroup=default


感谢[@yangxb2010000](https://github.com/yangxb2010000)提供上述功能



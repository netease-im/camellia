
# camellia-redis-zk
## 简介  
基于zk实现的一个用于camellia-redis-proxy的注册发现组件  
在camellia-redis-proxy-spring-boot-starter基础上，添加如下maven依赖  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-zk-registry-spring-boot-starter</artifactId>
    <version>a.b.c</version>
</dependency>
```
并且在application.yml添加如下配置（更多详细配置参考CamelliaRedisProxyZkRegistryProperties）  
```
camellia-zk:
  enable: true
  zk-url: 127.0.0.1:2181
  base-path: /camellia
```
则启动camellia-redis-proxy之后，会自动register到zk  
如果想手动取消注册，可以注入CamelliaRedisProxyZkRegisterBoot这个bean，随后调用deregister接口（示例参见camellia-redis-proxy-samples）  

客户端侧，需要引入以下依赖   
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-zk-discovery</artifactId>
    <version>a.b.c</version>
</dependency>
```

调用示例（使用RedisProxyJedisPool代替原始的JedisPool即可）  

```
public class Demo {
    public static void main(String[] args) {
        ZkProxyDiscovery discovery = new ZkProxyDiscovery("127.0.0.1:2181", "/camellia", "camellia-redis-proxy-server");
        RedisProxyJedisPool jedisPool = new RedisProxyJedisPool(discovery);
        try (Jedis jedis = jedisPool.getResource()) {
            String setex = jedis.setex("k1", 100, "v1");
            System.out.println(setex);
            String value = jedis.get("k1");
            System.out.println(value);
        }
    }
}
```

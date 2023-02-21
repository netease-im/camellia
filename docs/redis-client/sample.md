
### 更详细的一个例子
包括如何设置超时时间、连接池等相关参数，如何访问redis-cluster等，参见如下示例：
```java
public class TestCamelliaRedisTemplate {

    public static void test() {
        //设置连接池和超时参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMinIdle(0);
        jedisPoolConfig.setMaxIdle(32);
        jedisPoolConfig.setMaxTotal(32);
        jedisPoolConfig.setMaxWaitMillis(2000);
        int timeout = 2000;
        int maxAttempts = 5;
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(jedisPoolConfig, timeout))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(jedisPoolConfig, timeout, timeout, maxAttempts))
                .build();

        //1、访问单点redis
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis://passwd@127.0.0.1:6379"));
        //2、访问redis-sentinel
//        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:26379/master"));
        //3、访问redis-cluster
//        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379"));

        //传入CamelliaRedisEnv和ResourceTable，初始化CamelliaRedisTemplate对象
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, resourceTable);

        //所有方法的入参和返回均和jedis保持一致
        String value = template.get("k1");
        System.out.println(value);

        //pipeline也和jedis的pipeline类似，不同点在于，每次使用完pipeline对象，务必调用close方法；可以使用try-resource语法自动close（因为ICamelliaRedisPipeline实现了Closeable接口）
        try (ICamelliaRedisPipeline pipelined = template.pipelined();) {
            Response<String> r1 = pipelined.get("k1");
            Response<Long> r2 = pipelined.hset("hk1", "hv1", "1");
            pipelined.sync();
            System.out.println(r1.get());
            System.out.println(r2.get());
        }
    }

    public static void main(String[] args) {
        test();
    }
}
```
CamelliaRedisTemplate初始化需要两个参数：
* CamelliaRedisEnv  
描述了一些配置信息，包括连接池参数、超时、redis-cluster的重试次数等     
CamelliaRedisEnv会管理底层的redis连接，因此不同CamelliaRedisTemplate可以共用同一个CamelliaRedisEnv实例，此时相同的redis后端会共用同一组连接（即使是不同的CamelliaRedisTemplate实例）      
* ResourceTable  
表示了路由表，表示CamelliaRedisTemplate的请求指向哪个redis地址(支持的后端redis类型，参见：[resource-samples](redis-resources.md))，支持单点redis、redis-sentinel、redis-cluster，此外也支持配置分片、读写分离、双写等      
上面的示例中表示了使用ResourceTableUtil去生成了指向单个地址的ResourceTable
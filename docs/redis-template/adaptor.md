##使用适配器快速从Jedis切换到CamelliaRedisTemplate

使用jedis时的代码：
```java
public class TestAdaptor1 {

    public static void main(String[] args) {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6379);
        Jedis jedis = null;
        try {
            //直接请求
            jedis = jedisPool.getResource();
            String setex = jedis.setex("k1", 100, "v1");
            System.out.println(setex);
            String k1 = jedis.get("k1");
            System.out.println(k1);
            //使用pipeline请求
            Pipeline pipelined = jedis.pipelined();
            Response<Long> response1 = pipelined.sadd("sk1", "sv1");
            Response<Long> response2 = pipelined.zadd("zk1", 1.0, "zv1");
            pipelined.sync();
            System.out.println(response1.get());
            System.out.println(response2.get());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
```

切换到CamelliaRedisTemplate时只需修改一行代码，如下：
```java
public class TestAdaptor1 {

    public static void main(String[] args) {
        //首先你要先初始化一个CamelliaRedisTemplate
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://@127.0.0.1:6379");
        //随后初始化JedisPoolCamelliaAdaptor
        JedisPoolCamelliaAdaptor jedisPool = new JedisPoolCamelliaAdaptor(template);
        //然后你就可以像使用普通JedisPool一样使用了（注意：Jedis和Pipeline的部分方法是不支持的，会直接返回异常，如：brpop、pipeline里的mget等）
        Jedis jedis = null;
        try {
            //直接请求
            jedis = jedisPool.getResource();
            String setex = jedis.setex("k1", 100, "v1");
            System.out.println(setex);
            String k1 = jedis.get("k1");
            System.out.println(k1);
            //使用pipeline请求
            Pipeline pipelined = jedis.pipelined();
            Response<Long> response1 = pipelined.sadd("sk1", "sv1");
            Response<Long> response2 = pipelined.zadd("zk1", 1.0, "zv1");
            pipelined.sync();
            System.out.println(response1.get());
            System.out.println(response2.get());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
```
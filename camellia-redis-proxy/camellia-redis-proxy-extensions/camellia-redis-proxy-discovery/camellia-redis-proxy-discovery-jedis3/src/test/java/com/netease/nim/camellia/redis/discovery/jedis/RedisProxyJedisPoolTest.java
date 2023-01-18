package com.netease.nim.camellia.redis.discovery.jedis;

import com.netease.nim.camellia.redis.proxy.discovery.common.AffinityProxySelector;
import com.netease.nim.camellia.redis.proxy.discovery.common.LocalConfProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.discovery.common.RandomProxySelector;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class RedisProxyJedisPoolTest {

    private static final RedisProxyJedisPool.JedisPoolInitializer mockInitializer = (context) -> new MockJedisPool();

    private static class MockJedisPool extends JedisPool {

        private final MockJedis jedis = new MockJedis();

        @Override
        public Jedis getResource() {
            return jedis;
        }

        @Override
        public void returnResource(Jedis resource) {

        }
    }

    private static class MockJedis extends Jedis {
        @Override
        public void close() {
        }
    }

    AffinityProxySelector selector = new AffinityProxySelector();
    RedisProxyJedisPool jedisPool = new RedisProxyJedisPool.Builder()
            .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
            .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
            .password("pass123")
            .jedisPoolInitializer(mockInitializer)
            .proxySelector(selector).timeout(2000).build();
    @Before
    public void setUp() throws Exception {
        
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * 冒烟测试
     */
    @Test
    public void getResource() {
        Jedis client = jedisPool.getResource();
        client.set("1","3");
        String testVal = client.get("1");
        Assert.assertEquals(testVal,"3");
    }
    private boolean getResourceMultiTiems(JedisPool pool){

        Jedis client = pool.getResource();

        String clientID = "";

        clientID = client.toString();
        pool.returnResource(client);
        boolean isEqual = true;
        for(int i=0 ; i < 1000; i++){
            Jedis affinityClient = pool.getResource();
            //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
            //如果toString一样，说明选择了同一个client
            String selectClient = affinityClient.toString();
            pool.returnResource(affinityClient);
            if(!clientID.equals(selectClient)){
                isEqual=false;
                break;
            }

        }
        return isEqual;
    }
    /**
     * 顺序多次调用测试
     */
    @Test
    public void getResourceMultiTiems(){
        boolean isEqual = getResourceMultiTiems(jedisPool);
        Assert.assertTrue(isEqual);
    }


    private boolean getResourceMultiThread(JedisPool pool,boolean isAffinity) throws InterruptedException {
        Jedis client = pool.getResource();

        String clientID = "";

        clientID = client.toString();
        pool.returnResource(client);
        List<Runnable> future = new ArrayList<>();
        AtomicInteger c = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(1000);
        for(int i=0 ; i < 1000; i++){
            String finalClientID = clientID;
            future.add(() -> {
                Jedis affinityClient = pool.getResource();
                //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
                //如果toString一样，说明选择了同一个client
                String selectClient = affinityClient.toString();
                affinityClient.close();
                boolean isEqual = finalClientID.equals(selectClient);
                if (isEqual) {
                    c.incrementAndGet();
                }
                countDownLatch.countDown();
            });
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        future.forEach(executorService::submit);
        countDownLatch.await();
        boolean isAllSuccess = true;
        if (isAffinity) {
            isAllSuccess = c.get() == 1000;
        }
        return isAllSuccess;
    }
    /**
     * 并发测试，并进行亲和性校验 
     */
    @Test
    public void getResourceMultiThread() throws InterruptedException {
        long start = System.currentTimeMillis();
        boolean isAllSuccess =  getResourceMultiThread(jedisPool,true);
        System.out.println("affinity selector spend " + (System.currentTimeMillis()-start) + "ms");
        Assert.assertTrue(isAllSuccess);
    }
    /**
     * 对比多线程性能测试
     * @throws InterruptedException
     */
    @Test
    public void compareEfficiencyMultiThreadRandomSelector() throws InterruptedException {
        RandomProxySelector randomSelector = new RandomProxySelector();
        RedisProxyJedisPool radomRedisPool = new RedisProxyJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
                .password("pass123")
                .jedisPoolInitializer(mockInitializer)
                .proxySelector(randomSelector)
                .timeout(2000)
                .build();
        long start = System.currentTimeMillis();
        getResourceMultiThread(radomRedisPool,false);
        System.out.println("random selector multi thread spend " + (System.currentTimeMillis()-start) + "ms");

        start = System.currentTimeMillis();
        boolean isAllSuccess =  getResourceMultiThread(jedisPool,true);
        System.out.println("affinity selector multi thread spend " + (System.currentTimeMillis()-start) + "ms");
        Assert.assertTrue(isAllSuccess);
    }

    /**
     * 高可用故障注入测试
     */
    @Test
    public void getResourceMultiThreadFail(){
        AffinityProxySelector innerTestselector = Mockito.spy(new AffinityProxySelector());
        RedisProxyJedisPool innerJedisPoll = new RedisProxyJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
                .password("pass123")
                .proxySelector(innerTestselector)
                .jedisPoolInitializer(mockInitializer)
                .timeout(2000).build();


        Jedis client = innerJedisPoll.getResource();

        String clientID;

        clientID = client.toString();
        innerJedisPoll.returnResource(client);
        boolean isEqual = true;
        
        for(int i=0 ; i < 100; i++){
//            Mockito.when(selector.next(Mockito.eq(true))).thenThrow(new Exception());
            int span = ThreadLocalRandom.current().nextInt(5,10);
//            if(i %span == 0) {
//                Mockito.doThrow(new RedisProxyJedisPoolException())
//                        .when(innerTestselector).next(Mockito.eq(true));
//            }
            if(i >50){
                Mockito.doReturn(null).when(innerTestselector).next(Mockito.eq(true));
            }
            Jedis affinityClient = innerJedisPoll.getResource();

            //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
            //如果toString一样，说明选择了同一个client
            String selectClient = affinityClient.toString();
            innerJedisPoll.returnResource(affinityClient);
            if(!clientID.equals(selectClient)){
                isEqual=false;
                System.out.println("发生了re select");
            }
        }
        Assert.assertTrue(isEqual);
    }

    /**
     * 串行性能测试
     */
    @Test
    public void compareEfficiency(){
        //预热
        getResourceMultiTiems();

        RandomProxySelector randomSelector = new RandomProxySelector();
        RedisProxyJedisPool radomRedisPool = new RedisProxyJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
                .password("pass123")
                .proxySelector(randomSelector)
                .jedisPoolInitializer(mockInitializer)
                .timeout(2000)
                .build();
        long start = System.currentTimeMillis();
        getResourceMultiTiems(radomRedisPool);
        System.out.println("random selector spend " + (System.currentTimeMillis()-start) + "ms");


        start = System.currentTimeMillis();
        boolean isEqual = getResourceMultiTiems(jedisPool);
        System.out.println("affinity selector spend " + (System.currentTimeMillis()-start) + "ms");
        Assert.assertTrue(isEqual);
    }
    
}
package com.netease.nim.camellia.redis.discovery.jedis;

import com.netease.nim.camellia.redis.proxy.discovery.common.AffinityProxySelector;
import com.netease.nim.camellia.redis.proxy.discovery.common.LocalConfProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.discovery.common.Proxy;
import com.netease.nim.camellia.redis.proxy.discovery.common.RandomProxySelector;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class RedisProxyJedisPoolTest {
    private static final Logger logger = LoggerFactory.getLogger(RedisProxyJedisPoolTest.class);


    AffinityProxySelector affinityProxySelector = new AffinityProxySelector();
    RandomProxySelector randomProxySelector = new RandomProxySelector();
    RedisProxyJedisPool affinityJedisPool = new RedisProxyJedisPool.Builder()
            .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
            .proxyDiscovery(mockProxyDiscovery())
            .password("pass123")
            .jedisPoolInitializer(mockInitializer(null))
            .proxySelector(affinityProxySelector)
            .timeout(2000)
            .build();
    RedisProxyJedisPool randomJedisPool = new RedisProxyJedisPool.Builder()
            .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
            .proxyDiscovery(mockProxyDiscovery())
            .password("pass123")
            .jedisPoolInitializer(mockInitializer(null))
            .proxySelector(randomProxySelector)
            .timeout(2000)
            .build();

    private RedisProxyJedisPool.JedisPoolInitializer mockInitializer(List<JedisPool> poolList) {
        if (poolList == null) {
            poolList = new ArrayList<>();
        }
        int proxyCount = 100;
        for (int i = 0; i < proxyCount; i++) {
            JedisPool jedisPool = Mockito.mock(JedisPool.class);
            Jedis jedis = Mockito.mock(Jedis.class);
            Mockito.when(jedisPool.getResource()).thenReturn(jedis);
            poolList.add(jedisPool);
        }
        List<JedisPool> finalPoolList = poolList;
        RedisProxyJedisPool.JedisPoolInitializer mockInitializer = context -> finalPoolList.get(ThreadLocalRandom.current().nextInt(0, 100000) % proxyCount);
        return mockInitializer;
    }

    private LocalConfProxyDiscovery mockProxyDiscovery() {
        LocalConfProxyDiscovery localConfProxyDiscovery = Mockito.spy(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"));
        List<Proxy> proxies = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            proxies.add(new Proxy("127.0.0.1", 26379 + i));
        }

        Mockito.doReturn(proxies).when(localConfProxyDiscovery).findAll();
        return localConfProxyDiscovery;
    }

    private boolean getResourceMultiThread(JedisPool pool, boolean isAffinity) {
        Jedis client = pool.getResource();

        String clientID = "";

        clientID = client.toString();
        pool.returnResource(client);
        List<Runnable> future = new ArrayList<>();
        AtomicInteger c = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(1000);
        for (int i = 0; i < 1000; i++) {
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
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean isAllSuccess = true;
        if (isAffinity) {
            isAllSuccess = c.get() == 1000;
        }
        return isAllSuccess;
    }

    private boolean getResourceMultiTiems(JedisPool pool) {

        Jedis client = pool.getResource();

        String clientID = "";

        clientID = client.toString();
        pool.returnResource(client);
        boolean isEqual = true;
        for (int i = 0; i < 1000; i++) {
            Jedis affinityClient = pool.getResource();
            //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
            //如果toString一样，说明选择了同一个client
            String selectClient = affinityClient.toString();
            pool.returnResource(affinityClient);
            if (!clientID.equals(selectClient)) {
                isEqual = false;
                break;
            }

        }
        return isEqual;
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {
    }

    /**
     * 冒烟测试
     */
    @Test
    public void getResource() {
        Jedis client = affinityJedisPool.getResource();
        Assert.assertNotNull(client);
    }

    /**
     * 顺序多次调用测试
     */
    @Test
    public void getResourceMultiTiems() {
        boolean isEqual = getResourceMultiTiems(affinityJedisPool);
        Assert.assertTrue(isEqual);
    }


    /**
     * 并发测试，并进行亲和性校验
     */
    @Test
    public void getResourceMultiThread() {
        long start = System.currentTimeMillis();
        boolean isAllSuccess = getResourceMultiThread(affinityJedisPool, true);
        System.out.println("affinity selector spend " + (System.currentTimeMillis() - start) + "ms");
        Assert.assertTrue(isAllSuccess);
    }

    /**
     * 对比多线程性能测试
     */
    @Test
    public void compareEfficiencyMultiThreadRandomSelector() {
        long start = System.currentTimeMillis();
        getResourceMultiThread(randomJedisPool, false);
        System.out.println("random selector multi thread spend " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        boolean isAllSuccess = getResourceMultiThread(affinityJedisPool, true);
        System.out.println("affinity selector multi thread spend " + (System.currentTimeMillis() - start) + "ms");
        Assert.assertTrue(isAllSuccess);
    }


    /**
     * 高可用故障注入测试
     */
    @Test
    public void getResourceMultiThreadFail() {

        List<JedisPool> poolList = new ArrayList<>();
        AffinityProxySelector innerTestselector = Mockito.spy(new AffinityProxySelector());
        RedisProxyJedisPool innerJedisPoll = new RedisProxyJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(mockProxyDiscovery())
                .proxySelector(innerTestselector)
                .jedisPoolInitializer(mockInitializer(poolList))
                .timeout(2000).build();


        Jedis client = innerJedisPoll.getResource();

        String clientID;
        innerJedisPoll.returnResource(client);
        AtomicBoolean isEqual = new AtomicBoolean(true);

        clientID = client.toString();

        List<Runnable> future = new ArrayList<>();
        AtomicInteger c = new AtomicInteger();
        int times = 100;
        CountDownLatch countDownLatch = new CountDownLatch(times);
        for (int i = 0; i < times; i++) {
            String finalClientID = clientID;


            int finalI = i;

            future.add(() -> {
                try {
                    if (finalI == 50) {
                        Mockito.doReturn(null).when(innerTestselector).next(Mockito.eq(true));
                    }
                    if (finalI == 56) {
                        Mockito.doCallRealMethod().when(innerTestselector).next(Mockito.eq(true));
                    }
                    

                    Jedis affinityClient = innerJedisPoll.getResource();
                    //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
                    //如果toString一样，说明选择了同一个client
                    String selectClient = affinityClient.toString();
                    affinityClient.close();
                    boolean isEqualEach = finalClientID.equals(selectClient);
                    if (!isEqualEach && isEqual.get()) {
                        isEqual.set(false);
                        logger.debug("出现切换是正常情况，一次都没出现是不正常的,old client是{}; 新的client是{}", finalClientID, selectClient);
                        System.out.println("出现切换是正常情况，一次都没出现是不正常的");
                    }
                } catch (Exception ex) {
//                    logger.error("{}", ex);
                } finally {
                    countDownLatch.countDown();
                }

            });
        }
        ExecutorService executorService = Executors.newCachedThreadPool();
        future.forEach(executorService::submit);
        try {
            countDownLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("count down:{}", countDownLatch.getCount());

//        
//        for(int i=0 ; i < 100; i++){
//            if(i == 50){
//                Mockito.doReturn(null).when(innerTestselector).next(Mockito.eq(true));
//            }
//            Jedis affinityClient = innerJedisPoll.getResource();
//
//            //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
//            //如果toString一样，说明选择了同一个client
//            String selectClient = affinityClient.toString();
//            innerJedisPoll.returnResource(affinityClient);
//            if(!clientID.equals(selectClient) && isEqual.get()){
//                //发生切换才是正常的情况
//                isEqual.set(false);
//                System.out.println("发生了re select");
//            }
//        }


        Assert.assertFalse(isEqual.get());
    }

    /**
     * 串行性能测试
     */
    @Test
    public void compareEfficiency() {
        //预热
        long start = 0;
        getResourceMultiTiems();

        start = System.currentTimeMillis();
        boolean isEqual = getResourceMultiTiems(affinityJedisPool);
        System.out.println("affinity selector spend " + (System.currentTimeMillis() - start) + "ms");
        Assert.assertTrue(isEqual);
        
        
        start = System.currentTimeMillis();
        getResourceMultiTiems(randomJedisPool);
        System.out.println("random selector spend " + (System.currentTimeMillis() - start) + "ms");


        
    }

}
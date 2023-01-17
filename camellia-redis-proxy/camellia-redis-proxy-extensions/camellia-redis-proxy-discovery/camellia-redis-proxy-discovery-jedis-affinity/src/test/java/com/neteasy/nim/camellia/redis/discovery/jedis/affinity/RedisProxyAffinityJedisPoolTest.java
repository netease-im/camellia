package com.neteasy.nim.camellia.redis.discovery.jedis.affinity;

import com.netease.nim.camellia.redis.proxy.discovery.common.LocalConfProxyDiscovery;
import com.netease.nim.camellia.redis.proxy.discovery.common.RandomProxySelector;
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

public class RedisProxyAffinityJedisPoolTest {
    private static final Logger logger = LoggerFactory.getLogger(RedisProxyAffinityJedisPoolTest.class);
    AffinityProxySelector selector = new AffinityProxySelector();
    RedisProxyAffinityJedisPool jedisPool = new RedisProxyAffinityJedisPool.Builder()
            .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
            .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
            .password("pass123")
            .proxySelector(selector).testOnBorrow(true).timeout(2000).build();
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
        Assert.assertEquals(isEqual,true);
    }


    private boolean getResourceMultiThread(JedisPool pool,boolean isAffinity) throws InterruptedException {
        Jedis client = pool.getResource();

        String clientID = "";

        clientID = client.toString();
        pool.returnResource(client);
        List<Callable<Boolean>> future = new ArrayList<>();
        List<Future<Boolean>> result = new ArrayList<>();
        List<Boolean> booleanRet = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(1000);
        for(int i=0 ; i < 1000; i++){
            String finalClientID = clientID;
            future.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Jedis affinityClient = pool.getResource();
                    //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
                    //如果toString一样，说明选择了同一个client
                    String selectClient = affinityClient.toString();
                    pool.returnResource(affinityClient);
                    boolean isEqual = finalClientID.equals(selectClient);
                    booleanRet.add(isEqual);
                    countDownLatch.countDown();
                    return isEqual;
                }
            });
        }
        future.forEach(f->{
            result.add(Executors.newCachedThreadPool().submit(f));
        });
        countDownLatch.await(10,TimeUnit.SECONDS);
        boolean isAllSuccess = true;
        if(isAffinity){
            isAllSuccess = booleanRet.stream().allMatch(item->item.equals(true));
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
        logger.info("affinity selector spend {} ms",(System.currentTimeMillis()-start));
        Assert.assertEquals(isAllSuccess,true);
    }
    /**
     * 对比多线程性能测试
     * @throws InterruptedException
     */
    @Test
    public void compareEfficiencyMultiThreadRandomSelector() throws InterruptedException {
        RandomProxySelector randomSelector = new RandomProxySelector();
        RedisProxyAffinityJedisPool radomRedisPool = new RedisProxyAffinityJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
                .password("pass123")
                .proxySelector(randomSelector).testOnBorrow(true).timeout(2000).build();
        long start = System.currentTimeMillis();
        getResourceMultiThread(radomRedisPool,false);
        logger.info("random selector multi thread spend {} ms",(System.currentTimeMillis()-start));



        start = System.currentTimeMillis();
        boolean isAllSuccess =  getResourceMultiThread(jedisPool,false);
        logger.info("affinity selector multi thread spend {} ms",(System.currentTimeMillis()-start));
        Assert.assertEquals(isAllSuccess,true);
    }

    /**
     * 高可用故障注入测试
     */
    @Test
    public void getResourceMultiThreadFail(){

        
        AffinityProxySelector innerTestselector = Mockito.spy(new AffinityProxySelector());
        RedisProxyAffinityJedisPool innerJedisPoll = new RedisProxyAffinityJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
                .password("pass123")
                .proxySelector(innerTestselector).testOnBorrow(true).timeout(2000).build();
        
        
        long poolID = innerJedisPoll.getId();
        Jedis client = innerJedisPoll.getResource();

        String clientID = "";

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
//                logger.info("发生了re select");
            }else{
//                System.out.println("又回到了正常");
            }

        }
        Assert.assertEquals(isEqual,false);
    }

    /**
     * 串行性能测试
     */
    @Test
    public void compareEfficiency(){
        //预热
        getResourceMultiTiems();
        
        

        long start = System.currentTimeMillis();
        
        RandomProxySelector randomSelector = new RandomProxySelector();
        RedisProxyAffinityJedisPool radomRedisPool = new RedisProxyAffinityJedisPool.Builder()
                .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
                .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
                .password("pass123")
                .proxySelector(randomSelector).testOnBorrow(true).timeout(2000).build();
        getResourceMultiTiems(radomRedisPool);
        logger.info("random selector spend {} ms",(System.currentTimeMillis()-start));


        start = System.currentTimeMillis();
        boolean isEqual = getResourceMultiTiems(jedisPool);
        logger.info("affinity selector spend {} ms",(System.currentTimeMillis()-start));
        Assert.assertEquals(isEqual,true);
    }
    
}
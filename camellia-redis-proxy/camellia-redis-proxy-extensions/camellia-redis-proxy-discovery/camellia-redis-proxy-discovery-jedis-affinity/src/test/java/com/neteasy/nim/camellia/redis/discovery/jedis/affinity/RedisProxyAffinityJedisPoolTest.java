package com.neteasy.nim.camellia.redis.discovery.jedis.affinity;

import com.netease.nim.camellia.redis.proxy.discovery.common.LocalConfProxyDiscovery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class RedisProxyAffinityJedisPoolTest {
    private static final Logger logger = LoggerFactory.getLogger(RedisProxyAffinityJedisPoolTest.class);
    RedisProxyAffinityJedisPool jedisPool = new RedisProxyAffinityJedisPool.Builder()
            .jedisPoolInitialSize(3).jedisPoolLazyInit(true).poolConfig(new JedisPoolConfig())
            .proxyDiscovery(new LocalConfProxyDiscovery("127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381"))
            .password("pass123")
            .proxySelector(new AffinityProxySelector()).testOnBorrow(true).timeout(2000).build();
    @Before
    public void setUp() throws Exception {
        
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getResource() {
        Jedis client = jedisPool.getResource();
        client.set("1","3");
        String testVal = client.get("1");
        Assert.assertEquals(testVal,"3");
    }
    @Test
    public void getResourceMultiTiems(){
        long poolID = jedisPool.getId();
        Jedis client = jedisPool.getResource();
        
        String clientID = "";
        
        clientID = client.toString();
        jedisPool.returnResource(client);
        boolean isEqual = true;
        for(int i=0 ; i < 100; i++){
            Jedis affinityClient = jedisPool.getResource();
            //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
            //如果toString一样，说明选择了同一个client
            String selectClient = affinityClient.toString();
            jedisPool.returnResource(affinityClient);
            if(!clientID.equals(selectClient)){
                isEqual=false;
                System.out.println("原始"+clientID);
                System.out.println("轮询"+selectClient);
                break;
            }
            
        }
        Assert.assertEquals(isEqual,true);
    }


    @Test
    public void getResourceMultiThread()  {
        long poolID = jedisPool.getId();
        Jedis client = jedisPool.getResource();

        String clientID = "";

        clientID = client.toString();
        jedisPool.returnResource(client);
        boolean isEqual = true;
        List<Callable<Boolean>> future = new ArrayList<>();
        List<Future<Boolean>> result = new ArrayList<>();
        for(int i=0 ; i < 100; i++){
            String finalClientID = clientID;
            future.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Jedis affinityClient = jedisPool.getResource();
                    //由于这个pool封装的比较死，无法获知选择的是哪个proxy，只能通过toString的方式获取。
                    //如果toString一样，说明选择了同一个client
                    String selectClient = affinityClient.toString();
                    jedisPool.returnResource(affinityClient);
                    return !finalClientID.equals(selectClient);
                }
            });
           

        }
        future.forEach(f->{
            result.add(Executors.newCachedThreadPool().submit(f));    
        });
        List<Boolean> booleanRet = new ArrayList<>();
        List<CompletableFuture<Boolean>> completableFutures = new ArrayList<>();
        for (Future<Boolean> booleanFuture : result) {
            CompletableFuture f = CompletableFuture.supplyAsync(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    try {
                        return booleanFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                    } finally {
                        return false;
                    }
                }
            });
            completableFutures.add(f);
        }
        
        CompletableFuture completableFuture = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[]{}));
        try {
            completableFuture.get();
            for (CompletableFuture tmpCF : completableFutures) {
                booleanRet.add((Boolean) tmpCF.get()); 
            }
        } catch (InterruptedException | ExecutionException e) {
            Assert.fail(e.getMessage());   
        }
        boolean isFail = booleanRet.stream().anyMatch(ret->false);
        if(isFail){
            Assert.fail();
        }
        logger.info("执行完毕");
        Assert.assertEquals(isEqual,true);
        
    }
}
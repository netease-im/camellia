package com.netease.nim.camellia.redis.pipeline;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public class ResponseQueable {

    private Map<Client, Queue<Item>> map = new HashMap<>();
    private final List<Item> fallbackList = new ArrayList<>();
    private CamelliaRedisEnv env;

    public ResponseQueable(CamelliaRedisEnv env) {
        this.env = env;
    }

    private static class Item {

        private Response<?> response;
        private Resource resource;
        private Object key;
        private Fallback fallback;
        private Exception failReason;

        Item(Response<?> response, Resource resource, Object key, Fallback fallback) {
            this.response = response;
            this.resource = resource;
            this.key = key;
            this.fallback = fallback;
        }
    }

    static abstract class Fallback {

        abstract void invoke(Client client);

        void invoke(Client client, boolean withAsking) {
            if (withAsking) {
                client.asking();
            }
            invoke(client);
        }
    }

    /**
     * 生成一个包装返回值的Response对象，并且将对象引用关系缓存在本地
     * @param client response所在的client
     * @param builder Response对象的内容
     * @param <T> Response对象的内容Type
     * @param resource 操作的资源地址
     * @param key 操作的key
     * @param fallback 失败时的fallback
     * @return Response对象
     */
    <T> Response<T> getResponse(Client client, Builder<T> builder, Resource resource, Object key, Fallback fallback) {
        Queue<Item> queue = map.get(client);
        if (queue == null) {
            queue = new LinkedList<>();
            map.put(client, queue);
        }
        Response<T> response = new Response<>(builder);
        queue.add(new Item(response, resource, key, fallback));
        return response;
    }

    /**
     * 同步获取结果，调用结果会注入到Response对象中
     * @param redisClientPool RedisClientPool对象
     */
    void sync(final RedisClientPool redisClientPool) {
        boolean concurrentEnable = map.size() > 1 && env.isPipelineConcurrentEnable();

        int attempts = env.getPipelineMaxAttempts();

        final int initAttempts = attempts;
        List<Future> futureList = new ArrayList<>();
        for (final Map.Entry<Client, Queue<Item>> entry : map.entrySet()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Client client = entry.getKey();
                    Queue<Item> queue = entry.getValue();
                    handlerClient(redisClientPool, client, queue, initAttempts > 0);
                }
            };
            if (concurrentEnable) {
                Future<?> future = env.getConcurrentExec().submit(runnable);
                futureList.add(future);
            } else {
                runnable.run();
            }
        }
        try {
            for (Future future : futureList) {
                future.get();
            }
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }

        if (fallbackList.isEmpty()) return;

        while (attempts > 0) {
            attempts --;
            boolean retry = true;
            if (attempts == 0) {
                retry = false;
            }
            if (fallbackList.isEmpty()) break;
            //RedisClientPool clear
            redisClientPool.clear();

            //处理失败列表
            Map<Client, Queue<Item>> map = new HashMap<>();
            for (Item item : fallbackList) {
                byte[] key;
                if (item.key instanceof String) {
                    key = SafeEncoder.encode((String) item.key);
                } else if (item.key instanceof byte[]) {
                    key = (byte[]) item.key;
                } else {
                    throw new CamelliaRedisException("only support string/byte[] key");
                }
                Exception failReason = item.failReason;
                boolean withAsking = false;
                Client client;
                if (failReason instanceof JedisMovedDataException || failReason instanceof JedisClusterException
                        || failReason instanceof JedisConnectionException) {
                    client = redisClientPool.getClient(item.resource, key);
                } else if (failReason instanceof JedisAskDataException) {
                    HostAndPort targetNode = ((JedisAskDataException) failReason).getTargetNode();
                    client = redisClientPool.getClient(item.resource, targetNode.getHost(), targetNode.getPort());
                    withAsking = true;
                } else {
                    throw new CamelliaRedisException("only support JedisMovedDataException/JedisAskDataException/JedisConnectionException/JedisClusterException");
                }
                item.fallback.invoke(client, withAsking);

                Queue<Item> queue = map.get(client);
                if (queue == null) {
                    queue = new LinkedList<>();
                    map.put(client, queue);
                }
                if (withAsking) {
                    //asking response
                    queue.add(new Item(new Response<>(BuilderFactory.STRING), null, null, null));
                }
                queue.add(item);
            }

            //clear ResponseQueable
            clear();

            //invoke
            concurrentEnable = map.size() > 1 && env.isPipelineConcurrentEnable();
            final boolean finalRetry = retry;
            List<Future> retryFutureList = new ArrayList<>();
            for (final Map.Entry<Client, Queue<Item>> entry : map.entrySet()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Client client = entry.getKey();
                        Queue<Item> queue = entry.getValue();
                        handlerClient(redisClientPool, client, queue, finalRetry);
                    }
                };
                if (concurrentEnable) {
                    Future<?> future = env.getConcurrentExec().submit(runnable);
                    retryFutureList.add(future);
                } else {
                    runnable.run();
                }
            }
            try {
                for (Future future : retryFutureList) {
                    future.get();
                }
            } catch (Exception e) {
                throw new CamelliaRedisException(e);
            }
        }
    }

    private void handlerClient(RedisClientPool redisClientPool, Client client, Queue<Item> queue, boolean retry) {
        List<Object> list = client.getAll();
        for (Object o : list) {
            Item item = queue.poll();
            if (item != null) {
                //只有redis cluster需要处理MOVED和ASK
                if (item.resource != null && item.resource instanceof RedisClusterResource) {
                    if (retry && o instanceof Exception) {
                        redisClientPool.handlerException(item.resource, (Exception) o);
                        if (o instanceof JedisMovedDataException || o instanceof JedisClusterException || o instanceof JedisConnectionException) {
                            synchronized (fallbackList) {
                                item.failReason = (Exception) o;
                                fallbackList.add(item);
                                continue;
                            }
                        } else if (o instanceof JedisAskDataException) {
                            synchronized (fallbackList) {
                                item.failReason = (Exception) o;
                                fallbackList.add(item);
                                continue;
                            }
                        }
                    }
                }
                item.response.set(o);
            }
        }
    }

    /**
     * 清空缓存，调用之后可以复用本对象
     */
    void clear() {
        map.clear();
        fallbackList.clear();
    }
}

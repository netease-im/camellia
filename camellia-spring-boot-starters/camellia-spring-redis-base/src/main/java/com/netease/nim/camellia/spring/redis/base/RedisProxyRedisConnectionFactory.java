package com.netease.nim.camellia.spring.redis.base;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPoolException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.ExceptionTranslationStrategy;
import org.springframework.data.redis.PassThroughExceptionTranslationStrategy;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.jedis.JedisConverters;
import redis.clients.jedis.Jedis;

/**
 *
 * Created by caojiajun on 2020/12/2
 */
public class RedisProxyRedisConnectionFactory implements RedisConnectionFactory {

    private static final ExceptionTranslationStrategy EXCEPTION_TRANSLATION
            = new PassThroughExceptionTranslationStrategy(JedisConverters.exceptionConverter());

    private final String clientName;
    private final RedisProxyJedisPool redisProxyJedisPool;
    private boolean convertPipelineAndTxResults = true;

    public RedisProxyRedisConnectionFactory(RedisProxyJedisPool redisProxyJedisPool) {
        this.redisProxyJedisPool = redisProxyJedisPool;
        this.clientName = redisProxyJedisPool.getClientName();
    }

    public void setConvertPipelineAndTxResults(boolean convertPipelineAndTxResults) {
        this.convertPipelineAndTxResults = convertPipelineAndTxResults;
    }

    @Override
    public RedisConnection getConnection() {
        Jedis jedis = redisProxyJedisPool.getResource();
        return new RedisProxyJedisConnection(jedis, redisProxyJedisPool, clientName);
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return convertPipelineAndTxResults;
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException e) {
        if (e instanceof CamelliaRedisException || e instanceof RedisProxyJedisPoolException) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                e = (RuntimeException) cause;
            }
        }
        return EXCEPTION_TRANSLATION.translate(e);
    }
}

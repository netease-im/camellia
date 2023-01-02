package com.netease.nim.camellia.spring.redis.base;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.adaptor.JedisPoolCamelliaAdaptor;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
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
 * Created by caojiajun on 2021/7/30
 */
public class CamelliaRedisTemplateRedisConnectionFactory implements RedisConnectionFactory {
    private static final ExceptionTranslationStrategy EXCEPTION_TRANSLATION
            = new PassThroughExceptionTranslationStrategy(JedisConverters.exceptionConverter());

    private final JedisPoolCamelliaAdaptor pool;
    private boolean convertPipelineAndTxResults = true;

    public CamelliaRedisTemplateRedisConnectionFactory(CamelliaRedisTemplate template) {
        this.pool = new JedisPoolCamelliaAdaptor(template);
    }

    public void setConvertPipelineAndTxResults(boolean convertPipelineAndTxResults) {
        this.convertPipelineAndTxResults = convertPipelineAndTxResults;
    }

    @Override
    public RedisConnection getConnection() {
        Jedis jedis = pool.getResource();
        return new CamelliaRedisTemplateJedisConnection(jedis, pool);
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
        if (e instanceof CamelliaRedisException) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                e = (RuntimeException) cause;
            }
        }
        return EXCEPTION_TRANSLATION.translate(e);
    }
}

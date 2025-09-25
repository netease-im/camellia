package com.netease.nim.camellia.redis.pipeline;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShardingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.utils.LogUtil;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.redis.intercept.InterceptContext;
import com.netease.nim.camellia.redis.intercept.RedisInterceptor;
import com.netease.nim.camellia.redis.resource.PipelineResource;
import com.netease.nim.camellia.redis.resource.RedisClientResourceUtil;
import com.netease.nim.camellia.redis.util.CamelliaBitPosParams;
import com.netease.nim.camellia.redis.util.SetParamsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 封装了pipeline的接口
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaRedisPipelineImpl implements ICamelliaRedisPipeline0 {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisPipelineImpl.class);

    private Resource resource;
    private RedisClientPool clientPool;
    private ResponseQueable queable;
    private List<RedisInterceptor> interceptorList;
    public CamelliaRedisPipelineImpl(Resource resource) {
        if (resource == null) return;
        this.resource = RedisClientResourceUtil.parseResourceByUrl(resource);
        if (resource instanceof PipelineResource) {
            this.queable = ((PipelineResource) resource).getQueable();
            this.clientPool = ((PipelineResource) resource).getClientPool();
            this.interceptorList = ((PipelineResource) resource).getRedisEnv().getInterceptorList();
        } else {
            throw new IllegalArgumentException("not PipelineResource");
        }
    }

    @Override
    public void sync() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    private boolean interceptEnable() {
        return interceptorList != null && !interceptorList.isEmpty();
    }

    private void before(String key, String command) {
        if (!interceptEnable()) {
            return;
        }
        if (key == null) return;
        InterceptContext context = new InterceptContext(resource, key.getBytes(StandardCharsets.UTF_8), command, true);
        for (RedisInterceptor interceptor : interceptorList) {
            try {
                interceptor.before(context);
            } catch (Exception e) {
                logger.error("interceptor before error, context = {}", JSONObject.toJSONString(context), e);
            }
        }
    }

    private void before(byte[] key, String command) {
        if (!interceptEnable()) {
            return;
        }
        if (key == null) return;
        InterceptContext context = new InterceptContext(resource, key, command, true);
        for (RedisInterceptor interceptor : interceptorList) {
            try {
                interceptor.before(context);
            } catch (Exception e) {
                logger.error("interceptor before error, context = {}", JSONObject.toJSONString(context), e);
            }
        }
    }

    private void after(String key, String command) {
        if (!interceptEnable()) {
            return;
        }
        if (key == null) return;
        InterceptContext context = new InterceptContext(resource, key.getBytes(StandardCharsets.UTF_8), command, true);
        for (RedisInterceptor interceptor : interceptorList) {
            try {
                interceptor.after(context);
            } catch (Exception e) {
                logger.error("interceptor after error, context = {}", JSONObject.toJSONString(context), e);
            }
        }
    }

    private void after(byte[] key, String command) {
        if (!interceptEnable()) {
            return;
        }
        if (key == null) return;
        InterceptContext context = new InterceptContext(resource, key, command, true);
        for (RedisInterceptor interceptor : interceptorList) {
            try {
                interceptor.after(context);
            } catch (Exception e) {
                logger.error("interceptor after error, context = {}", JSONObject.toJSONString(context), e);
            }
        }
    }

    @WriteOp
    @Override
    public Response<Long> append(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "append(byte[] key, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.append(key, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.append(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> decr(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "decr(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.decr(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.decr(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.set(key, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.set(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> get(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "get(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.get(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.get(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> decrBy(@ShardingParam final byte[] key, final long integer) {
        LogUtil.debugLog(resource, key);
        String command = "decrBy(byte[] key, long integer)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.decrBy(key, integer);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.decrBy(key, integer);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> del(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "del(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.del(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.del(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> echo(@ShardingParam final byte[] string) {
        LogUtil.debugLog(resource, string);
        String command = "echo(byte[] string)";
        before(string, command);
        try {
            Client client = clientPool.getClient(resource, string);
            client.echo(string);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, string, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.echo(string);
                }
            });
        } finally {
            after(string, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> exists(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "exists(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.exists(key);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.exists(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> expire(@ShardingParam final byte[] key, final int seconds) {
        LogUtil.debugLog(resource, key);
        String command = "expire(byte[] key, int seconds)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.expire(key, seconds);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.expire(key, seconds);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> pexpire(@ShardingParam final byte[] key, final long milliseconds) {
        LogUtil.debugLog(resource, key);
        String command = "pexpire(byte[] key, long milliseconds)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.pexpire(key, milliseconds);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pexpire(key, milliseconds);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> expireAt(@ShardingParam final byte[] key, final long unixTime) {
        LogUtil.debugLog(resource, key);
        String command = "expireAt(byte[] key, long unixTime)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.expireAt(key, unixTime);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.expireAt(key, unixTime);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> pexpireAt(@ShardingParam final byte[] key, final long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        String command = "pexpireAt(byte[] key, long millisecondsTimestamp)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.pexpireAt(key, millisecondsTimestamp);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pexpireAt(key, millisecondsTimestamp);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> get(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "get(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.get(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.get(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> getbit(@ShardingParam final byte[] key, final long offset) {
        LogUtil.debugLog(resource, key);
        String command = "getbit(byte[] key, long offset)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.getbit(key, offset);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.getbit(key, offset);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<byte[]> getSet(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "getSet(byte[] key, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.getSet(key, value);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.getSet(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> getrange(@ShardingParam final byte[] key, final long startOffset, final long endOffset) {
        LogUtil.debugLog(resource, key);
        String command = "getrange(byte[] key, long startOffset, long endOffset)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.getrange(key, startOffset, endOffset);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.getrange(key, startOffset, endOffset);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hdel(@ShardingParam final byte[] key, final byte[]... field) {
        LogUtil.debugLog(resource, key);
        String command = "hdel(byte[] key, byte[]... field)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hdel(key, field);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hdel(key, field);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> hexists(@ShardingParam final byte[] key, final byte[] field) {
        LogUtil.debugLog(resource, key);
        String command = "hexists(byte[] key, byte[] field)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hexists(key, field);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hexists(key, field);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> hget(@ShardingParam final byte[] key, final byte[] field) {
        LogUtil.debugLog(resource, key);
        String command = "hget(byte[] key, byte[] field)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hget(key, field);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hget(key, field);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Map<byte[], byte[]>> hgetAll(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hgetAll(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hgetAll(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_MAP, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hgetAll(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hincrBy(@ShardingParam final byte[] key, final byte[] field, final long value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrBy(byte[] key, byte[] field, long value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hincrBy(key, field, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hincrBy(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> hincrByFloat(@ShardingParam final byte[] key, final byte[] field, final double value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrByFloat(byte[] key, byte[] field, double value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hincrByFloat(key, field, value);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hincrByFloat(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> hkeys(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hkeys(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hkeys(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hkeys(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> hlen(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hlen(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hlen(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hlen(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> hmget(@ShardingParam final byte[] key, final byte[]... fields) {
        LogUtil.debugLog(resource, key);
        String command = "hmget(byte[] key, byte[]... fields)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hmget(key, fields);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hmget(key, fields);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> hmset(@ShardingParam final byte[] key, final Map<byte[], byte[]> hash) {
        LogUtil.debugLog(resource, key);
        String command = "hmset(byte[] key, Map<byte[], byte[]> hash)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hmset(key, hash);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hmset(key, hash);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hset(@ShardingParam final byte[] key, final byte[] field, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "hset(byte[] key, byte[] field, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hset(key, field, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hset(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hsetnx(@ShardingParam final byte[] key, final byte[] field, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "hsetnx(byte[] key, byte[] field, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hsetnx(key, field, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hsetnx(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> hvals(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hvals(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.hvals(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hvals(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> incr(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "incr(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.incr(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.incr(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> incrBy(@ShardingParam final byte[] key, final long integer) {
        LogUtil.debugLog(resource, key);
        String command = "incrBy(byte[] key, long integer)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.incrBy(key, integer);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.incrBy(key, integer);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> incrByFloat(@ShardingParam final byte[] key, final double value) {
        LogUtil.debugLog(resource, key);
        String command = "incrByFloat(byte[] key, double value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.incrByFloat(key, value);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.incrByFloat(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> lindex(@ShardingParam final byte[] key, final long index) {
        LogUtil.debugLog(resource, key);
        String command = "lindex(byte[] key, long index)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lindex(key, index);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lindex(key, index);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> llen(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "llen(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.llen(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.llen(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<byte[]> lpop(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "lpop(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lpop(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lpop(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> linsert(@ShardingParam final String key, ListPosition where, String pivot, String value) {
        LogUtil.debugLog(resource, key);
        String command = "linsert(String key, ListPosition where, String pivot, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.linsert(key, where, pivot, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.linsert(key, where, pivot, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> linsert(@ShardingParam final byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.linsert(key, where, pivot, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.linsert(key, where, pivot, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> lpush(@ShardingParam final byte[] key, final byte[]... string) {
        LogUtil.debugLog(resource, key);
        String command = "lpush(byte[] key, byte[]... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lpush(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lpush(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> lpushx(@ShardingParam final byte[] key, final byte[]... bytes) {
        LogUtil.debugLog(resource, key);
        String command = "lpushx(byte[] key, byte[]... bytes)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lpushx(key, bytes);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lpushx(key, bytes);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> lrange(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "lrange(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lrange(key, start, end);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lrange(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> lrem(@ShardingParam final byte[] key, final long count, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "lrem(byte[] key, long count, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lrem(key, count, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lrem(key, count, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> lset(@ShardingParam final byte[] key, final long index, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "lset(byte[] key, long index, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.lset(key, index, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lset(key, index, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> ltrim(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "ltrim(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.ltrim(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.ltrim(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> persist(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "persist(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.persist(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.persist(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<byte[]> rpop(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "rpop(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.rpop(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.rpop(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> rpush(@ShardingParam final byte[] key, final byte[]... string) {
        LogUtil.debugLog(resource, key);
        String command = "rpush(byte[] key, byte[]... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.rpush(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.rpush(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> rpushx(@ShardingParam final byte[] key, final byte[]... string) {
        LogUtil.debugLog(resource, key);
        String command = "rpushx(byte[] key, byte[]... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.rpushx(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.rpushx(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> sadd(@ShardingParam final byte[] key, final byte[]... member) {
        LogUtil.debugLog(resource, key);
        String command = "sadd(byte[] key, byte[]... member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.sadd(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sadd(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> scard(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "scard(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.scard(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.scard(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "set(byte[] key, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.set(key, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.set(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx, final long time) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.set(key, value, SetParamsUtils.setParams(nxxx, expx, time));
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value, SetParamsUtils.setParams(nxxx, expx, time));
            }
        });
    }

    @WriteOp
    @Override
    public Response<Boolean> setbit(@ShardingParam final byte[] key, final long offset, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setbit(byte[] key, long offset, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.setbit(key, offset, value);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setbit(key, offset, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> setrange(@ShardingParam final byte[] key, final long offset, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setrange(byte[] key, long offset, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.setrange(key, offset, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setrange(key, offset, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> setex(@ShardingParam final byte[] key, final int seconds, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setex(byte[] key, int seconds, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.setex(key, seconds, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setex(key, seconds, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> psetex(@ShardingParam final byte[] key, final long milliseconds, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "psetex(byte[] key, long milliseconds, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.psetex(key, milliseconds, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.psetex(key, milliseconds, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> setnx(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setnx(byte[] key, byte[] value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.setnx(key, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setnx(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> smembers(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "smembers(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.smembers(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.smembers(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> sismember(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "sismember(byte[] key, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.sismember(key, member);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sismember(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> sort(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "sort(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.sort(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sort(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> sort(@ShardingParam final byte[] key, final SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        String command = "sort(byte[] key, SortingParams sortingParameters)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.sort(key, sortingParameters);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sort(key, sortingParameters);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<byte[]> spop(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "spop(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.spop(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.spop(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Set<byte[]>> spop(@ShardingParam final byte[] key, final long count) {
        LogUtil.debugLog(resource, key);
        String command = "spop(byte[] key, long count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.spop(key, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.spop(key, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> srandmember(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.srandmember(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.srandmember(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> srandmember(@ShardingParam final byte[] key, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(byte[] key, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.srandmember(key, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.srandmember(key, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> srem(@ShardingParam final byte[] key, final byte[]... member) {
        LogUtil.debugLog(resource, key);
        String command = "srem(byte[] key, byte[]... member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.srem(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.srem(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> strlen(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "strlen(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.strlen(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.strlen(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> substr(@ShardingParam final byte[] key, final int start, final int end) {
        LogUtil.debugLog(resource, key);
        String command = "substr(byte[] key, int start, int end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.substr(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.substr(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> ttl(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "ttl(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.ttl(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.ttl(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> pttl(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "pttl(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.pttl(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pttl(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> type(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "type(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.type(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.type(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final double score, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, double score, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zadd(key, score, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, score, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final double score, final byte[] member, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, double score, byte[] member, ZAddParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zadd(key, score, member, params);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, score, member, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zcard(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "zcard(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zcard(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zcard(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final byte[] key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(byte[] key, double min, double max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zcount(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zcount(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final byte[] key, final double score, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(byte[] key, double score, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zincrby(key, score, member);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zincrby(key, score, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final byte[] key, final double score, final byte[] member, final ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(byte[] key, double score, byte[] member, ZIncrByParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zincrby(key, score, member, params);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zincrby(key, score, member, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrange(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrange(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrange(key, start, end);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrange(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, double min, double max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScore(key, min, max);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScore(key, min, max);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScore(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScore(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, double min, double max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScoreWithScores(key, min, max);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScoreWithScores(key, min, max);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScoreWithScores(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByScoreWithScores(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, double max, double min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScore(key, max, min);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final byte[] max, final byte[] min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, byte[] max, byte[] min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScore(key, max, min);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScore(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScore(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, double max, double min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScoreWithScores(key, max, min);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] max, final byte[] min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScoreWithScores(key, max, min);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeWithScores(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeWithScores(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeWithScores(key, start, end);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeWithScores(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zrank(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zrank(byte[] key, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrank(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrank(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zrem(@ShardingParam final byte[] key, final byte[]... member) {
        LogUtil.debugLog(resource, key);
        String command = "zrem(byte[] key, byte[]... member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrem(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrem(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByRank(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByRank(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zremrangeByRank(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByRank(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final byte[] key, final double start, final double end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(byte[] key, double start, double end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zremrangeByScore(SafeEncoder.encode(key), start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByScore(SafeEncoder.encode(key), start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final byte[] key, final byte[] start, final byte[] end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(byte[] key, byte[] start, byte[] end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zremrangeByScore(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByScore(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrange(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrange(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrange(key, start, end);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrange(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeWithScores(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeWithScores(key, start, end);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeWithScores(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zrevrank(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrank(byte[] key, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrank(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrank(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Double> zscore(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zscore(byte[] key, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zscore(key, member);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zscore(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<Double>> zmscore(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        String command = "zmscore(String key, String[] members)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zmscore(key, members);
            return queable.getResponse(client, BuilderFactory.DOUBLE_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zmscore(key, members);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<Double>> zmscore(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "zmscore(byte[] key, byte[][] members)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zmscore(key, members);
            return queable.getResponse(client, BuilderFactory.DOUBLE_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zmscore(key, members);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zlexcount(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zlexcount(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zlexcount(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zlexcount(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByLex(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByLex(key, min, max);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByLex(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByLex(@ShardingParam final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrangeByLex(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByLex(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByLex(@ShardingParam final byte[] key, final byte[] max, final byte[] min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(byte[] key, byte[] max, byte[] min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByLex(key, max, min);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByLex(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByLex(@ShardingParam final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zrevrangeByLex(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByLex(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByLex(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByLex(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zremrangeByLex(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByLex(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.bitcount(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitcount(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(byte[] key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.bitcount(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitcount(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> pfadd(@ShardingParam final byte[] key, final byte[]... elements) {
        LogUtil.debugLog(resource, key);
        String command = "pfadd(byte[] key, byte[]... elements)";
        before(key, command);
        try {
            final Client client = clientPool.getClient(resource, key);
            client.pfadd(key, elements);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pfadd(key, elements);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> pfcount(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "pfcount(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.pfcount(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pfcount(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final byte[] key, final double longitude, final double latitude, final byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(byte[] key, double longitude, double latitude, byte[] member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.geoadd(key, longitude, latitude, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geoadd(key, longitude, latitude, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.geoadd(key, memberCoordinateMap);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geoadd(key, memberCoordinateMap);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final byte[] key, final byte[] member1, final byte[] member2) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(byte[] key, byte[] member1, byte[] member2)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.geodist(key, member1, member2);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geodist(key, member1, member2);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final byte[] key, final byte[] member1, final byte[] member2, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.geodist(key, member1, member2, unit);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geodist(key, member1, member2, unit);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> geohash(@ShardingParam final byte[] key, final byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "geohash(byte[] key, byte[]... members)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.geohash(key, members);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geohash(key, members);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoCoordinate>> geopos(@ShardingParam final byte[] key, final byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "geopos(byte[] key, byte[]... members)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.geopos(key, members);
            return queable.getResponse(client, BuilderFactory.GEO_COORDINATE_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geopos(key, members);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.georadius(key, longitude, latitude, radius, unit);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadius(key, longitude, latitude, radius, unit);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.georadius(key, longitude, latitude, radius, unit, param);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadius(key, longitude, latitude, radius, unit, param);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final byte[] key, final byte[] member, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.georadiusByMember(key, member, radius, unit);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadiusByMember(key, member, radius, unit);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final byte[] key, final byte[] member, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.georadiusByMember(key, member, radius, unit, param);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadiusByMember(key, member, radius, unit, param);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<Long>> bitfield(@ShardingParam final byte[] key, final byte[]... elements) {
        LogUtil.debugLog(resource, key);
        String command = "bitfield(byte[] key, byte[]... elements)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.bitfield(key, elements);
            return queable.getResponse(client, BuilderFactory.LONG_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitfield(key, elements);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final byte[] key, final boolean value, final BitPosParams params) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(byte[] key, boolean value, BitPosParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.bitpos(key, value, params);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitpos(key, value, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> append(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "append(String key, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.append(key, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.append(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> decr(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "decr(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.decr(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.decr(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> decrBy(@ShardingParam final String key, final long integer) {
        LogUtil.debugLog(resource, key);
        String command = "decrBy(String key, long integer)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.decrBy(key, integer);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.decrBy(key, integer);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> del(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "del(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.del(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.del(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> echo(@ShardingParam final String string) {
        LogUtil.debugLog(resource, string);
        String command = "echo(String string)";
        before(string, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(string));
            client.echo(string);
            return queable.getResponse(client, BuilderFactory.STRING, resource, string, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.echo(string);
                }
            });
        } finally {
            after(string, command);
        }
    }

    @WriteOp
    @Override
    public Response<Boolean> exists(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "exists(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.exists(key);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.exists(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> expire(@ShardingParam final String key, final int seconds) {
        LogUtil.debugLog(resource, key);
        String command = "expire(String key, int seconds)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.expire(key, seconds);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.expire(key, seconds);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> pexpire(@ShardingParam final String key, final long milliseconds) {
        LogUtil.debugLog(resource, key);
        String command = "pexpire(String key, long milliseconds)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.pexpire(key, milliseconds);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pexpire(key, milliseconds);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> expireAt(@ShardingParam final String key, final long unixTime) {
        LogUtil.debugLog(resource, key);
        String command = "expireAt(String key, long unixTime)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.expireAt(key, unixTime);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.expireAt(key, unixTime);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> pexpireAt(@ShardingParam final String key, final long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        String command = "pexpireAt(String key, long millisecondsTimestamp)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.pexpireAt(key, millisecondsTimestamp);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pexpireAt(key, millisecondsTimestamp);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> getbit(@ShardingParam final String key, final long offset) {
        LogUtil.debugLog(resource, key);
        String command = "getbit(String key, long offset)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.getbit(key, offset);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.getbit(key, offset);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> getrange(@ShardingParam final String key, final long startOffset, final long endOffset) {
        LogUtil.debugLog(resource, key);
        String command = "getrange(String key, long startOffset, long endOffset)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.getrange(key, startOffset, endOffset);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.getrange(key, startOffset, endOffset);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> getSet(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "getSet(String key, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.getSet(key, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.getSet(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hdel(@ShardingParam final String key, final String... field) {
        LogUtil.debugLog(resource, key);
        String command = "hdel(String key, String... field)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hdel(key, field);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hdel(key, field);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> hexists(@ShardingParam final String key, final String field) {
        LogUtil.debugLog(resource, key);
        String command = "hexists(String key, String field)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hexists(key, field);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hexists(key, field);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> hget(@ShardingParam final String key, final String field) {
        LogUtil.debugLog(resource, key);
        String command = "hget(String key, String field)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hget(key, field);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hget(key, field);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Map<String, String>> hgetAll(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "hgetAll(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hgetAll(key);
            return queable.getResponse(client, BuilderFactory.STRING_MAP, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hgetAll(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hincrBy(@ShardingParam final String key, final String field, final long value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrBy(String key, String field, long value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hincrBy(key, field, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hincrBy(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> hkeys(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "hkeys(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hkeys(key);
            return queable.getResponse(client, BuilderFactory.STRING_SET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hkeys(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> hlen(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "hlen(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hlen(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hlen(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> hmget(@ShardingParam final String key, final String... fields) {
        LogUtil.debugLog(resource, key);
        String command = "hmget(String key, String... fields)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hmget(key, fields);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hmget(key, fields);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> hmset(@ShardingParam final String key, final Map<String, String> hash) {
        LogUtil.debugLog(resource, key);
        String command = "hmset(String key, Map<String, String> hash)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hmset(key, hash);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hmset(key, hash);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hset(@ShardingParam final String key, final String field, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "hset(String key, String field, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hset(key, field, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hset(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> hsetnx(@ShardingParam final String key, final String field, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "hsetnx(String key, String field, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hsetnx(key, field, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hsetnx(key, field, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> hvals(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "hvals(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hvals(key);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hvals(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> incr(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "incr(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.incr(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.incr(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> incrBy(@ShardingParam final String key, final long integer) {
        LogUtil.debugLog(resource, key);
        String command = "incrBy(String key, long integer)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.incrBy(key, integer);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.incrBy(key, integer);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> lindex(@ShardingParam final String key, final long index) {
        LogUtil.debugLog(resource, key);
        String command = "lindex(String key, long index)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lindex(key, index);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lindex(key, index);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> llen(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "llen(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.llen(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.llen(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> lpop(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "lpop(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lpop(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lpop(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> lpush(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        String command = "lpush(String key, String... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lpush(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lpush(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> lpushx(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        String command = "lpushx(String key, String... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lpushx(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lpushx(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> lrange(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "lrange(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lrange(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lrange(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> lrem(@ShardingParam final String key, final long count, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "lrem(String key, long count, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lrem(key, count, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lrem(key, count, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> lset(@ShardingParam final String key, final long index, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "lset(String key, long index, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.lset(key, index, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.lset(key, index, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> ltrim(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "ltrim(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.ltrim(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.ltrim(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> persist(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "persist(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.persist(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.persist(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> rpop(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "rpop(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.rpop(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.rpop(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> rpush(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        String command = "rpush(String key, String... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.rpush(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.rpush(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> rpushx(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        String command = "rpushx(String key, String... string)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.rpushx(key, string);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.rpushx(key, string);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> sadd(@ShardingParam final String key, final String... member) {
        LogUtil.debugLog(resource, key);
        String command = "sadd(String key, String... member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.sadd(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sadd(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> scard(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "scard(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.scard(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.scard(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Boolean> sismember(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "sismember(String key, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.sismember(key, member);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sismember(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Boolean> setbit(@ShardingParam final String key, final long offset, final boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "setbit(String key, long offset, boolean value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.setbit(key, offset, value);
            return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setbit(key, offset, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> setex(@ShardingParam final String key, final int seconds, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "setex(String key, int seconds, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.setex(key, seconds, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setex(key, seconds, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> setnx(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "setnx(String key, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.setnx(key, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setnx(key, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> setrange(@ShardingParam final String key, final long offset, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "setrange(String key, long offset, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.setrange(key, offset, value);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.setrange(key, offset, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> smembers(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "smembers(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.smembers(key);
            return queable.getResponse(client, BuilderFactory.STRING_SET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.smembers(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> sort(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "sort(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.sort(key);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sort(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> sort(@ShardingParam final String key, final SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        String command = "sort(String key, SortingParams sortingParameters)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.sort(key, sortingParameters);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.sort(key, sortingParameters);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> spop(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "spop(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.spop(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.spop(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Set<String>> spop(@ShardingParam final String key, final long count) {
        LogUtil.debugLog(resource, key);
        String command = "spop(String key, long count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.spop(key, count);
            return queable.getResponse(client, BuilderFactory.STRING_SET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.spop(key, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> srandmember(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.srandmember(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.srandmember(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> srem(@ShardingParam final String key, final String... member) {
        LogUtil.debugLog(resource, key);
        String command = "srem(String key, String... member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.srem(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.srem(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> strlen(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "strlen(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.strlen(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.strlen(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> substr(@ShardingParam final String key, final int start, final int end) {
        LogUtil.debugLog(resource, key);
        String command = "substr(String key, int start, int end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.substr(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.substr(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> ttl(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "ttl(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.ttl(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.ttl(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<String> type(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "type(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.type(key);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.type(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final double score, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, double score, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zadd(key, score, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, score, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final double score, final String member, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, double score, String member, ZAddParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zadd(key, score, member, params);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, score, member, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final Map<String, Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, Map<String, Double> scoreMembers)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zadd(key, scoreMembers);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, scoreMembers);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final Map<String, Double> scoreMembers, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, Map<String, Double> scoreMembers, ZAddParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zadd(key, scoreMembers, params);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, scoreMembers, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zcard(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "zcard(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zcard(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zcard(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final String key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(String key, double min, double max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zcount(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zcount(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final String key, final double score, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(String key, double score, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zincrby(key, score, member);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zincrby(key, score, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final String key, final double score, final String member, final ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(String key, double score, String member, ZIncrByParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zincrby(key, score, member, params);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zincrby(key, score, member, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrange(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrange(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrange(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrange(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, double min, double max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScore(key, min, max);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, String min, String max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScore(key, min, max);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScore(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, double min, double max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScoreWithScores(key, min, max);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScoreWithScores(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, double max, double min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScore(key, max, min);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final String max, final String min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, String max, String min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScore(key, max, min);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScore(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, double max, double min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScoreWithScores(key, max, min);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeWithScores(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeWithScores(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeWithScores(key, start, end);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeWithScores(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zrank(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "zrank(String key, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrank(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrank(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zrem(@ShardingParam final String key, final String... member) {
        LogUtil.debugLog(resource, key);
        String command = "zrem(String key, String... member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrem(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrem(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByRank(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByRank(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zremrangeByRank(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByRank(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final String key, final double start, final double end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(String key, double start, double end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zremrangeByScore(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByScore(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrange(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrange(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrange(key, start, end);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrange(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeWithScores(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeWithScores(key, start, end);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeWithScores(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zrevrank(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrank(String key, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrank(key, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrank(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Double> zscore(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "zscore(String key, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zscore(key, member);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zscore(key, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zlexcount(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        String command = "zlexcount(String key, String min, String max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zlexcount(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zlexcount(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByLex(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(String key, String min, String max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByLex(key, min, max);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByLex(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByLex(@ShardingParam final String key, final String min, final String max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(String key, String min, String max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByLex(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByLex(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByLex(@ShardingParam final String key, final String max, final String min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(String key, String max, String min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByLex(key, max, min);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByLex(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByLex(@ShardingParam final String key, final String max, final String min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(String key, String max, String min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByLex(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByLex(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByLex(@ShardingParam final String key, final String start, final String end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByLex(String key, String start, String end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zremrangeByLex(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByLex(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.bitcount(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitcount(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(String key, long start, long end)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.bitcount(key, start, end);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitcount(key, start, end);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> pfadd(@ShardingParam final String key, final String... elements) {
        LogUtil.debugLog(resource, key);
        String command = "pfadd(String key, String... elements)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.pfadd(key, elements);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pfadd(key, elements);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> pfcount(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "pfcount(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.pfcount(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pfcount(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<Long>> bitfield(@ShardingParam final String key, final String... arguments) {
        LogUtil.debugLog(resource, key);
        String command = "bitfield(String key, String... arguments)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.bitfield(key, arguments);
            return queable.getResponse(client, BuilderFactory.LONG_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitfield(key, arguments);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final String key, final double longitude, final double latitude, final String member) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(String key, double longitude, double latitude, String member)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.geoadd(key, longitude, latitude, member);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geoadd(key, longitude, latitude, member);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.geoadd(key, memberCoordinateMap);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geoadd(key, memberCoordinateMap);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final String key, final String member1, final String member2) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(String key, String member1, String member2)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.geodist(key, member1, member2);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geodist(key, member1, member2);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final String key, final String member1, final String member2, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(String key, String member1, String member2, GeoUnit unit)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.geodist(key, member1, member2, unit);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geodist(key, member1, member2, unit);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> geohash(@ShardingParam final String key, final String... members) {
        LogUtil.debugLog(resource, key);
        String command = "geohash(String key, String... members)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.geohash(key, members);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geohash(key, members);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoCoordinate>> geopos(@ShardingParam final String key, final String... members) {
        LogUtil.debugLog(resource, key);
        String command = "geopos(String key, String... members)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.geopos(key, members);
            return queable.getResponse(client, BuilderFactory.GEO_COORDINATE_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.geopos(key, members);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final String key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(String key, double longitude, double latitude, double radius, GeoUnit unit)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.georadius(key, longitude, latitude, radius, unit);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadius(key, longitude, latitude, radius, unit);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final String key, final double longitude, final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.georadius(key, longitude, latitude, radius, unit, param);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadius(key, longitude, latitude, radius, unit, param);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final String key, final String member, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(String key, String member, double radius, GeoUnit unit)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.georadiusByMember(key, member, radius, unit);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadiusByMember(key, member, radius, unit);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final String key, final String member, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.georadiusByMember(key, member, radius, unit, param);
            return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.georadiusByMember(key, member, radius, unit, param);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final Map<byte[], Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, Map<byte[], Double> scoreMembers)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zadd(key, scoreMembers);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, scoreMembers);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zadd(key, scoreMembers, params);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zadd(key, scoreMembers, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> dump(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "dump(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.dump(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.dump(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<byte[]> dump(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "dump(byte[] key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.dump(key);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.dump(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> restore(@ShardingParam final byte[] key, final int ttl, final byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        String command = "restore(byte[] key, int ttl, byte[] serializedValue)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.restore(key, ttl, serializedValue);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.restore(key, ttl, serializedValue);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> restore(@ShardingParam final String key, final int ttl, final byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        String command = "restore(String key, int ttl, byte[] serializedValue)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.restore(key, ttl, serializedValue);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.restore(key, ttl, serializedValue);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> hincrByFloat(@ShardingParam final String key, final String field, final double increment) {
        LogUtil.debugLog(resource, key);
        String command = "hincrByFloat(String key, String field, double increment)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.hincrByFloat(key, field, increment);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.hincrByFloat(key, field, increment);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Double> incrByFloat(@ShardingParam final String key, final double increment) {
        LogUtil.debugLog(resource, key);
        String command = "incrByFloat(String key, double increment)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.incrByFloat(key, increment);
            return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.incrByFloat(key, increment);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final String key, final String value, final String nxxx) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value, String nxxx)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.set(key, value, SetParamsUtils.setParams(nxxx, null, 0));
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.set(key, value, SetParamsUtils.setParams(nxxx, null, 0));
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final byte[] key, final byte[] value, final byte[] nxxx) {
        LogUtil.debugLog(resource, key);
        String command = "set(byte[] key, byte[] value, byte[] nxxx)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.set(key, value, SetParamsUtils.setParams(nxxx, null, 0));
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.set(key, value, SetParamsUtils.setParams(nxxx, null, 0));
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final String key, final String value, final String nxxx, final String expx, final int time) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value, String nxxx, String expx, int time)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.set(key, value, SetParamsUtils.setParams(nxxx, expx, time));
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.set(key, value, SetParamsUtils.setParams(nxxx, expx, time));
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> psetex(@ShardingParam final String key, final long milliseconds, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "psetex(String key, long milliseconds, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.psetex(key, milliseconds, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.psetex(key, milliseconds, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<String> psetex(@ShardingParam final String key, final int milliseconds, final String value) {
        LogUtil.debugLog(resource, key);
        String command = "psetex(String key, int milliseconds, String value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.psetex(key, milliseconds, value);
            return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.psetex(key, milliseconds, value);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> srandmember(@ShardingParam final String key, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(String key, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.srandmember(key, count);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.srandmember(key, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> pttl(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        String command = "pttl(String key)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.pttl(key);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.pttl(key);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final String min, final String max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, String min, String max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScore(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScore(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final String min, final String max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, String min, String max, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScoreWithScores(key, min, max, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, String min, String max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrangeByScoreWithScores(key, min, max);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrangeByScoreWithScores(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final String max, final String min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, String max, String min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScore(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScore(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final String max, final String min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min, offset, count);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final String max, final String min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, String max, String min)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zrevrangeByScoreWithScores(key, max, min);
            return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zrevrangeByScoreWithScores(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(String key, String min, String max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zremrangeByScore(key, max, min);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zremrangeByScore(key, max, min);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final String key, final boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(String key, boolean value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.bitpos(key, value, new CamelliaBitPosParams());
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitpos(key, value, new CamelliaBitPosParams());
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final String key, final boolean value, final BitPosParams params) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(String key, boolean value, BitPosParams params)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.bitpos(key, value, params);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitpos(key, value, params);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final byte[] key, final boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(byte[] key, boolean value)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.bitpos(key, value, new CamelliaBitPosParams());
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.bitpos(key, value, new CamelliaBitPosParams());
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, key);
            client.zcount(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zcount(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(String key, String min, String max)";
        before(key, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
            client.zcount(key, min, max);
            return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.zcount(key, min, max);
                }
            });
        } finally {
            after(key, command);
        }
    }


    @Override
    public Response<List<byte[]>> mget(byte[]... keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response<List<String>> mget(String... keys) {
        throw new UnsupportedOperationException();
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> mget0(@ShardingParam byte[] shardingKey, byte[]... keys) {
        LogUtil.debugLog(resource, shardingKey);
        String command = "mget0(byte[] shardingKey, byte[]... keys)";
        before(shardingKey, command);
        try {
            Client client = clientPool.getClient(resource, shardingKey);
            client.mget(keys);
            return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, shardingKey, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.mget(keys);
                }
            });
        } finally {
            after(shardingKey, command);
        }
    }

    @ReadOp
    @Override
    public Response<List<String>> mget0(@ShardingParam String shardingKey, String... keys) {
        LogUtil.debugLog(resource, shardingKey);
        String command = "mget0(String shardingKey, String... keys)";
        before(shardingKey, command);
        try {
            Client client = clientPool.getClient(resource, SafeEncoder.encode(shardingKey));
            client.mget(keys);
            return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, shardingKey, new ResponseQueable.Fallback() {
                @Override
                public void invoke(Client client) {
                    client.mget(keys);
                }
            });
        } finally {
            after(shardingKey, command);
        }
    }

}
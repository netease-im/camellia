package com.netease.nim.camellia.redis;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShardingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.utils.LogUtil;
import com.netease.nim.camellia.redis.intercept.InterceptContext;
import com.netease.nim.camellia.redis.intercept.RedisInterceptor;
import com.netease.nim.camellia.redis.resource.*;
import com.netease.nim.camellia.redis.util.CamelliaRedisInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaRedisImpl implements ICamelliaRedis {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisImpl.class);

    private Resource resource;
    private ICamelliaRedis redis;
    private List<RedisInterceptor> interceptorList;
    public CamelliaRedisImpl(Resource resource) {
        if (resource == null) return;
        this.resource = resource;
        CamelliaRedisEnv env;
        if (resource instanceof ResourceWrapper) {
            env = ((ResourceWrapper) resource).getEnv();
            interceptorList = env.getInterceptorList();
        } else {
            throw new IllegalArgumentException("not ResourceWrapper");
        }
        redis = CamelliaRedisInitializer.init(resource, env);
    }

    @Override
    public List<Jedis> getJedisList() {
        return redis.getJedisList();
    }

    @Override
    public Jedis getJedis(byte[] key) {
        return redis.getJedis(key);
    }

    private boolean interceptEnable() {
        return interceptorList != null && !interceptorList.isEmpty();
    }

    private void before(String key, String command) {
        if (!interceptEnable()) {
            return;
        }
        if (key == null) return;
        InterceptContext context = new InterceptContext(resource, key.getBytes(StandardCharsets.UTF_8), command, false);
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
        InterceptContext context = new InterceptContext(resource, key, command, false);
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
        InterceptContext context = new InterceptContext(resource, key.getBytes(StandardCharsets.UTF_8), command, false);
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
        InterceptContext context = new InterceptContext(resource, key, command, false);
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
    public String set(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "set(byte[] key, byte[] value)";
        before(key, command);
        try {
            return redis.set(key, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] get(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "get(byte[] key)";
        before(key, command);
        try {
            return redis.get(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam byte[] key, byte[] value, SetParams setParams) {
        LogUtil.debugLog(resource, key);
        String command = "set(byte[] key, byte[] value, SetParams setParams)";
        before(key, command);
        try {
            return redis.set(key, value, setParams);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value)";
        before(key, command);
        try {
            return redis.set(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value, SetParams setParams) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value, SetParams setParams)";
        before(key, command);
        try {
            return redis.set(key, value, setParams);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String get(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "get(String key)";
        before(key, command);
        try {
            return redis.get(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean exists(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "exists(String key)";
        before(key, command);
        try {
            return redis.exists(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long persist(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "persist(String key)";
        before(key, command);
        try {
            return redis.persist(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String type(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "type(String key)";
        before(key, command);
        try {
            return redis.type(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long expire(@ShardingParam String key, int seconds) {
        LogUtil.debugLog(resource, key);
        String command = "expire(String key, int seconds)";
        before(key, command);
        try {
            return redis.expire(key, seconds);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long pexpire(@ShardingParam String key, long milliseconds) {
        LogUtil.debugLog(resource, key);
        String command = "pexpire(String key, long milliseconds)";
        before(key, command);
        try {
            return redis.pexpire(key, milliseconds);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long expireAt(@ShardingParam String key, long unixTime) {
        LogUtil.debugLog(resource, key);
        String command = "expireAt(String key, long unixTime)";
        before(key, command);
        try {
            return redis.expireAt(key, unixTime);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long pexpireAt(@ShardingParam String key, long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        String command = "pexpireAt(String key, long millisecondsTimestamp)";
        before(key, command);
        try {
            return redis.pexpireAt(key, millisecondsTimestamp);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long ttl(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "ttl(String key)";
        before(key, command);
        try {
            return redis.ttl(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long pttl(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "pttl(String key)";
        before(key, command);
        try {
            return redis.pttl(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam String key, long offset, boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "setbit(String key, long offset, boolean value)";
        before(key, command);
        try {
            return redis.setbit(key, offset, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam String key, long offset, String value) {
        LogUtil.debugLog(resource, key);
        String command = "setbit(String key, long offset, String value)";
        before(key, command);
        try {
            return redis.setbit(key, offset, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean getbit(@ShardingParam String key, long offset) {
        LogUtil.debugLog(resource, key);
        String command = "getbit(String key, long offset)";
        before(key, command);
        try {
            return redis.getbit(key, offset);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long setrange(@ShardingParam String key, long offset, String value) {
        LogUtil.debugLog(resource, key);
        String command = "setrange(String key, long offset, String value)";
        before(key, command);
        try {
            return redis.setrange(key, offset, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String getrange(@ShardingParam String key, long startOffset, long endOffset) {
        LogUtil.debugLog(resource, key);
        String command = "getrange(String key, long startOffset, long endOffset)";
        before(key, command);
        try {
            return redis.getrange(key, startOffset, endOffset);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String getSet(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        String command = "getSet(String key, String value)";
        before(key, command);
        try {
            return redis.getSet(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long setnx(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        String command = "setnx(String key, String value)";
        before(key, command);
        try {
            return redis.setnx(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String setex(@ShardingParam String key, int seconds, String value) {
        LogUtil.debugLog(resource, key);
        String command = "setex(String key, int seconds, String value)";
        before(key, command);
        try {
            return redis.setex(key, seconds, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String psetex(@ShardingParam String key, long milliseconds, String value) {
        LogUtil.debugLog(resource, key);
        String command = "psetex(String key, long milliseconds, String value)";
        before(key, command);
        try {
            return redis.psetex(key, milliseconds, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long decrBy(@ShardingParam String key, long integer) {
        LogUtil.debugLog(resource, key);
        String command = "decrBy(String key, long integer)";
        before(key, command);
        try {
            return redis.decrBy(key, integer);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long decr(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "decr(String key)";
        before(key, command);
        try {
            return redis.decr(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long incrBy(@ShardingParam String key, long integer) {
        LogUtil.debugLog(resource, key);
        String command = "incrBy(String key, long integer)";
        before(key, command);
        try {
            return redis.incrBy(key, integer);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double incrByFloat(@ShardingParam String key, double value) {
        LogUtil.debugLog(resource, key);
        String command = "incrByFloat(String key, double value)";
        before(key, command);
        try {
            return redis.incrByFloat(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long incr(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "incr(String key)";
        before(key, command);
        try {
            return redis.incr(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long append(@ShardingParam String key, String value) {
        LogUtil.debugLog(resource, key);
        String command = "append(String key, String value)";
        before(key, command);
        try {
            return redis.append(key, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String substr(@ShardingParam String key, int start, int end) {
        LogUtil.debugLog(resource, key);
        String command = "substr(String key, int start, int end)";
        before(key, command);
        try {
            return redis.substr(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hset(@ShardingParam String key, String field, String value) {
        LogUtil.debugLog(resource, key);
        String command = "hset(String key, String field, String value)";
        before(key, command);
        try {
            return redis.hset(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String hget(@ShardingParam String key, String field) {
        LogUtil.debugLog(resource, key);
        String command = "hget(String key, String field)";
        before(key, command);
        try {
            return redis.hget(key, field);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hsetnx(@ShardingParam String key, String field, String value) {
        LogUtil.debugLog(resource, key);
        String command = "hsetnx(String key, String field, String value)";
        before(key, command);
        try {
            return redis.hsetnx(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String hmset(@ShardingParam String key, Map<String, String> hash) {
        LogUtil.debugLog(resource, key);
        String command = "hmset(String key, Map<String, String> hash)";
        before(key, command);
        try {
            return redis.hmset(key, hash);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> hmget(@ShardingParam String key, String... fields) {
        LogUtil.debugLog(resource, key);
        String command = "hmget(String key, String... fields)";
        before(key, command);
        try {
            return redis.hmget(key, fields);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hincrBy(@ShardingParam String key, String field, long value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrBy(String key, String field, long value)";
        before(key, command);
        try {
            return redis.hincrBy(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double hincrByFloat(@ShardingParam String key, String field, double value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrByFloat(String key, String field, double value)";
        before(key, command);
        try {
            return redis.hincrByFloat(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean hexists(@ShardingParam String key, String field) {
        LogUtil.debugLog(resource, key);
        String command = "hexists(String key, String field)";
        before(key, command);
        try {
            return redis.hexists(key, field);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hdel(@ShardingParam String key, String... field) {
        LogUtil.debugLog(resource, key);
        String command = "hdel(String key, String... field)";
        before(key, command);
        try {
            return redis.hdel(key, field);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long hlen(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "hlen(String key)";
        before(key, command);
        try {
            return redis.hlen(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> hkeys(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "hkeys(String key)";
        before(key, command);
        try {
            return redis.hkeys(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> hvals(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "hvals(String key)";
        before(key, command);
        try {
            return redis.hvals(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Map<String, String> hgetAll(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "hgetAll(String key)";
        before(key, command);
        try {
            return redis.hgetAll(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long rpush(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        String command = "rpush(String key, String... string)";
        before(key, command);
        try {
            return redis.rpush(key, string);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long lpush(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        String command = "lpush(String key, String... string)";
        before(key, command);
        try {
            return redis.lpush(key, string);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long llen(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "llen(String key)";
        before(key, command);
        try {
            return redis.llen(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long linsert(@ShardingParam String key, ListPosition where, String pivot, String value) {
        LogUtil.debugLog(resource, key);
        String command = "linsert(String key, ListPosition where, String pivot, String value)";
        before(key, command);
        try {
            return redis.linsert(key, where, pivot, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long linsert(@ShardingParam byte[] key, ListPosition where, byte[] pivot, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "linsert(byte[] key, ListPosition where, byte[] pivot, byte[] value)";
        before(key, command);
        try {
            return redis.linsert(key, where, pivot, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> lrange(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "lrange(String key, long start, long end)";
        before(key, command);
        try {
            return redis.lrange(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String ltrim(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "ltrim(String key, long start, long end)";
        before(key, command);
        try {
            return redis.ltrim(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String lindex(@ShardingParam String key, long index) {
        LogUtil.debugLog(resource, key);
        String command = "lindex(String key, long index)";
        before(key, command);
        try {
            return redis.lindex(key, index);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String lset(@ShardingParam String key, long index, String value) {
        LogUtil.debugLog(resource, key);
        String command = "lset(String key, long index, String value)";
        before(key, command);
        try {
            return redis.lset(key, index, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long lrem(@ShardingParam String key, long count, String value) {
        LogUtil.debugLog(resource, key);
        String command = "lrem(String key, long count, String value)";
        before(key, command);
        try {
            return redis.lrem(key, count, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String lpop(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "lpop(String key)";
        before(key, command);
        try {
            return redis.lpop(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String rpop(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "rpop(String key)";
        before(key, command);
        try {
            return redis.rpop(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long sadd(@ShardingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        String command = "sadd(String key, String... member)";
        before(key, command);
        try {
            return redis.sadd(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> smembers(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "smembers(String key)";
        before(key, command);
        try {
            return redis.smembers(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long srem(@ShardingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        String command = "srem(String key, String... member)";
        before(key, command);
        try {
            return redis.srem(key, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String spop(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "spop(String key)";
        before(key, command);
        try {
            return redis.spop(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Set<String> spop(@ShardingParam String key, long count) {
        LogUtil.debugLog(resource, key);
        String command = "spop(String key, long count)";
        before(key, command);
        try {
            return redis.spop(key, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long scard(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "scard(String key)";
        before(key, command);
        try {
            return redis.scard(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean sismember(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        String command = "sismember(String key, String member)";
        before(key, command);
        try {
            return redis.sismember(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<Boolean> smismember(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        String command = "smismember(String key, String[] members)";
        before(key, command);
        try {
            return redis.smismember(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<Boolean> smismember(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "smismember(byte[] key, byte[][] members)";
        before(key, command);
        try {
            return redis.smismember(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String srandmember(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(String key)";
        before(key, command);
        try {
            return redis.srandmember(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> srandmember(@ShardingParam String key, int count) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(String key, int count)";
        before(key, command);
        try {
            return redis.srandmember(key, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long strlen(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "strlen(String key)";
        before(key, command);
        try {
            return redis.strlen(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, double score, String member) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, double score, String member)";
        before(key, command);
        try {
            return redis.zadd(key, score, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, double score, String member, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, double score, String member, ZAddParams params)";
        before(key, command);
        try {
            return redis.zadd(key, score, member, params);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, Map<String, Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, Map<String, Double> scoreMembers)";
        before(key, command);
        try {
            return redis.zadd(key, scoreMembers);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam String key, Map<String, Double> scoreMembers, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(String key, Map<String, Double> scoreMembers, ZAddParams params)";
        before(key, command);
        try {
            return redis.zadd(key, scoreMembers, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrange(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrange(String key, long start, long end)";
        before(key, command);
        try {
            return redis.zrange(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zrem(@ShardingParam String key, String... member) {
        LogUtil.debugLog(resource, key);
        String command = "zrem(String key, String... member)";
        before(key, command);
        try {
            return redis.zrem(key, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam String key, double score, String member) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(String key, double score, String member)";
        before(key, command);
        try {
            return redis.zincrby(key, score, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam String key, double score, String member, ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(String key, double score, String member, ZIncrByParams params)";
        before(key, command);
        try {
            return redis.zincrby(key, score, member, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zrank(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        String command = "zrank(String key, String member)";
        before(key, command);
        try {
            return redis.zrank(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zrevrank(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrank(String key, String member)";
        before(key, command);
        try {
            return redis.zrevrank(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrange(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrange(String key, long start, long end)";
        before(key, command);
        try {
            return redis.zrevrange(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeWithScores(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeWithScores(String key, long start, long end)";
        before(key, command);
        try {
            return redis.zrangeWithScores(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeWithScores(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeWithScores(String key, long start, long end)";
        before(key, command);
        try {
            return redis.zrevrangeWithScores(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zcard(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "zcard(String key)";
        before(key, command);
        try {
            return redis.zcard(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Double zscore(@ShardingParam String key, String member) {
        LogUtil.debugLog(resource, key);
        String command = "zscore(String key, String member)";
        before(key, command);
        try {
            return redis.zscore(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> sort(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "sort(String key)";
        before(key, command);
        try {
            return redis.sort(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> sort(@ShardingParam String key, SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        String command = "sort(String key, SortingParams sortingParameters)";
        before(key, command);
        try {
            return redis.sort(key, sortingParameters);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(String key, double min, double max)";
        before(key, command);
        try {
            return redis.zcount(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(String key, String min, String max)";
        before(key, command);
        try {
            return redis.zcount(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, double min, double max)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, String min, String max)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, double max, double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, double max, double min)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, String max, String min)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrangeByScore(@ShardingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(String key, String min, String max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, double min, double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, double min, double max)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, double max, double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, double max, double min)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByScore(@ShardingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(String key, String max, String min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, String min, String max)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, String max, String min)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(String key, String min, String max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByRank(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByRank(String key, long start, long end)";
        before(key, command);
        try {
            return redis.zremrangeByRank(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam String key, double start, double end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(String key, double start, double end)";
        before(key, command);
        try {
            return redis.zremrangeByScore(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam String key, String start, String end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(String key, String start, String end)";
        before(key, command);
        try {
            return redis.zremrangeByScore(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zlexcount(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        String command = "zlexcount(String key, String min, String max)";
        before(key, command);
        try {
            return redis.zlexcount(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrangeByLex(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(String key, String min, String max)";
        before(key, command);
        try {
            return redis.zrangeByLex(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrangeByLex(@ShardingParam String key, String min, String max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(String key, String min, String max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByLex(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByLex(@ShardingParam String key, String max, String min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(String key, String max, String min)";
        before(key, command);
        try {
            return redis.zrevrangeByLex(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<String> zrevrangeByLex(@ShardingParam String key, String max, String min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(String key, String max, String min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByLex(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByLex(@ShardingParam String key, String min, String max) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByLex(String key, String min, String max)";
        before(key, command);
        try {
            return redis.zremrangeByLex(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long lpushx(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        String command = "lpushx(String key, String... string)";
        before(key, command);
        try {
            return redis.lpushx(key, string);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long rpushx(@ShardingParam String key, String... string) {
        LogUtil.debugLog(resource, key);
        String command = "rpushx(String key, String... string)";
        before(key, command);
        try {
            return redis.rpushx(key, string);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "del(String key)";
        before(key, command);
        try {
            return redis.del(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String echo(@ShardingParam String string) {
        LogUtil.debugLog(resource, string);
        String command = "echo(String string)";
        before(string, command);
        try {
            return redis.echo(string);
        } finally {
            after(string, command);
        }
    }

    @ReadOp
    @Override
    public Long bitcount(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(String key)";
        before(key, command);
        try {
            return redis.bitcount(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long bitcount(@ShardingParam String key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(String key, long start, long end)";
        before(key, command);
        try {
            return redis.bitcount(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam String key, boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(String key, boolean value)";
        before(key, command);
        try {
            return redis.bitpos(key, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam String key, boolean value, BitPosParams params) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(String key, boolean value, BitPosParams params)";
        before(key, command);
        try {
            return redis.bitpos(key, value, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam byte[] key, boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(byte[] key, boolean value)";
        before(key, command);
        try {
            return redis.bitpos(key, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long bitpos(@ShardingParam byte[] key, boolean value, BitPosParams params) {
        LogUtil.debugLog(resource, key);
        String command = "bitpos(byte[] key, boolean value, BitPosParams params)";
        before(key, command);
        try {
            return redis.bitpos(key, value, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<String, String>> hscan(@ShardingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        String command = "hscan(String key, String cursor)";
        before(key, command);
        try {
            return redis.hscan(key, cursor);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<String, String>> hscan(@ShardingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        String command = "hscan(String key, String cursor, ScanParams params)";
        before(key, command);
        try {
            return redis.hscan(key, cursor, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<String> sscan(@ShardingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        String command = "sscan(String key, String cursor)";
        before(key, command);
        try {
            return redis.sscan(key, cursor);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<String> sscan(@ShardingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        String command = "sscan(String key, String cursor, ScanParams params)";
        before(key, command);
        try {
            return redis.sscan(key, cursor, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam String key, String cursor) {
        LogUtil.debugLog(resource, key);
        String command = "zscan(String key, String cursor)";
        before(key, command);
        try {
            return redis.zscan(key, cursor);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam String key, String cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zscan(String key, String cursor, ScanParams params)";
        before(key, command);
        try {
            return redis.zscan(key, cursor, params);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long pfadd(@ShardingParam String key, String... elements) {
        LogUtil.debugLog(resource, key);
        String command = "pfadd(String key, String... elements)";
        before(key, command);
        try {
            return redis.pfadd(key, elements);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public long pfcount(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "pfcount(String key)";
        before(key, command);
        try {
            return redis.pfcount(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam String key, double longitude, double latitude, String member) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(String key, double longitude, double latitude, String member)";
        before(key, command);
        try {
            return redis.geoadd(key, longitude, latitude, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap)";
        before(key, command);
        try {
            return redis.geoadd(key, memberCoordinateMap);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam String key, String member1, String member2) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(String key, String member1, String member2)";
        before(key, command);
        try {
            return redis.geodist(key, member1, member2);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam String key, String member1, String member2, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(String key, String member1, String member2, GeoUnit unit)";
        before(key, command);
        try {
            return redis.geodist(key, member1, member2, unit);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<String> geohash(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        String command = "geohash(String key, String... members)";
        before(key, command);
        try {
            return redis.geohash(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoCoordinate> geopos(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        String command = "geopos(String key, String... members)";
        before(key, command);
        try {
            return redis.geopos(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam String key, double longitude, double latitude, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(String key, double longitude, double latitude, double radius, GeoUnit unit)";
        before(key, command);
        try {
            return redis.georadius(key, longitude, latitude, radius, unit);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            return redis.georadius(key, longitude, latitude, radius, unit, param);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam String key, String member, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(String key, String member, double radius, GeoUnit unit)";
        before(key, command);
        try {
            return redis.georadiusByMember(key, member, radius, unit);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            return redis.georadiusByMember(key, member, radius, unit, param);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<Long> bitfield(@ShardingParam String key, String... arguments) {
        LogUtil.debugLog(resource, key);
        String command = "bitfield(String key, String... arguments)";
        before(key, command);
        try {
            return redis.bitfield(key, arguments);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam(type = ShardingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        String command = "del(byte[]... keys)";
        for (byte[] key : keys) {
            before(key, command);
        }
        try {
            return redis.del(keys);
        } finally {
            for (byte[] key : keys) {
                after(key, command);
            }
        }
    }

    @ReadOp
    @Override
    public Long exists(@ShardingParam(type = ShardingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        String command = "exists(byte[]... keys)";
        for (byte[] key : keys) {
            before(key, command);
        }
        try {
            return redis.exists(keys);
        } finally {
            for (byte[] key : keys) {
                after(key, command);
            }
        }
    }

    @ReadOp
    @Override
    public Map<byte[], byte[]> mget(@ShardingParam(type = ShardingParam.Type.Collection) byte[]... keys) {
        LogUtil.debugLog(resource, keys);
        String command = "mget(byte[]... keys)";
        if (interceptEnable()) {
            for (byte[] key : keys) {
                before(key, command);
            }
        }
        try {
            return redis.mget(keys);
        } finally {
            if (interceptEnable()) {
                for (byte[] key : keys) {
                    after(key, command);
                }
            }
        }
    }

    @WriteOp
    @Override
    public String mset(@ShardingParam(type = ShardingParam.Type.Collection) Map<byte[], byte[]> keysvalues) {
        LogUtil.debugLog(resource, keysvalues);
        String command = "mset(byte[]... keysvalues)";
        if (interceptEnable()) {
            for (byte[] key : keysvalues.keySet()) {
                before(key, command);
            }
        }
        try {
            return redis.mset(keysvalues);
        } finally {
            if (interceptEnable()) {
                for (byte[] key : keysvalues.keySet()) {
                    after(key, command);
                }
            }
        }
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        String command = "del(String... keys)";
        if (interceptEnable()) {
            for (String key : keys) {
                before(key, command);
            }
        }
        try {
            return redis.del(keys);
        } finally {
            if (interceptEnable()) {
                for (String key : keys) {
                    after(key, command);
                }
            }
        }
    }

    @ReadOp
    @Override
    public Long exists(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        String command = "exists(String... keys)";
        if (interceptEnable()) {
            for (String key : keys) {
                before(key, command);
            }
        }
        try {
            return redis.exists(keys);
        } finally {
            if (interceptEnable()) {
                for (String key : keys) {
                    after(key, command);
                }
            }
        }
    }

    @ReadOp
    @Override
    public Map<String, String> mget(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        LogUtil.debugLog(resource, keys);
        String command = "mget(String... keys)";
        for (String key : keys) {
            before(key, command);
        }
        try {
            return redis.mget(keys);
        } finally {
            for (String key : keys) {
                after(key, command);
            }
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time) {
        LogUtil.debugLog(resource, key);
        String command = "set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time)";
        before(key, command);
        try {
            return redis.set(key, value, nxxx, expx, time);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean exists(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "exists(byte[] key)";
        before(key, command);
        try {
            return redis.exists(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long persist(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "persist(byte[] key)";
        before(key, command);
        try {
            return redis.persist(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String type(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "type(byte[] key)";
        before(key, command);
        try {
            return redis.type(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long expire(@ShardingParam byte[] key, int seconds) {
        LogUtil.debugLog(resource, key);
        String command = "expire(byte[] key, int seconds)";
        before(key, command);
        try {
            return redis.expire(key, seconds);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long pexpire(@ShardingParam byte[] key, long milliseconds) {
        LogUtil.debugLog(resource, key);
        String command = "pexpire(byte[] key, long milliseconds)";
        before(key, command);
        try {
            return redis.pexpire(key, milliseconds);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long expireAt(@ShardingParam byte[] key, long unixTime) {
        LogUtil.debugLog(resource, key);
        String command = "expireAt(byte[] key, long unixTime)";
        before(key, command);
        try {
            return redis.expireAt(key, unixTime);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long pexpireAt(@ShardingParam byte[] key, long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        String command = "pexpireAt(byte[] key, long millisecondsTimestamp)";
        before(key, command);
        try {
            return redis.pexpireAt(key, millisecondsTimestamp);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long ttl(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "ttl(byte[] key)";
        before(key, command);
        try {
            return redis.ttl(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long pttl(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "pttl(byte[] key)";
        before(key, command);
        try {
            return redis.pttl(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam byte[] key, long offset, boolean value) {
        LogUtil.debugLog(resource, key);
        String command = "setbit(byte[] key, long offset, boolean value)";
        before(key, command);
        try {
            return redis.setbit(key, offset, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Boolean setbit(@ShardingParam byte[] key, long offset, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setbit(byte[] key, long offset, byte[] value)";
        before(key, command);
        try {
            return redis.setbit(key, offset, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean getbit(@ShardingParam byte[] key, long offset) {
        LogUtil.debugLog(resource, key);
        String command = "getbit(byte[] key, long offset)";
        before(key, command);
        try {
            return redis.getbit(key, offset);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long setrange(@ShardingParam byte[] key, long offset, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setrange(byte[] key, long offset, byte[] value)";
        before(key, command);
        try {
            return redis.setrange(key, offset, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] getrange(@ShardingParam byte[] key, long startOffset, long endOffset) {
        LogUtil.debugLog(resource, key);
        String command = "getrange(byte[] key, long startOffset, long endOffset)";
        before(key, command);
        try {
            return redis.getrange(key, startOffset, endOffset);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public byte[] getSet(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "getSet(byte[] key, byte[] value)";
        before(key, command);
        try {
            return redis.getSet(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long setnx(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setnx(byte[] key, byte[] value)";
        before(key, command);
        try {
            return redis.setnx(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String setex(@ShardingParam byte[] key, int seconds, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "setex(byte[] key, int seconds, byte[] value)";
        before(key, command);
        try {
            return redis.setex(key, seconds, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String psetex(@ShardingParam byte[] key, long milliseconds, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "psetex(byte[] key, long milliseconds, byte[] value)";
        before(key, command);
        try {
            return redis.psetex(key, milliseconds, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long decrBy(@ShardingParam byte[] key, long integer) {
        LogUtil.debugLog(resource, key);
        String command = "decrBy(byte[] key, long integer)";
        before(key, command);
        try {
            return redis.decrBy(key, integer);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long decr(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "decr(byte[] key)";
        before(key, command);
        try {
            return redis.decr(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long incrBy(@ShardingParam byte[] key, long integer) {
        LogUtil.debugLog(resource, key);
        String command = "incrBy(byte[] key, long integer)";
        before(key, command);
        try {
            return redis.incrBy(key, integer);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double incrByFloat(@ShardingParam byte[] key, double value) {
        LogUtil.debugLog(resource, key);
        String command = "incrByFloat(byte[] key, double value)";
        before(key, command);
        try {
            return redis.incrByFloat(key, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long incr(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "incr(byte[] key)";
        before(key, command);
        try {
            return redis.incr(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long append(@ShardingParam byte[] key, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "append(byte[] key, byte[] value)";
        before(key, command);
        try {
            return redis.append(key, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] substr(@ShardingParam byte[] key, int start, int end) {
        LogUtil.debugLog(resource, key);
        String command = "substr(byte[] key, int start, int end)";
        before(key, command);
        try {
            return redis.substr(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hset(@ShardingParam byte[] key, byte[] field, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "hset(byte[] key, byte[] field, byte[] value)";
        before(key, command);
        try {
            return redis.hset(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] hget(@ShardingParam byte[] key, byte[] field) {
        LogUtil.debugLog(resource, key);
        String command = "hget(byte[] key, byte[] field)";
        before(key, command);
        try {
            return redis.hget(key, field);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hsetnx(@ShardingParam byte[] key, byte[] field, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "hsetnx(byte[] key, byte[] field, byte[] value)";
        before(key, command);
        try {
            return redis.hsetnx(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String hmset(@ShardingParam byte[] key, Map<byte[], byte[]> hash) {
        LogUtil.debugLog(resource, key);
        String command = "hmset(byte[] key, Map<byte[], byte[]> hash)";
        before(key, command);
        try {
            return redis.hmset(key, hash);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> hmget(@ShardingParam byte[] key, byte[]... fields) {
        LogUtil.debugLog(resource, key);
        String command = "hmget(byte[] key, byte[]... fields)";
        before(key, command);
        try {
            return redis.hmget(key, fields);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hincrBy(@ShardingParam byte[] key, byte[] field, long value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrBy(byte[] key, byte[] field, long value)";
        before(key, command);
        try {
            return redis.hincrBy(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double hincrByFloat(@ShardingParam byte[] key, byte[] field, double value) {
        LogUtil.debugLog(resource, key);
        String command = "hincrByFloat(byte[] key, byte[] field, double value)";
        before(key, command);
        try {
            return redis.hincrByFloat(key, field, value);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean hexists(@ShardingParam byte[] key, byte[] field) {
        LogUtil.debugLog(resource, key);
        String command = "hexists(byte[] key, byte[] field)";
        before(key, command);
        try {
            return redis.hexists(key, field);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long hdel(@ShardingParam byte[] key, byte[]... field) {
        LogUtil.debugLog(resource, key);
        String command = "hdel(byte[] key, byte[]... field)";
        before(key, command);
        try {
            return redis.hdel(key, field);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long hlen(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hlen(byte[] key)";
        before(key, command);
        try {
            return redis.hlen(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> hkeys(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hkeys(byte[] key)";
        before(key, command);
        try {
            return redis.hkeys(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> hvals(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hvals(byte[] key)";
        before(key, command);
        try {
            return redis.hvals(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Map<byte[], byte[]> hgetAll(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "hgetAll(byte[] key)";
        before(key, command);
        try {
            return redis.hgetAll(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long rpush(@ShardingParam byte[] key, byte[]... args) {
        LogUtil.debugLog(resource, key);
        String command = "rpush(byte[] key, byte[]... args)";
        before(key, command);
        try {
            return redis.rpush(key, args);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long lpush(@ShardingParam byte[] key, byte[]... args) {
        LogUtil.debugLog(resource, key);
        String command = "lpush(byte[] key, byte[]... args)";
        before(key, command);
        try {
            return redis.lpush(key, args);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long llen(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "llen(byte[] key)";
        before(key, command);
        try {
            return redis.llen(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> lrange(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "lrange(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.lrange(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public String ltrim(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "ltrim(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.ltrim(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] lindex(@ShardingParam byte[] key, long index) {
        LogUtil.debugLog(resource, key);
        String command = "lindex(byte[] key, long index)";
        before(key, command);
        try {
            return redis.lindex(key, index);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String lset(@ShardingParam byte[] key, long index, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "lset(byte[] key, long index, byte[] value)";
        before(key, command);
        try {
            return redis.lset(key, index, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long lrem(@ShardingParam byte[] key, long count, byte[] value) {
        LogUtil.debugLog(resource, key);
        String command = "lrem(byte[] key, long count, byte[] value)";
        before(key, command);
        try {
            return redis.lrem(key, count, value);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public byte[] lpop(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "lpop(byte[] key)";
        before(key, command);
        try {
            return redis.lpop(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public byte[] rpop(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "rpop(byte[] key)";
        before(key, command);
        try {
            return redis.rpop(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long sadd(@ShardingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        String command = "sadd(byte[] key, byte[]... member)";
        before(key, command);
        try {
            return redis.sadd(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> smembers(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "smembers(byte[] key)";
        before(key, command);
        try {
            return redis.smembers(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long srem(@ShardingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        String command = "srem(byte[] key, byte[]... member)";
        before(key, command);
        try {
            return redis.srem(key, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public byte[] spop(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "spop(byte[] key)";
        before(key, command);
        try {
            return redis.spop(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Set<byte[]> spop(@ShardingParam byte[] key, long count) {
        LogUtil.debugLog(resource, key);
        String command = "spop(byte[] key, long count)";
        before(key, command);
        try {
            return redis.spop(key, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long scard(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "scard(byte[] key)";
        before(key, command);
        try {
            return redis.scard(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Boolean sismember(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "sismember(byte[] key, byte[] member)";
        before(key, command);
        try {
            return redis.sismember(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] srandmember(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(byte[] key)";
        before(key, command);
        try {
            return redis.srandmember(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> srandmember(@ShardingParam byte[] key, int count) {
        LogUtil.debugLog(resource, key);
        String command = "srandmember(byte[] key, int count)";
        before(key, command);
        try {
            return redis.srandmember(key, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long strlen(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "strlen(byte[] key)";
        before(key, command);
        try {
            return redis.strlen(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, double score, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, double score, byte[] member)";
        before(key, command);
        try {
            return redis.zadd(key, score, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, double score, byte[] member, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, double score, byte[] member, ZAddParams params)";
        before(key, command);
        try {
            return redis.zadd(key, score, member, params);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, Map<byte[], Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, Map<byte[], Double> scoreMembers)";
        before(key, command);
        try {
            return redis.zadd(key, scoreMembers);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zadd(@ShardingParam byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params)";
        before(key, command);
        try {
            return redis.zadd(key, scoreMembers, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrange(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrange(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.zrange(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zrem(@ShardingParam byte[] key, byte[]... member) {
        LogUtil.debugLog(resource, key);
        String command = "zrem(byte[] key, byte[]... member)";
        before(key, command);
        try {
            return redis.zrem(key, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam byte[] key, double score, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(byte[] key, double score, byte[] member)";
        before(key, command);
        try {
            return redis.zincrby(key, score, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Double zincrby(@ShardingParam byte[] key, double score, byte[] member, ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zincrby(byte[] key, double score, byte[] member, ZIncrByParams params)";
        before(key, command);
        try {
            return redis.zincrby(key, score, member, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zrank(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zrank(byte[] key, byte[] member)";
        before(key, command);
        try {
            return redis.zrank(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zrevrank(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrank(byte[] key, byte[] member)";
        before(key, command);
        try {
            return redis.zrevrank(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrange(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrange(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.zrevrange(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeWithScores(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeWithScores(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.zrangeWithScores(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeWithScores(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeWithScores(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.zrevrangeWithScores(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zcard(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "zcard(byte[] key)";
        before(key, command);
        try {
            return redis.zcard(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Double zscore(@ShardingParam byte[] key, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "zscore(byte[] key, byte[] member)";
        before(key, command);
        try {
            return redis.zscore(key, member);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<Double> zmscore(@ShardingParam String key, String... members) {
        LogUtil.debugLog(resource, key);
        String command = "zmscore(String key, String[] members)";
        before(key, command);
        try {
            return redis.zmscore(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<Double> zmscore(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "zmscore(byte[] key, byte[][] members)";
        before(key, command);
        try {
            return redis.zmscore(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> sort(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "sort(byte[] key)";
        before(key, command);
        try {
            return redis.sort(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> sort(@ShardingParam byte[] key, SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        String command = "sort(byte[] key, SortingParams sortingParameters)";
        before(key, command);
        try {
            return redis.sort(key, sortingParameters);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(byte[] key, double min, double max)";
        before(key, command);
        try {
            return redis.zcount(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zcount(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zcount(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            return redis.zcount(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, double min, double max)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, double max, double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, double max, double min)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, byte[] max, byte[] min)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByScore(@ShardingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScore(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, double min, double max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, double min, double max)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, double max, double min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, double max, double min)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, double min, double max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByScore(@ShardingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScore(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrangeByScoreWithScores(@ShardingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByScoreWithScores(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, double max, double min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(@ShardingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByRank(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByRank(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.zremrangeByRank(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam byte[] key, double start, double end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(byte[] key, double start, double end)";
        before(key, command);
        try {
            return redis.zremrangeByScore(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByScore(@ShardingParam byte[] key, byte[] start, byte[] end) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByScore(byte[] key, byte[] start, byte[] end)";
        before(key, command);
        try {
            return redis.zremrangeByScore(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long zlexcount(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zlexcount(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            return redis.zlexcount(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByLex(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            return redis.zrangeByLex(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrangeByLex(@ShardingParam byte[] key, byte[] min, byte[] max, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count)";
        before(key, command);
        try {
            return redis.zrangeByLex(key, min, max, offset, count);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByLex(@ShardingParam byte[] key, byte[] max, byte[] min) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(byte[] key, byte[] max, byte[] min)";
        before(key, command);
        try {
            return redis.zrevrangeByLex(key, max, min);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Set<byte[]> zrevrangeByLex(@ShardingParam byte[] key, byte[] max, byte[] min, int offset, int count) {
        LogUtil.debugLog(resource, key);
        String command = "zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count)";
        before(key, command);
        try {
            return redis.zrevrangeByLex(key, max, min, offset, count);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long zremrangeByLex(@ShardingParam byte[] key, byte[] min, byte[] max) {
        LogUtil.debugLog(resource, key);
        String command = "zremrangeByLex(byte[] key, byte[] min, byte[] max)";
        before(key, command);
        try {
            return redis.zremrangeByLex(key, min, max);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long lpushx(@ShardingParam byte[] key, byte[]... arg) {
        LogUtil.debugLog(resource, key);
        String command = "lpushx(byte[] key, byte[]... arg)";
        before(key, command);
        try {
            return redis.lpushx(key, arg);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long rpushx(@ShardingParam byte[] key, byte[]... arg) {
        LogUtil.debugLog(resource, key);
        String command = "rpushx(byte[] key, byte[]... arg)";
        before(key, command);
        try {
            return redis.rpushx(key, arg);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long del(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "del(byte[] key)";
        before(key, command);
        try {
            return redis.del(key);
        } finally {
            after(key, command);
        }
    }


    @ReadOp
    @Override
    public Long bitcount(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(byte[] key)";
        before(key, command);
        try {
            return redis.bitcount(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Long bitcount(@ShardingParam byte[] key, long start, long end) {
        LogUtil.debugLog(resource, key);
        String command = "bitcount(byte[] key, long start, long end)";
        before(key, command);
        try {
            return redis.bitcount(key, start, end);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long pfadd(@ShardingParam byte[] key, byte[]... elements) {
        LogUtil.debugLog(resource, key);
        String command = "pfadd(byte[] key, byte[]... elements)";
        before(key, command);
        try {
            return redis.pfadd(key, elements);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public long pfcount(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "pfcount(byte[] key)";
        before(key, command);
        try {
            return redis.pfcount(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam byte[] key, double longitude, double latitude, byte[] member) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(byte[] key, double longitude, double latitude, byte[] member)";
        before(key, command);
        try {
            return redis.geoadd(key, longitude, latitude, member);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public Long geoadd(@ShardingParam byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        String command = "geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap)";
        before(key, command);
        try {
            return redis.geoadd(key, memberCoordinateMap);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam byte[] key, byte[] member1, byte[] member2) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(byte[] key, byte[] member1, byte[] member2)";
        before(key, command);
        try {
            return redis.geodist(key, member1, member2);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public Double geodist(@ShardingParam byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit)";
        before(key, command);
        try {
            return redis.geodist(key, member1, member2, unit);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<byte[]> geohash(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "geohash(byte[] key, byte[]... members)";
        before(key, command);
        try {
            return redis.geohash(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoCoordinate> geopos(@ShardingParam byte[] key, byte[]... members) {
        LogUtil.debugLog(resource, key);
        String command = "geopos(byte[] key, byte[]... members)";
        before(key, command);
        try {
            return redis.geopos(key, members);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit)";
        before(key, command);
        try {
            return redis.georadius(key, longitude, latitude, radius, unit);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadius(@ShardingParam byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            return redis.georadius(key, longitude, latitude, radius, unit, param);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam byte[] key, byte[] member, double radius, GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit)";
        before(key, command);
        try {
            return redis.georadiusByMember(key, member, radius, unit);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<GeoRadiusResponse> georadiusByMember(@ShardingParam byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        String command = "georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param)";
        before(key, command);
        try {
            return redis.georadiusByMember(key, member, radius, unit, param);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(@ShardingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        String command = "hscan(byte[] key, byte[] cursor)";
        before(key, command);
        try {
            return redis.hscan(key, cursor);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(@ShardingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        String command = "hscan(byte[] key, byte[] cursor, ScanParams params)";
        before(key, command);
        try {
            return redis.hscan(key, cursor, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<byte[]> sscan(@ShardingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        String command = "sscan(byte[] key, byte[] cursor)";
        before(key, command);
        try {
            return redis.sscan(key, cursor);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<byte[]> sscan(@ShardingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        String command = "sscan(byte[] key, byte[] cursor, ScanParams params)";
        before(key, command);
        try {
            return redis.sscan(key, cursor, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam byte[] key, byte[] cursor) {
        LogUtil.debugLog(resource, key);
        String command = "zscan(byte[] key, byte[] cursor)";
        before(key, command);
        try {
            return redis.zscan(key, cursor);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public ScanResult<Tuple> zscan(@ShardingParam byte[] key, byte[] cursor, ScanParams params) {
        LogUtil.debugLog(resource, key);
        String command = "zscan(byte[] key, byte[] cursor, ScanParams params)";
        before(key, command);
        try {
            return redis.zscan(key, cursor, params);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public List<Long> bitfield(@ShardingParam byte[] key, byte[]... arguments) {
        LogUtil.debugLog(resource, key);
        String command = "bitfield(byte[] key, byte[]... arguments)";
        before(key, command);
        try {
            return redis.bitfield(key, arguments);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value, String nxxx, String expx, long time) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value, String nxxx, String expx, long time)";
        before(key, command);
        try {
            return redis.set(key, value, nxxx, expx, time);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam String key, String value, String nxxx) {
        LogUtil.debugLog(resource, key);
        String command = "set(String key, String value, String nxxx)";
        before(key, command);
        try {
            return redis.set(key, value, nxxx);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String set(@ShardingParam byte[] key, byte[] value, byte[] nxxx) {
        LogUtil.debugLog(resource, key);
        String command = "set(byte[] key, byte[] value, byte[] nxxx)";
        before(key, command);
        try {
            return redis.set(key, value, nxxx);
        } finally {
            after(key, command);
        }
    }

    @Override
    public Object eval(byte[] script, int keyCount, byte[]... params) {
        throw new CamelliaRedisException("not invoke here");
    }

    @Override
    public Object evalsha(byte[] sha1, int keyCount, byte[]... params) {
        throw new CamelliaRedisException("not invoke here");
    }

    @ReadOp
    @Override
    public byte[] dump(@ShardingParam String key) {
        LogUtil.debugLog(resource, key);
        String command = "dump(String key)";
        before(key, command);
        try {
            return redis.dump(key);
        } finally {
            after(key, command);
        }
    }

    @ReadOp
    @Override
    public byte[] dump(@ShardingParam byte[] key) {
        LogUtil.debugLog(resource, key);
        String command = "dump(byte[] key)";
        before(key, command);
        try {
            return redis.dump(key);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String restore(@ShardingParam byte[] key, int ttl, byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        String command = "restore(byte[] key, int ttl, byte[] serializedValue)";
        before(key, command);
        try {
            return redis.restore(key, ttl, serializedValue);
        } finally {
            after(key, command);
        }
    }

    @WriteOp
    @Override
    public String restore(@ShardingParam String key, int ttl, byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        String command = "restore(String key, int ttl, byte[] serializedValue)";
        before(key, command);
        try {
            return redis.restore(key, ttl, serializedValue);
        } finally {
            after(key, command);
        }
    }
}
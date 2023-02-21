package com.netease.nim.camellia.redis.pipeline;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShardingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.redis.resource.PipelineResource;
import com.netease.nim.camellia.redis.resource.RedisClientResourceUtil;
import com.netease.nim.camellia.redis.util.CamelliaBitPosParams;
import com.netease.nim.camellia.redis.base.utils.LogUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 封装了pipeline的接口
 * Created by caojiajun on 2019/7/22.
 */
public class CamelliaRedisPipelineImpl implements ICamelliaRedisPipeline {

    private Resource resource;
    private RedisClientPool clientPool;
    private ResponseQueable queable;

    public CamelliaRedisPipelineImpl(Resource resource) {
        if (resource == null) return;
        this.resource = RedisClientResourceUtil.parseResourceByUrl(resource);
        if (resource instanceof PipelineResource) {
            this.queable = ((PipelineResource) resource).getQueable();
            this.clientPool = ((PipelineResource) resource).getClientPool();
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

    @WriteOp
    @Override
    public Response<Long> append(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.append(key, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.append(key, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> decr(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.decr(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.decr(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.set(key, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> get(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.get(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.get(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> decrBy(@ShardingParam final byte[] key, final long integer) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.decrBy(key, integer);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.decrBy(key, integer);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> del(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.del(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.del(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> echo(@ShardingParam final byte[] string) {
        LogUtil.debugLog(resource, string);
        Client client = clientPool.getClient(resource, string);
        client.echo(string);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, string, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.echo(string);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> exists(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.exists(key);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.exists(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> expire(@ShardingParam final byte[] key, final int seconds) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.expire(key, seconds);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.expire(key, seconds);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> pexpire(@ShardingParam final byte[] key, final long milliseconds) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.pexpire(key, milliseconds);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pexpire(key, milliseconds);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> expireAt(@ShardingParam final byte[] key, final long unixTime) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.expireAt(key, unixTime);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.expireAt(key, unixTime);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> pexpireAt(@ShardingParam final byte[] key, final long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.pexpireAt(key, millisecondsTimestamp);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pexpireAt(key, millisecondsTimestamp);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> get(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.get(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.get(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> getbit(@ShardingParam final byte[] key, final long offset) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.getbit(key, offset);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.getbit(key, offset);
            }
        });
    }

    @WriteOp
    @Override
    public Response<byte[]> getSet(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.getSet(key, value);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.getSet(key, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> getrange(@ShardingParam final byte[] key, final long startOffset, final long endOffset) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.getrange(key, startOffset, endOffset);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.getrange(key, startOffset, endOffset);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hdel(@ShardingParam final byte[] key, final byte[]... field) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hdel(key, field);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hdel(key, field);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> hexists(@ShardingParam final byte[] key, final byte[] field) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hexists(key, field);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hexists(key, field);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> hget(@ShardingParam final byte[] key, final byte[] field) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hget(key, field);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hget(key, field);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Map<byte[], byte[]>> hgetAll(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hgetAll(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_MAP, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hgetAll(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hincrBy(@ShardingParam final byte[] key, final byte[] field, final long value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hincrBy(key, field, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hincrBy(key, field, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> hincrByFloat(@ShardingParam final byte[] key, final byte[] field, final double value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hincrByFloat(key, field, value);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hincrByFloat(key, field, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> hkeys(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hkeys(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hkeys(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> hlen(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hlen(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hlen(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> hmget(@ShardingParam final byte[] key, final byte[]... fields) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hmget(key, fields);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hmget(key, fields);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> hmset(@ShardingParam final byte[] key, final Map<byte[], byte[]> hash) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hmset(key, hash);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hmset(key, hash);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hset(@ShardingParam final byte[] key, final byte[] field, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hset(key, field, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hset(key, field, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hsetnx(@ShardingParam final byte[] key, final byte[] field, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hsetnx(key, field, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hsetnx(key, field, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> hvals(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.hvals(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hvals(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> incr(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.incr(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.incr(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> incrBy(@ShardingParam final byte[] key, final long integer) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.incrBy(key, integer);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.incrBy(key, integer);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> incrByFloat(@ShardingParam final byte[] key, final double value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.incrByFloat(key, value);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.incrByFloat(key, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> lindex(@ShardingParam final byte[] key, final long index) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lindex(key, index);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lindex(key, index);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> linsert(@ShardingParam final byte[] key, final BinaryClient.LIST_POSITION where, final byte[] pivot, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.linsert(key, where, pivot, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.linsert(key, where, pivot, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> llen(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.llen(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.llen(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<byte[]> lpop(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lpop(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lpop(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> lpush(@ShardingParam final byte[] key, final byte[]... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lpush(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lpush(key, string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> lpushx(@ShardingParam final byte[] key, final byte[]... bytes) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lpushx(key, bytes);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lpushx(key, bytes);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> lrange(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lrange(key, start, end);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lrange(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> lrem(@ShardingParam final byte[] key, final long count, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lrem(key, count, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lrem(key, count, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> lset(@ShardingParam final byte[] key, final long index, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.lset(key, index, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lset(key, index, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> ltrim(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.ltrim(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.ltrim(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> persist(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.persist(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.persist(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<byte[]> rpop(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.rpop(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.rpop(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> rpush(@ShardingParam final byte[] key, final byte[]... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.rpush(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.rpush(key, string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> rpushx(@ShardingParam final byte[] key, final byte[]... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.rpushx(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.rpushx(key, string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> sadd(@ShardingParam final byte[] key, final byte[]... member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.sadd(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sadd(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> scard(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.scard(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.scard(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.set(key, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> set(final byte[] key, final byte[] value, final byte[] nxxx, final byte[] expx, final long time) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.set(key, value, nxxx, expx, time);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value, nxxx, expx, time);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Boolean> setbit(@ShardingParam final byte[] key, final long offset, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.setbit(key, offset, value);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setbit(key, offset, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> setrange(@ShardingParam final byte[] key, final long offset, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.setrange(key, offset, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setrange(key, offset, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> setex(@ShardingParam final byte[] key, final int seconds, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.setex(key, seconds, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setex(key, seconds, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> psetex(@ShardingParam final byte[] key, final long milliseconds, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.psetex(key, milliseconds, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.psetex(key, milliseconds, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> setnx(@ShardingParam final byte[] key, final byte[] value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.setnx(key, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setnx(key, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> smembers(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.smembers(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.smembers(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> sismember(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.sismember(key, member);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sismember(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> sort(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.sort(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sort(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> sort(@ShardingParam final byte[] key, final SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.sort(key, sortingParameters);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sort(key, sortingParameters);
            }
        });
    }

    @WriteOp
    @Override
    public Response<byte[]> spop(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.spop(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.spop(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Set<byte[]>> spop(@ShardingParam final byte[] key, final long count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.spop(key, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.spop(key, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> srandmember(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.srandmember(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.srandmember(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> srandmember(@ShardingParam final byte[] key, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.srandmember(key, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.srandmember(key, count);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> srem(@ShardingParam final byte[] key, final byte[]... member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.srem(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.srem(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> strlen(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.strlen(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.strlen(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> substr(@ShardingParam final byte[] key, final int start, final int end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.substr(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.substr(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> ttl(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.ttl(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.ttl(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> pttl(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.pttl(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pttl(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> type(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.type(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.type(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final double score, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zadd(key, score, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zadd(key, score, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final double score, final byte[] member, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zadd(key, score, member, params);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zadd(key, score, member, params);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zcard(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zcard(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zcard(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final byte[] key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zcount(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zcount(key, min, max);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final byte[] key, final double score, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zincrby(key, score, member);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zincrby(key, score, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final byte[] key, final double score, final byte[] member, final ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zincrby(key, score, member, params);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zincrby(key, score, member, params);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrange(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrange(key, start, end);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrange(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScore(key, min, max);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScore(key, min, max);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScore(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByScore(@ShardingParam final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScore(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScoreWithScores(key, min, max);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScoreWithScores(key, min, max);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScoreWithScores(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByScoreWithScores(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScore(key, max, min);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final byte[] max, final byte[] min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScore(key, max, min);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScore(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByScore(@ShardingParam final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScore(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScoreWithScores(key, max, min);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] max, final byte[] min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScoreWithScores(key, max, min);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScoreWithScores(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByScoreWithScores(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeWithScores(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeWithScores(key, start, end);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeWithScores(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zrank(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrank(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrank(key, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zrem(@ShardingParam final byte[] key, final byte[]... member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrem(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrem(key, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByRank(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zremrangeByRank(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByRank(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final byte[] key, final double start, final double end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zremrangeByScore(SafeEncoder.encode(key), start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByScore(SafeEncoder.encode(key), start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final byte[] key, final byte[] start, final byte[] end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zremrangeByScore(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByScore(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrange(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrange(key, start, end);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrange(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeWithScores(key, start, end);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeWithScores(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zrevrank(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrank(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrank(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Double> zscore(@ShardingParam final byte[] key, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zscore(key, member);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zscore(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zlexcount(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zlexcount(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zlexcount(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByLex(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByLex(key, min, max);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByLex(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrangeByLex(@ShardingParam final byte[] key, final byte[] min, final byte[] max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrangeByLex(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByLex(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByLex(@ShardingParam final byte[] key, final byte[] max, final byte[] min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByLex(key, max, min);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByLex(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<byte[]>> zrevrangeByLex(@ShardingParam final byte[] key, final byte[] max, final byte[] min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zrevrangeByLex(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByLex(key, max, min, offset, count);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByLex(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zremrangeByLex(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByLex(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.bitcount(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitcount(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final byte[] key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.bitcount(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitcount(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> pfadd(@ShardingParam final byte[] key, final byte[]... elements) {
        LogUtil.debugLog(resource, key);
        final Client client = clientPool.getClient(resource, key);
        client.pfadd(key, elements);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pfadd(key, elements);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> pfcount(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.pfcount(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pfcount(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final byte[] key, final double longitude, final double latitude, final byte[] member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.geoadd(key, longitude, latitude, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geoadd(key, longitude, latitude, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final byte[] key, final Map<byte[], GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.geoadd(key, memberCoordinateMap);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geoadd(key, memberCoordinateMap);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final byte[] key, final byte[] member1, final byte[] member2) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.geodist(key, member1, member2);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geodist(key, member1, member2);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final byte[] key, final byte[] member1, final byte[] member2, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.geodist(key, member1, member2, unit);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geodist(key, member1, member2, unit);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<byte[]>> geohash(@ShardingParam final byte[] key, final byte[]... members) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.geohash(key, members);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geohash(key, members);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoCoordinate>> geopos(@ShardingParam final byte[] key, final byte[]... members) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.geopos(key, members);
        return queable.getResponse(client, BuilderFactory.GEO_COORDINATE_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geopos(key, members);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.georadius(key, longitude, latitude, radius, unit);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadius(key, longitude, latitude, radius, unit);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final byte[] key, final double longitude, final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.georadius(key, longitude, latitude, radius, unit, param);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadius(key, longitude, latitude, radius, unit, param);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final byte[] key, final byte[] member, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.georadiusByMember(key, member, radius, unit);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadiusByMember(key, member, radius, unit);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final byte[] key, final byte[] member, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.georadiusByMember(key, member, radius, unit, param);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadiusByMember(key, member, radius, unit, param);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<Long>> bitfield(@ShardingParam final byte[] key, final byte[]... elements) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.bitfield(key, elements);
        return queable.getResponse(client, BuilderFactory.LONG_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitfield(key, elements);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final byte[] key, final boolean value, final BitPosParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.bitpos(key, value, params);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitpos(key, value, params);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> append(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.append(key, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.append(key, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> decr(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.decr(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.decr(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> decrBy(@ShardingParam final String key, final long integer) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.decrBy(key, integer);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.decrBy(key, integer);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> del(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.del(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.del(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> echo(@ShardingParam final String string) {
        LogUtil.debugLog(resource, string);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(string));
        client.echo(string);
        return queable.getResponse(client, BuilderFactory.STRING, resource, string, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.echo(string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Boolean> exists(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.exists(key);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.exists(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> expire(@ShardingParam final String key, final int seconds) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.expire(key, seconds);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.expire(key, seconds);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> pexpire(@ShardingParam final String key, final long milliseconds) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.pexpire(key, milliseconds);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pexpire(key, milliseconds);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> expireAt(@ShardingParam final String key, final long unixTime) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.expireAt(key, unixTime);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.expireAt(key, unixTime);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> pexpireAt(@ShardingParam final String key, final long millisecondsTimestamp) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.pexpireAt(key, millisecondsTimestamp);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pexpireAt(key, millisecondsTimestamp);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> getbit(@ShardingParam final String key, final long offset) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.getbit(key, offset);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.getbit(key, offset);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> getrange(@ShardingParam final String key, final long startOffset, final long endOffset) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.getrange(key, startOffset, endOffset);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.getrange(key, startOffset, endOffset);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> getSet(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.getSet(key, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.getSet(key, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hdel(@ShardingParam final String key, final String... field) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hdel(key, field);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hdel(key, field);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> hexists(@ShardingParam final String key, final String field) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hexists(key, field);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hexists(key, field);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> hget(@ShardingParam final String key, final String field) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hget(key, field);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hget(key, field);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Map<String, String>> hgetAll(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hgetAll(key);
        return queable.getResponse(client, BuilderFactory.STRING_MAP, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hgetAll(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hincrBy(@ShardingParam final String key, final String field, final long value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hincrBy(key, field, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hincrBy(key, field, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> hkeys(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hkeys(key);
        return queable.getResponse(client, BuilderFactory.STRING_SET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hkeys(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> hlen(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hlen(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hlen(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> hmget(@ShardingParam final String key, final String... fields) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hmget(key, fields);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hmget(key, fields);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> hmset(@ShardingParam final String key, final Map<String, String> hash) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hmset(key, hash);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hmset(key, hash);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hset(@ShardingParam final String key, final String field, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hset(key, field, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hset(key, field, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> hsetnx(@ShardingParam final String key, final String field, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hsetnx(key, field, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hsetnx(key, field, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> hvals(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hvals(key);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hvals(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> incr(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.incr(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.incr(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> incrBy(@ShardingParam final String key, final long integer) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.incrBy(key, integer);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.incrBy(key, integer);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> lindex(@ShardingParam final String key, final long index) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lindex(key, index);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lindex(key, index);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> linsert(@ShardingParam final String key, final BinaryClient.LIST_POSITION where, final String pivot, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.linsert(key, where, pivot, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.linsert(key, where, pivot, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> llen(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.llen(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.llen(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> lpop(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lpop(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lpop(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> lpush(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lpush(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lpush(key, string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> lpushx(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lpushx(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lpushx(key, string);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> lrange(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lrange(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lrange(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> lrem(@ShardingParam final String key, final long count, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lrem(key, count, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lrem(key, count, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> lset(@ShardingParam final String key, final long index, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.lset(key, index, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.lset(key, index, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> ltrim(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.ltrim(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.ltrim(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> persist(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.persist(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.persist(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> rpop(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.rpop(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.rpop(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> rpush(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.rpush(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.rpush(key, string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> rpushx(@ShardingParam final String key, final String... string) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.rpushx(key, string);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.rpushx(key, string);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> sadd(@ShardingParam final String key, final String... member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.sadd(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sadd(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> scard(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.scard(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.scard(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Boolean> sismember(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.sismember(key, member);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sismember(key, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Boolean> setbit(@ShardingParam final String key, final long offset, final boolean value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.setbit(key, offset, value);
        return queable.getResponse(client, BuilderFactory.BOOLEAN, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setbit(key, offset, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> setex(@ShardingParam final String key, final int seconds, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.setex(key, seconds, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setex(key, seconds, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> setnx(@ShardingParam final String key, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.setnx(key, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setnx(key, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> setrange(@ShardingParam final String key, final long offset, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.setrange(key, offset, value);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.setrange(key, offset, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> smembers(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.smembers(key);
        return queable.getResponse(client, BuilderFactory.STRING_SET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.smembers(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> sort(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.sort(key);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sort(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> sort(@ShardingParam final String key, final SortingParams sortingParameters) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.sort(key, sortingParameters);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.sort(key, sortingParameters);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> spop(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.spop(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.spop(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Set<String>> spop(@ShardingParam final String key, final long count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.spop(key, count);
        return queable.getResponse(client, BuilderFactory.STRING_SET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.spop(key, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> srandmember(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.srandmember(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.srandmember(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> srem(@ShardingParam final String key, final String... member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.srem(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.srem(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> strlen(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.strlen(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.strlen(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> substr(@ShardingParam final String key, final int start, final int end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.substr(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.substr(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> ttl(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.ttl(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.ttl(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<String> type(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.type(key);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.type(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final double score, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zadd(key, score, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zadd(key, score, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final double score, final String member, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zadd(key, score, member, params);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zadd(key, score, member, params);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final Map<String, Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zadd(key, scoreMembers);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zadd(key, scoreMembers);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final String key, final Map<String, Double> scoreMembers, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zadd(key, scoreMembers, params);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zadd(key, scoreMembers, params);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zcard(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zcard(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zcard(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final String key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zcount(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zcount(key, min, max);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final String key, final double score, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zincrby(key, score, member);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zincrby(key, score, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> zincrby(@ShardingParam final String key, final double score, final String member, final ZIncrByParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zincrby(key, score, member, params);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zincrby(key, score, member, params);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrange(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrange(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrange(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScore(key, min, max);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScore(key, min, max);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScore(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final double min, final double max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScoreWithScores(key, min, max);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final double min, final double max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScoreWithScores(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScore(key, max, min);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final String max, final String min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScore(key, max, min);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScore(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final double max, final double min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScoreWithScores(key, max, min);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final double max, final double min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScoreWithScores(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeWithScores(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeWithScores(key, start, end);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeWithScores(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zrank(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrank(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrank(key, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zrem(@ShardingParam final String key, final String... member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrem(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrem(key, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByRank(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zremrangeByRank(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByRank(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final String key, final double start, final double end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zremrangeByScore(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByScore(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrange(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrange(key, start, end);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrange(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeWithScores(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeWithScores(key, start, end);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeWithScores(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zrevrank(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrank(key, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrank(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Double> zscore(@ShardingParam final String key, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zscore(key, member);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zscore(key, member);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zlexcount(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zlexcount(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zlexcount(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByLex(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByLex(key, min, max);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByLex(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByLex(@ShardingParam final String key, final String min, final String max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByLex(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByLex(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByLex(@ShardingParam final String key, final String max, final String min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByLex(key, max, min);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByLex(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByLex(@ShardingParam final String key, final String max, final String min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByLex(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByLex(key, max, min, offset, count);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByLex(@ShardingParam final String key, final String start, final String end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zremrangeByLex(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByLex(key, start, end);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.bitcount(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitcount(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitcount(@ShardingParam final String key, final long start, final long end) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.bitcount(key, start, end);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitcount(key, start, end);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> pfadd(@ShardingParam final String key, final String... elements) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.pfadd(key, elements);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pfadd(key, elements);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> pfcount(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.pfcount(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pfcount(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<Long>> bitfield(@ShardingParam final String key, final String... arguments) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.bitfield(key, arguments);
        return queable.getResponse(client, BuilderFactory.LONG_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitfield(key, arguments);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final String key, final double longitude, final double latitude, final String member) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.geoadd(key, longitude, latitude, member);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geoadd(key, longitude, latitude, member);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> geoadd(@ShardingParam final String key, final Map<String, GeoCoordinate> memberCoordinateMap) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.geoadd(key, memberCoordinateMap);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geoadd(key, memberCoordinateMap);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final String key, final String member1, final String member2) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.geodist(key, member1, member2);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geodist(key, member1, member2);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Double> geodist(@ShardingParam final String key, final String member1, final String member2, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.geodist(key, member1, member2, unit);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geodist(key, member1, member2, unit);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> geohash(@ShardingParam final String key, final String... members) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.geohash(key, members);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geohash(key, members);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoCoordinate>> geopos(@ShardingParam final String key, final String... members) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.geopos(key, members);
        return queable.getResponse(client, BuilderFactory.GEO_COORDINATE_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.geopos(key, members);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final String key, final double longitude, final double latitude, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.georadius(key, longitude, latitude, radius, unit);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadius(key, longitude, latitude, radius, unit);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadius(@ShardingParam final String key, final double longitude, final double latitude, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.georadius(key, longitude, latitude, radius, unit, param);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadius(key, longitude, latitude, radius, unit, param);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final String key, final String member, final double radius, final GeoUnit unit) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.georadiusByMember(key, member, radius, unit);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadiusByMember(key, member, radius, unit);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<GeoRadiusResponse>> georadiusByMember(@ShardingParam final String key, final String member, final double radius, final GeoUnit unit, final GeoRadiusParam param) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.georadiusByMember(key, member, radius, unit, param);
        return queable.getResponse(client, BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.georadiusByMember(key, member, radius, unit, param);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final Map<byte[], Double> scoreMembers) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zaddBinary(key, scoreMembers);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zaddBinary(key, scoreMembers);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zadd(@ShardingParam final byte[] key, final Map<byte[], Double> scoreMembers, final ZAddParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zaddBinary(key, scoreMembers, params);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zaddBinary(key, scoreMembers, params);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> dump(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.dump(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.dump(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<byte[]> dump(@ShardingParam final byte[] key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.dump(key);
        return queable.getResponse(client, BuilderFactory.BYTE_ARRAY, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.dump(key);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> restore(@ShardingParam final byte[] key, final int ttl, final byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.restore(key, ttl, serializedValue);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.restore(key, ttl, serializedValue);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> restore(@ShardingParam final String key, final int ttl, final byte[] serializedValue) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.restore(key, ttl, serializedValue);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.restore(key, ttl, serializedValue);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> hincrByFloat(@ShardingParam final String key, final String field, final double increment) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.hincrByFloat(key, field, increment);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.hincrByFloat(key, field, increment);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Double> incrByFloat(@ShardingParam final String key, final double increment) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.incrByFloat(key, increment);
        return queable.getResponse(client, BuilderFactory.DOUBLE, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.incrByFloat(key, increment);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final String key, final String value, final String nxxx) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.set(key, value, nxxx);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value, nxxx);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final byte[] key, final byte[] value, final byte[] nxxx) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.set(key, value, nxxx);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value, nxxx);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> set(@ShardingParam final String key, final String value, final String nxxx, final String expx, final int time) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.set(key, value, nxxx, expx, time);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.set(key, value, nxxx, expx, time);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> psetex(@ShardingParam final String key, final long milliseconds, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.psetex(key, milliseconds, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.psetex(key, milliseconds, value);
            }
        });
    }

    @WriteOp
    @Override
    public Response<String> psetex(@ShardingParam final String key, final int milliseconds, final String value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.psetex(key, milliseconds, value);
        return queable.getResponse(client, BuilderFactory.STRING, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.psetex(key, milliseconds, value);
            }
        });
    }

    @ReadOp
    @Override
    public Response<List<String>> srandmember(@ShardingParam final String key, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.srandmember(key, count);
        return queable.getResponse(client, BuilderFactory.STRING_LIST, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.srandmember(key, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> pttl(@ShardingParam final String key) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.pttl(key);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.pttl(key);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrangeByScore(@ShardingParam final String key, final String min, final String max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScore(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScore(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final String min, final String max, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScoreWithScores(key, min, max, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrangeByScoreWithScores(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrangeByScoreWithScores(key, min, max);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrangeByScoreWithScores(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<String>> zrevrangeByScore(@ShardingParam final String key, final String max, final String min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScore(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.STRING_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScore(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final String max, final String min, final int offset, final int count) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScoreWithScores(key, max, min, offset, count);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Set<Tuple>> zrevrangeByScoreWithScores(@ShardingParam final String key, final String max, final String min) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zrevrangeByScoreWithScores(key, max, min);
        return queable.getResponse(client, BuilderFactory.TUPLE_ZSET, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zrevrangeByScoreWithScores(key, max, min);
            }
        });
    }

    @WriteOp
    @Override
    public Response<Long> zremrangeByScore(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zremrangeByScore(key, max, min);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zremrangeByScore(key, max, min);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final String key, final boolean value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.bitpos(key, value, new CamelliaBitPosParams());
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitpos(key, value, new CamelliaBitPosParams());
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final String key, final boolean value, final BitPosParams params) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.bitpos(key, value, params);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitpos(key, value, params);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> bitpos(@ShardingParam final byte[] key, final boolean value) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.bitpos(key, value, new CamelliaBitPosParams());
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.bitpos(key, value, new CamelliaBitPosParams());
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final byte[] key, final byte[] min, final byte[] max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, key);
        client.zcount(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zcount(key, min, max);
            }
        });
    }

    @ReadOp
    @Override
    public Response<Long> zcount(@ShardingParam final String key, final String min, final String max) {
        LogUtil.debugLog(resource, key);
        Client client = clientPool.getClient(resource, SafeEncoder.encode(key));
        client.zcount(key, min, max);
        return queable.getResponse(client, BuilderFactory.LONG, resource, key, new ResponseQueable.Fallback() {
            @Override
            public void invoke(Client client) {
                client.zcount(key, min, max);
            }
        });
    }
}

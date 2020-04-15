package com.netease.nim.camellia.redis.proxy.command.sync;


import com.netease.nim.camellia.redis.proxy.enums.CommandFinder;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;

public interface ISyncCommandProcessorPipeline {

    @CommandFinder(RedisCommand.PING)
    PipelineResponse ping();

    @CommandFinder(RedisCommand.ECHO)
    PipelineResponse echo(byte[] echo);

    /**
     * 数据库
     */
    @CommandFinder(RedisCommand.DEL)
    PipelineResponse del(byte[][] keys);

    @CommandFinder(RedisCommand.EXISTS)
    PipelineResponse exists(byte[][] keys);

    @CommandFinder(RedisCommand.TYPE)
    PipelineResponse type(byte[] key);

    @CommandFinder(RedisCommand.SORT)
    PipelineResponse sort(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.EXPIRE)
    PipelineResponse expire(byte[] key, byte[] seconds);

    @CommandFinder(RedisCommand.PEXPIRE)
    PipelineResponse pexpire(byte[] key, byte[] millis);

    @CommandFinder(RedisCommand.EXPIREAT)
    PipelineResponse expireat(byte[] key, byte[] timestamp);

    @CommandFinder(RedisCommand.PEXPIREAT)
    PipelineResponse pexpireat(byte[] key, byte[] timestamp);

    @CommandFinder(RedisCommand.TTL)
    PipelineResponse ttl(byte[] key);

    @CommandFinder(RedisCommand.PTTL)
    PipelineResponse pttl(byte[] key);

    @CommandFinder(RedisCommand.PERSIST)
    PipelineResponse persist(byte[] key);

    /**
     * 字符串
     */
    @CommandFinder(RedisCommand.GET)
    PipelineResponse get(byte[] key);

    @CommandFinder(RedisCommand.INCR)
    PipelineResponse incr(byte[] key);

    @CommandFinder(RedisCommand.INCRBY)
    PipelineResponse incrby(byte[] key, byte[] increment);

    @CommandFinder(RedisCommand.INCRBYFLOAT)
    PipelineResponse incrbyfloat(byte[] key, byte[] increment);

    @CommandFinder(RedisCommand.DECR)
    PipelineResponse decr(byte[] key);

    @CommandFinder(RedisCommand.DECRBY)
    PipelineResponse decrBy(byte[] key, byte[] decrement);

    @CommandFinder(RedisCommand.SET)
    PipelineResponse set(byte[] key, byte[] value, byte[][] args);

    @CommandFinder(RedisCommand.SETEX)
    PipelineResponse setex(byte[] key, byte[] seconds, byte[] value);

    @CommandFinder(RedisCommand.PSETEX)
    PipelineResponse psetex(byte[] key, byte[] millis, byte[] value);

    @CommandFinder(RedisCommand.SETNX)
    PipelineResponse setnx(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.GETSET)
    PipelineResponse getset(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.STRLEN)
    PipelineResponse strlen(byte[] key);

    @CommandFinder(RedisCommand.APPEND)
    PipelineResponse append(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.SETRANGE)
    PipelineResponse setrange(byte[] key, byte[] offset, byte[] value);

    @CommandFinder(RedisCommand.GETRANGE)
    PipelineResponse getrange(byte[] key, byte[] start, byte[] end);

    @CommandFinder(RedisCommand.SUBSTR)
    PipelineResponse substr(byte[] key, byte[] start, byte[] end);

    /**
     * 有序集合
     */
    @CommandFinder(RedisCommand.ZADD)
    PipelineResponse zadd(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.ZSCORE)
    PipelineResponse zscore(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZINCRBY)
    PipelineResponse zincrby(byte[] key, byte[] increment, byte[] member);

    @CommandFinder(RedisCommand.ZCARD)
    PipelineResponse zcard(byte[] key);

    @CommandFinder(RedisCommand.ZCOUNT)
    PipelineResponse zcount(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZRANGE)
    PipelineResponse zrange(byte[] key, byte[] start, byte[] stop, byte[] withscores);

    @CommandFinder(RedisCommand.ZREVRANGE)
    PipelineResponse zrevrange(byte[] key, byte[] start, byte[] stop, byte[] withscores);

    @CommandFinder(RedisCommand.ZRANGEBYSCORE)
    PipelineResponse zrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZREVRANGEBYSCORE)
    PipelineResponse zrevrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZRANGEBYLEX)
    PipelineResponse zrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZREVRANGEBYLEX)
    PipelineResponse zrevrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZREMRANGEBYRANK)
    PipelineResponse zremrangebyrank(byte[] key, byte[] start, byte[] stop);

    @CommandFinder(RedisCommand.ZREMRANGEBYSCORE)
    PipelineResponse zremrangebyscore(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZREMRANGEBYLEX)
    PipelineResponse zremrangebylex(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZRANK)
    PipelineResponse zrank(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZREVRANK)
    PipelineResponse zrevrank(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZREM)
    PipelineResponse zrem(byte[] key, byte[][] members);

    @CommandFinder(RedisCommand.ZLEXCOUNT)
    PipelineResponse zlexcount(byte[] key, byte[] min, byte[] max);

    /**
     * 集合
     */
    @CommandFinder(RedisCommand.SADD)
    PipelineResponse sadd(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.SISMEMBER)
    PipelineResponse sismember(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.SPOP)
    PipelineResponse spop(byte[] key);

    @CommandFinder(RedisCommand.SRANDMEMBER)
    PipelineResponse srandmember(byte[] key, byte[] count);

    @CommandFinder(RedisCommand.SREM)
    PipelineResponse srem(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.SCARD)
    PipelineResponse scard(byte[] key);

    @CommandFinder(RedisCommand.SMEMBERS)
    PipelineResponse smembers(byte[] key);

    /**
     * 列表
     */
    @CommandFinder(RedisCommand.LPUSH)
    PipelineResponse lpush(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.LPUSHX)
    PipelineResponse lpushx(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.RPUSH)
    PipelineResponse rpush(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.RPUSHX)
    PipelineResponse rpushx(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.LPOP)
    PipelineResponse lpop(byte[] key);

    @CommandFinder(RedisCommand.RPOP)
    PipelineResponse rpop(byte[] key);

    @CommandFinder(RedisCommand.LREM)
    PipelineResponse lrem(byte[] key, byte[] count, byte[] value);

    @CommandFinder(RedisCommand.LLEN)
    PipelineResponse llen(byte[] key);

    @CommandFinder(RedisCommand.LINDEX)
    PipelineResponse lindex(byte[] key, byte[] index);

    @CommandFinder(RedisCommand.LINSERT)
    PipelineResponse linsert(byte[] key, byte[] beforeAfter, byte[] pivot, byte[] value);

    @CommandFinder(RedisCommand.LSET)
    PipelineResponse lset(byte[] key, byte[] index, byte[] value);

    @CommandFinder(RedisCommand.LRANGE)
    PipelineResponse lrange(byte[] key, byte[] start, byte[] stop);

    @CommandFinder(RedisCommand.LTRIM)
    PipelineResponse ltrim(byte[] key, byte[] start, byte[] stop);

    /**
     * 哈希表
     */
    @CommandFinder(RedisCommand.HSET)
    PipelineResponse hset(byte[] key, byte[] field, byte[] value);

    @CommandFinder(RedisCommand.HSETNX)
    PipelineResponse hsetnx(byte[] key, byte[] field, byte[] value);

    @CommandFinder(RedisCommand.HGET)
    PipelineResponse hget(byte[] key, byte[] field);

    @CommandFinder(RedisCommand.HEXISTS)
    PipelineResponse hexists(byte[] key, byte[] field);

    @CommandFinder(RedisCommand.HDEL)
    PipelineResponse hdel(byte[] key, byte[] field);

    @CommandFinder(RedisCommand.HLEN)
    PipelineResponse hlen(byte[] key);

    @CommandFinder(RedisCommand.HINCRBY)
    PipelineResponse hincrby(byte[] key, byte[] field, byte[] increment);

    @CommandFinder(RedisCommand.HINCRBYFLOAT)
    PipelineResponse hincrbyfloat(byte[] key, byte[] field, byte[] increment);

    @CommandFinder(RedisCommand.HMSET)
    PipelineResponse hmset(byte[] key, byte[][] kvs);

    @CommandFinder(RedisCommand.HMGET)
    PipelineResponse hmget(byte[] key, byte[][] fields);

    @CommandFinder(RedisCommand.HKEYS)
    PipelineResponse hkeys(byte[] key);

    @CommandFinder(RedisCommand.HVALS)
    PipelineResponse hvals(byte[] key);

    @CommandFinder(RedisCommand.HGETALL)
    PipelineResponse hgetall(byte[] key);

    /**
     * 地理位置
     */
    @CommandFinder(RedisCommand.GEOADD)
    PipelineResponse geoadd(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.GEOPOS)
    PipelineResponse geopos(byte[] key, byte[][] members);

    @CommandFinder(RedisCommand.GEODIST)
    PipelineResponse geodist(byte[] key, byte[] member1, byte[] member2, byte[] unit);

    @CommandFinder(RedisCommand.GEORADIUS)
    PipelineResponse georadius(byte[] key, byte[] longtitude, byte[] latitude, byte[] radius, byte[] unit, byte[][] args);

    @CommandFinder(RedisCommand.GEORADIUSBYMEMBER)
    PipelineResponse georadiusbymember(byte[] key, byte[] member, byte[] radius, byte[] unit, byte[][] args);

    @CommandFinder(RedisCommand.GEOHASH)
    PipelineResponse geohash(byte[] key, byte[][] members);

    /**
     * 位图
     */
    @CommandFinder(RedisCommand.SETBIT)
    PipelineResponse setbit(byte[] key, byte[] offset, byte[] value);

    @CommandFinder(RedisCommand.GETBIT)
    PipelineResponse getbit(byte[] key, byte[] offset);

    @CommandFinder(RedisCommand.BITCOUNT)
    PipelineResponse bitcount(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.BITFIELD)
    PipelineResponse bitfield(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.BITPOS)
    PipelineResponse bitpos(byte[] key, byte[] bit, byte[][] args);
}

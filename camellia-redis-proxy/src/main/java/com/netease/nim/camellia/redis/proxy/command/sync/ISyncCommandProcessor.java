package com.netease.nim.camellia.redis.proxy.command.sync;


import com.netease.nim.camellia.redis.proxy.enums.CommandFinder;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;

public interface ISyncCommandProcessor {

    @CommandFinder(RedisCommand.PING)
    StatusReply ping();

    @CommandFinder(RedisCommand.ECHO)
    BulkReply echo(byte[] echo);

    /**
     * 数据库
     */
    @CommandFinder(RedisCommand.DEL)
    Reply del(byte[][] keys);

    @CommandFinder(RedisCommand.EXISTS)
    IntegerReply exists(byte[][] keys);

    @CommandFinder(RedisCommand.TYPE)
    StatusReply type(byte[] key);

    @CommandFinder(RedisCommand.SORT)
    MultiBulkReply sort(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.EXPIRE)
    IntegerReply expire(byte[] key, byte[] seconds);

    @CommandFinder(RedisCommand.PEXPIRE)
    IntegerReply pexpire(byte[] key, byte[] millis);

    @CommandFinder(RedisCommand.EXPIREAT)
    IntegerReply expireat(byte[] key, byte[] timestamp);

    @CommandFinder(RedisCommand.PEXPIREAT)
    IntegerReply pexpireat(byte[] key, byte[] timestamp);

    @CommandFinder(RedisCommand.TTL)
    IntegerReply ttl(byte[] key);

    @CommandFinder(RedisCommand.PTTL)
    IntegerReply pttl(byte[] key);

    @CommandFinder(RedisCommand.PERSIST)
    IntegerReply persist(byte[] key);

    /**
     * 字符串
     */
    @CommandFinder(RedisCommand.GET)
    BulkReply get(byte[] key);

    @CommandFinder(RedisCommand.INCR)
    IntegerReply incr(byte[] key);

    @CommandFinder(RedisCommand.INCRBY)
    IntegerReply incrby(byte[] key, byte[] increment);

    @CommandFinder(RedisCommand.INCRBYFLOAT)
    BulkReply incrbyfloat(byte[] key, byte[] increment);

    @CommandFinder(RedisCommand.DECR)
    IntegerReply decr(byte[] key);

    @CommandFinder(RedisCommand.DECRBY)
    IntegerReply decrBy(byte[] key, byte[] decrement);

    @CommandFinder(RedisCommand.SET)
    Reply set(byte[] key, byte[] value, byte[][] args);

    @CommandFinder(RedisCommand.SETEX)
    StatusReply setex(byte[] key, byte[] seconds, byte[] value);

    @CommandFinder(RedisCommand.PSETEX)
    StatusReply psetex(byte[] key, byte[] millis, byte[] value);

    @CommandFinder(RedisCommand.SETNX)
    IntegerReply setnx(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.GETSET)
    BulkReply getset(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.STRLEN)
    IntegerReply strlen(byte[] key);

    @CommandFinder(RedisCommand.APPEND)
    IntegerReply append(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.SETRANGE)
    IntegerReply setrange(byte[] key, byte[] offset, byte[] value);

    @CommandFinder(RedisCommand.GETRANGE)
    BulkReply getrange(byte[] key, byte[] start, byte[] end);

    @CommandFinder(RedisCommand.MSET)
    StatusReply mset(byte[][] kvs);

    @CommandFinder(RedisCommand.MGET)
    MultiBulkReply mget(byte[][] keys);

    @CommandFinder(RedisCommand.SUBSTR)
    BulkReply substr(byte[] key, byte[] start, byte[] end);

    /**
     * 有序集合
     */
    @CommandFinder(RedisCommand.ZADD)
    IntegerReply zadd(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.ZSCORE)
    BulkReply zscore(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZINCRBY)
    BulkReply zincrby(byte[] key, byte[] increment, byte[] member);

    @CommandFinder(RedisCommand.ZCARD)
    IntegerReply zcard(byte[] key);

    @CommandFinder(RedisCommand.ZCOUNT)
    IntegerReply zcount(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZRANGE)
    MultiBulkReply zrange(byte[] key, byte[] start, byte[] stop, byte[] withscores);

    @CommandFinder(RedisCommand.ZREVRANGE)
    MultiBulkReply zrevrange(byte[] key, byte[] start, byte[] stop, byte[] withscores);

    @CommandFinder(RedisCommand.ZRANGEBYSCORE)
    MultiBulkReply zrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZREVRANGEBYSCORE)
    MultiBulkReply zrevrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZRANGEBYLEX)
    MultiBulkReply zrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZREVRANGEBYLEX)
    MultiBulkReply zrevrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args);

    @CommandFinder(RedisCommand.ZREMRANGEBYRANK)
    IntegerReply zremrangebyrank(byte[] key, byte[] start, byte[] stop);

    @CommandFinder(RedisCommand.ZREMRANGEBYSCORE)
    IntegerReply zremrangebyscore(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZREMRANGEBYLEX)
    IntegerReply zremrangebylex(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZRANK)
    Reply zrank(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZREVRANK)
    Reply zrevrank(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZREM)
    IntegerReply zrem(byte[] key, byte[][] members);

    @CommandFinder(RedisCommand.ZLEXCOUNT)
    IntegerReply zlexcount(byte[] key, byte[] min, byte[] max);

    @CommandFinder(RedisCommand.ZSCAN)
    MultiBulkReply zscan(byte[] key, byte[] cursor, byte[][] args);

    /**
     * 集合
     */
    @CommandFinder(RedisCommand.SADD)
    IntegerReply sadd(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.SISMEMBER)
    IntegerReply sismember(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.SPOP)
    BulkReply spop(byte[] key);

    @CommandFinder(RedisCommand.SRANDMEMBER)
    Reply srandmember(byte[] key, byte[] count);

    @CommandFinder(RedisCommand.SREM)
    IntegerReply srem(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.SCARD)
    IntegerReply scard(byte[] key);

    @CommandFinder(RedisCommand.SMEMBERS)
    MultiBulkReply smembers(byte[] key);

    @CommandFinder(RedisCommand.SSCAN)
    MultiBulkReply sscan(byte[] key, byte[] cursor, byte[][] args);

    /**
     * 列表
     */
    @CommandFinder(RedisCommand.LPUSH)
    IntegerReply lpush(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.LPUSHX)
    IntegerReply lpushx(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.RPUSH)
    IntegerReply rpush(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.RPUSHX)
    IntegerReply rpushx(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.LPOP)
    BulkReply lpop(byte[] key);

    @CommandFinder(RedisCommand.RPOP)
    BulkReply rpop(byte[] key);

    @CommandFinder(RedisCommand.LREM)
    IntegerReply lrem(byte[] key, byte[] count, byte[] value);

    @CommandFinder(RedisCommand.LLEN)
    IntegerReply llen(byte[] key);

    @CommandFinder(RedisCommand.LINDEX)
    BulkReply lindex(byte[] key, byte[] index);

    @CommandFinder(RedisCommand.LINSERT)
    IntegerReply linsert(byte[] key, byte[] beforeAfter, byte[] pivot, byte[] value);

    @CommandFinder(RedisCommand.LSET)
    StatusReply lset(byte[] key, byte[] index, byte[] value);

    @CommandFinder(RedisCommand.LRANGE)
    MultiBulkReply lrange(byte[] key, byte[] start, byte[] stop);

    @CommandFinder(RedisCommand.LTRIM)
    StatusReply ltrim(byte[] key, byte[] start, byte[] stop);

    /**
     * 哈希表
     */
    @CommandFinder(RedisCommand.HSET)
    IntegerReply hset(byte[] key, byte[] field, byte[] value);

    @CommandFinder(RedisCommand.HSETNX)
    IntegerReply hsetnx(byte[] key, byte[] field, byte[] value);

    @CommandFinder(RedisCommand.HGET)
    Reply hget(byte[] key, byte[] field);

    @CommandFinder(RedisCommand.HEXISTS)
    IntegerReply hexists(byte[] key, byte[] field);

    @CommandFinder(RedisCommand.HDEL)
    IntegerReply hdel(byte[] key, byte[] field);

    @CommandFinder(RedisCommand.HLEN)
    IntegerReply hlen(byte[] key);

    @CommandFinder(RedisCommand.HINCRBY)
    IntegerReply hincrby(byte[] key, byte[] field, byte[] increment);

    @CommandFinder(RedisCommand.HINCRBYFLOAT)
    BulkReply hincrbyfloat(byte[] key, byte[] field, byte[] increment);

    @CommandFinder(RedisCommand.HMSET)
    StatusReply hmset(byte[] key, byte[][] kvs);

    @CommandFinder(RedisCommand.HMGET)
    MultiBulkReply hmget(byte[] key, byte[][] fields);

    @CommandFinder(RedisCommand.HKEYS)
    MultiBulkReply hkeys(byte[] key);

    @CommandFinder(RedisCommand.HVALS)
    MultiBulkReply hvals(byte[] key);

    @CommandFinder(RedisCommand.HGETALL)
    MultiBulkReply hgetall(byte[] key);

    @CommandFinder(RedisCommand.HSCAN)
    MultiBulkReply hscan(byte[] key, byte[] cursor, byte[][] args);

    /**
     * 地理位置
     */
    @CommandFinder(RedisCommand.GEOADD)
    IntegerReply geoadd(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.GEOPOS)
    MultiBulkReply geopos(byte[] key, byte[][] members);

    @CommandFinder(RedisCommand.GEODIST)
    BulkReply geodist(byte[] key, byte[] member1, byte[] member2, byte[] unit);

    @CommandFinder(RedisCommand.GEORADIUS)
    MultiBulkReply georadius(byte[] key, byte[] longtitude, byte[] latitude, byte[] radius, byte[] unit, byte[][] args);

    @CommandFinder(RedisCommand.GEORADIUSBYMEMBER)
    MultiBulkReply georadiusbymember(byte[] key, byte[] member, byte[] radius, byte[] unit, byte[][] args);

    @CommandFinder(RedisCommand.GEOHASH)
    MultiBulkReply geohash(byte[] key, byte[][] members);

    /**
     * 位图
     */
    @CommandFinder(RedisCommand.SETBIT)
    IntegerReply setbit(byte[] key, byte[] offset, byte[] value);

    @CommandFinder(RedisCommand.GETBIT)
    IntegerReply getbit(byte[] key, byte[] offset);

    @CommandFinder(RedisCommand.BITCOUNT)
    IntegerReply bitcount(byte[] key, byte[][] args);

    @CommandFinder(RedisCommand.BITPOS)
    IntegerReply bitpos(byte[] key, byte[] bit, byte[][] args);

    @CommandFinder(RedisCommand.BITFIELD)
    Reply bitfield(byte[] key, byte[][] args);
}

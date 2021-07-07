package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.redis.proxy.enums.CommandFinder;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;

/**
 *
 * Created by caojiajun on 2020/2/25.
 */
public interface IRedisHBaseCommandProcessor {

    @CommandFinder(RedisCommand.PING)
    StatusReply ping();

    @CommandFinder(RedisCommand.ECHO)
    BulkReply echo(byte[] echo);


    /**
     * 数据库
     */
    @CommandFinder(RedisCommand.DEL)
    IntegerReply del(byte[][] keys);

    @CommandFinder(RedisCommand.EXISTS)
    IntegerReply exists(byte[][] keys);

    @CommandFinder(RedisCommand.TYPE)
    StatusReply type(byte[] key);

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

    /**
     * 字符串
     */
    @CommandFinder(RedisCommand.GET)
    BulkReply get(byte[] key);

    @CommandFinder(RedisCommand.SET)
    Reply set(byte[] key, byte[] value, byte[][] args);

    @CommandFinder(RedisCommand.SETEX)
    StatusReply setex(byte[] key, byte[] seconds, byte[] value);

    @CommandFinder(RedisCommand.MSET)
    StatusReply mset(byte[][] kvs);

    @CommandFinder(RedisCommand.PSETEX)
    StatusReply psetex(byte[] key, byte[] millis, byte[] value);

    @CommandFinder(RedisCommand.SETNX)
    IntegerReply setnx(byte[] key, byte[] value);

    @CommandFinder(RedisCommand.MGET)
    MultiBulkReply mget(byte[][] keys);

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
    IntegerReply zrank(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZREVRANK)
    IntegerReply zrevrank(byte[] key, byte[] member);

    @CommandFinder(RedisCommand.ZREM)
    IntegerReply zrem(byte[] key, byte[][] members);

    @CommandFinder(RedisCommand.ZLEXCOUNT)
    IntegerReply zlexcount(byte[] key, byte[] min, byte[] max);

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
}

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
}

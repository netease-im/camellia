package com.netease.nim.camellia.redis.proxy.enums;

import redis.clients.util.SafeEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/18.
 */
public enum RedisCommand {

    /**
     * 实现了这些命令
     */
    PING(true, Type.READ),
    AUTH(true, Type.READ),
    QUIT(true, Type.READ),
    SET(true, Type.WRITE),
    GET(true, Type.READ),
    EXISTS(true, Type.READ),
    DEL(true, Type.WRITE),
    TYPE(true, Type.READ),
    EXPIRE(true, Type.WRITE),
    EXPIREAT(true, Type.WRITE),
    TTL(true, Type.READ),
    GETSET(true, Type.WRITE),
    MGET(true, Type.READ),
    SETNX(true, Type.WRITE),
    SETEX(true, Type.WRITE),
    MSET(true, Type.WRITE),
    SUBSTR(true, Type.READ),
    DECRBY(true, Type.WRITE),
    DECR(true, Type.WRITE),
    INCRBY(true, Type.WRITE),
    INCR(true, Type.WRITE),
    APPEND(true, Type.WRITE),
    HSET(true, Type.WRITE),
    HGET(true, Type.READ),
    HSETNX(true, Type.WRITE),
    HMSET(true, Type.WRITE),
    HMGET(true, Type.READ),
    HINCRBY(true, Type.WRITE),
    HEXISTS(true, Type.READ),
    HDEL(true, Type.WRITE),
    HLEN(true, Type.READ),
    HKEYS(true, Type.READ),
    HVALS(true, Type.READ),
    HGETALL(true, Type.READ),
    RPUSH(true, Type.WRITE),
    LPUSH(true, Type.WRITE),
    LLEN(true, Type.READ),
    LRANGE(true, Type.READ),
    LTRIM(true, Type.READ),
    LINDEX(true, Type.READ),
    LSET(true, Type.WRITE),
    LREM(true, Type.WRITE),
    LPOP(true, Type.WRITE),
    RPOP(true, Type.WRITE),
    SADD(true, Type.WRITE),
    SMEMBERS(true, Type.READ),
    SREM(true, Type.WRITE),
    SPOP(true, Type.WRITE),
    SCARD(true, Type.READ),
    SISMEMBER(true, Type.READ),
    SRANDMEMBER(true, Type.READ),
    ZADD(true, Type.WRITE),
    ZINCRBY(true, Type.WRITE),
    ZRANK(true, Type.READ),
    ZCARD(true, Type.READ),
    ZSCORE(true, Type.READ),
    SORT(true, Type.READ),
    ZCOUNT(true, Type.READ),
    ZRANGE(true, Type.READ),
    ZRANGEBYSCORE(true, Type.READ),
    ZRANGEBYLEX(true, Type.READ),
    ZREVRANK(true, Type.READ),
    ZREVRANGE(true, Type.READ),
    ZREVRANGEBYSCORE(true, Type.READ),
    ZREVRANGEBYLEX(true, Type.READ),
    ZREM(true, Type.WRITE),
    ZREMRANGEBYRANK(true, Type.WRITE),
    ZREMRANGEBYSCORE(true, Type.WRITE),
    ZREMRANGEBYLEX(true, Type.WRITE),
    ZLEXCOUNT(true, Type.READ),
    STRLEN(true, Type.READ),
    LPUSHX(true, Type.WRITE),
    PERSIST(true, Type.WRITE),
    RPUSHX(true, Type.WRITE),
    LINSERT(true, Type.WRITE),
    SETBIT(true, Type.WRITE),
    GETBIT(true, Type.READ),
    BITPOS(true, Type.WRITE),
    SETRANGE(true, Type.WRITE),
    GETRANGE(true, Type.READ),
    BITCOUNT(true, Type.READ),
    PEXPIRE(true, Type.WRITE),
    PEXPIREAT(true, Type.WRITE),
    PTTL(true, Type.READ),
    INCRBYFLOAT(true, Type.WRITE),
    PSETEX(true, Type.WRITE),
    CLIENT(true, Type.WRITE),
    HINCRBYFLOAT(true, Type.WRITE),
    HSCAN(true, Type.READ),
    SSCAN(true, Type.READ),
    ZSCAN(true, Type.READ),
    GEOADD(true, Type.WRITE),
    GEODIST(true, Type.READ),
    GEOHASH(true, Type.READ),
    GEOPOS(true, Type.READ),
    GEORADIUS(true, Type.READ),
    GEORADIUSBYMEMBER(true, Type.READ),
    BITFIELD(true, Type.READ),
    ECHO(true, Type.READ),
    EVAL(true, Type.WRITE),
    EVALSHA(true, Type.WRITE),

    /**
     * 这些命令没有实现
     */
    PFADD(false, null),
    PFCOUNT(false, null),
    SMOVE(false, null),
    FLUSHDB(false, null),
    KEYS(false, null),
    RANDOMKEY(false, null),
    RENAME(false, null),
    RENAMENX(false, null),
    RENAMEX(false, null),
    DBSIZE(false, null),
    SELECT(false, null),
    MOVE(false, null),
    FLUSHALL(false, null),
    MSETNX(false, null),
    RPOPLPUSH(false, null),
    SINTER(false, null),
    SINTERSTORE(false, null),
    SUNION(false, null),
    SUNIONSTORE(false, null),
    SDIFF(false, null),
    SDIFFSTORE(false, null),
    MULTI(false, null),
    DISCARD(false, null),
    EXEC(false, null),
    WATCH(false, null),
    UNWATCH(false, null),
    BLPOP(false, null),
    BRPOP(false, null),
    SUBSCRIBE(false, null),
    PUBLISH(false, null),
    UNSUBSCRIBE(false, null),
    PSUBSCRIBE(false, null),
    PUNSUBSCRIBE(false, null),
    PUBSUB(false, null),
    ZUNIONSTORE(false, null),
    ZINTERSTORE(false, null),
    SAVE(false, null),
    BGSAVE(false, null),
    BGREWRITEAOF(false, null),
    LASTSAVE(false, null),
    SHUTDOWN(false, null),
    INFO(false, null),
    MONITOR(false, null),
    SLAVEOF(false, null),
    CONFIG(false, null),
    SYNC(false, null),
    DEBUG(false, null),
    BRPOPLPUSH(false, null),
    SCRIPT(false, null),
    SLOWLOG(false, null),
    OBJECT(false, null),
    BITOP(false, null),
    SENTINEL(false, null),
    DUMP(false, null),
    RESTORE(false, null),
    TIME(false, null),
    MIGRATE(false, null),
    SCAN(false, null),
    WAIT(false, null),
    CLUSTER(false, null),
    ASKING(false, null),
    PFMERGE(false, null),
    READONLY(false, null),
    ;

    private final boolean support;
    private final byte[] raw;
    private final Type type;

    RedisCommand(boolean support, Type type) {
        this.raw = SafeEncoder.encode(name());
        this.support = support;
        this.type = type;
    }

    public byte[] raw() {
        return raw;
    }

    public Type getType() {
        return type;
    }

    public boolean isSupport() {
        return support;
    }

    public static enum Type {
        READ,
        WRITE,
        ;
    }

    private static final Map<String, RedisCommand> supportCommandMap = new HashMap<>();
    private static final Map<String, RedisCommand> commandMap = new HashMap<>();

    static {
        for (RedisCommand command : RedisCommand.values()) {
            if (command.isSupport() && command.getType() != null) {
                supportCommandMap.put(command.name().toLowerCase(), command);
            }
            commandMap.put(command.name().toLowerCase(), command);
        }

    }

    public static RedisCommand getSupportRedisCommandByName(String command) {
        return supportCommandMap.get(command);
    }

    public static RedisCommand getRedisCommandByName(String command) {
        return commandMap.get(command);
    }
}

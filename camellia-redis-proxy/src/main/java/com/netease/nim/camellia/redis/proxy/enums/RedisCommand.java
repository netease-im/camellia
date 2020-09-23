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
     * FULL_SUPPORT
     */
    PING(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    AUTH(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    QUIT(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SET(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    GET(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    EXISTS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    DEL(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    TYPE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    EXPIRE(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    EXPIREAT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    TTL(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    GETSET(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    MGET(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SETNX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SETEX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    MSET(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SUBSTR(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    DECRBY(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    DECR(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    INCRBY(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    INCR(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    APPEND(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HSET(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HGET(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    HSETNX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HMSET(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HMGET(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    HINCRBY(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HEXISTS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    HDEL(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HLEN(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    HKEYS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    HVALS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    HGETALL(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    RPUSH(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    LPUSH(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    LLEN(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    LRANGE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    LTRIM(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    LINDEX(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    LSET(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    LREM(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    LPOP(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    RPOP(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SADD(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SMEMBERS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SREM(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SPOP(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SCARD(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SISMEMBER(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SRANDMEMBER(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZADD(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    ZINCRBY(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    ZRANK(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZCARD(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZSCORE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SORT(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZCOUNT(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZRANGE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZRANGEBYSCORE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZRANGEBYLEX(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZREVRANK(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZREVRANGE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZREVRANGEBYSCORE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZREVRANGEBYLEX(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZREM(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    ZREMRANGEBYRANK(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    ZREMRANGEBYSCORE(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    ZREMRANGEBYLEX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    ZLEXCOUNT(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    STRLEN(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    LPUSHX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    PERSIST(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    RPUSHX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    LINSERT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SETBIT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    GETBIT(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    BITPOS(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    SETRANGE(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    GETRANGE(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    BITCOUNT(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    PEXPIRE(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    PEXPIREAT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    PTTL(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    INCRBYFLOAT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    PSETEX(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    CLIENT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HINCRBYFLOAT(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    HSCAN(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    SSCAN(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ZSCAN(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    GEOADD(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),
    GEODIST(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    GEOHASH(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    GEOPOS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    GEORADIUS(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    GEORADIUSBYMEMBER(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    BITFIELD(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    ECHO(CommandSupportType.FULL_SUPPORT, Type.READ, false),
    PFADD(CommandSupportType.FULL_SUPPORT, Type.WRITE, false),

    /**
     * Restrictive Support(support only when all the keys in these command route to same redis-serve or same redis-cluster slot)
     */
    EVAL(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    EVALSHA(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    PFCOUNT(CommandSupportType.RESTRICTIVE_SUPPORT, Type.READ, false),
    PFMERGE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    RENAME(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    RENAMENX(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    SINTER(CommandSupportType.RESTRICTIVE_SUPPORT, Type.READ, false),
    SINTERSTORE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    SUNION(CommandSupportType.RESTRICTIVE_SUPPORT, Type.READ, false),
    SUNIONSTORE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    SDIFF(CommandSupportType.RESTRICTIVE_SUPPORT, Type.READ, false),
    SDIFFSTORE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    SMOVE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    ZUNIONSTORE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    ZINTERSTORE(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    BITOP(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    MSETNX(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    BLPOP(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, true),
    BRPOP(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, true),
    RPOPLPUSH(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, false),
    BRPOPLPUSH(CommandSupportType.RESTRICTIVE_SUPPORT, Type.WRITE, true),

    /**
     * Partially Support(support only when route to singleton redis or redis-sentinel)
     */
    KEYS(CommandSupportType.PARTIALLY_SUPPORT, Type.READ, false),
    SCAN(CommandSupportType.PARTIALLY_SUPPORT, Type.READ, false),

    /**
     * NOT_SUPPORT
     */
    FLUSHDB(CommandSupportType.NOT_SUPPORT, Type.WRITE, false),
    RANDOMKEY(CommandSupportType.NOT_SUPPORT, Type.READ, false),
    DBSIZE(CommandSupportType.NOT_SUPPORT, Type.READ, false),
    SELECT(CommandSupportType.NOT_SUPPORT, null, false),
    MOVE(CommandSupportType.NOT_SUPPORT, null, false),
    FLUSHALL(CommandSupportType.NOT_SUPPORT, Type.WRITE, false),
    MULTI(CommandSupportType.NOT_SUPPORT, null, false),
    DISCARD(CommandSupportType.NOT_SUPPORT, null, false),
    EXEC(CommandSupportType.NOT_SUPPORT, null, false),
    WATCH(CommandSupportType.NOT_SUPPORT, null, false),
    UNWATCH(CommandSupportType.NOT_SUPPORT, null, false),
    SUBSCRIBE(CommandSupportType.NOT_SUPPORT, null, false),
    PUBLISH(CommandSupportType.NOT_SUPPORT, null, false),
    UNSUBSCRIBE(CommandSupportType.NOT_SUPPORT, null, false),
    PSUBSCRIBE(CommandSupportType.NOT_SUPPORT, null, false),
    PUNSUBSCRIBE(CommandSupportType.NOT_SUPPORT, null, false),
    PUBSUB(CommandSupportType.NOT_SUPPORT, null, false),
    SAVE(CommandSupportType.NOT_SUPPORT, null, false),
    BGSAVE(CommandSupportType.NOT_SUPPORT, null, false),
    BGREWRITEAOF(CommandSupportType.NOT_SUPPORT, null, false),
    LASTSAVE(CommandSupportType.NOT_SUPPORT, null, false),
    SHUTDOWN(CommandSupportType.NOT_SUPPORT, null, false),
    INFO(CommandSupportType.NOT_SUPPORT, null, false),
    MONITOR(CommandSupportType.NOT_SUPPORT, null, false),
    SLAVEOF(CommandSupportType.NOT_SUPPORT, null, false),
    CONFIG(CommandSupportType.NOT_SUPPORT, null, false),
    SYNC(CommandSupportType.NOT_SUPPORT, null, false),
    DEBUG(CommandSupportType.NOT_SUPPORT, null, false),
    SCRIPT(CommandSupportType.NOT_SUPPORT, null, false),
    SLOWLOG(CommandSupportType.NOT_SUPPORT, null, false),
    OBJECT(CommandSupportType.NOT_SUPPORT, null, false),
    SENTINEL(CommandSupportType.NOT_SUPPORT, null, false),
    DUMP(CommandSupportType.NOT_SUPPORT, null, false),
    RESTORE(CommandSupportType.NOT_SUPPORT, null, false),
    TIME(CommandSupportType.NOT_SUPPORT, null, false),
    MIGRATE(CommandSupportType.NOT_SUPPORT, null, false),
    WAIT(CommandSupportType.NOT_SUPPORT, null, false),
    CLUSTER(CommandSupportType.NOT_SUPPORT, null, false),
    ASKING(CommandSupportType.NOT_SUPPORT, null, false),
    READONLY(CommandSupportType.NOT_SUPPORT, null, false),
    ;

    private final CommandSupportType supportType;
    private final byte[] raw;
    private final Type type;
    private final boolean blocking;

    RedisCommand(CommandSupportType supportType, Type type, boolean blocking) {
        this.raw = SafeEncoder.encode(name());
        this.supportType = supportType;
        this.type = type;
        this.blocking = blocking;
    }

    public byte[] raw() {
        return raw;
    }

    public Type getType() {
        return type;
    }

    public CommandSupportType getSupportType() {
        return supportType;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public static enum Type {
        READ,
        WRITE,
        ;
    }

    public static enum CommandSupportType {

        //full support commands
        FULL_SUPPORT(1),

        //only support while keys in this command location at the same server or same slot.
        RESTRICTIVE_SUPPORT(2),

        //only support while have singleton upstream redis server
        PARTIALLY_SUPPORT(3),

        //not support
        NOT_SUPPORT(Integer.MAX_VALUE),
        ;

        private final int value;

        CommandSupportType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private static final Map<String, RedisCommand> supportCommandMap = new HashMap<>();
    private static final Map<String, RedisCommand> commandMap = new HashMap<>();

    static {
        for (RedisCommand command : RedisCommand.values()) {
            if (command.getSupportType() != CommandSupportType.NOT_SUPPORT && command.getType() != null) {
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

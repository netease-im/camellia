package com.netease.nim.camellia.redis.proxy.enums;

import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 *
 * Created by caojiajun on 2019/11/18.
 */
public enum RedisKeyword {
    AGGREGATE, ALPHA, ASC, BY, DESC,
    GET, LIMIT, MESSAGE, NO, NOSORT,
    PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE,
    OK, ONE, QUEUED, SET, STORE, SUBSCRIBE,
    UNSUBSCRIBE, WEIGHTS, WITHSCORES, RESETSTAT,
    RESET, FLUSH, EXISTS, LOAD,
    KILL, LEN, REFCOUNT, ENCODING, IDLETIME,
    AND, OR, XOR, NOT, GETNAME, SETNAME,
    LIST, MATCH, COUNT, PING, PONG,
    NX, XX, EX, PX, CH,
    BEFORE, AFTER,
    WITHCOORD, WITHDIST, WITHHASH,
    STREAMS,BLOCK,
    SLOTS, NODES, INFO, PROXY_HEARTBEAT,
    DELETE,DUMP,RESTORE,STATS,
    ;

    private final byte[] raw;

    RedisKeyword() {
        raw = Utils.stringToBytes(name());
    }

    public byte[] getRaw() {
        return raw;
    }
}

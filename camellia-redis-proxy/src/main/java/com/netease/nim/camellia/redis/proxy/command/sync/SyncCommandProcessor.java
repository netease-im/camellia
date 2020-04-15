package com.netease.nim.camellia.redis.proxy.command.sync;


import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import redis.clients.jedis.*;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.util.SafeEncoder;

import java.util.*;

/**
 *
 * Created by caojiajun on 2019/11/5.
 */
public class SyncCommandProcessor implements ISyncCommandProcessor {

    private CamelliaRedisTemplate template;

    public SyncCommandProcessor(CamelliaRedisTemplate template) {
        this.template = template;
    }

    public SyncCommandProcessorPipeline pipelined() {
        return new SyncCommandProcessorPipeline(template);
    }

    @Override
    public BulkReply get(byte[] key) {
        if (key == null) {
            throw Utils.illegalArgumentException();
        }
        byte[] value = template.get(key);
        if (value == null) return BulkReply.NIL_REPLY;
        return new BulkReply(value);
    }

    @Override
    public IntegerReply incr(byte[] key) {
        if (key == null) {
            throw Utils.illegalArgumentException();
        }
        Long value = template.incr(key);
        return new IntegerReply(value);
    }

    @Override
    public IntegerReply incrby(byte[] key, byte[] increment) {
        if (key == null || increment == null) {
            throw Utils.illegalArgumentException();
        }
        Long incrBy = template.incrBy(key, Utils.bytesToNum(increment));
        return new IntegerReply(incrBy);
    }

    @Override
    public BulkReply incrbyfloat(byte[] key, byte[] increment) {
        if (key == null || increment == null) {
            throw Utils.illegalArgumentException();
        }
        Double incrByFloat = template.incrByFloat(key, Utils.bytesToDouble(increment));
        return new BulkReply(Utils.doubleToBytes(incrByFloat));
    }

    @Override
    public IntegerReply decr(byte[] key) {
        Long decr = template.decr(key);
        return new IntegerReply(decr);
    }

    @Override
    public IntegerReply decrBy(byte[] key, byte[] decrement) {
        Long decrBy = template.decrBy(key, Utils.bytesToNum(decrement));
        return new IntegerReply(decrBy);
    }

    @Override
    public Reply del(byte[][] keys) {
        if (keys == null || keys.length == 0) {
            throw Utils.illegalArgumentException();
        }
        Long del = template.del(keys);
        return new IntegerReply(del);
    }

    @Override
    public IntegerReply exists(byte[][] keys) {
        Long exists = template.exists(keys);
        return new IntegerReply(exists);
    }

    @Override
    public StatusReply type(byte[] key) {
        String type = template.type(key);
        return new StatusReply(type);
    }

    @Override
    public MultiBulkReply sort(byte[] key, byte[][] args) {
        SortingParams sortingParams = ParamUtils.sortingParams(args);
        List<byte[]> sort = template.sort(key, sortingParams);
        return ParamUtils.collection2MultiBulkReply(sort);
    }

    @Override
    public MultiBulkReply mget(byte[][] keys) {
        List<byte[]> mget = template.mget(keys);
        Reply[] replies = new Reply[mget.size()];
        for (int i = 0; i< keys.length; i++) {
            byte[] value = mget.get(i);
            if (value == null) {
                replies[i] = BulkReply.NIL_REPLY;
            } else {
                replies[i] = new BulkReply(value);
            }
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public BulkReply substr(byte[] key, byte[] start, byte[] end) {
        byte[] substr = template.substr(key, (int) Utils.bytesToNum(start), (int) Utils.bytesToNum(end));
        return new BulkReply(substr);
    }

    @Override
    public Reply set(byte[] key, byte[] value1, byte[][] args) {
        if (args == null || args.length == 0) {
            String value = template.set(key, value1);
            return new StatusReply(value);
        }
        String nxxx = null;
        String expx = null;
        Long time = null;
        boolean needTime = false;
        for (byte[] arg : args) {
            if (needTime) {
                time = Utils.bytesToNum(arg);
                needTime = false;
                continue;
            }
            String argStr = new String(arg, Utils.utf8Charset);
            if (argStr.equalsIgnoreCase(RedisKeyword.NX.name())) {
                nxxx = RedisKeyword.NX.name();
            } else if (argStr.equalsIgnoreCase(RedisKeyword.XX.name())) {
                nxxx = RedisKeyword.XX.name();
            } else if (argStr.equalsIgnoreCase(RedisKeyword.EX.name())) {
                expx = RedisKeyword.EX.name();
                needTime = true;
            } else if (argStr.equalsIgnoreCase(RedisKeyword.PX.name())) {
                expx = RedisKeyword.PX.name();
                needTime = true;
            } else {
                return ErrorReply.SYNTAX_ERROR;
            }
        }
        if (needTime) {
            return ErrorReply.SYNTAX_ERROR;
        }
        if (nxxx != null && expx != null) {
            String set = template.set(key, value1, SafeEncoder.encode(nxxx), SafeEncoder.encode(expx), time);
            if (set == null) return BulkReply.NIL_REPLY;
            return new StatusReply(set);
        } else if (nxxx == null && expx == null) {
            String set = template.set(key, value1);
            if (set == null) return BulkReply.NIL_REPLY;
            return new StatusReply(set);
        } else if (nxxx != null) {
            if (nxxx.equalsIgnoreCase(RedisKeyword.NX.name())) {
                Long setnx = template.setnx(key, value1);
                if (setnx > 0) {
                    return StatusReply.OK;
                } else {
                    return BulkReply.NIL_REPLY;
                }
            } else if (nxxx.equalsIgnoreCase(RedisKeyword.XX.name())) {
                //当前jedis版本不支持
                return ErrorReply.NOT_SUPPORT;
            }
        } else {
            if (expx.equalsIgnoreCase(RedisKeyword.EX.name())) {
                String setex = template.setex(key, Math.toIntExact(time), value1);
                if (setex == null) return BulkReply.NIL_REPLY;
                return new StatusReply(setex);
            } else if (expx.equalsIgnoreCase(RedisKeyword.PX.name())) {
                String psetex = template.psetex(key, time, value1);
                if (psetex == null) return BulkReply.NIL_REPLY;
                return new StatusReply(psetex);
            }
        }
        return ErrorReply.SYNTAX_ERROR;
    }

    @Override
    public IntegerReply expire(byte[] key, byte[] seconds) {
        Long expire = template.expire(key, (int) Utils.bytesToNum(seconds));
        return new IntegerReply(expire);
    }

    @Override
    public IntegerReply pexpire(byte[] key, byte[] millis) {
        Long expire = template.pexpire(key, Utils.bytesToNum(millis));
        return new IntegerReply(expire);
    }

    @Override
    public IntegerReply expireat(byte[] key, byte[] timestamp) {
        Long expireAt = template.expireAt(key, Utils.bytesToNum(timestamp));
        return new IntegerReply(expireAt);
    }

    @Override
    public IntegerReply pexpireat(byte[] key, byte[] timestamp) {
        Long expireAt = template.pexpireAt(key, Utils.bytesToNum(timestamp));
        return new IntegerReply(expireAt);
    }

    @Override
    public IntegerReply ttl(byte[] key) {
        Long ttl = template.ttl(key);
        return new IntegerReply(ttl);
    }

    @Override
    public IntegerReply pttl(byte[] key) {
        Long ttl = template.pttl(SafeEncoder.encode(key));
        return new IntegerReply(ttl);
    }

    @Override
    public IntegerReply persist(byte[] key) {
        Long persist = template.persist(key);
        return new IntegerReply(persist);
    }

    @Override
    public StatusReply setex(byte[] key, byte[] seconds, byte[] value2) {
        String value = template.setex(key, (int) Utils.bytesToNum(seconds), value2);
        return new StatusReply(value);
    }

    @Override
    public StatusReply psetex(byte[] key, byte[] millis, byte[] value2) {
        String value = template.psetex(key, Utils.bytesToNum(millis), value2);
        return new StatusReply(value);
    }

    @Override
    public IntegerReply setnx(byte[] key, byte[] value1) {
        Long value = template.setnx(key, value1);
        return new IntegerReply(value);
    }

    @Override
    public BulkReply getset(byte[] key, byte[] value) {
        byte[] set = template.getSet(key, value);
        if (set == null) return BulkReply.NIL_REPLY;
        return new BulkReply(set);
    }

    @Override
    public IntegerReply strlen(byte[] key) {
        Long strlen = template.strlen(key);
        return new IntegerReply(strlen);
    }

    @Override
    public IntegerReply append(byte[] key, byte[] value) {
        Long append = template.append(key, value);
        return new IntegerReply(append);
    }

    @Override
    public IntegerReply setrange(byte[] key, byte[] offset, byte[] value) {
        Long setrange = template.setrange(key, Utils.bytesToNum(offset), value);
        return new IntegerReply(setrange);
    }

    @Override
    public BulkReply getrange(byte[] key, byte[] start, byte[] end) {
        byte[] getrange = template.getrange(key, Utils.bytesToNum(start), Utils.bytesToNum(end));
        return new BulkReply(getrange);
    }

    @Override
    public StatusReply mset(byte[][] kvs) {
        String mset = template.mset(kvs);
        return new StatusReply(mset);
    }

    @Override
    public MultiBulkReply zrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            Set<byte[]> set = template.zrangeByScore(key, min, max);
            return ParamUtils.collection2MultiBulkReply(set);
        }
        if (params.withScores && !params.withLimit) {
            Set<Tuple> tuples = template.zrangeByScoreWithScores(key, min, max);
            return ParamUtils.tuples2MultiBulkReply(tuples);
        }
        if (!params.withScores) {
            Set<byte[]> bytes = template.zrangeByScore(key, min, max, params.offset, params.count);
            return ParamUtils.collection2MultiBulkReply(bytes);
        }
        Set<Tuple> tuples = template.zrangeByScoreWithScores(key, min, max, params.offset, params.count);
        return ParamUtils.tuples2MultiBulkReply(tuples);
    }

    @Override
    public IntegerReply zadd(byte[] key, byte[][] args) {
        if (args.length < 2) {
            throw Utils.illegalArgumentException();
        }
        boolean nx = false;
        boolean xx = false;
        boolean ch = false;
        int index = 0;
        int paramCount = 0;
        ZAddParams params = ZAddParams.zAddParams();
        for (int i=index; i<=index + 1; i++) {
            byte[] arg = args[index];
            if (!nx) {
                nx = Utils.checkStringIgnoreCase(arg, RedisKeyword.NX.name());
                if (nx) {
                    params.nx();
                    paramCount ++;
                    continue;
                }
            }
            if (!xx) {
                xx = Utils.checkStringIgnoreCase(arg, RedisKeyword.XX.name());
                if (xx) {
                    params.xx();
                    paramCount ++;
                    continue;
                }
            }
            if (!ch) {
                ch = Utils.checkStringIgnoreCase(arg, RedisKeyword.CH.name());
                if (ch) {
                    params.ch();
                    paramCount ++;
                    continue;
                }
            }
            break;
        }
        int leaveArgs = args.length - paramCount;
        if (leaveArgs % 2 != 0) {
            throw Utils.illegalArgumentException();
        }
        Map<byte[], Double> scoreMembers = new HashMap<>();
        for (int i=paramCount; i<args.length; i=i+2) {
            double score = Utils.bytesToDouble(args[i]);
            byte[] value = args[i+1];
            scoreMembers.put(value, score);
        }
        Long zadd = template.zadd(key, scoreMembers, params);
        return new IntegerReply(zadd);
    }

    @Override
    public BulkReply zscore(byte[] key, byte[] member) {
        Double zscore = template.zscore(key, member);
        return new BulkReply(Utils.doubleToBytes(zscore));
    }

    @Override
    public BulkReply zincrby(byte[] key, byte[] increment, byte[] member) {
        Double zincrby = template.zincrby(key, Utils.bytesToDouble(increment), member);
        return new BulkReply(Utils.doubleToBytes(zincrby));
    }

    @Override
    public IntegerReply zcard(byte[] key) {
        Long zcard = template.zcard(key);
        return new IntegerReply(zcard);
    }

    @Override
    public IntegerReply zcount(byte[] key, byte[] min, byte[] max) {
        Long zcount = template.zcount(key, min, max);
        return new IntegerReply(zcount);
    }

    @Override
    public MultiBulkReply zrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores == null) {
            Set<byte[]> zrange = template.zrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            return ParamUtils.collection2MultiBulkReply(zrange);
        } else {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Set<Tuple> tuples = template.zrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                return ParamUtils.tuples2MultiBulkReply(tuples);
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        }
    }

    @Override
    public MultiBulkReply zrevrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores == null) {
            Set<byte[]> zrange = template.zrevrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            return ParamUtils.collection2MultiBulkReply(zrange);
        } else {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Set<Tuple> tuples = template.zrevrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                return ParamUtils.tuples2MultiBulkReply(tuples);
            }
            throw new IllegalArgumentException(Utils.syntaxError);
        }
    }

    @Override
    public MultiBulkReply zrevrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            Set<byte[]> set = template.zrevrangeByScore(key, min, max);
            return ParamUtils.collection2MultiBulkReply(set);
        }
        if (params.withScores && !params.withLimit) {
            Set<Tuple> tuples = template.zrevrangeByScoreWithScores(key, min, max);
            return ParamUtils.tuples2MultiBulkReply(tuples);
        }
        if (!params.withScores) {
            Set<byte[]> bytes = template.zrevrangeByScore(key, min, max, params.offset, params.count);
            return ParamUtils.collection2MultiBulkReply(bytes);
        }
        Set<Tuple> tuples = template.zrevrangeByScoreWithScores(key, min, max, params.offset, params.count);
        return ParamUtils.tuples2MultiBulkReply(tuples);
    }

    @Override
    public MultiBulkReply zrevrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit) {
            Set<byte[]> set = template.zrevrangeByLex(key, min, max);
            return ParamUtils.collection2MultiBulkReply(set);
        } else {
            Set<byte[]> bytes = template.zrevrangeByLex(key, min, max, params.offset, params.count);
            return ParamUtils.collection2MultiBulkReply(bytes);
        }
    }

    @Override
    public MultiBulkReply zrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (params.withLimit) {
            Set<byte[]> bytes = template.zrangeByLex(key, min, max, params.offset, params.count);
            return ParamUtils.collection2MultiBulkReply(bytes);
        } else {
            Set<byte[]> bytes = template.zrangeByLex(key, min, max);
            return ParamUtils.collection2MultiBulkReply(bytes);
        }
    }

    @Override
    public Reply zrank(byte[] key, byte[] member) {
        Long zrank = template.zrank(key, member);
        if (zrank == null) {
            return BulkReply.NIL_REPLY;
        }
        return new IntegerReply(zrank);
    }

    @Override
    public Reply zrevrank(byte[] key, byte[] member) {
        Long zrevrank = template.zrevrank(key, member);
        if (zrevrank == null) {
            return BulkReply.NIL_REPLY;
        }
        return new IntegerReply(zrevrank);
    }

    @Override
    public IntegerReply zrem(byte[] key, byte[][] members) {
        Long zrem = template.zrem(key, members);
        return new IntegerReply(zrem);
    }

    @Override
    public IntegerReply zlexcount(byte[] key, byte[] min, byte[] max) {
        Long zlexcount = template.zlexcount(key, min, max);
        return new IntegerReply(zlexcount);
    }

    @Override
    public MultiBulkReply zscan(byte[] key, byte[] cursor, byte[][] args) {
        ScanParams scanParams = ParamUtils.parseScanParams(args);
        ScanResult<Tuple> zscan = template.zscan(key, cursor, scanParams);
        Reply[] replies = new Reply[2];
        replies[0] = new BulkReply(zscan.getCursorAsBytes());
        replies[1] = ParamUtils.tuples2MultiBulkReply(zscan.getResult());
        return new MultiBulkReply(replies);
    }

    @Override
    public IntegerReply zremrangebyrank(byte[] key, byte[] start, byte[] stop) {
        Long zremrangeByRank = template.zremrangeByRank(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        return new IntegerReply(zremrangeByRank);
    }

    @Override
    public IntegerReply zremrangebyscore(byte[] key, byte[] min, byte[] max) {
        Long zremrangeByScore = template.zremrangeByScore(key, min, max);
        return new IntegerReply(zremrangeByScore);
    }

    @Override
    public IntegerReply zremrangebylex(byte[] key, byte[] min, byte[] max) {
        Long zremrangeByLex = template.zremrangeByLex(key, min, max);
        return new IntegerReply(zremrangeByLex);
    }

    @Override
    public IntegerReply hset(byte[] key, byte[] field, byte[] value) {
        Long hset = template.hset(key, field, value);
        return new IntegerReply(hset);
    }

    @Override
    public IntegerReply hsetnx(byte[] key, byte[] field, byte[] value) {
        Long hset = template.hsetnx(key, field, value);
        return new IntegerReply(hset);
    }

    @Override
    public Reply hget(byte[] key, byte[] field) {
        byte[] value = template.hget(key, field);
        if (value == null) return BulkReply.NIL_REPLY;
        return new BulkReply(value);
    }

    @Override
    public IntegerReply hexists(byte[] key, byte[] field) {
        Boolean hexists = template.hexists(key, field);
        return hexists ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0;
    }

    @Override
    public IntegerReply hdel(byte[] key, byte[] field) {
        Long hdel = template.hdel(key, field);
        return new IntegerReply(hdel);
    }

    @Override
    public IntegerReply hlen(byte[] key) {
        Long hlen = template.hlen(key);
        return new IntegerReply(hlen);
    }

    @Override
    public IntegerReply hincrby(byte[] key, byte[] field, byte[] increment) {
        if (increment == null) {
            throw Utils.illegalArgumentException();
        }
        Long ret = template.hincrBy(key, field, Utils.bytesToNum(increment));
        return new IntegerReply(ret);
    }

    @Override
    public BulkReply hincrbyfloat(byte[] key, byte[] field, byte[] increment) {
        Double hincrByFloat = template.hincrByFloat(key, field, Utils.bytesToDouble(increment));
        return new BulkReply(Utils.doubleToBytes(hincrByFloat));
    }

    @Override
    public StatusReply hmset(byte[] key, byte[][] kvs) {
        if (kvs == null || kvs.length % 2 != 0) {
            throw Utils.illegalArgumentException();
        }
        Map<byte[], byte[]> kvMap = new HashMap<>();
        for (int i=0; i< kvs.length / 2; i+=2) {
            byte[] field = kvs[i];
            byte[] value = kvs[i+1];
            kvMap.put(field, value);
        }
        String hmset = template.hmset(key, kvMap);
        return new StatusReply(hmset);
    }

    @Override
    public MultiBulkReply hmget(byte[] key, byte[][] fields) {
        if (fields == null || fields.length == 0) {
            throw Utils.illegalArgumentException();
        }
        List<byte[]> hmget = template.hmget(key, fields);
        return ParamUtils.collection2MultiBulkReply(hmget);
    }

    @Override
    public MultiBulkReply hkeys(byte[] key) {
        Set<byte[]> hkeys = template.hkeys(key);
        return ParamUtils.collection2MultiBulkReply(hkeys);
    }

    @Override
    public MultiBulkReply hvals(byte[] key) {
        List<byte[]> hkeys = template.hvals(key);
        return ParamUtils.collection2MultiBulkReply(hkeys);
    }

    @Override
    public MultiBulkReply hgetall(byte[] key) {
        Map<byte[], byte[]> map = template.hgetAll(key);
        Reply[] replies = new Reply[map.size()*2];
        int index = 0;
        for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
            replies[index] = new BulkReply(entry.getKey());
            replies[index + 1] = new BulkReply(entry.getValue());
            index += 2;
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public MultiBulkReply hscan(byte[] key, byte[] cursor, byte[][] args) {
        ScanResult<Map.Entry<byte[], byte[]>> hscan;
        if (args == null || args.length == 0) {
            hscan = template.hscan(key, cursor);
        } else {
            ScanParams scanParams = ParamUtils.parseScanParams(args);
            hscan = template.hscan(key, cursor, scanParams);
        }
        Reply[] replies = new Reply[2];
        byte[] bytes = hscan.getCursorAsBytes();
        replies[0] = new BulkReply(bytes);
        Reply[] subReplies = new Reply[hscan.getResult().size() * 2];
        int index = 0;
        for (Map.Entry<byte[], byte[]> entry : hscan.getResult()) {
            subReplies[index] = new BulkReply(entry.getKey());
            subReplies[index + 1] = new BulkReply(entry.getValue());
            index += 2;
        }
        replies[1] = new MultiBulkReply(subReplies);
        return new MultiBulkReply(replies);
    }

    @Override
    public IntegerReply lpush(byte[] key, byte[][] args) {
        Long lpush = template.lpush(key, args);
        return new IntegerReply(lpush);
    }

    @Override
    public IntegerReply lpushx(byte[] key, byte[][] args) {
        Long lpushx = template.lpushx(key, args);
        return new IntegerReply(lpushx);
    }

    @Override
    public IntegerReply rpush(byte[] key, byte[][] args) {
        Long rpush = template.rpush(key, args);
        return new IntegerReply(rpush);
    }

    @Override
    public IntegerReply rpushx(byte[] key, byte[][] args) {
        Long rpushx = template.rpushx(key, args);
        return new IntegerReply(rpushx);
    }

    @Override
    public BulkReply lpop(byte[] key) {
        byte[] lpop = template.lpop(key);
        if (lpop == null) return BulkReply.NIL_REPLY;
        return new BulkReply(lpop);
    }

    @Override
    public BulkReply rpop(byte[] key) {
        byte[] rpop = template.rpop(key);
        if (rpop == null) return BulkReply.NIL_REPLY;
        return new BulkReply(rpop);
    }

    @Override
    public IntegerReply lrem(byte[] key, byte[] count, byte[] value) {
        Long lrem = template.lrem(key, Utils.bytesToNum(count), value);
        return new IntegerReply(lrem);
    }

    @Override
    public IntegerReply llen(byte[] key) {
        Long llen = template.llen(key);
        return new IntegerReply(llen);
    }

    @Override
    public BulkReply lindex(byte[] key, byte[] index) {
        byte[] lindex = template.lindex(key, Utils.bytesToNum(index));
        if (lindex == null) return BulkReply.NIL_REPLY;
        return new BulkReply(lindex);
    }

    @Override
    public IntegerReply linsert(byte[] key, byte[] beforeAfter, byte[] pivot, byte[] value) {
        BinaryClient.LIST_POSITION position;
        if (Utils.checkStringIgnoreCase(beforeAfter, RedisKeyword.BEFORE.name())) {
            position = BinaryClient.LIST_POSITION.BEFORE;
        } else if (Utils.checkStringIgnoreCase(beforeAfter, RedisKeyword.AFTER.name())) {
            position = BinaryClient.LIST_POSITION.AFTER;
        } else {
            throw new IllegalArgumentException(Utils.syntaxError);
        }
        Long linsert = template.linsert(key, position, pivot, value);
        return new IntegerReply(linsert);
    }

    @Override
    public StatusReply lset(byte[] key, byte[] index, byte[] value) {
        String lset = template.lset(key, Utils.bytesToNum(index), value);
        return new StatusReply(lset);
    }

    @Override
    public MultiBulkReply lrange(byte[] key, byte[] start, byte[] stop) {
        List<byte[]> lrange = template.lrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        Reply[] replies = new Reply[lrange.size()];
        for (int i=0; i<lrange.size(); i++) {
            replies[i] = new BulkReply(lrange.get(i));
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public StatusReply ltrim(byte[] key, byte[] start, byte[] stop) {
        String ltrim = template.ltrim(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        return new StatusReply(ltrim);
    }

    @Override
    public IntegerReply sadd(byte[] key, byte[][] args) {
        Long sadd = template.sadd(key, args);
        return new IntegerReply(sadd);
    }

    @Override
    public IntegerReply sismember(byte[] key, byte[] member) {
        Boolean sismember = template.sismember(key, member);
        return sismember ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0;
    }

    @Override
    public BulkReply spop(byte[] key) {
        byte[] spop = template.spop(key);
        if (spop == null) return BulkReply.NIL_REPLY;
        return new BulkReply(spop);
    }

    @Override
    public Reply srandmember(byte[] key, byte[] count) {
        if (count == null) {
            byte[] srandmember = template.srandmember(key);
            if (srandmember == null) {
                return BulkReply.NIL_REPLY;
            }
            return new BulkReply(srandmember);
        } else {
            List<byte[]> srandmember = template.srandmember(key, (int) Utils.bytesToNum(count));
            Reply[] replies = new Reply[srandmember.size()];
            int index = 0;
            for (byte[] bytes : srandmember) {
                if (bytes == null) {
                    replies[index] = BulkReply.NIL_REPLY;
                } else {
                    replies[index] = new BulkReply(bytes);
                }
                index ++;
            }
            return new MultiBulkReply(replies);
        }
    }

    @Override
    public IntegerReply srem(byte[] key, byte[][] args) {
        if (args.length < 0) {
            throw Utils.illegalArgumentException();
        }
        Long srem = template.srem(key, args);
        return new IntegerReply(srem);
    }

    @Override
    public IntegerReply scard(byte[] key) {
        Long scard = template.scard(key);
        return new IntegerReply(scard);
    }

    @Override
    public MultiBulkReply smembers(byte[] key) {
        Set<byte[]> smembers = template.smembers(key);
        Reply[] replies = new Reply[smembers.size()];
        int index = 0;
        for (byte[] smember : smembers) {
            replies[index] = new BulkReply(smember);
            index ++;
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public MultiBulkReply sscan(byte[] key, byte[] cursor, byte[][] args) {
        ScanResult<byte[]> sscan;
        if (args == null || args.length == 0) {
            sscan = template.sscan(key, cursor);
        } else {
            ScanParams scanParams = ParamUtils.parseScanParams(args);
            sscan = template.sscan(key, cursor, scanParams);
        }
        Reply[] replies = new Reply[2];
        replies[0] = new BulkReply(sscan.getCursorAsBytes());
        Reply[] subReplies = new Reply[sscan.getResult().size()];
        int index = 0;
        for (byte[] bytes : sscan.getResult()) {
            subReplies[index] = new BulkReply(bytes);
            index ++;
        }
        replies[1] = new MultiBulkReply(subReplies);
        return new MultiBulkReply(replies);
    }

    @Override
    public StatusReply ping() {
        return StatusReply.PONG;
    }

    @Override
    public IntegerReply geoadd(byte[] key, byte[][] args) {
        if (args.length % 3 != 0) {
            throw Utils.illegalArgumentException();
        }
        Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
        for (int i=0; i<args.length / 3; i+=3) {
            byte[] member = args[i];
            GeoCoordinate geoCoordinate = new GeoCoordinate(Utils.bytesToDouble(args[i+1]), Utils.bytesToDouble(args[i+2]));
            memberCoordinateMap.put(member, geoCoordinate);
        }
        Long geoadd = template.geoadd(key, memberCoordinateMap);
        return new IntegerReply(geoadd);
    }

    @Override
    public MultiBulkReply geopos(byte[] key, byte[][] members) {
        if (members == null || members.length == 0) {
            throw Utils.illegalArgumentException();
        }
        List<GeoCoordinate> geopos = template.geopos(key, members);
        return ParamUtils.geoList2MultiBulkReply(geopos);
    }

    @Override
    public BulkReply geodist(byte[] key, byte[] member1, byte[] member2, byte[] unit) {
        Double geodist;
        if (unit == null || unit.length == 0) {
            geodist = template.geodist(key, member1, member2);
        } else {
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            geodist = template.geodist(key, member1, member2, targetUnit);
        }
        if (geodist == null) {
            return BulkReply.NIL_REPLY;
        }
        return new BulkReply(Utils.doubleToBytes(geodist));
    }


    @Override
    public MultiBulkReply georadius(byte[] key, byte[] longtitude, byte[] latitude, byte[] radius, byte[] unit, byte[][] args) {
        List<GeoRadiusResponse> georadius;
        ParamUtils.GeoRadiusParams geoRadiusParams;
        if (args == null || args.length == 0) {
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            geoRadiusParams = new ParamUtils.GeoRadiusParams();
            georadius = template.georadius(key, Utils.bytesToDouble(longtitude),
                    Utils.bytesToDouble(latitude), Utils.bytesToDouble(radius), targetUnit);
        } else {
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            geoRadiusParams = ParamUtils.parseGeoRadiusParams(args);
            georadius = template.georadius(key, Utils.bytesToDouble(longtitude),
                    Utils.bytesToDouble(latitude), Utils.bytesToDouble(radius), targetUnit, geoRadiusParams.param);

        }
        Reply[] replies = new Reply[georadius.size()];
        int index = 0;
        for (GeoRadiusResponse georadiu : georadius) {
            replies[index] = ParamUtils.parseGeoRadiusResponse(georadiu, geoRadiusParams.withCoord, geoRadiusParams.withDist);
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public MultiBulkReply georadiusbymember(byte[] key, byte[] member, byte[] radius, byte[] unit, byte[][] args) {
        List<GeoRadiusResponse> georadius;
        ParamUtils.GeoRadiusParams geoRadiusParams;
        if (args == null || args.length == 0) {
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            geoRadiusParams = new ParamUtils.GeoRadiusParams();
            georadius = template.georadiusByMember(key, member, Utils.bytesToDouble(radius), targetUnit);
        } else {
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            geoRadiusParams = ParamUtils.parseGeoRadiusParams(args);
            georadius = template.georadiusByMember(key, member, Utils.bytesToDouble(radius), targetUnit, geoRadiusParams.param);
        }
        Reply[] replies = new Reply[georadius.size()];
        int index = 0;
        for (GeoRadiusResponse georadiu : georadius) {
            replies[index] = ParamUtils.parseGeoRadiusResponse(georadiu, geoRadiusParams.withCoord, geoRadiusParams.withDist);
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public MultiBulkReply geohash(byte[] key, byte[][] members) {
        List<byte[]> geohash = template.geohash(key, members);
        return ParamUtils.collection2MultiBulkReply(geohash);
    }

    @Override
    public IntegerReply setbit(byte[] key, byte[] offset, byte[] value) {
        Boolean setbit = template.setbit(key, Utils.bytesToNum(offset), value);
        return setbit ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0;
    }

    @Override
    public IntegerReply getbit(byte[] key, byte[] offset) {
        Boolean getbit = template.getbit(key, Utils.bytesToNum(offset));
        return getbit ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0;
    }

    @Override
    public IntegerReply bitcount(byte[] key, byte[][] args) {
        Long bitcount;
        if (args == null || args.length == 0) {
            bitcount = template.bitcount(key);
        } else if (args.length == 2) {
            long start = Utils.bytesToNum(args[0]);
            long end = Utils.bytesToNum(args[1]);
            bitcount = template.bitcount(key, start, end);
        } else {
            throw Utils.illegalArgumentException();
        }
        return new IntegerReply(bitcount);
    }

    @Override
    public IntegerReply bitpos(byte[] key, byte[] bit, byte[][] args) {
        long bitValue = Utils.bytesToNum(bit);
        if (bitValue != 0 && bitValue != 1) {
            throw new IllegalArgumentException(Utils.syntaxError);
        }
        BitPosParams params = ParamUtils.bitposParam(args);
        Long bitpos = template.bitpos(SafeEncoder.encode(key), bitValue == 1, params);
        return new IntegerReply(bitpos);
    }

    @Override
    public Reply bitfield(byte[] key, byte[][] args) {
        List<Long> bitfield = template.bitfield(key, args);
        if (bitfield == null) return BulkReply.NIL_REPLY;
        Reply[] replies = new Reply[bitfield.size()];
        int index = 0;
        for (Long item : bitfield) {
            replies[index] = new IntegerReply(item);
        }
        return new MultiBulkReply(replies);
    }

    @Override
    public BulkReply echo(byte[] echo) {
        String response = template.echo(SafeEncoder.encode(echo));
        return new BulkReply(response.getBytes(Utils.utf8Charset));
    }
}

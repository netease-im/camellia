package com.netease.nim.camellia.redis.proxy.command.sync;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import redis.clients.jedis.*;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.util.SafeEncoder;

import java.util.*;


/**
 *
 * Created by caojiajun on 2019/12/30.
 */
public class SyncCommandProcessorPipeline implements ISyncCommandProcessorPipeline {

    private ICamelliaRedisPipeline pipelined;

    public SyncCommandProcessorPipeline(CamelliaRedisTemplate template) {
        pipelined = template.pipelined();
    }

    public void sync() {
        pipelined.sync();
    }

    public void close() {
        pipelined.close();
    }

    @Override
    public PipelineResponse get(byte[] key) {
        Response<byte[]> response = pipelined.get(key);
        return new PipelineResponse<>(response, r -> {
            byte[] bytes = r.get();
            return new BulkReply(bytes);
        });
    }

    @Override
    public PipelineResponse incr(byte[] key) {
        Response<Long> response = pipelined.incr(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse incrby(byte[] key, byte[] increment) {
        Response<Long> response = pipelined.incrBy(key, Utils.bytesToNum(increment));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse incrbyfloat(byte[] key, byte[] increment) {
        Response<Double> response = pipelined.incrByFloat(key, Utils.bytesToDouble(increment));
        return new PipelineResponse<>(response, r -> new BulkReply(Utils.doubleToBytes(r.get())));
    }

    @Override
    public PipelineResponse decr(byte[] key) {
        Response<Long> response = pipelined.decr(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse decrBy(byte[] key, byte[] decrement) {
        Response<Long> response = pipelined.decrBy(key, Utils.bytesToNum(decrement));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse set(byte[] key, byte[] value1, byte[][] args) {
        if (args == null || args.length == 0) {
            Response<String> response = pipelined.set(key, value1);
            return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
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
                return new PipelineResponse(ErrorReply.SYNTAX_ERROR);
            }
        }
        if (needTime) {
            return new PipelineResponse(ErrorReply.SYNTAX_ERROR);
        }
        if (nxxx != null && expx != null) {
            Response<String> response = pipelined.set(key, value1, SafeEncoder.encode(nxxx), SafeEncoder.encode(expx), time);
            return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
        } else if (nxxx == null && expx == null) {
            Response<String> response = pipelined.set(key, value1);
            return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
        } else if (nxxx != null) {
            if (nxxx.equalsIgnoreCase(RedisKeyword.NX.name())) {
                Response<Long> response = pipelined.setnx(key, value1);
                return new PipelineResponse<>(response, response1 -> {
                    Long setnx = response1.get();
                    if (setnx > 0) {
                        return StatusReply.OK;
                    } else {
                        return BulkReply.NIL_REPLY;
                    }
                });
            } else if (nxxx.equalsIgnoreCase(RedisKeyword.XX.name())) {
                //当前jedis版本不支持
                return new PipelineResponse(ErrorReply.NOT_SUPPORT);
            }
        } else {
            if (expx.equalsIgnoreCase(RedisKeyword.EX.name())) {
                Response<String> response = pipelined.setex(key, Math.toIntExact(time), value1);
                return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
            } else if (expx.equalsIgnoreCase(RedisKeyword.PX.name())) {
                Response<String> response = pipelined.psetex(key, Math.toIntExact(time), value1);
                return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
            }
        }
        return new PipelineResponse(ErrorReply.SYNTAX_ERROR);
    }

    @Override
    public PipelineResponse setex(byte[] key, byte[] seconds, byte[] value) {
        Response<String> response = pipelined.setex(key, (int) Utils.bytesToNum(seconds), value);
        return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
    }

    @Override
    public PipelineResponse psetex(byte[] key, byte[] millis, byte[] value) {
        Response<String> response = pipelined.psetex(key, Utils.bytesToNum(millis), value);
        return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
    }

    @Override
    public PipelineResponse setnx(byte[] key, byte[] value) {
        Response<Long> response = pipelined.setnx(key, value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse getset(byte[] key, byte[] value) {
        Response<byte[]> response = pipelined.getSet(key, value);
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse strlen(byte[] key) {
        Response<Long> response = pipelined.strlen(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse append(byte[] key, byte[] value) {
        Response<Long> response = pipelined.append(key, value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse setrange(byte[] key, byte[] offset, byte[] value) {
        Response<Long> response = pipelined.setrange(key, Utils.bytesToNum(offset), value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse getrange(byte[] key, byte[] start, byte[] end) {
        Response<byte[]> response = pipelined.getrange(key, Utils.bytesToNum(start), Utils.bytesToNum(end));
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse substr(byte[] key, byte[] start, byte[] end) {
        Response<String> response = pipelined.substr(key, (int) Utils.bytesToNum(start), (int) Utils.bytesToNum(end));
        return new PipelineResponse<>(response, r -> new BulkReply(r.get() == null ? null : SafeEncoder.encode(r.get())));
    }

    @Override
    public PipelineResponse zadd(byte[] key, byte[][] args) {
        if (args.length < 2) {
            return new PipelineResponse(new ErrorReply("wrong number of arguments for 'add' command"));
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
                    paramCount ++;
                    params.nx();
                    continue;
                }
            }
            if (!xx) {
                xx = Utils.checkStringIgnoreCase(arg, RedisKeyword.XX.name());
                if (xx) {
                    paramCount ++;
                    params.xx();
                    continue;
                }
            }
            if (!ch) {
                ch = Utils.checkStringIgnoreCase(arg, RedisKeyword.CH.name());
                if (ch) {
                    paramCount ++;
                    params.ch();
                    continue;
                }
            }
            break;
        }
        int leaveArgs = args.length - paramCount;
        if (leaveArgs % 2 != 0) {
            return new PipelineResponse(new ErrorReply("wrong number of arguments for 'add' command"));
        }
        Map<byte[], Double> scoreMembers = new HashMap<>();
        for (int i=paramCount; i<args.length; i=i+2) {
            double score = Utils.bytesToDouble(args[i]);
            byte[] value = args[i+1];
            scoreMembers.put(value, score);
        }
        Response<Long> response = pipelined.zadd(key, scoreMembers, params);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zscore(byte[] key, byte[] member) {
        Response<Double> response = pipelined.zscore(key, member);
        return new PipelineResponse<>(response, r -> new BulkReply(Utils.doubleToBytes(r.get())));
    }

    @Override
    public PipelineResponse zincrby(byte[] key, byte[] increment, byte[] member) {
        Response<Double> response = pipelined.zincrby(key, Utils.bytesToDouble(increment), member);
        return new PipelineResponse<>(response, r -> new BulkReply(Utils.doubleToBytes(r.get())));
    }

    @Override
    public PipelineResponse zcard(byte[] key) {
        Response<Long> response = pipelined.zcard(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zcount(byte[] key, byte[] min, byte[] max) {
        Response<Long> response = pipelined.zcount(key, Utils.bytesToDouble(min), Utils.bytesToDouble(max));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores == null) {
            Response<Set<byte[]>> response = pipelined.zrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        } else {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Response<Set<Tuple>> response = pipelined.zrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                return new PipelineResponse(() -> {
                    Set<Tuple> tuples = response.get();
                    return ParamUtils.tuples2MultiBulkReply(tuples);
                });
            }
            return new PipelineResponse(new ErrorReply(Utils.syntaxError));
        }
    }

    @Override
    public PipelineResponse zrevrange(byte[] key, byte[] start, byte[] stop, byte[] withscores) {
        if (withscores == null) {
            Response<Set<byte[]>> response = pipelined.zrevrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        } else {
            if (Utils.checkStringIgnoreCase(withscores, RedisKeyword.WITHSCORES.name())) {
                Response<Set<Tuple>> response = pipelined.zrevrangeWithScores(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
                return new PipelineResponse(() -> {
                    Set<Tuple> tuples = response.get();
                    return ParamUtils.tuples2MultiBulkReply(tuples);
                });
            }
            return new PipelineResponse(new ErrorReply(Utils.syntaxError));
        }
    }

    @Override
    public PipelineResponse zrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            Response<Set<byte[]>> response = pipelined.zrangeByScore(key, min, max);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        }
        if (params.withScores && !params.withLimit) {
            Response<Set<Tuple>> response = pipelined.zrangeByScoreWithScores(key, min, max);
            return new PipelineResponse(() -> {
                Set<Tuple> tuples = response.get();
                return ParamUtils.tuples2MultiBulkReply(tuples);
            });
        }
        if (!params.withScores) {
            Response<Set<byte[]>> response = pipelined.zrangeByScore(key, min, max, params.offset, params.count);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        }
        Response<Set<Tuple>> response = pipelined.zrangeByScoreWithScores(key, min, max, params.offset, params.count);
        return new PipelineResponse(() -> {
            Set<Tuple> tuples = response.get();
            return ParamUtils.tuples2MultiBulkReply(tuples);
        });
    }

    @Override
    public PipelineResponse zrevrangebyscore(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (!params.withLimit && !params.withScores) {
            Response<Set<byte[]>> response = pipelined.zrevrangeByScore(key, min, max);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        }
        if (params.withScores && !params.withLimit) {
            Response<Set<Tuple>> response = pipelined.zrevrangeByScoreWithScores(key, min, max);
            return new PipelineResponse(() -> {
                Set<Tuple> tuples = response.get();
                return ParamUtils.tuples2MultiBulkReply(tuples);
            });
        }
        if (!params.withScores) {
            Response<Set<byte[]>> response = pipelined.zrevrangeByScore(key, min, max, params.offset, params.count);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        }
        Response<Set<Tuple>> response = pipelined.zrevrangeByScoreWithScores(key, min, max, params.offset, params.count);
        return new PipelineResponse(() -> {
            Set<Tuple> tuples = response.get();
            return ParamUtils.tuples2MultiBulkReply(tuples);
        });
    }

    @Override
    public PipelineResponse zrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (params.withLimit) {
            Response<Set<byte[]>> response = pipelined.zrangeByLex(key, min, max, params.offset, params.count);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        } else {
            Response<Set<byte[]>> response = pipelined.zrangeByLex(key, min, max);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        }
    }

    @Override
    public PipelineResponse zrevrangebylex(byte[] key, byte[] min, byte[] max, byte[][] args) {
        ParamUtils.ZRangeParams params = ParamUtils.parseZRangeParams(args);
        if (params.withLimit) {
            Response<Set<byte[]>> response = pipelined.zrevrangeByLex(key, min, max, params.offset, params.count);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        } else {
            Response<Set<byte[]>> response = pipelined.zrevrangeByLex(key, min, max);
            return new PipelineResponse(() -> {
                Set<byte[]> bytes = response.get();
                return ParamUtils.collection2MultiBulkReply(bytes);
            });
        }
    }

    @Override
    public PipelineResponse zremrangebyrank(byte[] key, byte[] start, byte[] stop) {
        Response<Long> response = pipelined.zremrangeByRank(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zremrangebyscore(byte[] key, byte[] min, byte[] max) {
        Response<Long> response = pipelined.zremrangeByScore(key, Utils.bytesToNum(min), Utils.bytesToNum(max));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zremrangebylex(byte[] key, byte[] min, byte[] max) {
        Response<Long> response = pipelined.zremrangeByLex(key, min, max);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zrank(byte[] key, byte[] member) {
        Response<Long> response = pipelined.zrank(key, member);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zrevrank(byte[] key, byte[] member) {
        Response<Long> response = pipelined.zrevrank(key, member);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zrem(byte[] key, byte[][] members) {
        Response<Long> response = pipelined.zrem(key, members);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse zlexcount(byte[] key, byte[] min, byte[] max) {
        Response<Long> response = pipelined.zlexcount(key, min, max);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse sadd(byte[] key, byte[][] args) {
        Response<Long> response = pipelined.sadd(key, args);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse sismember(byte[] key, byte[] member) {
        Response<Boolean> response = pipelined.sismember(key, member);
        return new PipelineResponse<>(response, r -> response.get() ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0);
    }

    @Override
    public PipelineResponse spop(byte[] key) {
        Response<byte[]> response = pipelined.spop(key);
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse srandmember(byte[] key, byte[] count) {
        if (count == null) {
            Response<byte[]> response = pipelined.srandmember(key);
            return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
        } else {
            Response<List<byte[]>> response = pipelined.srandmember(key, (int) Utils.bytesToNum(count));
            return new PipelineResponse<>(() -> {
                List<byte[]> srandmember = response.get();
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
            });
        }
    }

    @Override
    public PipelineResponse srem(byte[] key, byte[][] args) {
        Response<Long> response = pipelined.srem(key, args);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse scard(byte[] key) {
        Response<Long> response = pipelined.scard(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse smembers(byte[] key) {
        Response<Set<byte[]>> response = pipelined.smembers(key);
        return new PipelineResponse(() -> {
            Set<byte[]> smembers = response.get();
            Reply[] replies = new Reply[smembers.size()];
            int index = 0;
            for (byte[] smember : smembers) {
                replies[index] = new BulkReply(smember);
                index ++;
            }
            return new MultiBulkReply(replies);
        });
    }

    @Override
    public PipelineResponse lpush(byte[] key, byte[][] args) {
        Response<Long> response = pipelined.lpush(key, args);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse lpushx(byte[] key, byte[][] args) {
        Response<Long> response = pipelined.lpushx(key, args);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse rpush(byte[] key, byte[][] args) {
        Response<Long> response = pipelined.rpush(key, args);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse rpushx(byte[] key, byte[][] args) {
        Response<Long> response = pipelined.rpushx(key, args);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse lpop(byte[] key) {
        Response<byte[]> response = pipelined.lpop(key);
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse rpop(byte[] key) {
        Response<byte[]> response = pipelined.rpop(key);
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse lrem(byte[] key, byte[] count, byte[] value) {
        Response<Long> response = pipelined.lrem(key, Utils.bytesToNum(count), value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse llen(byte[] key) {
        Response<Long> response = pipelined.llen(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse lindex(byte[] key, byte[] index) {
        Response<byte[]> response = pipelined.lindex(key, Utils.bytesToNum(index));
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse linsert(byte[] key, byte[] beforeAfter, byte[] pivot, byte[] value) {
        BinaryClient.LIST_POSITION position;
        if (Utils.checkStringIgnoreCase(beforeAfter, RedisKeyword.BEFORE.name())) {
            position = BinaryClient.LIST_POSITION.BEFORE;
        } else if (Utils.checkStringIgnoreCase(beforeAfter, RedisKeyword.AFTER.name())) {
            position = BinaryClient.LIST_POSITION.AFTER;
        } else {
            throw new IllegalArgumentException(Utils.syntaxError);
        }
        Response<Long> response = pipelined.linsert(key, position, pivot, value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse lset(byte[] key, byte[] index, byte[] value) {
        Response<String> response = pipelined.lset(key, Utils.bytesToNum(index), value);
        return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
    }

    @Override
    public PipelineResponse lrange(byte[] key, byte[] start, byte[] stop) {
        Response<List<byte[]>> response = pipelined.lrange(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        return new PipelineResponse(() -> {
            List<byte[]> lrange = response.get();
            Reply[] replies = new Reply[lrange.size()];
            for (int i=0; i<lrange.size(); i++) {
                replies[i] = new BulkReply(lrange.get(i));
            }
            return new MultiBulkReply(replies);
        });
    }

    @Override
    public PipelineResponse ltrim(byte[] key, byte[] start, byte[] stop) {
        Response<String> response = pipelined.ltrim(key, Utils.bytesToNum(start), Utils.bytesToNum(stop));
        return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
    }

    @Override
    public PipelineResponse hset(byte[] key, byte[] field, byte[] value) {
        Response<Long> response = pipelined.hset(key, field, value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse hsetnx(byte[] key, byte[] field, byte[] value) {
        Response<Long> response = pipelined.hsetnx(key, field, value);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse hget(byte[] key, byte[] field) {
        Response<byte[]> response = pipelined.hget(key, field);
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    @Override
    public PipelineResponse hexists(byte[] key, byte[] field) {
        Response<Boolean> response = pipelined.hexists(key, field);
        return new PipelineResponse<>(response, r -> r.get() ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0);
    }

    @Override
    public PipelineResponse hdel(byte[] key, byte[] field) {
        Response<Long> response = pipelined.hdel(key, field);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse hlen(byte[] key) {
        Response<Long> response = pipelined.hlen(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse hincrby(byte[] key, byte[] field, byte[] increment) {
        Response<Long> response = pipelined.hincrBy(key, field, Utils.bytesToNum(increment));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse hincrbyfloat(byte[] key, byte[] field, byte[] increment) {
        Response<Double> response = pipelined.hincrByFloat(key, field, Utils.bytesToDouble(increment));
        return new PipelineResponse<>(response, r -> new BulkReply(Utils.doubleToBytes(r.get())));
    }

    @Override
    public PipelineResponse hmset(byte[] key, byte[][] kvs) {
        if (kvs == null || kvs.length % 2 != 0) {
            return new PipelineResponse(new ErrorReply("wrong number of arguments for 'hmset' command"));
        }
        Map<byte[], byte[]> kvMap = new HashMap<>();
        for (int i=0; i< kvs.length / 2; i+=2) {
            byte[] field = kvs[i];
            byte[] value = kvs[i+1];
            kvMap.put(field, value);
        }
        Response<String> response = pipelined.hmset(key, kvMap);
        return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
    }

    @Override
    public PipelineResponse hmget(byte[] key, byte[][] fields) {
        if (fields == null || fields.length == 0) {
            return new PipelineResponse(new ErrorReply("wrong number of arguments for 'hmget' command"));
        }
        Response<List<byte[]>> response = pipelined.hmget(key, fields);
        return new PipelineResponse(() -> {
            List<byte[]> hmget = response.get();
            return ParamUtils.collection2MultiBulkReply(hmget);
        });
    }

    @Override
    public PipelineResponse hkeys(byte[] key) {
        Response<Set<byte[]>> response = pipelined.hkeys(key);
        return new PipelineResponse(() -> {
            Set<byte[]> hkeys = response.get();
            return ParamUtils.collection2MultiBulkReply(hkeys);
        });
    }

    @Override
    public PipelineResponse hvals(byte[] key) {
        Response<List<byte[]>> response = pipelined.hvals(key);
        return new PipelineResponse(() -> {
            List<byte[]> hvals = response.get();
            return ParamUtils.collection2MultiBulkReply(hvals);
        });
    }

    @Override
    public PipelineResponse hgetall(byte[] key) {
        Response<Map<byte[], byte[]>> response = pipelined.hgetAll(key);
        return new PipelineResponse(() -> {
            Map<byte[], byte[]> map = response.get();
            Reply[] replies = new Reply[map.size()*2];
            int index = 0;
            for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
                replies[index] = new BulkReply(entry.getKey());
                replies[index + 1] = new BulkReply(entry.getValue());
                index += 2;
            }
            return new MultiBulkReply(replies);
        });
    }

    @Override
    public PipelineResponse geoadd(byte[] key, byte[][] args) {
        if (args.length % 3 != 0) {
            return new PipelineResponse(new ErrorReply("wrong number of arguments for 'geoadd' command"));
        }
        Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
        for (int i=0; i<args.length / 3; i+=3) {
            byte[] member = args[i];
            GeoCoordinate geoCoordinate = new GeoCoordinate(Utils.bytesToDouble(args[i+1]), Utils.bytesToDouble(args[i+2]));
            memberCoordinateMap.put(member, geoCoordinate);
        }
        Response<Long> response = pipelined.geoadd(key, memberCoordinateMap);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse geopos(byte[] key, byte[][] members) {
        if (members == null || members.length == 0) {
            return new PipelineResponse(new ErrorReply("wrong number of arguments for 'geopos' command"));
        }
        Response<List<GeoCoordinate>> response = pipelined.geopos(key, members);
        return new PipelineResponse(() -> {
            List<GeoCoordinate> geopos = response.get();
            return ParamUtils.geoList2MultiBulkReply(geopos);
        });
    }

    @Override
    public PipelineResponse geodist(byte[] key, byte[] member1, byte[] member2, byte[] unit) {
        if (unit == null || unit.length == 0) {
            Response<Double> response = pipelined.geodist(key, member1, member2);
            return new PipelineResponse<>(response, r -> new BulkReply(Utils.doubleToBytes(response.get())));
        } else {
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            Response<Double> response = pipelined.geodist(key, member1, member2, targetUnit);
            return new PipelineResponse<>(response, r -> new BulkReply(Utils.doubleToBytes(response.get())));
        }
    }

    @Override
    public PipelineResponse georadius(byte[] key, byte[] longtitude, byte[] latitude, byte[] radius, byte[] unit, byte[][] args) {
        ParamUtils.GeoRadiusParams geoRadiusParams;
        Response<List<GeoRadiusResponse>> response;
        if (args == null || args.length == 0) {
            geoRadiusParams = new ParamUtils.GeoRadiusParams();
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            response = pipelined.georadius(key, Utils.bytesToDouble(longtitude),
                    Utils.bytesToDouble(latitude), Utils.bytesToDouble(radius), targetUnit);
        } else {
            geoRadiusParams = ParamUtils.parseGeoRadiusParams(args);
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            response = pipelined.georadius(key, Utils.bytesToDouble(longtitude),
                    Utils.bytesToDouble(latitude), Utils.bytesToDouble(radius), targetUnit, geoRadiusParams.param);
        }
        return new PipelineResponse(() -> {
            List<GeoRadiusResponse> list = response.get();
            return ParamUtils.geoRadiusList(list, geoRadiusParams);
        });
    }

    @Override
    public PipelineResponse georadiusbymember(byte[] key, byte[] member, byte[] radius, byte[] unit, byte[][] args) {
        ParamUtils.GeoRadiusParams geoRadiusParams;
        Response<List<GeoRadiusResponse>> response;
        if (args == null || args.length == 0) {
            geoRadiusParams = new ParamUtils.GeoRadiusParams();
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            response = pipelined.georadiusByMember(key, member, Utils.bytesToDouble(radius), targetUnit);
        } else {
            geoRadiusParams = ParamUtils.parseGeoRadiusParams(args);
            GeoUnit targetUnit = ParamUtils.parseGeoUnit(unit);
            response = pipelined.georadiusByMember(key, member, Utils.bytesToDouble(radius), targetUnit, geoRadiusParams.param);
        }
        return new PipelineResponse(() -> {
            List<GeoRadiusResponse> list = response.get();
            return ParamUtils.geoRadiusList(list, geoRadiusParams);
        });
    }

    @Override
    public PipelineResponse geohash(byte[] key, byte[][] members) {
        Response<List<byte[]>> response = pipelined.geohash(key, members);
        return new PipelineResponse(() -> {
            List<byte[]> list = response.get();
            return ParamUtils.collection2MultiBulkReply(list);
        });
    }

    @Override
    public PipelineResponse setbit(byte[] key, byte[] offset, byte[] value) {
        Response<Boolean> response = pipelined.setbit(key, Utils.bytesToNum(offset), value);
        return new PipelineResponse<>(response, r -> r.get() ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0);
    }

    @Override
    public PipelineResponse getbit(byte[] key, byte[] offset) {
        Response<Boolean> response = pipelined.getbit(key, Utils.bytesToNum(offset));
        return new PipelineResponse<>(response, r -> r.get() ? IntegerReply.REPLY_1 : IntegerReply.REPLY_0);
    }

    @Override
    public PipelineResponse bitcount(byte[] key, byte[][] args) {
        if (args == null || args.length == 0) {
            Response<Long> response = pipelined.bitcount(key);
            return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
        } else if (args.length == 2) {
            long start = Utils.bytesToNum(args[0]);
            long end = Utils.bytesToNum(args[1]);
            Response<Long> response = pipelined.bitcount(key, start, end);
            return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
        } else {
            throw Utils.illegalArgumentException();
        }
    }

    @Override
    public PipelineResponse bitfield(byte[] key, byte[][] args) {
        Response<List<Long>> response = pipelined.bitfield(key, args);
        return new PipelineResponse(() -> {
            List<Long> bitfield = response.get();
            if (bitfield == null) return BulkReply.NIL_REPLY;
            Reply[] replies = new Reply[bitfield.size()];
            int index = 0;
            for (Long bit : bitfield) {
                replies[index] = new IntegerReply(bit);
                index ++;
            }
            return new MultiBulkReply(replies);
        });
    }

    @Override
    public PipelineResponse bitpos(byte[] key, byte[] bit, byte[][] args) {
        long bitValue = Utils.bytesToNum(bit);
        if (bitValue != 0 && bitValue != 1) {
            throw new IllegalArgumentException(Utils.syntaxError);
        }
        BitPosParams params = ParamUtils.bitposParam(args);
        Response<Long> response = pipelined.bitpos(key, bitValue == 1, params);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    public PipelineResponse expire(byte[] key, byte[] seconds) {
        Response<Long> response = pipelined.expire(key, (int) Utils.bytesToNum(seconds));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    public PipelineResponse pexpire(byte[] key, byte[] seconds) {
        Response<Long> response = pipelined.pexpire(key, Utils.bytesToNum(seconds));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    public PipelineResponse expireat(byte[] key, byte[] timestamp) {
        Response<Long> response = pipelined.expireAt(key, Utils.bytesToNum(timestamp));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    public PipelineResponse pexpireat(byte[] key, byte[] timestamp) {
        Response<Long> response = pipelined.pexpireAt(key, Utils.bytesToNum(timestamp));
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    public PipelineResponse ping() {
        return new PipelineResponse(StatusReply.PONG);
    }

    public PipelineResponse echo(byte[] echo) {
        Response<byte[]> response = pipelined.echo(echo);
        return new PipelineResponse<>(response, r -> new BulkReply(r.get()));
    }

    public PipelineResponse del(byte[][] keys) {
        List<Response<Long>> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            Response<Long> response = pipelined.del(key);
            list.add(response);
        }
        return new PipelineResponse<>(() -> {
            Long ret = 0L;
            for (Response<Long> response : list) {
                ret += response.get();
            }
            return new IntegerReply(ret);
        });
    }

    @Override
    public PipelineResponse exists(byte[][] keys) {
        List<Response<Boolean>> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            Response<Boolean> response = pipelined.exists(key);
            list.add(response);
        }
        return new PipelineResponse<>(() -> {
            Long ret = 0L;
            for (Response<Boolean> response : list) {
                ret += response.get() ? 1L : 0L;
            }
            return new IntegerReply(ret);
        });
    }

    @Override
    public PipelineResponse type(byte[] key) {
        Response<String> response = pipelined.type(key);
        return new PipelineResponse<>(response, r -> new StatusReply(r.get()));
    }

    @Override
    public PipelineResponse sort(byte[] key, byte[][] args) {
        SortingParams sortingParams = ParamUtils.sortingParams(args);
        Response<List<byte[]>> response = pipelined.sort(key, sortingParams);
        return new PipelineResponse(() -> ParamUtils.collection2MultiBulkReply(response.get()));
    }

    @Override
    public PipelineResponse ttl(byte[] key) {
        Response<Long> response = pipelined.ttl(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse pttl(byte[] key) {
        Response<Long> response = pipelined.pttl(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }

    @Override
    public PipelineResponse persist(byte[] key) {
        Response<Long> response = pipelined.persist(key);
        return new PipelineResponse<>(response, r -> new IntegerReply(r.get()));
    }
}

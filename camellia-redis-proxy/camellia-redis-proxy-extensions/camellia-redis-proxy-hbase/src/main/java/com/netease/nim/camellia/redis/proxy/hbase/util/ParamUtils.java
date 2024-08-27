package com.netease.nim.camellia.redis.proxy.hbase.util;

import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import redis.clients.jedis.*;

import java.util.Collection;

/**
 *
 * Created by caojiajun on 2019/12/31.
 */
public class ParamUtils {

    public static MultiBulkReply collection2MultiBulkReply(Collection<byte[]> collection) {
        if (collection == null || collection.isEmpty()) return MultiBulkReply.EMPTY;
        Reply[] replies = new Reply[collection.size()];
        int index = 0;
        for (byte[] bytes : collection) {
            replies[index] = new BulkReply(bytes);
            index ++;
        }
        return new MultiBulkReply(replies);
    }

    public static MultiBulkReply tuples2MultiBulkReply(Collection<Tuple> tuples) {
        if (tuples == null || tuples.isEmpty()) return MultiBulkReply.EMPTY;
        Reply[] replies = new Reply[tuples.size() * 2];
        int index = 0;
        for (Tuple tuple : tuples) {
            replies[index] = new BulkReply(tuple.getBinaryElement());
            index ++;
            replies[index] = new BulkReply(Utils.doubleToBytes(tuple.getScore()));
            index ++;
        }
        return new MultiBulkReply(replies);
    }

    public static ZRangeParams parseZRangeParams(byte[][] args) {
        ZRangeParams params = new ZRangeParams();
        if (args == null || args.length == 0) {
            return params;
        } else if (args.length == 1) {
            boolean withScores = Utils.checkStringIgnoreCase(args[0], RedisKeyword.WITHSCORES.name());
            if (!withScores) {
                throw Utils.illegalArgumentException();
            }
            params.withScores = true;
            return params;
        } else if (args.length == 3) {
            boolean limit = Utils.checkStringIgnoreCase(args[0], RedisKeyword.LIMIT.name());
            if (!limit) {
                throw Utils.illegalArgumentException();
            }
            int offset = (int) Utils.bytesToNum(args[1]);
            int count = (int) Utils.bytesToNum(args[2]);
            params.withLimit = true;
            params.offset = offset;
            params.count = count;
            return params;
        } else if (args.length == 4) {
            boolean withScores = Utils.checkStringIgnoreCase(args[0], RedisKeyword.WITHSCORES.name());
            int offset;
            int count;
            if (withScores) {
                boolean limit = Utils.checkStringIgnoreCase(args[1], RedisKeyword.LIMIT.name());
                if (!limit) {
                    throw Utils.illegalArgumentException();
                }
                offset = (int) Utils.bytesToNum(args[2]);
                count = (int) Utils.bytesToNum(args[3]);
            } else {
                boolean limit = Utils.checkStringIgnoreCase(args[0], RedisKeyword.LIMIT.name());
                if (!limit) {
                    throw Utils.illegalArgumentException();
                }
                offset = (int) Utils.bytesToNum(args[1]);
                count = (int) Utils.bytesToNum(args[2]);
                withScores = Utils.checkStringIgnoreCase(args[3], RedisKeyword.WITHSCORES.name());
                if (!withScores) {
                    throw Utils.illegalArgumentException();
                }
            }
            params.withScores = true;
            params.withLimit = true;
            params.offset = offset;
            params.count = count;
            return params;
        }
        throw Utils.illegalArgumentException();
    }


    public static class ZRangeParams {
        public boolean withScores = false;
        public boolean withLimit = false;
        public Integer offset = null;
        public Integer count = null;
    }

}

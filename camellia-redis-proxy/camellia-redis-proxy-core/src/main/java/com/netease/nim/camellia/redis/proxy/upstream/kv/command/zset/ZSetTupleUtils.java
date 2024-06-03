package com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset;

import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.ZSet;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.List;

/**
 * Created by caojiajun on 2024/5/14
 */
public class ZSetTupleUtils {

    public static MultiBulkReply toReply(List<ZSetTuple> list, boolean withScores) {
        if (list == null) {
            return MultiBulkReply.NIL_REPLY;
        }
        if (list.isEmpty()) {
            return MultiBulkReply.EMPTY;
        }
        Reply[] replies;
        if (withScores) {
            replies = new Reply[list.size()*2];
            int i = 0;
            for (ZSetTuple tuple : list) {
                replies[i] = new BulkReply(tuple.getMember().getKey());
                replies[i+1] = new BulkReply(tuple.getScore().getKey());
                i+=2;
            }
        } else {
            replies = new Reply[list.size()];
            int i = 0;
            for (ZSetTuple tuple : list) {
                replies[i] = new BulkReply(tuple.getMember().getKey());
                i+=1;
            }
        }
        return new MultiBulkReply(replies);
    }

    public static MultiBulkReply toReplyOfMember(List<ZSet.Member> list, boolean withScores) {
        if (list == null) {
            return MultiBulkReply.NIL_REPLY;
        }
        if (list.isEmpty()) {
            return MultiBulkReply.EMPTY;
        }
        Reply[] replies;
        if (withScores) {
            replies = new Reply[list.size()*2];
            int i = 0;
            for (ZSet.Member member : list) {
                replies[i] = new BulkReply(member.getMember().getKey());
                replies[i+1] = new BulkReply(Utils.doubleToBytes(member.getScore()));
                i+=2;
            }
        } else {
            replies = new Reply[list.size()];
            int i = 0;
            for (ZSet.Member member : list) {
                replies[i] = new BulkReply(member.getMember().getKey());
                i+=1;
            }
        }
        return new MultiBulkReply(replies);
    }

}

package com.netease.nim.camellia.redis.proxy.upstream.kv.utils;

import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2024/6/11
 */
public class ScriptReplyUtils {

    public static Reply check(Reply reply) {
        if (reply instanceof ErrorReply) {
            return reply;
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies[0] instanceof BulkReply) {
                byte[] raw = ((BulkReply) replies[0]).getRaw();
                if (Utils.bytesToString(raw).equalsIgnoreCase("1")) {
                    return replies[1];
                }
            }
        }
        return null;
    }
}

package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.MultiBulkHeaderReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Created by caojiajun on 2022/11/2
 */
public class ReplyAggregateDecoder extends MessageToMessageDecoder<Reply> {

    private final Deque<MultiReplyAggregate> depths = new ArrayDeque<>(4);

    @Override
    protected void decode(ChannelHandlerContext ctx, Reply msg, List<Object> out) {
        if (msg instanceof MultiBulkHeaderReply) {
            depths.push(new MultiReplyAggregate(((MultiBulkHeaderReply) msg).getSize()));
            return;
        }
        while (!depths.isEmpty()) {
            MultiReplyAggregate current = depths.peek();
            current.addChild(msg);
            if (current.ok()) {
                msg = new MultiBulkReply(current.children);
                depths.pop();
            } else {
                return;
            }
        }

        out.add(msg);
    }

    private static final class MultiReplyAggregate {
        private final Reply[] children;
        private int index = 0;
        MultiReplyAggregate(int length) {
            this.children = new Reply[length];
        }

        public void addChild(Reply reply) {
            children[index] = reply;
            index ++;
        }

        public boolean ok() {
            return index == children.length;
        }
    }
}

package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReplyHeader;
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

    private static final int cacheSize = 128;

    private final Deque<MultiReplyAggregate> depths = new ArrayDeque<>(4);

    private final MultiReplyAggregate[] cache = new MultiReplyAggregate[cacheSize];
    private int index = -1;
    {
        for (int i=0; i<cacheSize; i++) {
            cache[i] = new MultiReplyAggregate();
        }
    }

    private MultiReplyAggregate getAggregate(int len) {
        if (index == cacheSize - 1) {
            MultiReplyAggregate aggregate = cache[0];
            if (aggregate.isEmpty()) {
                aggregate.reset(len);
                index = 0;
                return aggregate;
            }
        } else {
            MultiReplyAggregate aggregate = cache[index + 1];
            if (aggregate.isEmpty()) {
                aggregate.reset(len);
                index = index + 1;
                return aggregate;
            }
        }
        return new MultiReplyAggregate(len);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Reply msg, List<Object> out) {
        if (msg instanceof MultiBulkReplyHeader) {
            depths.push(getAggregate(((MultiBulkReplyHeader) msg).getSize()));
            return;
        }
        while (!depths.isEmpty()) {
            MultiReplyAggregate current = depths.peek();
            current.addChild(msg);
            if (current.complete()) {
                msg = new MultiBulkReply(current.children);
                depths.pop();
                current.clear();
            } else {
                return;
            }
        }

        out.add(msg);
    }

    private static final class MultiReplyAggregate {
        private Reply[] children = null;
        private int index = 0;
        MultiReplyAggregate(int length) {
            this.children = new Reply[length];
        }

        public MultiReplyAggregate() {
        }

        public void addChild(Reply reply) {
            children[index] = reply;
            index ++;
        }

        public boolean isEmpty() {
            return index == 0 && children == null;
        }

        public void clear() {
            index = 0;
            children = null;
        }

        public void reset(int length) {
            children = new Reply[length];
            index = 0;
        }

        public boolean complete() {
            return index == children.length;
        }
    }
}

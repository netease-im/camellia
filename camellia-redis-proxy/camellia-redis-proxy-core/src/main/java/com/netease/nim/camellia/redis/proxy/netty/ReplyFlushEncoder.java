package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2025/2/14
 */
public class ReplyFlushEncoder extends MessageToMessageEncoder<Reply> {

    public static final AttributeKey<AtomicLong> ATTRIBUTE_KEY = AttributeKey.valueOf("BATCH");

    public static void init(ChannelHandlerContext ctx) {
        if (ReplyBatchFlushUtils.enable()) {
            ctx.channel().attr(ReplyFlushEncoder.ATTRIBUTE_KEY).set(new AtomicLong());
        }
    }

    public static void commandReceive(ChannelHandlerContext ctx, int count) {
        AtomicLong counter = ctx.channel().attr(ATTRIBUTE_KEY).get();
        if (counter != null) {
            counter.addAndGet(count);
        }
    }

    public static void reset(ChannelHandlerContext ctx) {
        AtomicLong counter = ctx.channel().attr(ATTRIBUTE_KEY).get();
        if (counter != null) {
            counter.set(0);
        }
    }

    private ByteBuf buf;
    private int batch = 0;

    @Override
    protected void encode(ChannelHandlerContext ctx, Reply reply, List<Object> list) throws Exception {
        if (buf == null) {
            buf = ctx.channel().alloc().ioBuffer();
        }
        reply.write(buf);
        if (needFlush(ctx)) {
            buf.capacity(buf.readableBytes());
            list.add(buf);
            buf = null;
            batch = 0;
        }
    }

    private boolean needFlush(ChannelHandlerContext ctx) {
        AtomicLong counter = ctx.channel().attr(ATTRIBUTE_KEY).get();
        if (counter == null) {
            return true;
        }
        long leave = counter.decrementAndGet();
        batch ++;
        if (batch >= ReplyBatchFlushUtils.getThreshold()) {
            return true;
        }
        if (leave < 0) {
            counter.set(0);
        }
        return leave <= 0;
    }
}

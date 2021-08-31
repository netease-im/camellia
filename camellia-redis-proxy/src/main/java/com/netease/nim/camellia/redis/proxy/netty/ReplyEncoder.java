package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.ReplyPack;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Write a reply.
 */
public class ReplyEncoder extends MessageToByteEncoder<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ReplyEncoder.class);

    private long id = 0;
    private final Map<Long, ReplyPack> packMap = new HashMap<>();

    public ReplyEncoder() {
        super();
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Object object, ByteBuf out) throws Exception {
        if (object instanceof ReplyPack) {
            ReplyPack pack = (ReplyPack) object;
            if (ctx.channel().isActive()) {
                if (RedisMonitor.isMonitorEnable()) {
                    if (pack.getReply() instanceof ErrorReply) {
                        RedisMonitor.incrFail(((ErrorReply) pack.getReply()).getError());
                    }
                }
                //avoid out of order
                long id = pack.getId();
                if (id == Long.MAX_VALUE) {
                    //if 10w qps in one connect, after 2924712 year, connect will be force closed
                    logger.error("reply pack id exceed to Long.MAX_VALUE, connect close");
                    ctx.close();
                    return;
                }
                if (this.id == id - 1) {
                    this.id = id;
                    Reply reply = pack.getReply();
                    reply.write(out);
                    while (!packMap.isEmpty()) {
                        ReplyPack replyPack = packMap.get(this.id + 1);
                        if (replyPack != null) {
                            this.id = replyPack.getId();
                            replyPack.getReply().write(out);
                            packMap.remove(replyPack.getId());
                        } else {
                            break;
                        }
                    }
                } else {
                    packMap.put(id, pack);
                }
            } else {
                if (RedisMonitor.isMonitorEnable()) {
                    RedisMonitor.incrFail("ChannelNotActive");
                }
                ErrorLogCollector.collect(ReplyEncoder.class, "channel not active, remote.ip=" + ctx.channel().remoteAddress());
            }
        } else if (object instanceof Reply) {
            if (ctx.channel().isActive()) {
                Reply reply = (Reply) object;
                if (RedisMonitor.isMonitorEnable()) {
                    if (reply instanceof ErrorReply) {
                        RedisMonitor.incrFail(((ErrorReply) reply).getError());
                    }
                }
                reply.write(out);
            } else {
                if (RedisMonitor.isMonitorEnable()) {
                    RedisMonitor.incrFail("ChannelNotActive");
                }
                ErrorLogCollector.collect(ReplyEncoder.class, "channel not active, remote.ip=" + ctx.channel().remoteAddress());
            }
        }
    }
}

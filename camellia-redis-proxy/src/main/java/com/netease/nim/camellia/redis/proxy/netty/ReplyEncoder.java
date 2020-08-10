package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Write a reply.
 */
public class ReplyEncoder extends MessageToByteEncoder<Reply> {

    private final CamelliaServerProperties serverProperties;

    public ReplyEncoder(CamelliaServerProperties serverProperties) {
        super();
        this.serverProperties = serverProperties;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Reply msg, ByteBuf out) throws Exception {
        if (ctx.channel().isActive()) {
            if (serverProperties.isMonitorEnable()) {
                if (msg instanceof ErrorReply) {
                    RedisMonitor.incrFail(((ErrorReply) msg).getError());
                }
            }
            msg.write(out);
        } else {
            if (serverProperties.isMonitorEnable()) {
                RedisMonitor.incrFail("ChannelNotActive");
            }
            ErrorLogCollector.collect(ReplyEncoder.class, "channel not active, remote.ip=" + ctx.channel().remoteAddress());
        }
    }
}

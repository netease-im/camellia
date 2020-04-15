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

    private CamelliaServerProperties serverProperties;

    public ReplyEncoder(CamelliaServerProperties serverProperties) {
        super();
        this.serverProperties = serverProperties;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, Reply msg, ByteBuf out) throws Exception {
        if (ctx.channel().isWritable()) {
            if (serverProperties.isMonitorEnable()) {
                if (msg instanceof ErrorReply) {
                    RedisMonitor.incrFail(((ErrorReply) msg).getError());
                }
            }
            msg.write(out);
        } else {
            if (serverProperties.isMonitorEnable()) {
                RedisMonitor.incrFail("ChannelNotWriteable");
            }
            ErrorLogCollector.collect(ReplyEncoder.class, "channel not writeable");
        }
    }
}

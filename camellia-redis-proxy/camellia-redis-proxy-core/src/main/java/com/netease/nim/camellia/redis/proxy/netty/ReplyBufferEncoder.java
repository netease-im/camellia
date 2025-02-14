package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.ReplyPack;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2025/2/14
 */
public class ReplyBufferEncoder extends MessageToMessageEncoder<Object> {
    private static final Logger logger = LoggerFactory.getLogger(ReplyEncoder.class);

    private long id = 0;
    private final Map<Long, ReplyPack> packMap = new HashMap<>();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object object, List<Object> list) throws Exception {
        if (object instanceof ReplyPack pack) {
            if (ctx.channel().isActive()) {
                if (ProxyMonitorCollector.isMonitorEnable()) {
                    if (pack.getReply() instanceof ErrorReply) {
                        CommandFailMonitor.incr(((ErrorReply) pack.getReply()).getError());
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
                    list.add(reply);
                    while (!packMap.isEmpty()) {
                        ReplyPack replyPack = packMap.remove(this.id + 1);
                        if (replyPack != null) {
                            this.id = replyPack.getId();
                            list.add(replyPack.getReply());
                        } else {
                            break;
                        }
                    }
                } else {
                    packMap.put(id, pack);
                }
            } else {
                if (ProxyMonitorCollector.isMonitorEnable()) {
                    CommandFailMonitor.incr("ChannelNotActive");
                }
                ErrorLogCollector.collect(ReplyEncoder.class, "channel not active, remote.ip=" + ctx.channel().remoteAddress());
            }
        } else if (object instanceof Reply reply) {
            if (ctx.channel().isActive()) {
                if (ProxyMonitorCollector.isMonitorEnable()) {
                    if (reply instanceof ErrorReply) {
                        CommandFailMonitor.incr(((ErrorReply) reply).getError());
                    }
                }
                list.add(reply);
            } else {
                if (ProxyMonitorCollector.isMonitorEnable()) {
                    CommandFailMonitor.incr("ChannelNotActive");
                }
                ErrorLogCollector.collect(ReplyEncoder.class, "channel not active, remote.ip=" + ctx.channel().remoteAddress());
            }
        }
    }
}

package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;
import com.netease.nim.camellia.hot.key.common.netty.SeqManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPackClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackClientHandler.class);

    public static String getName() {
        return "HotKeyPackHandler";
    }

    private final SeqManager manager;
    private final HotKeyPackConsumer handler;

    public HotKeyPackClientHandler(SeqManager manager, HotKeyPackConsumer handler) {
        this.manager = manager;
        this.handler = handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HotKeyPack) {
            HotKeyPack pack = (HotKeyPack) msg;
            if (pack.getHeader().isAck()) {
                manager.complete(pack);
            } else {
                handler.onPack(ctx.channel(), pack);
            }
        } else {
            logger.error("unknown pack, type = {}, value = {}", msg.getClass().getName(), msg);
        }
    }
}

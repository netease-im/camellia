package com.netease.nim.camellia.hot.key.common.netty.handler;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPackEncoder extends MessageToMessageEncoder<HotKeyPack> {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackEncoder.class);

    public static String getName() {
        return "HotKeyPackEncoder";
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, HotKeyPack hotKeyPack, List<Object> list) {
        try {
            ByteBuf buf = hotKeyPack.encode();
            list.add(buf);
        } catch (Exception e) {
            logger.error("encode error", e);
        }
    }
}

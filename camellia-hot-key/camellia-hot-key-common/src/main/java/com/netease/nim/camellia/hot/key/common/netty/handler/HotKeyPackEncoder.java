package com.netease.nim.camellia.hot.key.common.netty.handler;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyPack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPackEncoder extends MessageToMessageEncoder<HotKeyPack> {

    public static String getName() {
        return "HotKeyPackEncoder";
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, HotKeyPack hotKeyPack, List<Object> list) {
        ByteBuf buf = hotKeyPack.encode();
        list.add(buf);
    }
}

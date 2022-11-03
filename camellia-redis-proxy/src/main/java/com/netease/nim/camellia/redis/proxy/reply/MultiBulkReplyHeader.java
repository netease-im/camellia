package com.netease.nim.camellia.redis.proxy.reply;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

/**
 * Created by caojiajun on 2022/11/2
 */
public class MultiBulkReplyHeader implements Reply {

    private final int size;

    public MultiBulkReplyHeader(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    @Override
    public void write(ByteBuf byteBuf) throws IOException {
        //do nothing
    }
}

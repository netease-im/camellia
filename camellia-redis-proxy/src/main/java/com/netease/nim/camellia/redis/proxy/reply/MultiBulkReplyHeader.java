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

    private static final int CACHE_SIZE = 2048;
    private static final MultiBulkReplyHeader[] headers = new MultiBulkReplyHeader[CACHE_SIZE];
    static {
        for (int i=0; i<CACHE_SIZE; i++) {
            headers[i] = new MultiBulkReplyHeader(i);
        }
    }
    public static MultiBulkReplyHeader gen(int size) {
        if (size < CACHE_SIZE) {
            return headers[size];
        } else {
            return new MultiBulkReplyHeader(size);
        }
    }
}

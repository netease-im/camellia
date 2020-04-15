package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * Created by caojiajun on 2019/12/17.
 */
public class ReplyDecoder extends ReplayingDecoder<Void> {

    private static final Logger logger = LoggerFactory.getLogger(ReplyDecoder.class);

    private boolean isMultiBulkReply = false;
    private Reply[] replies = null;
    private int index = -1;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            if (!isMultiBulkReply) {
                Reply reply = decodeReply(in, 0);
                checkpoint();
                try {
                    out.add(reply);
                } finally {
                    this.isMultiBulkReply = false;
                    this.replies = null;
                    this.index = -1;
                }
            } else {
                //可能存在大包，使用checkpoint做一下优化
                if (replies == null || index == -1) {
                    logger.warn("will not invoke here");
                    throw new IllegalArgumentException();
                }
                for (int i=index; i<replies.length; i++) {
                    Reply reply = decodeReply(in, 1);
                    this.replies[i] = reply;
                    checkpoint();
                    this.index++;
                }
                try {
                    MultiBulkReply multiBulkReply = new MultiBulkReply(this.replies);
                    out.add(multiBulkReply);
                } finally {
                    this.isMultiBulkReply = false;
                    this.replies = null;
                    this.index = -1;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Reply decodeReply(ByteBuf in, int level) throws Exception {
        byte b = in.readByte();
        Marker marker = Marker.byValue(b);
        if (marker == null) {
            throw new IllegalArgumentException("unknown reply marker");
        }
        switch (marker) {
            case StatusReply:
                String status = readString(in);
                return new StatusReply(status);
            case ErrorReply:
                String error = readString(in);
                return new ErrorReply(error);
            case IntegerReply:
                long l = Utils.readLong(in);
                return new IntegerReply(l);
            case BulkReply:
                int num = (int) Utils.readLong(in);
                if (num == -1) {
                    return BulkReply.NIL_REPLY;
                } else {
                    byte[] bulk = new byte[num];
                    in.readBytes(bulk);
                    in.skipBytes(2);
                    return new BulkReply(bulk);
                }
            case MultiBulkReply://可能存在大包，使用checkpoint做一下优化
                int size = (int) Utils.readLong(in);
                if (size == -1) {
                    return MultiBulkReply.EMPTY;
                } else {
                    if (level == 0) {
                        checkpoint();
                        this.isMultiBulkReply = true;
                        this.replies = new Reply[size];
                        this.index = 0;
                        for (int i = index; i < size; i++) {
                            Reply reply = decodeReply(in, level + 1);
                            this.replies[i] = reply;
                            checkpoint();
                            this.index++;
                        }
                        return new MultiBulkReply(this.replies);
                    } else {
                        Reply[] replies = new Reply[size];
                        for (int i = 0; i < size; i++) {
                            Reply reply = decodeReply(in, level + 1);
                            replies[i] = reply;
                        }
                        return new MultiBulkReply(replies);
                    }
                }
            default:
                throw new IllegalArgumentException("not reply support marker");
        }
    }

    private String readString(ByteBuf in) {
        StringBuilder builder = new StringBuilder();
        byte s = in.readByte();
        while (s != Utils.CR) {
            builder.append((char) s);
            s = in.readByte();
        }
        in.skipBytes(1);
        return builder.toString();
    }

}

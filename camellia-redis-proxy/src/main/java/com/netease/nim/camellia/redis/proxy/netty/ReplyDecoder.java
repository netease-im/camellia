package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.reply.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by caojiajun on 2022/11/2
 */
public class ReplyDecoder extends ByteToMessageDecoder {

    private Marker marker;
    private int bulkSize = Integer.MIN_VALUE;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (true) {
            if (marker == null) {
                if (in.readableBytes() > 1) {
                    byte b = in.readByte();
                    this.marker = Marker.byValue(b);
                } else {
                    return;
                }
            }
            if (in.readableBytes() > 0) {
                int readerIndex = in.readerIndex();
                if (marker == Marker.StatusReply) {
                    ByteBuf byteBuf = readLine(in);
                    if (byteBuf == null) {
                        in.readerIndex(readerIndex);
                        return;
                    }
                    CharSequence charSequence = byteBuf.readCharSequence(byteBuf.readableBytes(), StandardCharsets.UTF_8);
                    StatusReply reply = new StatusReply(charSequence.toString());
                    out.add(reply);
                    marker = null;
                } else if (marker == Marker.BulkReply) {
                    if (bulkSize == Integer.MIN_VALUE) {
                        ByteBuf byteBuf = readLine(in);
                        if (byteBuf == null) {
                            in.readerIndex(readerIndex);
                            return;
                        }
                        bulkSize = (int) parseRedisNumber(byteBuf);
                        if (bulkSize == -1) {
                            out.add(BulkReply.NIL_REPLY);
                            marker = null;
                            bulkSize = Integer.MIN_VALUE;
                            continue;
                        }
                        readerIndex = in.readerIndex();
                    }
                    if (in.readableBytes() >= bulkSize + 2) {
                        byte[] data = new byte[bulkSize];
                        in.readBytes(data);
                        in.skipBytes(2);
                        out.add(new BulkReply(data));
                        marker = null;
                        bulkSize = Integer.MIN_VALUE;
                    } else {
                        in.readerIndex(readerIndex);
                        return;
                    }
                } else if (marker == Marker.IntegerReply) {
                    ByteBuf byteBuf = readLine(in);
                    if (byteBuf == null) {
                        in.readerIndex(readerIndex);
                        return;
                    }
                    long l = parseRedisNumber(byteBuf);
                    out.add(new IntegerReply(l));
                    marker = null;
                } else if (marker == Marker.MultiBulkReply) {
                    ByteBuf byteBuf = readLine(in);
                    if (byteBuf == null) {
                        in.readerIndex(readerIndex);
                        return;
                    }
                    long l = parseRedisNumber(byteBuf);
                    if (l == -1) {
                        out.add(MultiBulkReply.NIL_REPLY);
                    } else if (l == 0) {
                        out.add(MultiBulkReply.EMPTY);
                    } else {
                        out.add(new MultiBulkHeaderReply((int) l));
                    }
                    marker = null;
                } else if (marker == Marker.ErrorReply) {
                    ByteBuf byteBuf = readLine(in);
                    if (byteBuf == null) {
                        in.readerIndex(readerIndex);
                        return;
                    }
                    CharSequence charSequence = byteBuf.readCharSequence(byteBuf.readableBytes(), StandardCharsets.UTF_8);
                    ErrorReply reply = new ErrorReply(charSequence.toString());
                    out.add(reply);
                    marker = null;
                }
            } else {
                return;
            }
        }
    }

    private static final int POSITIVE_LONG_MAX_LENGTH = 19; // length of Long.MAX_VALUE
    private static final int EOL_LENGTH = 2;

    private final NumberProcessor numberProcessor = new NumberProcessor();

    private long parseRedisNumber(ByteBuf byteBuf) {
        final int readableBytes = byteBuf.readableBytes();
        final boolean negative = readableBytes > 0 && byteBuf.getByte(byteBuf.readerIndex()) == '-';
        final int extraOneByteForNegative = negative ? 1 : 0;
        if (readableBytes <= extraOneByteForNegative) {
            throw new IllegalArgumentException("no number to parse: " + byteBuf.toString(CharsetUtil.US_ASCII));
        }
        if (readableBytes > POSITIVE_LONG_MAX_LENGTH + extraOneByteForNegative) {
            throw new IllegalArgumentException("too many characters to be a valid RESP Integer: " +
                    byteBuf.toString(CharsetUtil.US_ASCII));
        }
        if (negative) {
            numberProcessor.reset();
            byteBuf.skipBytes(extraOneByteForNegative);
            byteBuf.forEachByte(numberProcessor);
            return -1 * numberProcessor.content();
        }
        numberProcessor.reset();
        byteBuf.forEachByte(numberProcessor);
        return numberProcessor.content();
    }

    private static final class NumberProcessor implements ByteProcessor {
        private long result;
        @Override
        public boolean process(byte value) {
            if (value < '0' || value > '9') {
                throw new IllegalArgumentException("bad byte in number: " + value);
            }
            result = result * 10 + (value - '0');
            return true;
        }
        public long content() {
            return result;
        }
        public void reset() {
            result = 0;
        }
    }

    private static ByteBuf readLine(ByteBuf in) {
        if (!in.isReadable(EOL_LENGTH)) {
            return null;
        }
        final int lfIndex = in.forEachByte(ByteProcessor.FIND_LF);
        if (lfIndex < 0) {
            return null;
        }
        ByteBuf data = in.readSlice(lfIndex - in.readerIndex() - 1); // `-1` is for CR
        in.skipBytes(2);
        return data;
    }
}

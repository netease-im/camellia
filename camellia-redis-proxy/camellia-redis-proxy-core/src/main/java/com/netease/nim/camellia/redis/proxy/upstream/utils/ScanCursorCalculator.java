package com.netease.nim.camellia.redis.proxy.upstream.utils;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2022/1/14
 */
public class ScanCursorCalculator {

    //用于描述nodes的index的bit位数
    private final int nodeBitLen;

    //用于描述real cursor的bit位数，最高位永远是0
    private final int realCursorBitLen;

    //用于计算node index的mask
    //例子：如果nodeBitLen=10，则nodeIndexMask  = 0111111111100000 0000000000000000 0000000000000000 0000000000000000
    private final long nodeIndexMask;

    //用于计算real cursor的mask
    //例子：如果nodeBitLen=10，则realCursorMask = 0000000000011111 1111111111111111 1111111111111111 1111111111111111
    private final long realCursorMask;

    public ScanCursorCalculator(int nodeBitLen) {
        this.nodeBitLen = nodeBitLen;
        this.realCursorBitLen = 63 - nodeBitLen;
        this.realCursorMask = ((1L << realCursorBitLen) - 1) & Long.MAX_VALUE;
        this.nodeIndexMask = ((1L << nodeBitLen) - 1) << realCursorBitLen;
    }

    public static int getSuitableNodeBitLen(int nodeSize) {
        if (nodeSize <= 2) {
            return 1;
        } else if (nodeSize <= 4) {
            return 2;
        } else if (nodeSize <= 8) {
            return 3;
        } else if (nodeSize <= 16) {
            return 4;
        } else {
            double d = Math.log(nodeSize) / Math.log(2);
            return (int) Math.ceil(d);
        }
    }

    public int getNodeBitLen() {
        return nodeBitLen;
    }

    public int getRealCursorBitLen() {
        return realCursorBitLen;
    }

    public long getNodeIndexMask() {
        return nodeIndexMask;
    }

    public long getRealCursorMask() {
        return realCursorMask;
    }

    public int parseNodeIndex(long cursor) {
        return (int) ((cursor & nodeIndexMask) >> realCursorBitLen);
    }

    public long parseRealCursor(long cursor) {
        return cursor & realCursorMask;
    }

    public int filterScanCommand(Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length <= 1) return -1;
        long requestCursor = Utils.bytesToNum(objects[1]);
        int nodeIndex;
        long realCursor;
        if (requestCursor == 0) {
            nodeIndex = 0;
            realCursor = 0;
        } else {
            nodeIndex = parseNodeIndex(requestCursor);
            realCursor = parseRealCursor(requestCursor);
        }
        // rewrite real requestCursor
        objects[1] = Utils.stringToBytes(String.valueOf(realCursor));
        return nodeIndex;
    }

    public Reply filterScanReply(Reply reply, int currentNodeIndex, int nodeSize) {
        if (reply instanceof MultiBulkReply multiBulkReply) {
            long newNodeIndex;
            long newCursor;
            if (multiBulkReply.getReplies().length == 2) {
                BulkReply cursorReply = (BulkReply) multiBulkReply.getReplies()[0];
                long replyCursor = Utils.bytesToNum(cursorReply.getRaw());
                if (replyCursor == 0L) {
                    if (currentNodeIndex < (nodeSize - 1)) {
                        newNodeIndex = currentNodeIndex + 1;
                    } else {
                        newNodeIndex = 0L;
                    }
                    newCursor = 0L;
                } else {
                    newCursor = replyCursor;
                    newNodeIndex = currentNodeIndex;
                }
                if (newCursor > realCursorMask) {
                    return new ErrorReply("Redis requestCursor is larger than " + realCursorMask + " is not supported for cluster mode.");
                }
                multiBulkReply.getReplies()[0] = new BulkReply(Utils.stringToBytes(String.valueOf(newNodeIndex << realCursorBitLen | newCursor)));
            }
        }
        return reply;
    }
}

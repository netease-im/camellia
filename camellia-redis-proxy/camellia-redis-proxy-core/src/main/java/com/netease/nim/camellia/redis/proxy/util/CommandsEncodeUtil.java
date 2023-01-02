package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.command.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.List;


/**
 *
 * Created by caojiajun on 2019/12/17.
 */
public class CommandsEncodeUtil {

    private static final byte[] CRLF = new byte[] {
            '\r', '\n'
    };

    private final static int[] SIZE_TABLE = {
            9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE
    };

    // Requires positive x
    private static int stringSize(int x) {
        for (int i = 0;; i++) {
            if (x <= SIZE_TABLE[i]) {
                return i + 1;
            }
        }
    }

    private static int paramCountSize(int paramCount) {
        // * + paramCount + CRLF
        return 1 + stringSize(paramCount) + 2;
    }

    private static int paramSize(byte[] param) {
        // $ + paramLength + CRLF + param + CRLF
        return 1 + stringSize(param.length) + 2 + param.length + 2;
    }

    private static int serializedSize(byte[]...args) {
        int size = paramCountSize(args.length);
        for (byte[] arg : args) {
            size += paramSize(arg);
        }
        return size;
    }

    public static ByteBuf encode(ByteBufAllocator alloc, byte[]... args) {
        int serializedSize = serializedSize(args);
        ByteBuf buf = alloc.ioBuffer(serializedSize, serializedSize);
        writeParamCount(buf, args.length);
        for (byte[] arg : args) {
            writeParam(buf, arg);
        }
        return buf;
    }

    public static ByteBuf encode(ByteBufAllocator alloc, List<Command> commands) {
        int serializedSize = 0;
        for (Command command : commands) {
            serializedSize += serializedSize(command.getObjects());
        }
        ByteBuf buf = alloc.ioBuffer(serializedSize, serializedSize);
        for (Command command : commands) {
            byte[][] args = command.getObjects();
            writeParamCount(buf, args.length);
            for (byte[] arg : args) {
                writeParam(buf, arg);
            }
        }
        return buf;
    }

    private static void writeParamCount(ByteBuf buf, int paramCount) {
        buf.writeByte('*').writeBytes(toBytes(paramCount)).writeBytes(CRLF);
    }

    private static void writeParam(ByteBuf buf, byte[] param) {
        buf.writeByte('$').writeBytes(toBytes(param.length)).writeBytes(CRLF).writeBytes(param)
                .writeBytes(CRLF);
    }

    public static byte[] toBytes(double value) {
        return toBytes(Double.toString(value));
    }

    public static byte[] toBytes(int value) {
        return toBytes(Integer.toString(value));
    }

    public static byte[] toBytes(long value) {
        return toBytes(Long.toString(value));
    }

    public static byte[] toBytes(String value) {
        return value.getBytes(Utils.utf8Charset);
    }
}

package com.netease.nim.camellia.hot.key.common.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;

public class Unpack {

    private final ByteBuf buffer;
    public static final int VAR_STR_LEN_MAX = 20*1024*1024;

    public Unpack(byte[] bytes, int offset, int length) {
        buffer = Unpooled.wrappedBuffer(bytes, offset, length).order();
    }

    public Unpack(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public Unpack(ByteBuf bb) {
        byte[] bytes = new byte[bb.readableBytes()];
        bb.readBytes(bytes);
        buffer = Unpooled.wrappedBuffer(bytes, 0, bytes.length);
    }

    public int GetSize() {
        return buffer.readableBytes();
    }

    public static Unpack copyOf(Unpack unpack) {
        return new Unpack(Unpooled.copiedBuffer(unpack.getBuffer()));
    }

    public Unpack() {
        buffer = null;
    }

    public ByteBuf getBuffer() {
        return buffer.duplicate();
    }

    public byte[] popFetch(int size) {
        try {
            if (size < 0)
                return null;
            if (size > VAR_STR_LEN_MAX) {
                throw new UnpackException();
            }
            byte[] fetch = new byte[size];
            buffer.readBytes(fetch);
            return fetch;
        } catch (IndexOutOfBoundsException ex) {
            throw new UnpackException(ex);
        }
    }

    public byte popByte() {
        try {
            return buffer.readByte();
        } catch (IndexOutOfBoundsException ex) {
            throw new UnpackException(ex);
        }
    }

    public byte[] popVarbin() {
        return popFetch(Varint.readInt(this));
    }

    public String popVarbin(String encode) {
        try {
            byte[] bytes = popVarbin();
            return new String(bytes, encode);
        } catch (UnsupportedEncodingException | IndexOutOfBoundsException ex) {
            throw new UnpackException(ex);
        }
    }

    public String popVarstr() {
        return popVarbin("utf-8");
    }

    public String popVarstr(String encode) {
        return popVarbin(encode);
    }

    public int popInt() {
        try {
            return buffer.readInt();
        } catch (IndexOutOfBoundsException ex) {
            throw new UnpackException(ex);
        }
    }

    public long popLong() {
        try {
            return buffer.readLong();
        } catch (IndexOutOfBoundsException ex) {
            throw new UnpackException(ex);
        }
    }

    public short popShort() {
        try {
            return buffer.readShort();
        } catch (IndexOutOfBoundsException ex) {
            throw new UnpackException(ex);
        }
    }

    public Marshallable popMarshallable(Marshallable mar) {
        mar.unmarshal(this);
        return mar;
    }

    public boolean popBoolean() {
        return popByte() > 0;
    }

    public String toString() {
        return buffer.toString();
    }

    public int popVarUint() {
        int value = 0;
        int i = 0;
        int b;
        while (((b = buffer.readByte()) & 0x80) != 0) {
            value |= (b & 0x7F) << i;
            i += 7;
            if (i > 35) {
                throw new UnpackException("Variable length quantity is too long");
            }
        }
        return value | (b << i);
    }

}

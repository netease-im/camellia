package com.netease.nim.camellia.hot.key.common.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Pack {

    private final ByteBuf buf;
    private static final int m_maxCapacity = 20 * 1024 * 1024;

    public Pack() {
        this(1024);
    }

    public Pack(int initialSize) {
        buf = Unpooled.buffer(initialSize).order(ByteOrder.LITTLE_ENDIAN);
    }

    public int size() {
        return buf.readableBytes();
    }

    public ByteBuf getBuffer(){
        return this.buf;
    }

    public Pack putBytes(byte[] bytes) {
        try {
            ensureCapacity(bytes.length);
            buf.writeBytes(bytes);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putByte(byte bt) {
        try {
            ensureCapacity(1);
            buf.writeByte(bt);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putVarstr(String str) {
        return putVarbin(str.getBytes(StandardCharsets.UTF_8));
    }

    public Pack putInt(int val) {
        try {
            ensureCapacity(Integer.SIZE / Byte.SIZE);
            buf.writeInt(val);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putBoolean(boolean val) {
        try {
            ensureCapacity(1);
            buf.writeBoolean(val);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putLong(long val) {
        try {
            ensureCapacity(Long.SIZE / Byte.SIZE);
            buf.writeLong(val);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putShort(short val) {
        try {
            ensureCapacity(Short.SIZE / Byte.SIZE);
            buf.writeShort(val);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putVarbin(byte[] bytes) {
        try {
            if (bytes.length > Unpack.VAR_STR_LEN_MAX) {
                throw new PackException();
            }
            int len  = bytes.length;
            int compactLen = Varint.getVarLen(len);
            ensureCapacity(compactLen + bytes.length);
            putVarUint(len);
            buf.writeBytes(bytes);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public Pack putMarshallable(Marshallable mar) {
        mar.marshal(this);
        return this;
    }

    public Pack putBuffer(ByteBuf buf) {
        try {
            ensureCapacity(buf.readableBytes());
            this.buf.writeBytes(buf);
            return this;
        } catch (IndexOutOfBoundsException bex) {
            throw new PackException();
        }
    }

    public void replaceInt(int off, int val) {
        try {
            buf.markWriterIndex()
                    .writerIndex(off)
                    .writeInt(val)
                    .resetWriterIndex();
        } catch (IndexOutOfBoundsException | IllegalArgumentException bex) {
            throw new PackException();
        }
    }

    public void ensureCapacity(int increament) {
        if (buf.writableBytes() >= increament) {
            return;
        }

        int requiredCapacity = buf.writerIndex() + increament;

        if (requiredCapacity > m_maxCapacity) {
            throw new IndexOutOfBoundsException("writableBytes("+ buf.writableBytes()+") + increament("+increament+") > maxCapacity("+m_maxCapacity+")");
        }

        int tmp = Math.max(requiredCapacity, buf.capacity() * 2);
        buf.capacity(Math.min(tmp, m_maxCapacity));

    }

    public void putVarUint(int value) {
        while ((value & 0xFFFFFF80) != 0) {
            buf.writeByte((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        buf.writeByte((byte) (value & 0x7F));
    }

}

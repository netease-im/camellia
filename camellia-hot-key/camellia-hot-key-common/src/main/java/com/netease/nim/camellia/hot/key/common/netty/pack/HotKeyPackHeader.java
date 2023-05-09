package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.netty.codec.Marshallable;
import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * Created by caojiajun on 2023/5/6
 */
public class HotKeyPackHeader implements Marshallable {

    private HotKeyCommand command;
    private long requestId;
    private byte tag = Tag.DEFAULT.getValue();

    @Override
    public void marshal(Pack pack) {
        pack.putByte(command.getCmd());
        pack.putLong(requestId);
        pack.putByte(tag);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        command = HotKeyCommand.getByValue(unpack.popByte());
        requestId = unpack.popLong();
        tag = unpack.popByte();
    }

    public static enum Tag {

        DEFAULT((byte) 0),
        EMPTY_BODY((byte) 1),//是否空包
        ACK((byte) 2),//是否是响应包

        ;

        private final byte value;

        Tag(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static Tag getByValue(int tagValue) {
            for (Tag t : Tag.values()) {
                if (t.getValue() == tagValue) {
                    return t;
                }
            }
            return DEFAULT;
        }
    }

    public HotKeyPackHeader() {
    }

    public HotKeyPackHeader(HotKeyCommand command, long requestId) {
        this.command = command;
        this.requestId = requestId;
    }

    public HotKeyPackHeader duplicate() {
        HotKeyPackHeader header = new HotKeyPackHeader();
        header.command = this.command;
        header.requestId = this.requestId;
        header.tag = this.tag;
        return header;
    }

    public HotKeyCommand getCommand() {
        return command;
    }

    public void setCommand(HotKeyCommand command) {
        this.command = command;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public void setAck() {
        tag |= Tag.ACK.getValue();
    }

    public boolean isAck() {
        return 0 != (tag & Tag.ACK.getValue());
    }

    public void setEmptyBody() {
        tag |= Tag.EMPTY_BODY.getValue();
    }

    public boolean isEmptyBody() {
        return 0 != (tag & Tag.EMPTY_BODY.getValue());
    }
}

package com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec;

import com.netease.nim.camellia.codec.Marshallable;
import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.Unpack;

/**
 * Created by caojiajun on 2023/7/7
 */
public class ProxyPackHeader implements Marshallable {

    private ProxyPackCmd cmd;
    private byte tag = 0;
    private long seqId;
    private Props context = new Props();

    @Override
    public void marshal(Pack pack) {
        pack.putByte(cmd.getValue());
        pack.putByte(tag);
        pack.putLong(seqId);
        pack.putMarshallable(context);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        this.cmd = ProxyPackCmd.getByValue(unpack.popByte());
        this.tag = unpack.popByte();
        this.seqId = unpack.popLong();
        this.context = new Props();
        unpack.popMarshallable(context);
    }

    public ProxyPackCmd getCmd() {
        return cmd;
    }

    public void setCmd(ProxyPackCmd cmd) {
        this.cmd = cmd;
    }

    public byte getTag() {
        return tag;
    }

    public void setTag(byte tag) {
        this.tag = tag;
    }

    public long getSeqId() {
        return seqId;
    }

    public void setSeqId(long seqId) {
        this.seqId = seqId;
    }

    public Props getContext() {
        return context;
    }

    public void setContext(Props context) {
        this.context = context;
    }

    public static enum Tag {

        DEFAULT((byte) 0),
        ACK((byte) 1),//是否是响应包

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

    public void setAck() {
        tag |= Tag.ACK.getValue();
    }

    public boolean isAck() {
        return 0 != (tag & Tag.ACK.getValue());
    }

}

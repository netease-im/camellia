package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.Unpack;

/**
 * Created by caojiajun on 2023/7/7
 */
public class HeartbeatAckPack extends TcpPackBody {

    private Props props = new Props();

    private static enum Tag {
        status(1),
        ;

        private final int value;

        Tag(int value) {
            this.value = value;
        }
    }

    public HeartbeatAckPack(boolean online) {
        props.putInteger(Tag.status.value, online ? 1 : 0);
    }

    public HeartbeatAckPack() {
    }

    public boolean isOnline() {
        return props.getInteger(Tag.status.value) == 1;
    }

    @Override
    public void marshal(Pack pack) {
        pack.putMarshallable(props);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        props = new Props();
        unpack.popMarshallable(props);
    }
}

package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Created by caojiajun on 2023/7/7
 */
public class TcpPack {

    private TcpPackHeader header;
    private TcpPackBody body;

    public TcpPack(TcpPackHeader header, TcpPackBody body) {
        this.header = header;
        this.body = body;
    }

    public TcpPack() {
    }

    public static TcpPack newPack(TcpPackHeader header, TcpPackBody body) {
        return new TcpPack(header, body);
    }

    public TcpPackHeader getHeader() {
        return header;
    }

    public TcpPackBody getBody() {
        return body;
    }

    public ByteBuf encode(ByteBufAllocator allocator) {
        Pack pack = new Pack(allocator, 1024);
        pack.putInt(0);

        pack.putMarshallable(header);
        if (body != null) {
            pack.putMarshallable(body);
        }

        pack.getBuffer().capacity(pack.getBuffer().readableBytes());

        pack.replaceInt(0, pack.size());

        return pack.getBuffer();
    }

    public void decode(Unpack unpack) {
        unpack.popInt();
        header = new TcpPackHeader();
        unpack.popMarshallable(header);
        TcpPackCmd cmd = header.getCmd();
        if (header.isAck()) {
            switch (cmd) {
                case HEARTBEAT:
                    body = new HeartbeatAckPack();
                    unpack.popMarshallable(body);
                    break;
                case REQUEST:
                    body = new RequestAckPack();
                    unpack.popMarshallable(body);
                    break;
                default:
                    throw new IllegalArgumentException("unknown TcpPackCmd");
            }
        } else {
            switch (cmd) {
                case HEARTBEAT:
                    body = new HeartbeatPack();
                    unpack.popMarshallable(body);
                    break;
                case REQUEST:
                    body = new RequestPack();
                    unpack.popMarshallable(body);
                    break;
                default:
                    throw new IllegalArgumentException("unknown TcpPackCmd");
            }
        }
    }
}

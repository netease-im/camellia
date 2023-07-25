package com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Created by caojiajun on 2023/7/7
 */
public class ProxyPack {

    private ProxyPackHeader header;
    private ProxyPackBody body;

    public ProxyPack(ProxyPackHeader header, ProxyPackBody body) {
        this.header = header;
        this.body = body;
    }

    public ProxyPack() {
    }

    public static ProxyPack newPack(ProxyPackHeader header, ProxyPackBody body) {
        return new ProxyPack(header, body);
    }

    public ProxyPackHeader getHeader() {
        return header;
    }

    public ProxyPackBody getBody() {
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
        header = new ProxyPackHeader();
        unpack.popMarshallable(header);
        ProxyPackCmd cmd = header.getCmd();
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

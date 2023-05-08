package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;
import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import io.netty.buffer.ByteBuf;


/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPack {

    private HotKeyPackHeader header;
    private HotKeyPackBody body;

    public HotKeyPack(HotKeyPackHeader header, HotKeyPackBody body) {
        this.header = header;
        this.body = body;
    }

    public HotKeyPack() {
    }

    public HotKeyPackHeader getHeader() {
        return header;
    }

    public HotKeyPackBody getBody() {
        return body;
    }

    public ByteBuf encode() {
        Pack pack = new Pack();
        pack.putInt(0);

        pack.putMarshallable(header);
        pack.putMarshallable(body);

        pack.getBuffer().capacity(pack.getBuffer().readableBytes());

        pack.replaceInt(0, pack.size());

        return pack.getBuffer();
    }

    public void decode(Unpack unpack) {
        unpack.popInt();
        header = new HotKeyPackHeader();
        unpack.popMarshallable(header);
        HotKeyCommand command = header.getCommand();
        if (header.isAck()) {
            switch (command) {
                case HEARTBEAT:
                    body = new HeartbeatRepPack();
                    unpack.popMarshallable(body);
                    break;
                case PUSH:
                    body = new PushRepPack();
                    unpack.popMarshallable(body);
                    break;
                case GET_CONFIG:
                    body = new GetConfigRepPack();
                    unpack.popMarshallable(body);
                    break;
                case NOTIFY_HOTKEY:
                    body = new NotifyHotKeyRepPack();
                    unpack.popMarshallable(body);
                    break;
                case NOTIFY_CONFIG:
                    body = new NotifyHotKeyConfigRepPack();
                    unpack.popMarshallable(body);
                    break;
                default:
                    throw new IllegalArgumentException("unknown HotKeyCommand");
            }
        } else {
            switch (command) {
                case HEARTBEAT:
                    body = new HeartbeatPack();
                    unpack.popMarshallable(body);
                    break;
                case PUSH:
                    body = new PushPack();
                    unpack.popMarshallable(body);
                    break;
                case GET_CONFIG:
                    body = new GetConfigPack();
                    unpack.popMarshallable(body);
                    break;
                case NOTIFY_HOTKEY:
                    body = new NotifyHotKeyPack();
                    unpack.popMarshallable(body);
                    break;
                case NOTIFY_CONFIG:
                    body = new NotifyHotKeyConfigPack();
                    unpack.popMarshallable(body);
                    break;
                default:
                    throw new IllegalArgumentException("unknown HotKeyCommand");
            }
        }
    }
}

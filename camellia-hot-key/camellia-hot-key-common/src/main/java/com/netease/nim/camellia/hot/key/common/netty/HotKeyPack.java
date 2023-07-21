package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;


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

    public static HotKeyPack newPack(HotKeyCommand command, HotKeyPackBody body) {
        HotKeyPackHeader header = new HotKeyPackHeader();
        header.setCommand(command);
        return new HotKeyPack(header, body);
    }

    public HotKeyPack() {
    }

    public HotKeyPackHeader getHeader() {
        return header;
    }

    public HotKeyPackBody getBody() {
        return body;
    }

    public ByteBuf encode(ByteBufAllocator allocator) {
        Pack pack = new Pack(allocator, 1024);
        pack.putInt(0);

        if (body == null) {
            header.setEmptyBody();
        }

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
        header = new HotKeyPackHeader();
        unpack.popMarshallable(header);
        if (header.isEmptyBody()) {
            return;
        }
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
                case HOT_KEY_CACHE_STATS:
                    body = new HotKeyCacheStatsRepPack();
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
                case HOT_KEY_CACHE_STATS:
                    body = new HotKeyCacheStatsPack();
                    unpack.popMarshallable(body);
                    break;
                default:
                    throw new IllegalArgumentException("unknown HotKeyCommand");
            }
        }
    }
}

package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;


/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPackConsumer {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackConsumer.class);

    private final HotKeyPackBizHandler handler;

    public HotKeyPackConsumer(HotKeyPackBizHandler handler) {
        this.handler = handler;
    }

    public final void onPack(Channel channel, HotKeyPack pack) {
        if (pack == null || channel == null) return;
        HotKeyPackHeader header = pack.getHeader();
        if (header == null) return;
        try {
            if (header.isEmptyBody()) {
                return;
            }
            CompletableFuture<? extends HotKeyPackBody> repPack = null;
            try {
                HotKeyCommand command = header.getCommand();
                if (command == HotKeyCommand.PUSH) {
                    repPack = handler.onPushPack((PushPack) pack.getBody());
                } else if (command == HotKeyCommand.GET_CONFIG) {
                    repPack = handler.onGetConfigPack((GetConfigPack) pack.getBody());
                } else if (command == HotKeyCommand.HEARTBEAT) {
                    repPack = handler.onHeartbeatPack((HeartbeatPack) pack.getBody());
                } else if (command == HotKeyCommand.NOTIFY_CONFIG) {
                    repPack = handler.onNotifyHotKeyConfigPack((NotifyHotKeyConfigPack) pack.getBody());
                } else if (command == HotKeyCommand.NOTIFY_HOTKEY) {
                    repPack = handler.onNotifyHotKeyPack((NotifyHotKeyPack) pack.getBody());
                }
            } catch (Exception e) {
                logger.error("on pack error, command = {}", pack.getHeader().getCommand(), e);
            } finally {
                if (repPack == null) {
                    sendRep(channel, header, null);
                } else {
                    repPack.thenAccept((Consumer<HotKeyPackBody>) body -> sendRep(channel, header, body));
                }
            }
        } catch (Exception e) {
            logger.error("submit pack task error, command = {}", pack.getHeader().getCommand(), e);
            sendRep(channel, header, null);
        }
    }

    private void sendRep(Channel channel, HotKeyPackHeader header, HotKeyPackBody body) {
        header.setAck();
        HotKeyPack rep = new HotKeyPack(header, body);
        channel.writeAndFlush(rep);
    }
}

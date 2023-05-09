package com.netease.nim.camellia.hot.key.common.netty;

import com.netease.nim.camellia.hot.key.common.netty.pack.*;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyPackConsumer {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyPackConsumer.class);

    private final HotKeyPackBizHandler handler;

    private final ExecutorService executor;

    public HotKeyPackConsumer(int workThread, HotKeyPackBizHandler handler) {
        this.executor = new ThreadPoolExecutor(workThread, workThread, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000000), new DefaultThreadFactory("camellia-hot-key-pack-consumer"), new ThreadPoolExecutor.AbortPolicy());
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
            executor.submit(() -> {
                HotKeyPackBody repPack = null;
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
                    sendRep(channel, header, repPack);
                }
            });
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

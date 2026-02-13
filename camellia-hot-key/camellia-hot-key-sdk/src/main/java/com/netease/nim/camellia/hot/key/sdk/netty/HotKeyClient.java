package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.hot.key.common.netty.*;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackDecoder;
import com.netease.nim.camellia.hot.key.common.netty.handler.HotKeyPackEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/5/8
 */
public class HotKeyClient {

    private static final Logger logger = LoggerFactory.getLogger(HotKeyClient.class);

    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(HotKeyConstants.Client.nettyWorkThread);
    private static final AtomicLong idGen = new AtomicLong(0);

    private final long id;
    private final ServerNode addr;
    private final SeqManager seqManager = new SeqManager();

    private Channel channel = null;
    private volatile boolean valid;

    public HotKeyClient(ServerNode addr, HotKeyPackConsumer consumer) {
        this.id = idGen.incrementAndGet();
        this.addr = addr;
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(nioEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, HotKeyConstants.Client.TCP_NODELAY)
                    .option(ChannelOption.SO_KEEPALIVE, HotKeyConstants.Client.SO_KEEPALIVE)
                    .option(ChannelOption.SO_RCVBUF, HotKeyConstants.Client.SO_RCVBUF)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, HotKeyConstants.Client.CLIENT_CONNECT_TIMEOUT_MILLIS)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeLine = channel.pipeline();
                            pipeLine.addLast(HotKeyPackEncoder.getName(), new HotKeyPackEncoder()); // OUT
                            pipeLine.addLast(HotKeyPackDecoder.getName(), new HotKeyPackDecoder()); // IN
                            pipeLine.addLast(HotKeyPackClientHandler.getName(), new HotKeyPackClientHandler(seqManager, consumer)); // IN
                        }
                    });
            ChannelFuture f = bootstrap.connect(addr.getHost(), addr.getPort()).sync();
            channel = f.channel();
            channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                logger.warn("hot key client closed, addr = {}, id = {}", addr, id, channelFuture.cause());
                stop();
            });
            seqManager.setChannel(channel);
            valid = true;
            logger.info("hot key client init success, addr = {}, id = {}", addr, id);
        } catch (Exception e) {
            stop();
            logger.error("hot key client start error, addr = {}, id = {}", addr, id, e);
        }
    }

    /**
     * 获取唯一id
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * 关闭连接
     */
    public synchronized void stop() {
        valid = false;
        try {
            seqManager.clear();
        } catch (Exception e) {
            logger.error("seqManager clear error, addr = {}, id = {}", addr, id, e);
        }
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            logger.error("channel close error, addr = {}, id = {}", addr, id, e);
        }
        logger.info("HotKeyClient stopped, addr = {}, id = {}", addr, id);
    }

    /**
     * 连接是否可用
     * @return true/false
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 发送一个包到服务器
     * @param hotKeyPack pack
     * @return 响应
     */
    public CompletableFuture<HotKeyPack> sendPack(HotKeyPack hotKeyPack) {
        hotKeyPack.getHeader().setSeqId(seqManager.genSeqId());
        CompletableFuture<HotKeyPack> future = seqManager.putSession(hotKeyPack);
        channel.writeAndFlush(hotKeyPack);
        return future;
    }
}

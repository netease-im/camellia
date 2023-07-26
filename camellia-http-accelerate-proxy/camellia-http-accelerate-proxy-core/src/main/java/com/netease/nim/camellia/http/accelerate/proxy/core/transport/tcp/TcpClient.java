package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config.TransportServerType;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.AbstractClient;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPack;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPackDecoder;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2023/7/7
 */
public class TcpClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);
    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("tcp-client"));

    private Channel channel;

    public TcpClient(ServerAddr addr) {
        super(addr);
    }

    @Override
    public TransportServerType getType() {
        return TransportServerType.tcp;
    }

    public void start0() throws Exception {
        boolean tcpNoDelay = DynamicConf.getBoolean("tcp.client.tcp.no.delay", true);
        boolean soTcpKeepAlive = DynamicConf.getBoolean("tcp.client.so.keep.alive", true);
        int soRcvBuf = DynamicConf.getInt("tcp.client.so.rcvbuf", 10*1024*1024);
        int soSndBuf = DynamicConf.getInt("tcp.client.so.sndbuf", 10*1024*1024);
        int connectTimeoutMillis = DynamicConf.getInt("tcp.client.connect.timeout.millis", 2000);
        int low = DynamicConf.getInt("tcp.client.write.buffer.water.mark.low", 128*1024);
        int high = DynamicConf.getInt("tcp.client.write.buffer.water.mark.high", 512*1024);
        Bootstrap bootstrap = new Bootstrap()
                .group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, tcpNoDelay)
                .option(ChannelOption.SO_KEEPALIVE, soTcpKeepAlive)
                .option(ChannelOption.SO_RCVBUF, soRcvBuf)
                .option(ChannelOption.SO_SNDBUF, soSndBuf)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(low, high))
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        ChannelPipeline pipeLine = channel.pipeline();
                        pipeLine.addLast(ProxyPackDecoder.getName(), new ProxyPackDecoder());
                        pipeLine.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                try {
                                    if (msg instanceof ProxyPack) {
                                        onProxyPack((ProxyPack) msg);
                                    } else {
                                        logger.warn("unknown pack");
                                    }
                                } catch (Exception e) {
                                    logger.error("pack error", e);
                                }
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                logger.warn("tcp connection closed, id = {}, addr = {}", getId(), getAddr());
                                super.channelInactive(ctx);
                            }
                        });
                    }
                });
        ServerAddr addr = getAddr();
        ChannelFuture f = bootstrap.connect(addr.getHost(), addr.getPort()).sync();
        channel = f.channel();
        channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
            logger.warn("tcp client closed, addr = {}, id = {}", getAddr(), getId(), channelFuture.cause());
            stop();
        });
    }

    public void send0(ProxyPack pack) {
        channel.writeAndFlush(pack.encode(channel.alloc()));
    }

    public void stop0() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            logger.error("channel close error, addr = {}, id = {}", getAddr(), getId(), e);
        }
        logger.info("TcpClient stopped, addr = {}, id = {}", getAddr(), getId());
    }

    @Override
    public int heartbeatIntervalSeconds() {
        return DynamicConf.getInt("tcp.client.heartbeat.interval.seconds", 10);
    }

    @Override
    public int heartbeatTimeoutSeconds() {
        return DynamicConf.getInt("tcp.client.heartbeat.timeout.seconds", 10);
    }
}

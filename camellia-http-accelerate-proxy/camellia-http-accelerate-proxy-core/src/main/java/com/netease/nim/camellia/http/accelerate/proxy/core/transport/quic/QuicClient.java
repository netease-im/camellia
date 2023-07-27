package com.netease.nim.camellia.http.accelerate.proxy.core.transport.quic;

import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config.TransportServerType;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.AbstractClient;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPack;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPackCmd;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPackDecoder;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


/**
 * Created by caojiajun on 2023/7/6
 */
public class QuicClient extends AbstractClient {

    private static final Logger logger = LoggerFactory.getLogger(QuicClient.class);

    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("quic-client"));

    private Channel channel;
    private QuicChannel quicChannel;
    private QuicStreamChannel streamChannel;
    private QuicStreamChannel heartbeatStreamChannel;

    public QuicClient(ServerAddr addr) {
        super(addr);
    }

    @Override
    public void start0() throws Exception {
        QuicSslContext context = QuicSslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).
                applicationProtocols("camellia").build();

        long maxIdleTimeoutMillis = DynamicConf.getLong("transport.quic.client.max.idle.timeout.millis", 60 * 1000L);
        long initialMaxData = DynamicConf.getLong("transport.quic.client.initial.max.data", 10000_0000L);
        long initialMaxStreamDataBidiLocal = DynamicConf.getLong("transport.quic.client.initial.max.stream.data.bidirectional.local", 10000_0000L);
        long initialMaxStreamDataBidiRemote = DynamicConf.getLong("transport.quic.client.initial.max.stream.data.bidirectional.remote", 10000_0000L);
        long initialMaxStreamsBidirectional = DynamicConf.getLong("transport.quic.client.initial.max.streams.bidirectional", 10000L);
        long initialMaxStreamsUnidirectional = DynamicConf.getLong("transport.quic.client.initial.max.streams.unidirectional", 10000L);

        ChannelHandler codec = new QuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(maxIdleTimeoutMillis, TimeUnit.MILLISECONDS)
                .initialMaxData(initialMaxData)
                .initialMaxStreamDataBidirectionalLocal(initialMaxStreamDataBidiLocal)
                .initialMaxStreamDataBidirectionalRemote(initialMaxStreamDataBidiRemote)
                .initialMaxStreamsBidirectional(initialMaxStreamsBidirectional)
                .initialMaxStreamsUnidirectional(initialMaxStreamsUnidirectional)
                .build();
        int connectTimeoutMillis = DynamicConf.getInt("transport.quic.client.connect.timeout.millis", 2000);

        Bootstrap bs = new Bootstrap();
        this.channel = bs.group(nioEventLoopGroup)
                .channel(NioDatagramChannel.class)
                .handler(codec)
                .bind(0)
                .sync()
                .channel();

        this.quicChannel = QuicChannel.newBootstrap(channel)
                .remoteAddress(new InetSocketAddress(getAddr().getHost(), getAddr().getPort()))
                .streamHandler(new ChannelInboundHandlerAdapter())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .connect()
                .get();
        this.streamChannel = quicChannel.createStream(QuicStreamType.BIDIRECTIONAL, new QuicStreamChannelInitializer(this)).sync().getNow();
        this.heartbeatStreamChannel = quicChannel.createStream(QuicStreamType.BIDIRECTIONAL, new QuicStreamChannelInitializer(this)).sync().getNow();
        this.channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
            logger.warn("quic client closed, addr = {}, id = {}", getAddr(), getId(), channelFuture.cause());
            stop();
        });
    }


    @Override
    public void send0(ProxyPack proxyPack) {
        if (proxyPack.getHeader().getCmd() == ProxyPackCmd.HEARTBEAT) {
            heartbeatStreamChannel.writeAndFlush(proxyPack.encode(channel.alloc()));
        } else {
            streamChannel.writeAndFlush(proxyPack.encode(channel.alloc()));
        }
    }

    @Override
    public void stop0() {
        if (streamChannel != null) {
            streamChannel.close();
        }
        if (heartbeatStreamChannel != null) {
            heartbeatStreamChannel.close();
        }
        if (quicChannel != null) {
            quicChannel.close();
        }
        if (channel != null) {
            channel.close();
        }
    }

    @Override
    public TransportServerType getType() {
        return TransportServerType.quic;
    }

    @Override
    public int heartbeatIntervalSeconds() {
        return DynamicConf.getInt("quic.client.heartbeat.interval.seconds", 10);
    }

    @Override
    public int heartbeatTimeoutSeconds() {
        return DynamicConf.getInt("quic.client.heartbeat.timeout.seconds", 10);
    }


    private static class QuicStreamChannelInitializer extends ChannelInitializer<Channel> {

        private final QuicClient quicClient;

        public QuicStreamChannelInitializer(QuicClient quicClient) {
            this.quicClient = quicClient;
        }
        @Override
        protected void initChannel(Channel channel) {
            ChannelPipeline pipeLine = channel.pipeline();
            pipeLine.addLast(ProxyPackDecoder.getName(), new ProxyPackDecoder());
            pipeLine.addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    try {
                        if (msg instanceof ProxyPack) {
                            quicClient.onProxyPack((ProxyPack) msg);
                        } else {
                            logger.warn("unknown pack");
                        }
                    } catch (Exception e) {
                        logger.error("pack error", e);
                    }
                }

                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    super.channelInactive(ctx);
                    logger.warn("quic stream closed, id = {}, addr = {}", quicClient.getId(), quicClient.getAddr());
                }
            });
        }
    }
}

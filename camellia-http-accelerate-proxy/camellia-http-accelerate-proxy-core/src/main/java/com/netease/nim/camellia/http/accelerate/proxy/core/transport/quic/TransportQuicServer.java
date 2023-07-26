package com.netease.nim.camellia.http.accelerate.proxy.core.transport.quic;

import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStartupStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.AbstractTransportServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPack;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.ProxyPackDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/7/6
 */
public class TransportQuicServer extends AbstractTransportServer {

    private static final Logger logger = LoggerFactory.getLogger(TransportQuicServer.class);

    private ServerStartupStatus status = ServerStartupStatus.INIT;

    public TransportQuicServer(IUpstreamRouter router) {
        super(router);
    }

    @Override
    public void start() {
        String host = DynamicConf.getString("transport.quic.server.host", "0.0.0.0");
        int port = DynamicConf.getInt("transport.quic.server.port", 11500);
        if (port <= 0) {
            logger.warn("transport tcp server skip start");
            status = ServerStartupStatus.SKIP;
            return;
        }
        try {
            boolean selfSignedEnable = DynamicConf.getBoolean("transport.quic.server.auto.self.signed.enable", true);
            File privateKey;
            File certificate;
            if (selfSignedEnable) {
                SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
                privateKey = selfSignedCertificate.privateKey();
                certificate = selfSignedCertificate.certificate();
                logger.info("privateKey file = {}", privateKey);
                logger.info("certificate file = {}", certificate);
            } else {
                String privateKeyFile = DynamicConf.getString("transport.quic.server.private.key.file", "private_key.key");
                String certificateFile = DynamicConf.getString("transport.quic.server.certificate.file", "certificate.crt");
                URL url1 = TransportQuicServer.class.getClassLoader().getResource(privateKeyFile);
                if (url1 == null) {
                    throw new IllegalArgumentException(privateKeyFile + " not exists");
                }
                URL url2 = TransportQuicServer.class.getClassLoader().getResource(certificateFile);
                if (url2 == null) {
                    throw new IllegalArgumentException(certificateFile + " not exists");
                }
                privateKey = new File(url1.getPath());
                certificate = new File(url2.getPath());
            }
            QuicSslContext context = QuicSslContextBuilder.forServer(privateKey, null, certificate)
                    .applicationProtocols("camellia").build();
            int workThread = DynamicConf.getInt("transport.quic.server.work.thread", Runtime.getRuntime().availableProcessors());

            long maxIdleTimeoutMillis = DynamicConf.getLong("transport.quic.server.max.idle.timeout.millis", 120 * 1000L);
            long initialMaxData = DynamicConf.getLong("transport.quic.server.initial.max.data", 10000_0000L);
            long initialMaxStreamDataBidiLocal = DynamicConf.getLong("transport.quic.server.initial.max.stream.data.bidirectional.local", 10000_0000L);
            long initialMaxStreamDataBidiRemote = DynamicConf.getLong("transport.quic.server.initial.max.stream.data.bidirectional.remote", 10000_0000L);
            long initialMaxStreamsBidirectional = DynamicConf.getLong("transport.quic.server.initial.max.streams.bidirectional", 10000L);
            long initialMaxStreamsUnidirectional = DynamicConf.getLong("transport.quic.server.initial.max.streams.unidirectional", 10000L);

            NioEventLoopGroup group = new NioEventLoopGroup(workThread, new DefaultThreadFactory("transport-quic-server-work-group"));
            ChannelHandler codec = new QuicServerCodecBuilder().sslContext(context)
                    .maxIdleTimeout(maxIdleTimeoutMillis, TimeUnit.MILLISECONDS)
                    .initialMaxData(initialMaxData)
                    .initialMaxStreamDataBidirectionalLocal(initialMaxStreamDataBidiLocal)
                    .initialMaxStreamDataBidirectionalRemote(initialMaxStreamDataBidiRemote)
                    .initialMaxStreamsBidirectional(initialMaxStreamsBidirectional)
                    .initialMaxStreamsUnidirectional(initialMaxStreamsUnidirectional)
                    .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public boolean isSharable() {
                            return true;
                        }
                    })
                    .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                        @Override
                        protected void initChannel(QuicStreamChannel ch)  {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(ProxyPackDecoder.getName(), new ProxyPackDecoder()); // IN
                            pipeline.addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                        try {
                                            if (msg instanceof ProxyPack) {
                                                onProxyPack(ctx, (ProxyPack) msg);
                                            } else {
                                                logger.warn("unknown pack");
                                            }
                                        } catch (Exception e) {
                                            logger.error("pack error", e);
                                        }
                                    }

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelActive(ctx);
                                        logger.info("new quic client connection, channel = {}", ctx.channel());
                                    }

                                    @Override
                                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                        super.channelInactive(ctx);
                                        logger.info("quic client connection disconnect, channel = {}", ctx.channel());
                                    }
                            });
                        }
                    }).build();
            Bootstrap bs = new Bootstrap();
            bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(host, port)
                    .sync();
            logger.info("transport quic server start success, host = {}, port = {}", host, port);
            status = ServerStartupStatus.SUCCESS;
        } catch (Exception e) {
            status = ServerStartupStatus.FAIL;
            logger.error("transport quic server start error, host = {}, port = {}", host, port, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ServerStartupStatus getStatus() {
        return status;
    }
}

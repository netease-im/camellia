package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.BindInfo;
import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import com.netease.nim.camellia.redis.proxy.netty.InitHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2024/1/16
 */
public class CamelliaRedisProxyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyHttpServer.class);

    private final CamelliaServerProperties serverProperties;
    private final InitHandler initHandler = new InitHandler(ChannelType.http);
    private final HttpCommandServerHandler serverHandler;

    public CamelliaRedisProxyHttpServer(CamelliaServerProperties serverProperties, ICommandInvoker invoker) {
        this.serverProperties = serverProperties;
        this.serverHandler = new HttpCommandServerHandler(invoker);
    }

    public BindInfo start() {
        try {
            if (serverProperties.getHttpPort() <= 0) {
                logger.info("CamelliaRedisProxyServer with http disabled, skip start");
                return null;
            }
            int bossThread = ProxyDynamicConf.getInt("http.server.boss.thread", 1);
            int workThread = ProxyDynamicConf.getInt("http.server.work.thread", Runtime.getRuntime().availableProcessors());
            EventLoopGroup bossGroup = new NioEventLoopGroup(bossThread, new DefaultThreadFactory("camellia-http-boss-group"));
            EventLoopGroup workerGroup = new NioEventLoopGroup(workThread, new DefaultThreadFactory("camellia-http-work-group"));
            ServerBootstrap bootstrap = new ServerBootstrap();
            int soBacklog = ProxyDynamicConf.getInt("http.server.so.backlog", 1024);
            int soSndBuf = ProxyDynamicConf.getInt("http.server.so.sndbuf", 10 * 1024 * 1024);
            int soRcvBuf = ProxyDynamicConf.getInt("http.server.so.rcvbuf", 10 * 1024 * 1024);
            boolean tcpNoDelay = ProxyDynamicConf.getBoolean("http.server.tcp.nodelay", true);
            boolean soKeepalive = ProxyDynamicConf.getBoolean("http.server.so.keepalive", true);

            int low = ProxyDynamicConf.getInt("http.server.write.buffer.water.mark.low", 128*1024);
            int high = ProxyDynamicConf.getInt("http.server.write.buffer.water.mark.high", 512*1024);

            int maxContentLength = ProxyDynamicConf.getInt("http.server.max.content.length", 20*1024*1024);

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, soBacklog)
                    .childOption(ChannelOption.SO_SNDBUF, soSndBuf)
                    .childOption(ChannelOption.SO_RCVBUF, soRcvBuf)
                    .childOption(ChannelOption.TCP_NODELAY, tcpNoDelay)
                    .childOption(ChannelOption.SO_KEEPALIVE, soKeepalive)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(low, high))
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(maxContentLength));
                            pipeline.addLast(new HttpResponsePackEncoder());
                            pipeline.addLast(initHandler);
                            pipeline.addLast(serverHandler);
                        }
                    });
            logger.info("CamelliaRedisProxyServer with http, bossThread = {}, workThread = {}", bossThread, workThread);
            logger.info("CamelliaRedisProxyServer with http, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                    soBacklog, soSndBuf, soRcvBuf, soKeepalive);
            logger.info("CamelliaRedisProxyServer with http, tcp_no_delay = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                    tcpNoDelay, low, high);
            return new BindInfo(BindInfo.Type.HTTP, bootstrap, serverProperties.getHttpPort());
        } catch (Exception e) {
            logger.error("CamelliaRedisProxyServer with http start error, port = {}", serverProperties.getHttpPort(), e);
            throw new IllegalStateException(e);
        }
    }
}

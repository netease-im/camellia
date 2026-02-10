package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.command.ICommandInvoker;
import com.netease.nim.camellia.redis.proxy.conf.*;
import com.netease.nim.camellia.redis.proxy.netty.BindInfo;
import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.netty.InitHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2024/1/16
 */
public class CamelliaRedisProxyHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyHttpServer.class);

    private final InitHandler initHandler = new InitHandler(ChannelType.http);
    private final HttpCommandServerHandler serverHandler;

    public CamelliaRedisProxyHttpServer(ICommandInvoker invoker) {
        this.serverHandler = new HttpCommandServerHandler(invoker);
    }

    public BindInfo start() {
        int port = ServerConf.httpPort();
        try {
            if (port <= 0) {
                logger.info("CamelliaRedisProxyHttpServer disabled, skip start");
                return null;
            }
            EventLoopGroupResult result = NettyConf.serverEventLoopGroup(NettyConf.Type.http_server);
            GlobalRedisProxyEnv.setHttpEventLoopGroupResult(result);

            ServerBootstrap bootstrap = new ServerBootstrap();

            int maxContentLength = ProxyDynamicConf.getInt("http.server.max.content.length", 20*1024*1024);

            bootstrap.group(result.bossGroup(), result.workGroup())
                    .channel(result.serverChannelClass())
                    .option(ChannelOption.SO_BACKLOG, NettyConf.soBacklog(NettyConf.Type.http_server))
                    .childOption(ChannelOption.SO_SNDBUF, NettyConf.soSndbuf(NettyConf.Type.http_server))
                    .childOption(ChannelOption.SO_RCVBUF, NettyConf.soRcvbuf(NettyConf.Type.http_server))
                    .childOption(ChannelOption.TCP_NODELAY, NettyConf.tcpNoDelay(NettyConf.Type.http_server))
                    .childOption(ChannelOption.SO_KEEPALIVE, NettyConf.soKeepalive(NettyConf.Type.http_server))
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(NettyConf.writeBufferWaterMarkLow(NettyConf.Type.http_server),
                                    NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.http_server)))
                    .childHandler(new ChannelInitializer<>() {
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
            logger.info("CamelliaRedisProxyHttpServer, boss_thread = {}, work_thread = {}", result.bossThread(), result.workThread());
            logger.info("CamelliaRedisProxyHttpServer, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                    NettyConf.soBacklog(NettyConf.Type.http_server), NettyConf.soSndbuf(NettyConf.Type.http_server),
                    NettyConf.soRcvbuf(NettyConf.Type.http_server), NettyConf.soKeepalive(NettyConf.Type.http_server));
            logger.info("CamelliaRedisProxyHttpServer, tcp_no_delay = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}, max_content_length = {}",
                    NettyConf.tcpNoDelay(NettyConf.Type.http_server), NettyConf.writeBufferWaterMarkLow(NettyConf.Type.http_server),
                    NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.http_server), maxContentLength);
            return new BindInfo(BindInfo.Type.HTTP, bootstrap, port);
        } catch (Exception e) {
            logger.error("CamelliaRedisProxyServer with http start error, port = {}", port, e);
            throw new IllegalStateException(e);
        }
    }
}

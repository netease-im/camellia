package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.conf.EventLoopGroupResult;
import com.netease.nim.camellia.redis.proxy.conf.NettyConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.unix.UnixChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2024/1/25
 */
public class CamelliaRedisProxyUdsServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyUdsServer.class);

    private final InitHandler udsInitHandler = new InitHandler(ChannelType.uds);
    private final ServerHandler serverHandler;

    public CamelliaRedisProxyUdsServer(ServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    public BindInfo start() {
        String udsPath = ServerConf.udsPath();
        try {
            if (udsPath == null || udsPath.isEmpty()) {
                logger.info("CamelliaRedisProxyServer with uds disabled, skip start");
                return null;
            }

            EventLoopGroupResult result = NettyConf.serverEventLoopGroup(NettyConf.Type.uds_server);
            GlobalRedisProxyEnv.setUdsEventLoopGroupResult(result);

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(result.bossGroup(), result.workGroup())
                    .channel(result.serverChannelClass())
                    .option(ChannelOption.SO_BACKLOG, NettyConf.soBacklog(NettyConf.Type.uds_server))
                    .childOption(ChannelOption.SO_SNDBUF, NettyConf.soSndbuf(NettyConf.Type.tcp_server))
                    .childOption(ChannelOption.SO_RCVBUF, NettyConf.soRcvbuf(NettyConf.Type.tcp_server))
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                            new WriteBufferWaterMark(NettyConf.writeBufferWaterMarkLow(NettyConf.Type.tcp_server),
                                    NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.tcp_server)))
                    .childHandler(new ChannelInitializer<UnixChannel>() {
                        @Override
                        public void initChannel(UnixChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            //idle close
                            if (Utils.idleCloseHandlerEnable()) {
                                pipeline.addLast(new IdleCloseHandler(NettyConf.readerIdleTimeSeconds(),
                                        NettyConf.writerIdleTimeSeconds(), NettyConf.allIdleTimeSeconds()));
                            }
                            //command decoder
                            pipeline.addLast(new CommandDecoder());
                            //reply encoder
                            if (ReplyBatchFlushUtils.enable()) {
                                pipeline.addLast(new ReplyFlushEncoder());
                                pipeline.addLast(new ReplyBufferEncoder());
                            } else {
                                pipeline.addLast(new ReplyEncoder());
                            }
                            //connect manager
                            pipeline.addLast(udsInitHandler);
                            //command transponder
                            pipeline.addLast(serverHandler);
                        }
                    });
            logger.info("CamelliaRedisProxyServer with uds, boss_thread = {}, work_thread = {}", result.bossThread(), result.workThread());
            logger.info("CamelliaRedisProxyServer with uds, so_backlog = {}, so_sendbuf = {}, so_rcvbuf = {}, so_keepalive = {}",
                    NettyConf.soBacklog(NettyConf.Type.uds_server), NettyConf.soSndbuf(NettyConf.Type.uds_server),
                    NettyConf.soRcvbuf(NettyConf.Type.uds_server), NettyConf.soKeepalive(NettyConf.Type.uds_server));
            logger.info("CamelliaRedisProxyServer with uds, tcp_no_delay = {}, write_buffer_water_mark_low = {}, write_buffer_water_mark_high = {}",
                    NettyConf.tcpNoDelay(NettyConf.Type.uds_server), NettyConf.writeBufferWaterMarkLow(NettyConf.Type.uds_server),
                    NettyConf.writeBufferWaterMarkHigh(NettyConf.Type.uds_server));
            return new BindInfo(BindInfo.Type.UDS, bootstrap, udsPath);
        } catch (Exception e) {
            logger.error("CamelliaRedisProxyServer with uds start error, udsPath = {}", udsPath, e);
            throw new IllegalStateException(e);
        }
    }
}

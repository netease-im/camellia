package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
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
    private final CamelliaServerProperties serverProperties;
    private final ServerHandler serverHandler;

    public CamelliaRedisProxyUdsServer(CamelliaServerProperties serverProperties, ServerHandler serverHandler) {
        this.serverProperties = serverProperties;
        this.serverHandler = serverHandler;
    }

    public BindInfo start() {
        String udsPath = serverProperties.getUdsPath();
        if (udsPath == null || udsPath.isEmpty()) {
            logger.info("CamelliaRedisProxyServer with uds disabled, skip start");
            return null;
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = GlobalRedisProxyEnv.getUdsBossGroup();
        EventLoopGroup workGroup = GlobalRedisProxyEnv.getUdsWorkGroup();
        Class<? extends ServerChannel> serverUdsChannelClass = GlobalRedisProxyEnv.getServerUdsChannelClass();
        if (bossGroup == null || workGroup == null || serverUdsChannelClass == null) {
            logger.warn("CamelliaRedisProxyServer with uds start failed because os not support");
            return null;
        }
        bootstrap.group(bossGroup, workGroup)
                .channel(serverUdsChannelClass)
                .option(ChannelOption.SO_BACKLOG, serverProperties.getSoBacklog())
                .childOption(ChannelOption.SO_SNDBUF, serverProperties.getSoSndbuf())
                .childOption(ChannelOption.SO_RCVBUF, serverProperties.getSoRcvbuf())
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                        new WriteBufferWaterMark(serverProperties.getWriteBufferWaterMarkLow(), serverProperties.getWriteBufferWaterMarkHigh()))
                .childHandler(new ChannelInitializer<UnixChannel>() {
                    @Override
                    public void initChannel(UnixChannel channel) {
                        ChannelPipeline pipeline = channel.pipeline();
                        //idle close
                        if (Utils.idleCloseHandlerEnable(serverProperties)) {
                            pipeline.addLast(new IdleCloseHandler(serverProperties.getReaderIdleTimeSeconds(),
                                    serverProperties.getWriterIdleTimeSeconds(), serverProperties.getAllIdleTimeSeconds()));
                        }
                        //command decoder
                        pipeline.addLast(new CommandDecoder(serverProperties.getCommandDecodeMaxBatchSize(), serverProperties.getCommandDecodeBufferInitializerSize()));
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
        return new BindInfo(BindInfo.Type.UDS, bootstrap, udsPath);
    }
}

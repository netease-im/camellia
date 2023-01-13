package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.auth.ConnectLimiter;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2019/11/7.
 */
@ChannelHandler.Sharable
public class InitHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);
    private CamelliaServerProperties serverProperties;

    public CamelliaServerProperties getServerProperties() {
        return serverProperties;
    }

    public void setServerProperties(CamelliaServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ChannelInfo channelInfo = ChannelInfo.init(ctx);
        int threshold = ConnectLimiter.connectThreshold();
        int currentConnect = ChannelMonitor.connect();
        if (currentConnect >= threshold) {
            ctx.close();
            logger.warn("too many connects, connect will be force closed, current = {}, max = {}, consid = {}, client.addr = {}",
                    currentConnect, threshold, channelInfo.getConsid(), ctx.channel().remoteAddress());
            return;
        }
        ChannelMonitor.init(channelInfo);
        if (logger.isDebugEnabled()) {
            logger.debug("channel init, consid = {}", channelInfo.getConsid());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Utils.enableQuickAck(ctx.channel(),serverProperties);
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        if (channelInfo != null) {
            channelInfo.clear();
            ChannelMonitor.remove(channelInfo);
            ConcurrentHashMap<String, RedisClient> map = channelInfo.getBindRedisClientCache();
            if (map != null) {
                for (Map.Entry<String, RedisClient> entry : map.entrySet()) {
                    RedisClient redisClient = entry.getValue();
                    if (redisClient == null || !redisClient.isValid()) continue;
                    if (logger.isDebugEnabled()) {
                        logger.debug("bind redis client cache will close interrupt by proxy client, consid = {}, client.addr = {}, upstream.redis.client = {}",
                                channelInfo.getConsid(), ctx.channel().remoteAddress(), redisClient.getClientName());
                    }
                    redisClient.stop(true);
                }
                map.clear();
            }
            RedisClient bindClient = channelInfo.getBindClient();
            if (bindClient != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("bind redis client will close for disconnect, consid = {}, client.addr = {}, upstream.redis.client = {}",
                            channelInfo.getConsid(), ctx.channel().remoteAddress(), bindClient.getClientName());
                }
                bindClient.stop(true);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("channel close, consid = {}", channelInfo == null ? "null" : channelInfo.getConsid());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ChannelInfo channelInfo = ChannelInfo.get(ctx);
        if (logger.isDebugEnabled()) {
            logger.debug("channel error, consid = {}", channelInfo == null ? "null" : channelInfo.getConsid(), cause);
        }
    }
}

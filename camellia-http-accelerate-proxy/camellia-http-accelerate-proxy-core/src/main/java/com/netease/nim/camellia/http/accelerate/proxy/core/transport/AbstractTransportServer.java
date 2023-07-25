package com.netease.nim.camellia.http.accelerate.proxy.core.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.constants.Constants;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LoggerUtils;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.status.ServerStatus;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.upstream.IUpstreamClient;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2023/7/25
 */
public abstract class AbstractTransportServer implements ITransportServer {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTransportServer.class);

    private final IUpstreamRouter router;

    public AbstractTransportServer(IUpstreamRouter router) {
        this.router = router;
    }

    protected final void onProxyPack(ChannelHandlerContext ctx, ProxyPack pack) {
        ProxyPackHeader header = pack.getHeader();
        if (header.getCmd() == ProxyPackCmd.HEARTBEAT) {
            if (header.isAck()) {
                logger.warn("illegal heartbeat ack pack");
            } else {
                header.setAck();
                HeartbeatAckPack ackPack = new HeartbeatAckPack(ServerStatus.getStatus() == ServerStatus.Status.ONLINE);
                ctx.channel().writeAndFlush(ProxyPack.newPack(header, ackPack).encode(ctx.alloc()));
            }
        } else if (header.getCmd() == ProxyPackCmd.REQUEST) {
            ServerStatus.updateLastUseTime();
            if (header.isAck()) {
                logger.warn("illegal request ack pack");
            } else {
                RequestPack requestPack = (RequestPack)pack.getBody();
                ProxyRequest proxyRequest = requestPack.getProxyRequest();
                proxyRequest.getLogBean().setTransportServerReceiveTime(System.currentTimeMillis());
                IUpstreamClient client = router.select(proxyRequest);
                CompletableFuture<ProxyResponse> future;
                if (client == null) {
                    future = new CompletableFuture<>();
                    proxyRequest.getLogBean().setErrorReason(ErrorReason.UPSTREAM_SERVER_ROUTE_FAIL);
                    future.complete(new ProxyResponse(Constants.BAD_GATEWAY, proxyRequest.getLogBean()));
                } else {
                    future = client.send(proxyRequest);
                }
                future.thenAccept(response -> {
                    try {
                        header.setAck();
                        response.getLogBean().setCode(response.getResponse().status().code());
                        ctx.channel().writeAndFlush(ProxyPack.newPack(header, new RequestAckPack(response)).encode(ctx.alloc()));
                    } finally {
                        LoggerUtils.logging(response.getLogBean());
                    }
                });
            }
        } else {
            logger.warn("unknown pack, seqId = {}", header.getSeqId());
        }
    }
}

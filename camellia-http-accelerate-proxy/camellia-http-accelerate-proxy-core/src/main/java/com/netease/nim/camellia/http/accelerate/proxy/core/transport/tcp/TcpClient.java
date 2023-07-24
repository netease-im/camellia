package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/7/7
 */
public class TcpClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);
    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("tcp-client"));
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("tcp-client-heartbeat"));

    private static final AtomicLong idGen = new AtomicLong(0);

    private final AtomicLong seqIdGen = new AtomicLong();
    private final long id;
    private final TcpAddr addr;
    private Channel channel;

    private volatile Status status;
    private ScheduledFuture<?> scheduledFuture;

    private final ConcurrentHashMap<Long, Request> requestMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<Boolean>> heartbeatMap = new ConcurrentHashMap<>();

    public TcpClient(TcpAddr addr) {
        this.addr = addr;
        this.id = idGen.incrementAndGet();
    }

    public void start() {
        try {
            boolean tcpNoDelay = DynamicConf.getBoolean("tcp.client.tcp.no.delay", true);
            boolean soTcpKeepAlive = DynamicConf.getBoolean("tcp.client.so.keep.alive", true);
            int soRcvBuf = DynamicConf.getInt("tcp.client.so.rcvbuf", 10*1024*1024);
            int soSndBuf = DynamicConf.getInt("tcp.client.so.sndbuf", 10*1024*1024);
            int connectTimeoutMillis = DynamicConf.getInt("tcp.client.connect.timeout.millis", 2000);
            Bootstrap bootstrap = new Bootstrap()
                    .group(nioEventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, tcpNoDelay)
                    .option(ChannelOption.SO_KEEPALIVE, soTcpKeepAlive)
                    .option(ChannelOption.SO_RCVBUF, soRcvBuf)
                    .option(ChannelOption.SO_SNDBUF, soSndBuf)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            ChannelPipeline pipeLine = channel.pipeline();
                            pipeLine.addLast(TcpPackDecoder.getName(), new TcpPackDecoder()); // IN
                            pipeLine.addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                    try {
                                        if (msg instanceof TcpPack) {
                                            onTcpPack((TcpPack) msg);
                                        } else {
                                            logger.warn("unknown pack");
                                        }
                                    } catch (Exception e) {
                                        logger.error("pack error", e);
                                    }
                                }
                            }); // IN
                        }
                    });
            ChannelFuture f = bootstrap.connect(addr.getHost(), addr.getPort()).sync();
            channel = f.channel();
            channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                logger.warn("tcp client closed, addr = {}, id = {}", addr, id, channelFuture.cause());
                stop();
            });
            status = Status.ONLINE;
            heartbeat();
            logger.info("tcp client start success, addr = {}, id = {}", addr, id);
            int intervalSeconds = DynamicConf.getInt("tcp.client.heartbeat.interval.seconds", 10);
            scheduledFuture = scheduler.scheduleAtFixedRate(this::heartbeat, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            stop();
            logger.error("tcp client start error, addr = {}, id = {}", addr, id, e);
        }
    }

    public void send(ProxyRequest request, CompletableFuture<ProxyResponse> future) {
        TcpPackHeader header = newHeader(TcpPackCmd.REQUEST);
        request.getLogBean().setTransportServerSendTime(System.currentTimeMillis());
        TcpPack pack = TcpPack.newPack(header, new RequestPack(request));
        requestMap.put(header.getSeqId(), new Request(future, request));
        channel.writeAndFlush(pack.encode(channel.alloc()));
    }

    private static class Request {
        CompletableFuture<ProxyResponse> future;
        ProxyRequest request;

        public Request(CompletableFuture<ProxyResponse> future, ProxyRequest request) {
            this.future = future;
            this.request = request;
        }
    }

    public synchronized void stop() {
        status = Status.INVALID;
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            logger.error("channel close error, addr = {}, id = {}", addr, id, e);
        }
        try {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        } catch (Exception e) {
            logger.error("scheduledFuture cancel error, addr = {}, id = {}", addr, id, e);
        }
        Set<Long> set1 = new HashSet<>(requestMap.keySet());
        for (Long seqId : set1) {
            Request request = requestMap.remove(seqId);
            if (request != null) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.GATEWAY_TIMEOUT);
                request.request.getLogBean().setErrorReason(ErrorReason.TCP_CLIENT_STOP);
                request.future.complete(new ProxyResponse(response, request.request.getLogBean()));
            }
        }
        Set<Long> set2 = new HashSet<>(heartbeatMap.keySet());
        for (Long seqId : set2) {
            CompletableFuture<Boolean> future = heartbeatMap.remove(seqId);
            if (future != null) {
                future.complete(false);
            }
        }
        logger.info("TcpClient stopped, addr = {}, id = {}", addr, id);
    }

    public long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public TcpAddr getAddr() {
        return addr;
    }

    public void setClosingStatus() {
        if (status != Status.INVALID) {
            status = Status.CLOSING;
        }
    }

    private void onTcpPack(TcpPack pack) {
        TcpPackHeader header = pack.getHeader();
        long seqId = header.getSeqId();
        if (header.getCmd() == TcpPackCmd.HEARTBEAT) {
            if (header.isAck()) {
                HeartbeatAckPack ackPack = (HeartbeatAckPack) pack.getBody();
                CompletableFuture<Boolean> future = heartbeatMap.remove(seqId);
                if (future != null) {
                    future.complete(ackPack.isOnline());
                } else {
                    logger.warn("unknown heartbeat seqId = {}", seqId);
                }
            } else {
                logger.warn("illegal heartbeat pack");
            }
        } else if (header.getCmd() == TcpPackCmd.REQUEST) {
            if (header.isAck()) {
                RequestAckPack ackPack = (RequestAckPack) pack.getBody();
                Request request = requestMap.remove(seqId);
                if (request != null) {
                    request.future.complete(ackPack.getProxyResponse());
                } else {
                    logger.warn("unknown request seqId = {}", seqId);
                }
            } else {
                logger.warn("illegal request pack");
            }
        } else {
            logger.warn("unknown pack");
        }
    }

    private void heartbeat() {
        try {
            if (status == Status.CLOSING) {
                return;
            }
            TcpPackHeader header = newHeader(TcpPackCmd.HEARTBEAT);
            TcpPack pack = TcpPack.newPack(header, new HeartbeatPack());
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            heartbeatMap.put(header.getSeqId(), future);
            channel.writeAndFlush(pack.encode(channel.alloc()));
            int timeout = DynamicConf.getInt("tcp.client.heartbeat.timeout.seconds", 20);
            Boolean online = future.get(timeout, TimeUnit.SECONDS);
            if (status != Status.INVALID && status != Status.CLOSING) {
                if (online != null && online) {
                    status = Status.ONLINE;
                } else {
                    status = Status.OFFLINE;
                }
            }
        } catch (Exception e) {
            logger.error("heartbeat timeout, addr = {}, id = {}", addr, id);
            status = Status.INVALID;
            stop();
        }
    }

    private TcpPackHeader newHeader(TcpPackCmd cmd) {
        TcpPackHeader header = new TcpPackHeader();
        header.setCmd(cmd);
        header.setSeqId(seqIdGen.incrementAndGet());
        return header;
    }


}

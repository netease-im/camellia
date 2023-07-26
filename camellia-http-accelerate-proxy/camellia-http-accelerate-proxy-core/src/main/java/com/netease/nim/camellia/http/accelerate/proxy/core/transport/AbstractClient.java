package com.netease.nim.camellia.http.accelerate.proxy.core.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.Status;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
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
 * Created by caojiajun on 2023/7/25
 */
public abstract class AbstractClient implements Client {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClient.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("transport-client-heartbeat"));
    private static final AtomicLong idGen = new AtomicLong(0);

    private final AtomicLong seqIdGen = new AtomicLong();

    private final ConcurrentHashMap<Long, Request> requestMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<Boolean>> heartbeatMap = new ConcurrentHashMap<>();

    private final long id;
    private final ServerAddr addr;
    private volatile Status status;
    private ScheduledFuture<?> scheduledFuture;

    public AbstractClient(ServerAddr addr) {
        this.addr = addr;
        this.id = idGen.incrementAndGet();
    }

    @Override
    public void start() {
        try {
            start0();
            status = Status.ONLINE;
            long startTime = System.currentTimeMillis();
            heartbeat();
            logger.info("transport client start success, type = {}, addr = {}, id = {}, heartbeatSpendMs = {}",
                    getType(), getAddr(), getId(), (System.currentTimeMillis() - startTime));
            int intervalSeconds = DynamicConf.getInt("transport.client.heartbeat.interval.seconds", 10);
            scheduledFuture = scheduler.scheduleAtFixedRate(this::heartbeat, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            stop();
            logger.error("transport client start error, type = {}, addr = {}, id = {}", getType(), addr, id, e);
        }
    }

    public abstract void start0() throws Exception;

    @Override
    public void send(ProxyRequest request, CompletableFuture<ProxyResponse> future) {
        ProxyPackHeader header = newHeader(ProxyPackCmd.REQUEST);
        request.getLogBean().setTransportServerSendTime(System.currentTimeMillis());
        ProxyPack pack = ProxyPack.newPack(header, new RequestPack(request));
        requestMap.put(header.getSeqId(), new Request(future, request));
        send0(pack);
    }

    public abstract void send0(ProxyPack proxyPack);

    public synchronized void stop() {
        status = Status.INVALID;
        try {
            stop0();
        } catch (Exception e) {
            logger.error("channel close error, type = {}, addr = {}, id = {}", getType(), getAddr(), getId(), e);
        }
        try {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        } catch (Exception e) {
            logger.error("scheduledFuture cancel error, type = {}, addr = {}, id = {}", getType(), getAddr(), getId(), e);
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
        logger.info("TransportClient stopped, type = {}, addr = {}, id = {}", getType(), getAddr(), getId());
    }

    public abstract void stop0();

    private static class Request {
        CompletableFuture<ProxyResponse> future;
        ProxyRequest request;

        public Request(CompletableFuture<ProxyResponse> future, ProxyRequest request) {
            this.future = future;
            this.request = request;
        }
    }

    protected final void onProxyPack(ProxyPack pack) {
        ProxyPackHeader header = pack.getHeader();
        long seqId = header.getSeqId();
        if (header.getCmd() == ProxyPackCmd.HEARTBEAT) {
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
        } else if (header.getCmd() == ProxyPackCmd.REQUEST) {
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
        long startTime = System.currentTimeMillis();
        try {
            if (status == Status.CLOSING) {
                return;
            }
            boolean disabled = DynamicConf.getBoolean("transport.client.heartbeat.disabled", false);
            if (disabled) {
                return;
            }
            ProxyPackHeader header = newHeader(ProxyPackCmd.HEARTBEAT);
            ProxyPack pack = ProxyPack.newPack(header, new HeartbeatPack());
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            heartbeatMap.put(header.getSeqId(), future);
            send0(pack);
            int timeout = DynamicConf.getInt("transport.client.heartbeat.timeout.seconds", 10);
            Boolean online = future.get(timeout, TimeUnit.SECONDS);
            if (status != Status.INVALID && status != Status.CLOSING) {
                if (online != null && online) {
                    status = Status.ONLINE;
                } else {
                    status = Status.OFFLINE;
                }
            }
        } catch (Exception e) {
            logger.error("heartbeat timeout, type = {}, addr = {}, id = {}, spendMs = {}", getType(), getAddr(), getId(), System.currentTimeMillis() - startTime);
            status = Status.INVALID;
            stop();
        }
    }

    private ProxyPackHeader newHeader(ProxyPackCmd cmd) {
        ProxyPackHeader header = new ProxyPackHeader();
        header.setCmd(cmd);
        header.setSeqId(seqIdGen.incrementAndGet());
        return header;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public ServerAddr getAddr() {
        return addr;
    }

    @Override
    public void setClosingStatus() {
        if (status != Status.INVALID) {
            status = Status.CLOSING;
        }
    }
}

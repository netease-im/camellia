package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.constants.Constants;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportClient;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/7/6
 */
public class TransportTcpClient implements ITransportClient {

    private static final Logger logger = LoggerFactory.getLogger(TransportTcpClient.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("tcp-client-scheduler"));
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
            0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(10000), new CamelliaThreadFactory("tcp-client-executor"));

    private final DynamicTcpAddrs dynamicTcpAddrs;
    private final DynamicValueGetter<Integer> connectCount;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;

    //连接管理
    private final ConcurrentHashMap<TcpAddr, CopyOnWriteArrayList<TcpClient>> map = new ConcurrentHashMap<>();
    //合法地址列表
    private CopyOnWriteArrayList<TcpAddr> validAddrs = new CopyOnWriteArrayList<>();

    public TransportTcpClient(DynamicTcpAddrs dynamicTcpAddrs, DynamicValueGetter<Integer> connectCount) {
        this.dynamicTcpAddrs = dynamicTcpAddrs;
        this.connectCount = connectCount;
    }

    @Override
    public void start() {
        refresh();
        int intervalSeconds = DynamicConf.getInt("tcp.client.refresh.interval.seconds", 30);
        scheduledFuture = scheduledExecutor.scheduleAtFixedRate(this::refresh, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledExecutor.shutdown();
        executor.shutdown();
        for (Map.Entry<TcpAddr, CopyOnWriteArrayList<TcpClient>> entry : map.entrySet()) {
            for (TcpClient client : entry.getValue()) {
                client.stop();
            }
        }
    }

    @Override
    public CompletableFuture<ProxyResponse> send(ProxyRequest proxyRequest) {
        CompletableFuture<ProxyResponse> future = new CompletableFuture<>();
        TcpClient tcpClient = selectClient();
        if (tcpClient != null) {
            proxyRequest.getLogBean().setTransportAddr(tcpClient.getAddr().toString());
            tcpClient.send(proxyRequest, future);
            return future;
        }
        try {
            proxyRequest.getRequest().content().retain();
            //走一下异步，确保不会阻塞
            executor.submit(() -> {
                try {
                    refresh();
                    TcpClient client = selectClient();
                    if (client != null) {
                        proxyRequest.getLogBean().setTransportAddr(client.getAddr().toString());
                        client.send(proxyRequest, future);
                    } else {
                        proxyRequest.getLogBean().setErrorReason(ErrorReason.TRANSPORT_SERVER_SELECT_FAIL);
                        future.complete(new ProxyResponse(Constants.BAD_GATEWAY, proxyRequest.getLogBean()));
                    }
                } catch (Exception e) {
                    proxyRequest.getLogBean().setErrorReason(ErrorReason.TRANSPORT_SERVER_SELECT_FAIL);
                    future.complete(new ProxyResponse(Constants.BAD_GATEWAY, proxyRequest.getLogBean()));
                    logger.error(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            logger.error("send error, request = {}", proxyRequest.getRequest(), e);
            proxyRequest.getLogBean().setErrorReason(ErrorReason.TRANSPORT_SERVER_SELECT_FAIL);
            future.complete(new ProxyResponse(Constants.BAD_GATEWAY, proxyRequest.getLogBean()));
        }
        return future;
    }

    private TcpClient selectClient() {
        int retry = 3;
        while (retry-- > 0) {
            try {
                if (validAddrs.isEmpty()) {
                    return null;
                }
                int index = ThreadLocalRandom.current().nextInt(validAddrs.size());
                TcpAddr addr = validAddrs.get(index);
                CopyOnWriteArrayList<TcpClient> list = map.get(addr);
                if (list.isEmpty()) {
                    validAddrs.remove(addr);
                }
                int index2 = ThreadLocalRandom.current().nextInt(list.size());
                TcpClient tcpClient = list.get(index2);
                if (tcpClient.getStatus() == Status.ONLINE) {
                    return tcpClient;
                }
                if (tcpClient.getStatus() == Status.OFFLINE) {
                    validAddrs.remove(addr);
                } else if (tcpClient.getStatus() == Status.INVALID) {
                    list.removeIf(client -> client.getStatus() == Status.INVALID);
                }
            } catch (Exception e) {
                logger.warn("select error, e = {}", e.toString());
            }
        }
        return null;
    }

    private void refresh() {
        if (refreshing.compareAndSet(false, true)) {
            try {
                List<TcpAddr> addrs = dynamicTcpAddrs.getAddrs();
                CopyOnWriteArrayList<TcpAddr> validAddrs = new CopyOnWriteArrayList<>();
                for (TcpAddr addr : addrs) {
                    try {
                        CopyOnWriteArrayList<TcpClient> list = map.get(addr);
                        if (list == null) {
                            list = new CopyOnWriteArrayList<>();
                            map.put(addr, list);
                        }
                        list.removeIf(client -> client.getStatus() == Status.INVALID);
                        Integer count = connectCount.get();
                        int diff = list.size() - count;
                        if (diff > 0) {
                            //remove
                            for (int i=count; i<list.size(); i++) {
                                TcpClient client = list.remove(i);
                                client.setClosingStatus();
                                logger.info("client will close after 60 seconds, addr = {}, id = {}", addr, client.getId());
                                scheduledExecutor.schedule(client::stop, 60, TimeUnit.SECONDS);
                            }
                        } else {
                            //add
                            diff = -diff;
                            for (int i=0; i<diff; i++) {
                                TcpClient client = new TcpClient(addr);
                                client.start();
                                if (client.getStatus() == Status.ONLINE
                                        || client.getStatus() == Status.OFFLINE) {
                                    list.add(client);
                                }
                            }
                        }
                        boolean valid = !list.isEmpty();
                        for (TcpClient client : list) {
                            if (client.getStatus() != Status.ONLINE) {
                                valid = false;
                            }
                        }
                        if (valid) {
                            validAddrs.add(addr);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                this.validAddrs = validAddrs;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                refreshing.compareAndSet(true, false);
            }
        }
    }
}

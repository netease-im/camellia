package com.netease.nim.camellia.http.accelerate.proxy.core.transport;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.constants.Constants;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.DynamicAddrs;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.Status;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.model.ServerAddr;
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
public abstract class AbstractTransportClients implements ITransportClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTransportClients.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("tcp-client-scheduler"));
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
            0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(10000), new CamelliaThreadFactory("tcp-client-executor"));

    private final DynamicAddrs dynamicAddrs;
    private final DynamicValueGetter<Integer> connectCount;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private ScheduledFuture<?> scheduledFuture;
    //连接管理
    private final ConcurrentHashMap<ServerAddr, CopyOnWriteArrayList<Client>> map = new ConcurrentHashMap<>();
    //合法地址列表
    private CopyOnWriteArrayList<ServerAddr> validAddrs = new CopyOnWriteArrayList<>();

    public AbstractTransportClients(DynamicAddrs dynamicAddrs, DynamicValueGetter<Integer> connectCount) {
        this.dynamicAddrs = dynamicAddrs;
        this.connectCount = connectCount;
    }

    public abstract Client initClinet(ServerAddr addr);

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
        for (Map.Entry<ServerAddr, CopyOnWriteArrayList<Client>> entry : map.entrySet()) {
            for (Client client : entry.getValue()) {
                client.stop();
            }
        }
    }

    @Override
    public CompletableFuture<ProxyResponse> send(ProxyRequest proxyRequest) {
        CompletableFuture<ProxyResponse> future = new CompletableFuture<>();
        Client client1 = selectClient();
        if (client1 != null) {
            proxyRequest.getLogBean().setTransportAddr(client1.getAddr().toString());
            proxyRequest.getLogBean().setTransportClientId(client1.getId());
            client1.send(proxyRequest, future);
            return future;
        }
        try {
            proxyRequest.getRequest().content().retain();
            //走一下异步，确保不会阻塞
            executor.submit(() -> {
                try {
                    refresh();
                    Client client2 = selectClient();
                    if (client2 != null) {
                        proxyRequest.getLogBean().setTransportAddr(client2.getAddr().toString());
                        proxyRequest.getLogBean().setTransportClientId(client2.getId());
                        client2.send(proxyRequest, future);
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

    private Client selectClient() {
        int retry = 3;
        while (retry-- > 0) {
            try {
                if (validAddrs.isEmpty()) {
                    return null;
                }
                int index = ThreadLocalRandom.current().nextInt(validAddrs.size());
                ServerAddr addr = validAddrs.get(index);
                CopyOnWriteArrayList<Client> list = map.get(addr);
                if (list.isEmpty()) {
                    validAddrs.remove(addr);
                }
                int index2 = ThreadLocalRandom.current().nextInt(list.size());
                Client client = list.get(index2);
                if (client.getStatus() == Status.ONLINE) {
                    return client;
                }
                if (client.getStatus() == Status.OFFLINE) {
                    validAddrs.remove(addr);
                } else if (client.getStatus() == Status.INVALID) {
                    list.removeIf(c -> c.getStatus() == Status.INVALID);
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
                List<ServerAddr> addrs = dynamicAddrs.getAddrs();
                CopyOnWriteArrayList<ServerAddr> validAddrs = new CopyOnWriteArrayList<>();
                for (ServerAddr addr : addrs) {
                    try {
                        CopyOnWriteArrayList<Client> list = map.get(addr);
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
                                Client client = list.remove(i);
                                client.setClosingStatus();
                                logger.info("client will close after 60 seconds, addr = {}, id = {}", addr, client.getId());
                                scheduledExecutor.schedule(client::stop, 60, TimeUnit.SECONDS);
                            }
                        } else {
                            //add
                            diff = -diff;
                            for (int i=0; i<diff; i++) {
                                Client client = initClinet(addr);
                                client.start();
                                if (client.getStatus() == Status.ONLINE
                                        || client.getStatus() == Status.OFFLINE) {
                                    list.add(client);
                                }
                            }
                        }
                        boolean valid = !list.isEmpty();
                        for (Client client : list) {
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

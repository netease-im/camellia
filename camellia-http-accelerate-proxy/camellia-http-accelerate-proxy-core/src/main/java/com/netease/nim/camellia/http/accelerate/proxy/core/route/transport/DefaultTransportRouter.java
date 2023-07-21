package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportClient;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.TcpAddr;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.TransportTcpClient;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.ConfigurationUtil;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultTransportRouter implements ITransportRouter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultTransportRouter.class);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("tcp-client-scheduler"));

    private final ConcurrentHashMap<String, DefaultDynamicTcpAddrs> addrsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DefaultConnectGetter> connectMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ITransportClient> serverMap = new ConcurrentHashMap<>();

    private TransportRouterConfig routerConfig;

    @Override
    public void start() {
        reload();
        logger.info("default transport router start success");
    }

    @Override
    public synchronized void reload() {
        boolean disabled = DynamicConf.getBoolean("transport.route.config.disabled", false);
        if (disabled) {
            logger.warn("default transport router disabled");
            return;
        }
        String file = DynamicConf.getString("transport.route.config", "transport_route.json");
        URL resource = Thread.currentThread().getContextClassLoader().getResource(file);
        if (resource == null) {
            throw new IllegalStateException(file + " not exists");
        }
        String path = resource.getPath();
        String jsonString = ConfigurationUtil.getJsonString(path);
        TransportRouterConfig config = JSONObject.parseObject(jsonString, TransportRouterConfig.class);
        if (config == null) {
            logger.error("parse {} failed", file);
            return;
        }
        Set<String> toRemovedServer = new HashSet<>(serverMap.keySet());
        List<TransportServer> servers = config.getServers();
        for (TransportServer server : servers) {
            String name = server.getServer();
            toRemovedServer.remove(name);
            TransportServerType type = server.getType();
            if (type == TransportServerType.tcp) {
                DefaultDynamicTcpAddrs addrs = addrsMap.get(name);
                if (addrs == null) {
                    addrs = new DefaultDynamicTcpAddrs(TcpAddr.toAddrs(server.getAddrs()));
                    addrsMap.put(name, addrs);
                } else {
                    addrs.updateAddrs(TcpAddr.toAddrs(server.getAddrs()));
                }
                DefaultConnectGetter connectGetter = connectMap.get(name);
                if (connectGetter == null) {
                    connectGetter = new DefaultConnectGetter(server.getConnect());
                    connectMap.put(name, connectGetter);
                } else {
                    connectGetter.updateConnect(server.getConnect());
                }
                ITransportClient transportClient = serverMap.get(name);
                if (transportClient == null) {
                    transportClient = new TransportTcpClient(addrs, connectGetter);
                    transportClient.start();
                    serverMap.put(name, transportClient);
                }
            } else {
                throw new IllegalArgumentException(type + " not support");
            }
        }
        if (!toRemovedServer.isEmpty()) {
            for (String server : toRemovedServer) {
                ITransportClient client = serverMap.get(server);
                if (client != null) {
                    logger.warn("server = {} will close after 120 seconds", server);
                    scheduledExecutor.schedule(client::stop, 120, TimeUnit.SECONDS);
                }
                addrsMap.remove(server);
                connectMap.remove(server);
            }
        }
        this.routerConfig = config;
    }

    @Override
    public ITransportClient select(ProxyRequest request) {
        try {
            List<TransportRoute> routes = routerConfig.getRoutes();
            for (TransportRoute route : routes) {
                String server = select0(request, route);
                if (server == null) continue;
                ITransportClient client = serverMap.get(server);
                if (client != null) {
                    return client;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private String select0(ProxyRequest request, TransportRoute route) {
        try {
            TransportRouteType type = route.getType();
            if (type == TransportRouteType.match_all) {
                return route.getServer();
            }
            String host = request.getLogBean().getHost();
            if (type == TransportRouteType.match_host) {
                if (route.getHost().equals(host)) {
                    return route.getServer();
                }
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
}

package com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config.*;
import com.netease.nim.camellia.http.accelerate.proxy.core.upstream.IUpstreamClient;
import com.netease.nim.camellia.http.accelerate.proxy.core.upstream.OkHttpUpstreamClient;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.ConfigurationUtil;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/7/7
 */
public class DefaultUpstreamRouter implements IUpstreamRouter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultUpstreamRouter.class);
    private final ConcurrentHashMap<String, DefaultDynamicUpstreamAddrs> addrsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DefaultDynamicHeartbeatTimeoutGetter> heartbeatTimeoutMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DefaultDynamicUpstreamHealthUriGetter> healthUriMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IUpstreamClient> upstreamMap = new ConcurrentHashMap<>();

    private UpstreamRouterConfig routerConfig;

    @Override
    public void start() {
        reload();
        logger.info("default upstream router start success");
    }

    @Override
    public synchronized void reload() {
        String file = DynamicConf.getString("upstream.route.config", "upstream_route.json");
        URL resource = Thread.currentThread().getContextClassLoader().getResource(file);
        if (resource == null) {
            throw new IllegalStateException(file + " not exists");
        }
        String path = resource.getPath();
        String jsonString = ConfigurationUtil.getJsonString(path);
        UpstreamRouterConfig config = JSONObject.parseObject(jsonString, UpstreamRouterConfig.class);
        if (config == null) {
            logger.warn("{} parse failed", file);
            return;
        }
        List<Upstream> upstreams = config.getUpstreams();
        for (Upstream upstream : upstreams) {
            String name = upstream.getUpstream();
            UpstreamType type = upstream.getType();
            if (type == UpstreamType.http) {
                DefaultDynamicUpstreamAddrs addrs = addrsMap.get(name);
                if (addrs == null) {
                    addrs = new DefaultDynamicUpstreamAddrs(upstream.getAddrs());
                    addrsMap.put(name, addrs);
                } else {
                    addrs.updateAddrs(upstream.getAddrs());
                }
                DefaultDynamicHeartbeatTimeoutGetter timeoutGetter = heartbeatTimeoutMap.get(name);
                if (timeoutGetter == null) {
                    timeoutGetter = new DefaultDynamicHeartbeatTimeoutGetter(upstream.getHeartbeatTimeout());
                    heartbeatTimeoutMap.put(name, timeoutGetter);
                } else {
                    timeoutGetter.updateTimeout(upstream.getHeartbeatTimeout());
                }
                DefaultDynamicUpstreamHealthUriGetter healthUriGetter = healthUriMap.get(name);
                if (healthUriGetter == null) {
                    healthUriGetter = new DefaultDynamicUpstreamHealthUriGetter(upstream.getHeartbeatUri());
                    healthUriMap.put(name, healthUriGetter);
                } else {
                    healthUriGetter.updateHealthUri(upstream.getHeartbeatUri());
                }
                IUpstreamClient upstreamClient = upstreamMap.get(name);
                if (upstreamClient == null) {
                    upstreamClient = new OkHttpUpstreamClient(addrs, healthUriGetter, timeoutGetter);
                    upstreamMap.put(name, upstreamClient);
                }
            } else {
                throw new IllegalArgumentException(type + " not support");
            }
        }
        this.routerConfig = config;
    }

    @Override
    public IUpstreamClient select(ProxyRequest request) {
        List<UpstreamRoute> routes = routerConfig.getRoutes();
        for (UpstreamRoute route : routes) {
            String upstream = select0(request, route);
            if (upstream == null) continue;
            IUpstreamClient client = upstreamMap.get(upstream);
            if (client != null) {
                return client;
            }
        }
        return null;
    }

    private String select0(ProxyRequest request, UpstreamRoute route) {
        try {
            UpstreamRouteType type = route.getType();
            if (type == UpstreamRouteType.match_all) {
                return route.getUpstream();
            }
            String host = request.getRequest().headers().get("Host");
            if (type == UpstreamRouteType.match_host) {
                if (route.getHost().equals(host)) {
                    return route.getUpstream();
                }
            }
            return null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
}

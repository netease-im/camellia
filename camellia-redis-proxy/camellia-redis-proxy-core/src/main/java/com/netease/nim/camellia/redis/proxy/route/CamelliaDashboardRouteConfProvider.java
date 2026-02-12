package com.netease.nim.camellia.redis.proxy.route;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiResponse;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2026/2/11
 */
public class CamelliaDashboardRouteConfProvider extends RouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDashboardRouteConfProvider.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-dashboard-route"));

    private final String password;
    private final CamelliaApi camelliaApi;
    private final boolean dynamic;

    private final CamelliaApiResponse defaultResponse;
    private final ConcurrentHashMap<String, CamelliaApiResponse> map = new ConcurrentHashMap<>();

    public CamelliaDashboardRouteConfProvider() {
        password = ServerConf.password();
        dynamic = ProxyDynamicConf.getBoolean("camellia.dashboard.dynamic", true);
        long bid = ProxyDynamicConf.getLong("camellia.dashboard.bid", -1);
        String bgroup = ProxyDynamicConf.getString("camellia.dashboard.bgroup", "default");
        String url = ProxyDynamicConf.getString("camellia.dashboard.url", null);
        int connectTimeoutMillis = ProxyDynamicConf.getInt("camellia.dashboard.connect.timeout.millis", 10000);
        int readTimeoutMillis = ProxyDynamicConf.getInt("camellia.dashboard.read.timeout.millis", 10000);
        String headers = ProxyDynamicConf.getString("camellia.dashboard.headers", null);
        Map<String, String> headerMap = new HashMap<>();
        if (headers != null) {
            JSONObject jsonObject = JSONObject.parseObject(headers);
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                headerMap.put(entry.getKey(), entry.getValue().toString());
            }
        }
        camelliaApi = CamelliaApiUtil.init(url, connectTimeoutMillis, readTimeoutMillis, headerMap);
        if (bid > 0 && !bgroup.isEmpty()) {
            defaultResponse = camelliaApi.getResourceTable(bid, bgroup, null);
            if (defaultResponse.getCode() != 200) {
                throw new IllegalArgumentException("code=" + defaultResponse.getCode());
            }
            map.put(bid + "|" + bgroup, defaultResponse);
        } else {
            defaultResponse = null;
        }
        int checkIntervalMillis = ProxyDynamicConf.getInt("camellia.dashboard.check.interval.millis", 5000);
        scheduler.scheduleAtFixedRate(this::reload, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = new ClientIdentity();
        if (userName != null && !userName.equals("default")) {
            clientIdentity.setPass(false);
            return clientIdentity;
        }
        clientIdentity.setPass(!StringUtil.isNullOrEmpty(this.password) && this.password.equals(password));
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return password != null;
    }

    @Override
    public ResourceTable getRouteConfig(long bid, String bgroup) {
        if (bid < 0 || bgroup == null || bgroup.isEmpty()) {
            if (defaultResponse == null) {
                return null;
            }
            return defaultResponse.getResourceTable();
        }
        String key = bid + "|" + bgroup;
        CamelliaApiResponse response = map.get(key);
        if (response != null) {
            return response.getResourceTable();
        }
        response = map.computeIfAbsent(key, k -> {
            CamelliaApiResponse newResponse = camelliaApi.getResourceTable(bid, bgroup, null);
            if (newResponse.getCode() != 200) {
                throw new IllegalArgumentException("code=" + newResponse.getCode());
            }
            return newResponse;
        });
        return response.getResourceTable();
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return dynamic;
    }

    private void reload() {
        try {
            Set<String> set = new HashSet<>(map.keySet());
            for (String key : set) {
                String[] split = key.split("\\|");
                long bid = Long.parseLong(split[0]);
                String bgroup = split[1];
                CamelliaApiResponse response = map.get(key);
                CamelliaApiResponse newResponse = camelliaApi.getResourceTable(bid, bgroup, response.getMd5());
                if (newResponse == null || newResponse.getCode() != 200) {
                    continue;
                }
                map.put(key, newResponse);
                invokeUpdateResourceTable(bid, bgroup, newResponse.getResourceTable());
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }
}

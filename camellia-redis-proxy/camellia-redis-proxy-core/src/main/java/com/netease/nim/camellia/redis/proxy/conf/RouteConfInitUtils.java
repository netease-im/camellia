package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.tools.utils.FileUtils;
import java.util.Map;

/**
 * Created by caojiajun on 2026/2/9
 */
public class RouteConfInitUtils {

    public static CamelliaRouteProperties init() {
        String type = ProxyDynamicConf.getString("route.type", "local");
        CamelliaRouteProperties properties = new CamelliaRouteProperties();
        if (type.equalsIgnoreCase(CamelliaRouteProperties.Type.LOCAL.name())) {
            properties.setType(CamelliaRouteProperties.Type.LOCAL);
            properties.setLocal(initLocal());
        } else if (type.equalsIgnoreCase(CamelliaRouteProperties.Type.REMOTE.name())) {
            properties.setType(CamelliaRouteProperties.Type.REMOTE);
            properties.setRemote(initRemote());
        } else if (type.equalsIgnoreCase(CamelliaRouteProperties.Type.CUSTOM.name())) {
            properties.setType(CamelliaRouteProperties.Type.CUSTOM);
            properties.setCustom(initCustom());
        } else {
            throw new IllegalArgumentException("unknown 'route.type'");
        }
        return properties;
    }

    private static CamelliaRouteProperties.LocalProperties initLocal() {
        String routeTable = ProxyDynamicConf.getString("local.route.table", null);
        CamelliaRouteProperties.LocalProperties localProperties = new CamelliaRouteProperties.LocalProperties();
        if (routeTable != null) {
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(routeTable);
            localProperties.setResourceTable(resourceTable);
        } else {
            String file = ProxyDynamicConf.getString("local.route.table.file", null);
            FileUtils.FileInfo fileInfo = FileUtils.readDynamic(file);
            if (fileInfo == null) {
                throw new IllegalArgumentException("illegal 'local.route.table.file'");
            }
            localProperties.setResourceTableFilePath(fileInfo.getFilePath());
            localProperties.setCheckIntervalMillis(ProxyDynamicConf.getInt("local.route.table.file.check.interval.millis", 3000));
        }
        return localProperties;
    }

    private static CamelliaRouteProperties.RemoteProperties initRemote() {
        String url = ProxyDynamicConf.getString("remote.route.url", null);
        long bid = ProxyDynamicConf.getLong("remote.route.bid", -1L);
        String bgroup = ProxyDynamicConf.getString("remote.route.bgroup", "default");
        boolean dynamic = ProxyDynamicConf.getBoolean("remote.route.dynamic", Constants.Remote.dynamic);
        boolean monitorEnable = ProxyDynamicConf.getBoolean("remote.route.monitor.enable", Constants.Remote.monitorEnable);
        long checkIntervalMillis = ProxyDynamicConf.getLong("remote.route.check.interval.millis", Constants.Remote.checkIntervalMillis);
        int connectTimeoutMillis = ProxyDynamicConf.getInt("remote.route.connect.timeout.millis", Constants.Remote.connectTimeoutMillis);
        int readTimeoutMillis = ProxyDynamicConf.getInt("remote.route.read.timeout.millis", Constants.Remote.readTimeoutMillis);
        String headers = ProxyDynamicConf.getString("remote.route.headers", null);
        CamelliaRouteProperties.RemoteProperties remoteProperties = new CamelliaRouteProperties.RemoteProperties();
        remoteProperties.setUrl(url);
        remoteProperties.setBid(bid);
        remoteProperties.setBgroup(bgroup);
        remoteProperties.setDynamic(dynamic);
        remoteProperties.setMonitorEnable(monitorEnable);
        remoteProperties.setCheckIntervalMillis(checkIntervalMillis);
        remoteProperties.setConnectTimeoutMillis(connectTimeoutMillis);
        remoteProperties.setReadTimeoutMillis(readTimeoutMillis);
        if (headers != null) {
            JSONObject headerJson = JSONObject.parseObject(headers);
            for (Map.Entry<String, Object> stringObjectEntry : headerJson.entrySet()) {
                remoteProperties.getHeaderMap().put(stringObjectEntry.getKey(), String.valueOf(stringObjectEntry.getValue()));
            }
        }
        return remoteProperties;
    }

    private static CamelliaRouteProperties.CustomProperties initCustom() {
        long bid = ProxyDynamicConf.getLong("custom.route.bid", -1L);
        String bgroup = ProxyDynamicConf.getString("custom.route.bgroup", "default");
        boolean dynamic = ProxyDynamicConf.getBoolean("custom.route.dynamic", Constants.Custom.dynamic);
        long reloadIntervalMillis = ProxyDynamicConf.getLong("custom.route.reload.interval.millis", Constants.Custom.reloadIntervalMillis);
        String proxyRouteConfUpdaterClassName = ProxyDynamicConf.getString("custom.route.conf.provider.class.name", null);
        ProxyRouteConfUpdater proxyRouteConfUpdater = (ProxyRouteConfUpdater) ServerConf.getProxyBeanFactory().getBean(BeanInitUtils.parseClass(proxyRouteConfUpdaterClassName));

        CamelliaRouteProperties.CustomProperties customProperties = new CamelliaRouteProperties.CustomProperties();
        customProperties.setBid(bid);
        customProperties.setBgroup(bgroup);
        customProperties.setDynamic(dynamic);
        customProperties.setReloadIntervalMillis(reloadIntervalMillis);
        customProperties.setProxyRouteConfUpdater(proxyRouteConfUpdater);
        return customProperties;
    }

}

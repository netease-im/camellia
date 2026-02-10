package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.redis.proxy.route.ProxyRouteConfUpdater;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2019/11/6.
 */
public class CamelliaRouteProperties {

    private Type type = Type.LOCAL;
    private LocalProperties local = new LocalProperties();
    private RemoteProperties remote;
    private CustomProperties custom;

    public static enum Type {
        LOCAL,
        REMOTE,
        CUSTOM,
        ;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public LocalProperties getLocal() {
        return local;
    }

    public void setLocal(LocalProperties local) {
        this.local = local;
    }

    public RemoteProperties getRemote() {
        return remote;
    }

    public void setRemote(RemoteProperties remote) {
        this.remote = remote;
    }

    public CustomProperties getCustom() {
        return custom;
    }

    public void setCustom(CustomProperties custom) {
        this.custom = custom;
    }

    public static class LocalProperties {
        private ResourceTable resourceTable;
        private String resourceTableFilePath;
        private long checkIntervalMillis = Constants.Remote.checkIntervalMillis;

        public ResourceTable getResourceTable() {
            return resourceTable;
        }

        public void setResourceTable(ResourceTable resourceTable) {
            this.resourceTable = resourceTable;
        }

        public String getResourceTableFilePath() {
            return resourceTableFilePath;
        }

        public void setResourceTableFilePath(String resourceTableFilePath) {
            this.resourceTableFilePath = resourceTableFilePath;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }
    }

    public static class RemoteProperties {

        private String url;
        private long bid;
        private String bgroup;
        private boolean dynamic = Constants.Remote.dynamic;
        private boolean monitorEnable = Constants.Remote.monitorEnable;
        private long checkIntervalMillis = Constants.Remote.checkIntervalMillis;
        private int connectTimeoutMillis = Constants.Remote.connectTimeoutMillis;
        private int readTimeoutMillis = Constants.Remote.readTimeoutMillis;
        private Map<String, String> headerMap = new HashMap<>();

        public Map<String, String> getHeaderMap() {
            return headerMap;
        }

        public void setHeaderMap(Map<String, String> headerMap) {
            this.headerMap = headerMap;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getBid() {
            return bid;
        }

        public void setBid(long bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
        }

        public boolean isMonitorEnable() {
            return monitorEnable;
        }

        public void setMonitorEnable(boolean monitorEnable) {
            this.monitorEnable = monitorEnable;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }
    }

    public static class CustomProperties {
        private long bid;
        private String bgroup;
        private boolean dynamic = Constants.Custom.dynamic;
        private long reloadIntervalMillis = Constants.Custom.reloadIntervalMillis;
        private ProxyRouteConfUpdater proxyRouteConfUpdater;

        public long getBid() {
            return bid;
        }

        public void setBid(long bid) {
            this.bid = bid;
        }

        public String getBgroup() {
            return bgroup;
        }

        public void setBgroup(String bgroup) {
            this.bgroup = bgroup;
        }

        public boolean isDynamic() {
            return dynamic;
        }

        public void setDynamic(boolean dynamic) {
            this.dynamic = dynamic;
        }

        public long getReloadIntervalMillis() {
            return reloadIntervalMillis;
        }

        public void setReloadIntervalMillis(long reloadIntervalMillis) {
            this.reloadIntervalMillis = reloadIntervalMillis;
        }

        public ProxyRouteConfUpdater getProxyRouteConfUpdater() {
            return proxyRouteConfUpdater;
        }

        public void setProxyRouteConfUpdater(ProxyRouteConfUpdater proxyRouteConfUpdater) {
            this.proxyRouteConfUpdater = proxyRouteConfUpdater;
        }
    }
}

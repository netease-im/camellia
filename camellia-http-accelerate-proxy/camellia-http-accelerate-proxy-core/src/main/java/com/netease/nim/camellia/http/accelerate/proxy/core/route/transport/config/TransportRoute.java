package com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.config;

/**
 * Created by caojiajun on 2023/7/10
 */
public class TransportRoute {

    private TransportRouteType type;
    private String host;
    private String server;
    private String backupServer;

    public TransportRouteType getType() {
        return type;
    }

    public void setType(TransportRouteType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getBackupServer() {
        return backupServer;
    }

    public void setBackupServer(String backupServer) {
        this.backupServer = backupServer;
    }
}

package com.netease.nim.camellia.feign.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by caojiajun on 2022/3/30
 */
@ConfigurationProperties(prefix = "camellia-feign")
public class CamelliaFeignProperties {

    private Remote remote = new Remote();
    private Client client = new Client();

    public Remote getRemote() {
        return remote;
    }

    public void setRemote(Remote remote) {
        this.remote = remote;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public static class Remote {
        private boolean enable;//是否走camellia-dashboard
        private String url;//camellia-dashboard的url
        private long checkIntervalMillis = 5000;

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getCheckIntervalMillis() {
            return checkIntervalMillis;
        }

        public void setCheckIntervalMillis(long checkIntervalMillis) {
            this.checkIntervalMillis = checkIntervalMillis;
        }
    }

    public static class Client {
        private long connectTimeoutMillis = 3000;//默认的连接超时时间
        private long readTimeoutMillis = 3000;//默认的请求超时时间
        private boolean circuitBreakerEnable = false;//默认的熔断器是否打开

        public long getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(long connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public long getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(long readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        public boolean isCircuitBreakerEnable() {
            return circuitBreakerEnable;
        }

        public void setCircuitBreakerEnable(boolean circuitBreakerEnable) {
            this.circuitBreakerEnable = circuitBreakerEnable;
        }
    }
}

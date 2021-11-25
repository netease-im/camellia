package com.netease.nim.camellia.redis.zk.registry.springboot;

import com.netease.nim.camellia.redis.proxy.discovery.zk.ZkConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
@ConfigurationProperties(prefix = "camellia-redis-zk-registry")
public class CamelliaRedisProxyZkRegistryProperties {

    /**
     * 是否注册到zk
     */
    private boolean enable;

    /**
     * zk的地址，例子：127.0.0.1:2181,127.0.0.2.2181
     */
    private String zkUrl;

    /**
     * zk的一些连接配置
     */
    private int sessionTimeoutMs = ZkConstants.sessionTimeoutMs;
    private int connectionTimeoutMs = ZkConstants.connectionTimeoutMs;
    private int baseSleepTimeMs = ZkConstants.baseSleepTimeMs;
    private int maxRetries = ZkConstants.maxRetries;

    /**
     * 注册到zk时的根路径
     */
    private String basePath = ZkConstants.basePath;

    /**
     * 手动指定注册的host
     */
    private String host;

    /**
     * 自动获取的情况下是否偏好使用hostname注册，若true，则ignoredInterfaces和preferredNetworks参数无效
     */
    private boolean preferredHostName = ZkConstants.preferredHostName;

    /**
     * host字段为空的情况下，自动获取host，此时，忽略哪些网卡，逗号分隔
     */
    private String ignoredInterfaces;

    /**
     * host字段为空的情况下，自动获取host，此时，偏向于使用哪些网卡，逗号分隔
     */
    private String preferredNetworks;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getZkUrl() {
        return zkUrl;
    }

    public void setZkUrl(String zkUrl) {
        this.zkUrl = zkUrl;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getBaseSleepTimeMs() {
        return baseSleepTimeMs;
    }

    public void setBaseSleepTimeMs(int baseSleepTimeMs) {
        this.baseSleepTimeMs = baseSleepTimeMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isPreferredHostName() {
        return preferredHostName;
    }

    public void setPreferredHostName(boolean preferredHostName) {
        this.preferredHostName = preferredHostName;
    }

    public String getIgnoredInterfaces() {
        return ignoredInterfaces;
    }

    public void setIgnoredInterfaces(String ignoredInterfaces) {
        this.ignoredInterfaces = ignoredInterfaces;
    }

    public String getPreferredNetworks() {
        return preferredNetworks;
    }

    public void setPreferredNetworks(String preferredNetworks) {
        this.preferredNetworks = preferredNetworks;
    }
}

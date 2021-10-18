package com.netease.nim.camellia.redis.proxy.nacos.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2021/10/18
 */
@ConfigurationProperties(prefix = "camellia-redis-proxy-nacos")
public class CamelliaRedisProxyNacosProperties {

    private boolean enable = false;
    private String serverAddr;
    private Map<String, String> nacosConf = new HashMap<>();
    private long timeoutMs = 10000;
    private List<ConfFile> confFileList = new ArrayList<>();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getServerAddr() {
        return serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    public Map<String, String> getNacosConf() {
        return nacosConf;
    }

    public void setNacosConf(Map<String, String> nacosConf) {
        this.nacosConf = nacosConf;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public List<ConfFile> getConfFileList() {
        return confFileList;
    }

    public void setConfFileList(List<ConfFile> confFileList) {
        this.confFileList = confFileList;
    }

    public static class ConfFile {
        private String fileName;
        private String dataId;
        private String group;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getDataId() {
            return dataId;
        }

        public void setDataId(String dataId) {
            this.dataId = dataId;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }
    }
}

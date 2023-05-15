package com.netease.nim.camellia.hot.key.server.config.nacos;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Created by caojiajun on 2023/5/15
 */
public class NacosHotKeyConfigService extends HotKeyConfigService {

    private static final Logger logger = LoggerFactory.getLogger(NacosHotKeyConfigService.class);

    private String dataId;
    private String group;
    private long timeoutMs;
    private ConfigService configService;
    private Map<String, String> configMap = new HashMap<>();

    @Override
    public HotKeyConfig get(String namespace) {
        String configStr = configMap.get(namespace);
        return JSONObject.parseObject(configStr, HotKeyConfig.class);
    }

    @Override
    public void init(HotKeyServerProperties properties) {
        Map<String, String> config = properties.getConfig();
        Properties nacosProps = new Properties();
        try {
            String prefix = "nacos.";
            for (Map.Entry<String, String> entry : config.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.startsWith(prefix)) {
                    key = key.substring(prefix.length());
                    nacosProps.put(key, value);
                }
            }
            this.configService = NacosFactory.createConfigService(nacosProps);
            this.dataId = nacosProps.getProperty("dataId");
            this.group = nacosProps.getProperty("group");
            if (dataId == null) {
                throw new IllegalArgumentException("missing 'nacos.dataId'");
            }
            if (group == null) {
                throw new IllegalArgumentException("missing 'nacos.group'");
            }
            String timeoutMsStr = nacosProps.getProperty("timeoutMs");
            if (timeoutMsStr == null) {
                this.timeoutMs = 10000L;
            } else {
                try {
                    this.timeoutMs = Long.parseLong(timeoutMsStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("illegal 'nacos.timeoutMs'");
                }
            }
            boolean success = reload();
            if (!success) {
                throw new IllegalStateException("reload from nacos error");
            }
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                @Override
                public void receiveConfigInfo(String content) {
                    try {
                        logger.info("nacos conf update!");
                        Set<String> namespaceSet = NacosHotKeyConfigService.this.configMap.keySet();
                        NacosHotKeyConfigService.this.configMap = toMap(content);
                        for (String namespace : namespaceSet) {
                            NacosHotKeyConfigService.this.invokeUpdate(namespace);
                        }
                    } catch (Exception e) {
                        logger.error("receiveConfigInfo error, content = {}", content);
                    }
                }
            });
            logger.info("NacosHotKeyConfigService init success, nacosProps = {}", nacosProps);
        } catch (Exception e) {
            logger.info("NacosHotKeyConfigService init error, nacosProps = {}", nacosProps, e);
            throw new IllegalArgumentException(e);
        }
    }

    private boolean reload() {
        try {
            String content = configService.getConfig(dataId, group, timeoutMs);
            configMap = toMap(content);
            return true;
        } catch (Exception e) {
            logger.error("reload from nacos error, dataId = {}, group = {}, timeouMs = {}", dataId, group, timeoutMs, e);
            return false;
        }
    }

    private Map<String, String> toMap(String content) {
        String[] split = content.split("\n");
        Map<String, String> conf = new HashMap<>();
        for (String line : split) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("#")) {
                continue;
            }
            int index = line.indexOf("=");
            String key = line.substring(0, index);
            String value = line.substring(index + 1);
            conf.put(key, value);
        }
        return conf;
    }
}

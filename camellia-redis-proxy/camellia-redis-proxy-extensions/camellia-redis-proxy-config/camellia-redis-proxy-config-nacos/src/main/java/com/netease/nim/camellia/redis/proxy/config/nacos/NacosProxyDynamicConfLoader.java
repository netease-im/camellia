package com.netease.nim.camellia.redis.proxy.config.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoaderUtil;
import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Created by caojiajun on 2023/3/29
 */
public class NacosProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(NacosProxyDynamicConfLoader.class);

    private Map<String, String> initConf = new HashMap<>();
    private Map<String, String> conf = new HashMap<>();

    private String dataId;
    private String group;
    private long timeoutMs;
    private ConfigService configService;
    private ConfigContentType contentType = ConfigContentType.properties;

    @Override
    public Map<String, String> load() {
        //reload
        reload();
        //conf
        Map<String, String> map = new HashMap<>(initConf);
        map.putAll(conf);
        //dynamic specific conf
        Pair<String, Map<String, String>> pair = ProxyDynamicConfLoaderUtil.tryLoadDynamicConfBySpecificFilePath(conf, contentType);
        if (pair.getSecond() != null) {
            map.putAll(pair.getSecond());
        }
        return map;
    }

    /**
     * Init config from nacos
     */
    @Override
    public void init(Map<String, String> initConf) {
        this.initConf = new HashMap<>(initConf);
        Properties nacosProps = new Properties();
        try {
            // Get nacos config by prefix.
            String prefix = "nacos.";
            for (Map.Entry<String, String> entry : initConf.entrySet()) {
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
            contentType = ConfigContentType.getByValue(initConf.get("nacos.config.type"), ConfigContentType.properties);
            boolean success = reload();
            if (!success) {
                throw new IllegalStateException("reload from nacos error");
            }
            // Listen config changes
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                @Override
                public void receiveConfigInfo(String content) {
                    try {
                        logger.info("nacos conf update!");
                        NacosProxyDynamicConfLoader.this.conf = ConfigurationUtil.contentToMap(content, contentType);
                        ProxyDynamicConf.reload();
                    } catch (Exception e) {
                        logger.error("receiveConfigInfo error, content = {}", content);
                    }
                }
            });
            logger.info("NacosProxyDynamicConfLoader init success, nacosProps = {}", nacosProps);
        } catch (Exception e) {
            logger.info("NacosProxyDynamicConfLoader init error, nacosProps = {}", nacosProps, e);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Reload config from nacos
     */
    private boolean reload() {
        try {
            String content = configService.getConfig(dataId, group, timeoutMs);
            this.conf = ConfigurationUtil.contentToMap(content, contentType);
            return true;
        } catch (Exception e) {
            logger.error("reload from nacos error, dataId = {}, group = {}, timeouMs = {}", dataId, group, timeoutMs, e);
            return false;
        }
    }


}

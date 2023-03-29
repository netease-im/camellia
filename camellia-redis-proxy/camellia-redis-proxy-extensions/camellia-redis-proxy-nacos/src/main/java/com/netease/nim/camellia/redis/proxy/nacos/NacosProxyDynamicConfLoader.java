package com.netease.nim.camellia.redis.proxy.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
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

    @Override
    public Map<String, String> load() {
        Map<String, String> map = new HashMap<>();
        map.putAll(initConf);
        map.putAll(conf);
        return map;
    }

    @Override
    public void updateInitConf(Map<String, String> initConf) {
        init(initConf);
    }

    private void init(Map<String, String> initConf) {
        try {
            Properties nacosProps = new Properties();
            String prefix = "nacos.";
            for (Map.Entry<String, String> entry : initConf.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.startsWith(prefix)) {
                    key = key.substring(prefix.length());
                    nacosProps.put(key, value);
                } else {
                    this.initConf.put(key, value);
                }
            }
            ConfigService configService = NacosFactory.createConfigService(nacosProps);
            String dataId = nacosProps.getProperty("dataId");
            String group = nacosProps.getProperty("group");
            long timeoutMs = nacosProps.getProperty("timeoutMs") == null ? 10000L : Long.parseLong(nacosProps.getProperty("timeoutMs"));
            String content = configService.getConfig(dataId, group, timeoutMs);
            this.conf = toMap(content);
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                @Override
                public void receiveConfigInfo(String content) {
                    try {
                        logger.info("nacos conf update!");
                        NacosProxyDynamicConfLoader.this.conf = toMap(content);
                        ProxyDynamicConf.reload();
                    } catch (Exception e) {
                        logger.error("receiveConfigInfo error, content = {}", content);
                    }
                }
            });
            logger.info("NacosProxyDynamicConfLoader init success, nacosProps = {}", nacosProps);
        } catch (NacosException e) {
            throw new IllegalArgumentException(e);
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
            int index = line.indexOf("=");
            String key = line.substring(0, index);
            String value = line.substring(index + 1);
            conf.put(key, value);
        }
        return conf;
    }

}

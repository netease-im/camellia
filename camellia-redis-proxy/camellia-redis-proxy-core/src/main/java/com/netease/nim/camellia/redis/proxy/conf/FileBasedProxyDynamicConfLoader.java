package com.netease.nim.camellia.redis.proxy.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by caojiajun on 2023/2/24
 */
public class FileBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedProxyDynamicConfLoader.class);
    private static final String defaultFileName = "camellia-redis-proxy.properties";

    private Map<String, String> initConf = new HashMap<>();

    @Override
    public Map<String, String> load() {
        Map<String, String> conf = new HashMap<>(initConf);
        String fileName = conf.get("dynamic.conf.file.name");
        if (fileName == null) {
            fileName = defaultFileName;
        }
        try {
            URL url = ProxyDynamicConf.class.getClassLoader().getResource(fileName);
            if (url != null) {
                Properties props = new Properties();
                try {
                    props.load(new FileInputStream(url.getPath()));
                } catch (IOException e) {
                    props.load(ProxyDynamicConf.class.getClassLoader().getResourceAsStream(fileName));
                }
                conf.putAll(ConfigurationUtil.propertiesToMap(props));
            }
            String filePath = conf.get("dynamic.conf.file.path");
            if (filePath != null) {
                try {
                    Properties props = new Properties();
                    props.load(new FileInputStream(filePath));
                    conf.putAll(ConfigurationUtil.propertiesToMap(props));
                } catch (Exception e) {
                    logger.error("dynamic.conf.file.path={} load error, use classpath:{} default", filePath, fileName, e);
                }
            }
            return conf;
        } catch (Exception e) {
            throw new IllegalArgumentException("load error, fileName = " + fileName, e);
        }
    }

    @Override
    public void updateInitConf(Map<String, String> initConf) {
        this.initConf = initConf;
    }
}

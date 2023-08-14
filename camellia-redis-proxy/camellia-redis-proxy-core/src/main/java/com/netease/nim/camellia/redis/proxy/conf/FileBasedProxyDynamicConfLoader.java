package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Load configuration from the file.
 * Created by caojiajun on 2023/2/24
 */
public class FileBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedProxyDynamicConfLoader.class);
    private static final String DEFAULT_FILE_NAME = "camellia-redis-proxy.properties";
    public static final String DYNAMIC_CONF_FILE_NAME = "dynamic.conf.file.name";
    /**
     * Absolute path to the configuration file
     */
    public static final String DYNAMIC_CONF_FILE_PATH = "dynamic.conf.file.path";

    private Map<String, String> initConf = new HashMap<>();

    /**
     * 如果没有配置{@link FileBasedProxyDynamicConfLoader#DYNAMIC_CONF_FILE_NAME}，就从 {@link FileBasedProxyDynamicConfLoader#DEFAULT_FILE_NAME} 中读取
     * 如果指定了{@link FileBasedProxyDynamicConfLoader#DYNAMIC_CONF_FILE_PATH} 也会进行读取。
     * 注意！配置文件的优先级是: {@link FileBasedProxyDynamicConfLoader#DYNAMIC_CONF_FILE_PATH},{@link FileBasedProxyDynamicConfLoader#DYNAMIC_CONF_FILE_NAME} or {@link FileBasedProxyDynamicConfLoader#DEFAULT_FILE_NAME}, {@link ProxyDynamicConf#conf}
     * <p>If DYNAMIC_CONF_FILE_NAME is not configured, it will read from DEFAULT_FILE_NAME.
     * If DYNAMIC_CONF_FILE_PATH is specified, it will also be read.
     * Notice! The priority of file: {@link FileBasedProxyDynamicConfLoader#DYNAMIC_CONF_FILE_PATH},{@link FileBasedProxyDynamicConfLoader#DYNAMIC_CONF_FILE_NAME} or {@link FileBasedProxyDynamicConfLoader#DEFAULT_FILE_NAME}, {@link ProxyDynamicConf#conf}
     */
    @Override
    public Map<String, String> load() {
        Map<String, String> conf = new HashMap<>(initConf);
        String fileName = conf.get(DYNAMIC_CONF_FILE_NAME);
        if (fileName == null) {
            fileName = DEFAULT_FILE_NAME;
        }
        try {
            URL url = ProxyDynamicConf.class.getClassLoader().getResource(fileName);
            if (url != null) {
                Properties props = new Properties();
                try {
                    props.load(Files.newInputStream(Paths.get(url.getPath())));
                } catch (IOException e) {
                    props.load(ProxyDynamicConf.class.getClassLoader().getResourceAsStream(fileName));
                }
                conf.putAll(ConfigurationUtil.propertiesToMap(props));
            }
            String filePath = conf.get(DYNAMIC_CONF_FILE_PATH);
            if (filePath != null) {
                try {
                    Properties props = new Properties();
                    props.load(Files.newInputStream(Paths.get(filePath)));
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
    public void init(Map<String, String> initConf) {
        this.initConf = new HashMap<>(initConf);
    }
}

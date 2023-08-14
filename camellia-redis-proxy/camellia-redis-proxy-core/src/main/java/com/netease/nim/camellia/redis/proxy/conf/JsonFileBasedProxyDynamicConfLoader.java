package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.tools.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Load configuration from the json file.
 */
public class JsonFileBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileBasedProxyDynamicConfLoader.class);
    private static final String DEFAULT_FILE_NAME = "camellia-redis-proxy.json";
    public static final String DYNAMIC_CONF_FILE_NAME = "dynamic.conf.file.name";
    public static final String DYNAMIC_CONF_FILE_PATH = "dynamic.conf.file.path";

    private Map<String, String> initConf = new HashMap<>();

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
                String data;
                try {
                    data = FileUtil.readFileByPath(url.getPath());
                } catch (Exception e) {
                    URL resource = ProxyDynamicConf.class.getClassLoader().getResource(fileName);
                    if (resource == null) {
                        throw new IllegalArgumentException("load error, fileName = " + fileName, e);
                    }
                    data = FileUtil.readFileByPath(resource.getPath());
                }
                Map<String, String> props = ConfigurationUtil.contentToMap(data, ConfigContentType.json);
                conf.putAll(props);
            }
            String filePath = conf.get(DYNAMIC_CONF_FILE_PATH);
            if (filePath != null) {
                try {
                    String data = FileUtil.readFileByPath(filePath);
                    Map<String, String> props = ConfigurationUtil.contentToMap(data, ConfigContentType.json);
                    conf.putAll(props);
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

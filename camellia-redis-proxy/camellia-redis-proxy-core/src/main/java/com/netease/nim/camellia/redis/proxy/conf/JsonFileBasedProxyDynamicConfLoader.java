package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.tools.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            FileUtils.FileInfo fileInfo = FileUtils.readByFileName(fileName);
            if (fileInfo != null && fileInfo.getFileContent() != null) {
                Map<String, String> map = ConfigurationUtil.contentToMap(fileInfo.getFileContent(), ConfigContentType.json);
                conf.putAll(map);
            }
            String filePath = conf.get(DYNAMIC_CONF_FILE_PATH);
            if (filePath != null) {
                FileUtils.FileInfo info = FileUtils.readByFilePath(filePath);
                if (info != null && info.getFileContent() != null) {
                    Map<String, String> map = ConfigurationUtil.contentToMap(info.getFileContent(), ConfigContentType.json);
                    conf.putAll(map);
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

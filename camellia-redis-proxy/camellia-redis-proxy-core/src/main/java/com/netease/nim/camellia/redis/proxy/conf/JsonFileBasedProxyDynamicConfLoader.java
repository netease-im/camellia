package com.netease.nim.camellia.redis.proxy.conf;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.tools.utils.FileUtils;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Load configuration from the json file.
 */
public class JsonFileBasedProxyDynamicConfLoader implements WritableProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileBasedProxyDynamicConfLoader.class);

    private static final String DEFAULT_FILE_NAME = "camellia-redis-proxy.json";
    public static final String DYNAMIC_CONF_FILE_NAME = "dynamic.conf.file.name";

    private Map<String, String> initConf = new HashMap<>();
    private String targetFilePath;
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Map<String, String> load() {
        lock.lock();
        try {
            //init conf
            Map<String, String> conf = new HashMap<>(initConf);
            String fileName = conf.get(DYNAMIC_CONF_FILE_NAME);
            if (fileName == null) {
                fileName = DEFAULT_FILE_NAME;
            }
            //conf
            FileUtils.FileInfo fileInfo = FileUtils.readByFileName(fileName);
            if (fileInfo != null && fileInfo.getFileContent() != null) {
                Map<String, String> map = ConfigurationUtil.contentToMap(fileInfo.getFileContent(), ConfigContentType.json);
                conf.putAll(map);
                if (fileInfo.getFilePath() != null) {
                    targetFilePath = fileInfo.getFilePath();
                }
            }
            String v = initConf.get("specific.file.path.writable");
            boolean specificFileWritable = v == null || Boolean.parseBoolean(v);
            //dynamic specific conf
            Pair<String, Map<String, String>> pair = ProxyDynamicConfLoaderUtil.tryLoadDynamicConfBySpecificFilePath(conf, ConfigContentType.json);
            if (pair.getFirst() != null) {
                if (specificFileWritable) {
                    targetFilePath = pair.getFirst();
                }
            }
            if (pair.getSecond() != null) {
                conf.putAll(pair.getSecond());
            }
            return conf;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void init(Map<String, String> initConf) {
        this.initConf = new HashMap<>(initConf);
    }

    @Override
    public boolean write(Map<String, String> config) {
        if (targetFilePath == null) {
            logger.warn("targetFilePath is null, skip write");
            return false;
        }
        lock.lock();
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, String> entry : config.entrySet()) {
                try {
                    JSONObject valueJson = JSONObject.parseObject(entry.getValue());
                    json.put(entry.getKey(), valueJson);
                } catch (Exception e) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }
            String jsonString = JSON.toJSONString(json, SerializerFeature.PrettyFormat);
            logger.info("try write config to file, targetFilePath = {}, config.size = {}", targetFilePath, config.size());
            boolean ok = FileUtils.write(targetFilePath, jsonString);
            if (ok) {
                logger.info("write config to file success, targetFilePath = {}, config.size = {}", targetFilePath, config.size());
            } else {
                logger.warn("write config to file fail, targetFilePath = {}, config.size = {}", targetFilePath, config.size());
            }
            return ok;
        } finally {
            lock.unlock();
        }
    }
}

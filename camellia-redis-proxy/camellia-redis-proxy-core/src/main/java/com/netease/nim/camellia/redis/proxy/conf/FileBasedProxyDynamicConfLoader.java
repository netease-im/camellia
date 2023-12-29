package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.tools.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Load configuration from the file.
 * Created by caojiajun on 2023/2/24
 */
public class FileBasedProxyDynamicConfLoader implements WritableProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedProxyDynamicConfLoader.class);
    private static final String DEFAULT_FILE_NAME = "camellia-redis-proxy.properties";
    public static final String DYNAMIC_CONF_FILE_NAME = "dynamic.conf.file.name";
    /**
     * Absolute path to the configuration file
     */
    public static final String DYNAMIC_CONF_FILE_PATH = "dynamic.conf.file.path";

    private Map<String, String> initConf = new HashMap<>();

    private String targetFilePath;

    private final ReentrantLock lock = new ReentrantLock();

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
        lock.lock();
        try {
            Map<String, String> conf = new HashMap<>(initConf);
            String fileName = conf.get(DYNAMIC_CONF_FILE_NAME);
            if (fileName == null) {
                fileName = DEFAULT_FILE_NAME;
            }
            try {
                FileUtils.FileInfo fileInfo = FileUtils.readByFileName(fileName);
                if (fileInfo != null && fileInfo.getFileContent() != null) {
                    Map<String, String> map = ConfigurationUtil.contentToMap(fileInfo.getFileContent(), ConfigContentType.properties);
                    conf.putAll(map);
                    if (fileInfo.getFilePath() != null) {
                        targetFilePath = fileInfo.getFilePath();
                    }
                }
                String filePath = conf.get(DYNAMIC_CONF_FILE_PATH);
                if (filePath != null) {
                    FileUtils.FileInfo info = FileUtils.readByFilePath(filePath);
                    if (info != null && info.getFileContent() != null) {
                        Map<String, String> map = ConfigurationUtil.contentToMap(info.getFileContent(), ConfigContentType.properties);
                        conf.putAll(map);
                        if (info.getFilePath() != null) {
                            targetFilePath = info.getFilePath();
                        }
                    }
                }
                String path = System.getProperty(DYNAMIC_CONF_FILE_PATH);
                if (path != null) {
                    FileUtils.FileInfo info = FileUtils.readByFilePath(path);
                    if (info != null && info.getFileContent() != null) {
                        Map<String, String> map = ConfigurationUtil.contentToMap(info.getFileContent(), ConfigContentType.properties);
                        conf.putAll(map);
                        if (info.getFilePath() != null) {
                            targetFilePath = info.getFilePath();
                        }
                    }
                }
                return conf;
            } catch (Exception e) {
                throw new IllegalArgumentException("load error, fileName = " + fileName, e);
            }
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
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : config.entrySet()) {
                builder.append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
            }
            logger.info("try write config to file, targetFilePath = {}, config.size = {}", targetFilePath, config.size());
            boolean ok = FileUtils.write(targetFilePath, builder.toString());
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

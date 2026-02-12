package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.tools.utils.FileUtils;
import com.netease.nim.camellia.tools.utils.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2024/6/27
 */
public class ProxyDynamicConfLoaderUtil {

    private static final String DYNAMIC_CONF_FILE_PATH = "dynamic.conf.file.path";

    public static Pair<String, Map<String, String>> tryLoadDynamicConfBySpecificFilePath(Map<String, String> configMap) {
        Map<String, String> conf = new HashMap<>();
        String targetFilePath = null;
        String filePath = configMap.get(DYNAMIC_CONF_FILE_PATH);
        if (filePath != null) {
            FileUtils.FileInfo info = FileUtils.readByFilePath(filePath);
            if (info != null && info.getFileContent() != null) {
                Map<String, String> map = ConfigurationUtil.contentToMap(info.getFileContent(), ConfigurationUtil.configContentType(info.getFilePath()));
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
                Map<String, String> map = ConfigurationUtil.contentToMap(info.getFileContent(), ConfigurationUtil.configContentType(info.getFilePath()));
                conf.putAll(map);
                if (info.getFilePath() != null) {
                    targetFilePath = info.getFilePath();
                }
            }
        }
        return new Pair<>(targetFilePath, conf);
    }
}

package com.netease.nim.camellia.hot.key.server.conf;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.exception.CamelliaHotKeyException;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.common.utils.HotKeyConfigUtils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.tools.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/5/10
 */
public class FileBasedHotKeyConfigService extends HotKeyConfigService {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedHotKeyConfigService.class);

    private String fileName = "camellia-hot-key-config.properties";

    private Map<String, HotKeyConfig> configMap = new HashMap<>();

    private boolean reload() {
        try {
            FileUtils.FileInfo fileInfo = FileUtils.readByFileName(fileName);
            if (fileInfo == null) {
                return false;
            }
            if (fileInfo.getFileContent() == null) {
                return false;
            }
            Map<String, String> map = ConfigurationUtil.contentToMap(fileInfo.getFileContent(), ConfigContentType.properties);

            Map<String, String> namespaceMap = new HashMap<>(map);
            if (namespaceMap.isEmpty()) {
                logger.warn("namespaceMap is empty");
                return false;
            }
            Map<String, HotKeyConfig> configMap = new HashMap<>();
            for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
                String namespace = entry.getKey();
                String ruleJsonFile = entry.getValue();
                FileUtils.FileInfo info = FileUtils.readByFileName(ruleJsonFile);
                if (info == null) {
                    continue;
                }
                if (info.getFileContent() == null) {
                    continue;
                }
                HotKeyConfig hotKeyConfig = JSONObject.parseObject(info.getFileContent(), HotKeyConfig.class);
                if (!namespace.equals(hotKeyConfig.getNamespace())) {
                    logger.warn("namespace not match, config will skip reload, {} <-> {}", namespace, hotKeyConfig.getNamespace());
                    continue;
                }
                if (!HotKeyConfigUtils.checkAndConvert(hotKeyConfig)) {
                    logger.warn("hotKeyConfig check fail, config will skip reload, namespace = {}, config = {}", namespace, JSONObject.toJSONString(hotKeyConfig));
                    continue;
                }
                configMap.put(hotKeyConfig.getNamespace(), hotKeyConfig);
            }
            if (!this.configMap.equals(configMap)) {
                this.configMap = configMap;
                logger.info("hot-key-config reload success, configMap = {}", JSONObject.toJSONString(configMap));
                for (Map.Entry<String, HotKeyConfig> entry : this.configMap.entrySet()) {
                    invokeUpdate(entry.getKey());
                }
            }
            if (configMap.isEmpty()) {
                logger.warn("configMap is empty");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("reload error", e);
            return false;
        }
    }

    @Override
    public HotKeyConfig get(String namespace) {
        return configMap.get(namespace);
    }

    @Override
    public void init(HotKeyServerProperties properties) {
        String fileName = properties.getConfig().get("hot.key.config.name");
        if (fileName != null) {
            this.fileName = fileName;
        }
        boolean success = reload();
        if (!success) {
            throw new CamelliaHotKeyException("init fail");
        }
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-hot-key-config-reload"))
                .scheduleAtFixedRate(this::reload, 60, 60, TimeUnit.SECONDS);
    }
}

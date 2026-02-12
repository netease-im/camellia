package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.conf.ApiBasedCamelliaConfig;
import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 走camellia-config获取配置，远程配置会覆盖本地配置
 * Created by caojiajun on 2023/3/17
 */
public class ApiBasedProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(ApiBasedProxyDynamicConfLoader.class);

    private ApiBasedCamelliaConfig camelliaConfig;
    private Map<String, String> initConf = new HashMap<>();

    /**
     * default constructor
     */
    public ApiBasedProxyDynamicConfLoader() {
    }

    @Override
    public Map<String, String> load() {
        //init conf
        Map<String, String> conf = new HashMap<>(initConf);
        //conf
        conf.putAll(camelliaConfig.getConf());
        //dynamic specific conf
        Pair<String, Map<String, String>> pair = ProxyDynamicConfLoaderUtil.tryLoadDynamicConfBySpecificFilePath(conf);
        if (pair.getSecond() != null) {
            conf.putAll(pair.getSecond());
        }
        return conf;
    }

    @Override
    public void init(Map<String, String> initConf) {
        this.initConf = new HashMap<>(initConf);
        String url = initConf.get("camellia.config.url");
        String namespace = initConf.get("camellia.config.namespace");
        if (url == null) {
            throw new IllegalArgumentException("missing 'camellia.config.url'");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("missing 'camellia.config.namespace'");
        }
        this.camelliaConfig = new ApiBasedCamelliaConfig(url, namespace);
        this.camelliaConfig.addCallback(ProxyDynamicConf::reload);
        logger.info("ApiBasedProxyDynamicConfLoader init success, url = {}, namespace = {}", url, namespace);
    }

}

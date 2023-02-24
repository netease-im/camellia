package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Map;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface ProxyDynamicConfLoader {

    Map<String, String> load();

    void updateInitConf(Map<String, String> initConf);

}

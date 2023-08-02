package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Map;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface ProxyDynamicConfLoader {

    /**
     * ProxyDynamicConf初始化时，会把yml文件中的kv配置通过这个接口传递给loader
     * 这个方法只会被调用一次
     * @param initConf yml中的初始配置
     */
    void init(Map<String, String> initConf);

    /**
     * ProxyDynamicConf会定期调用load方法来更新配置
     * @return 配置
     */
    Map<String, String> load();

}

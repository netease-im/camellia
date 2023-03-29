package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Map;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface ProxyDynamicConfLoader {

    /**
     * ProxyDynamicConf会定期调用load方法来更新配置
     * @return 配置
     */
    Map<String, String> load();

    /**
     * ProxyDynamicConf初始化时，会把yml文件中的kv配置通过这个接口传递给loader
     * 这个方法只会被调用一次
     * @param initConf yml中的初始配置
     */
    void updateInitConf(Map<String, String> initConf);

    /**
     * 设置回调，从而可以让loader中配置发生变更时第一时间被ProxyDynamicConf感知到，而不需要等ProxyDynamicConf来定期拉取更新
     * loader不一定需要实现这个方法
     * @param callback 回调类
     */
    default void addCallback(ProxyDynamicConfLoaderCallback callback) {

    }
}

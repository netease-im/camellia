package com.netease.nim.camellia.redis.proxy.conf;

import java.util.Map;

/**
 * Created by caojiajun on 2023/11/23
 */
public interface WritableProxyDynamicConfLoader extends ProxyDynamicConfLoader {

    boolean write(Map<String, String> config);

}

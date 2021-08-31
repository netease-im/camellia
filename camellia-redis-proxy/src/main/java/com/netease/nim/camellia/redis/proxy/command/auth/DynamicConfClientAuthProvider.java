package com.netease.nim.camellia.redis.proxy.command.auth;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用ProxyDynamicConf进行自定义配置（camellia-redis-proxy.properties）
 * 示例：
 * pass123.auth.conf=1|default  表示使用密码=pass123登录时，路由到bid=1,bgroup=default
 * Created by caojiajun on 2021/8/18
 */
public class DynamicConfClientAuthProvider implements ClientAuthProvider {

    private final ConcurrentLinkedHashMap<String, ClientIdentity> cache = new ConcurrentLinkedHashMap.Builder<String, ClientIdentity>()
            .initialCapacity(100).maximumWeightedCapacity(1000).build();
    private final ClientIdentity authFail;

    public DynamicConfClientAuthProvider() {
        authFail = new ClientIdentity();
        authFail.setPass(false);
        ProxyDynamicConf.registerCallback(cache::clear);
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        try {
            ClientIdentity clientIdentity = cache.get(password);
            if (clientIdentity != null) {
                return clientIdentity;
            }
            String string = ProxyDynamicConf.getString(password + ".auth.conf", null);
            if (string == null) {
                cache.put(password, authFail);
                return authFail;
            }
            String[] split = string.split("\\|");
            if (split.length != 2) {
                cache.put(password, authFail);
                return authFail;
            }
            long bid = Long.parseLong(split[0]);
            String bgroup = split[1];
            clientIdentity = new ClientIdentity();
            clientIdentity.setPass(true);
            clientIdentity.setBid(bid);
            clientIdentity.setBgroup(bgroup);
            cache.put(password, clientIdentity);
            return clientIdentity;
        } catch (Exception e) {
            cache.put(password, authFail);
            return authFail;
        }
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }
}

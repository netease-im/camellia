package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2023/8/3
 */
public class MultiTenantConfigSelector {

    private final Map<String, ClientIdentity> exactClientIdentityMap = new HashMap<>();
    private final Map<String, ClientIdentity> passwordFallbackClientIdentityMap = new HashMap<>();
    private final Map<String, String> resourceTableMap = new HashMap<>();

    private final List<MultiTenantConfig> configList;

    public MultiTenantConfigSelector(List<MultiTenantConfig> list) {
        this.configList = list;
        for (MultiTenantConfig config : list) {
            ClientIdentity clientIdentity = new ClientIdentity(config.getBid(), config.getBgroup(), true);
            String userName = config.getUsername();
            if (userName == null) {
                passwordFallbackClientIdentityMap.put(config.getPassword(), clientIdentity);
            } else {
                exactClientIdentityMap.put(buildAuthKey(userName, config.getPassword()), clientIdentity);
            }
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(config.getRoute());
            RedisResourceUtil.checkResourceTable(resourceTable);
            resourceTableMap.put(config.getBid() + "|" + config.getBgroup(), ReadableResourceTableUtil.readableResourceTable(resourceTable));
        }
    }

    public ClientIdentity selectClientIdentity(String userName, String password) {
        if (userName != null) {
            ClientIdentity clientIdentity = exactClientIdentityMap.get(buildAuthKey(userName, password));
            if (clientIdentity != null) {
                return clientIdentity;
            }
        }
        return passwordFallbackClientIdentityMap.get(password);
    }

    public String selectResourceTable(long bid, String bgroup) {
        return resourceTableMap.get(bid + "|" + bgroup);
    }

    public List<MultiTenantConfig> getConfigList() {
        return configList;
    }

    public Map<String, ClientIdentity> getExactClientIdentityMap() {
        return new HashMap<>(exactClientIdentityMap);
    }

    public Map<String, ClientIdentity> getPasswordFallbackClientIdentityMap() {
        return new HashMap<>(passwordFallbackClientIdentityMap);
    }

    public Map<String, String> getResourceTableMap() {
        return new HashMap<>(resourceTableMap);
    }

    private String buildAuthKey(String userName, String password) {
        return userName + "|" + password;
    }
}

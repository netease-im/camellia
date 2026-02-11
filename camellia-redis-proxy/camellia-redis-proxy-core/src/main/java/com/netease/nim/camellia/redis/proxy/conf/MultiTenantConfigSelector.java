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

    private final Map<String, ClientIdentity> clientIdentityMap = new HashMap<>();
    private final Map<String, ResourceTable> resourceTableMap = new HashMap<>();

    private final List<MultiTenantConfig> configList;

    public MultiTenantConfigSelector(List<MultiTenantConfig> list) {
        this.configList = list;
        for (MultiTenantConfig config : list) {
            clientIdentityMap.put(config.getPassword(), new ClientIdentity(config.getBid(), config.getBgroup(), true));
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(config.getRoute());
            RedisResourceUtil.checkResourceTable(resourceTable);
            resourceTableMap.put(config.getBid() + "|" + config.getBgroup(), resourceTable);
        }
    }

    public ClientIdentity selectClientIdentity(String password) {
        return clientIdentityMap.get(password);
    }

    public ResourceTable selectResourceTable(long bid, String bgroup) {
        return resourceTableMap.get(bid + "|" + bgroup);
    }

    public List<MultiTenantConfig> getConfigList() {
        return configList;
    }

    public Map<String, ClientIdentity> getClientIdentityMap() {
        return new HashMap<>(clientIdentityMap);
    }

    public Map<String, ResourceTable> getResourceTableMap() {
        return new HashMap<>(resourceTableMap);
    }
}

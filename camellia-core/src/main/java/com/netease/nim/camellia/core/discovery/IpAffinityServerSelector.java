package com.netease.nim.camellia.core.discovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2025/3/25
 */
public abstract class IpAffinityServerSelector<T> implements CamelliaServerSelector<T> {

    private final CamelliaServerSelector<T> serverSelector;
    private final IpAffinityConfig defaultConfig;

    public IpAffinityServerSelector(CamelliaServerSelector<T> serverSelector, IpAffinityConfig defaultConfig) {
        this.serverSelector = serverSelector;
        this.defaultConfig = defaultConfig;
    }

    /**
     * to target ip
     * @param t instance
     * @return ip
     */
    public abstract String toIp(T t);

    /**
     * you can override to dynamic config
     * @return config
     */
    public IpAffinityConfig getConfig() {
        return defaultConfig;
    }

    @Override
    public T pick(List<T> list, Object loadBalanceKey) {
        try {
            IpAffinityConfig config = getConfig();
            if (config == null) {
                return serverSelector.pick(list, loadBalanceKey);
            }
            List<T> filterList = new ArrayList<>(list.size());
            for (T resource : list) {
                boolean match = IpAffinityConfigUtils.match(config, toIp(resource));
                if (match) {
                    filterList.add(resource);
                }
            }
            if (filterList.isEmpty()) {
                return serverSelector.pick(list, loadBalanceKey);
            }
            T pick = serverSelector.pick(filterList, loadBalanceKey);
            if (pick != null) {
                return pick;
            }
            return serverSelector.pick(list, loadBalanceKey);
        } catch (Exception e) {
            return serverSelector.pick(list, loadBalanceKey);
        }
    }
}

package com.netease.nim.camellia.redis.proxy.route;

/**
 * Created by caojiajun on 2026/2/11
 */
public enum RouteConfProviderEnums {

    simple_tenant("default", DefaultRouteConfProvider.class),
    camellia_dashboard("camellia_dashboard", CamelliaDashboardRouteConfProvider.class),
    multi_tenants_v1("multi_tenants_v1", MultiTenantsV1RouteConfProvider.class),
    multi_tenants_v2("multi_tenants_v2", MultiTenantsV2RouteConfProvider.class),
    multi_tenants_simple_config("multi_tenants_simple_config", SimpleConfigRouteConfProvider.class),

    ;
    private final String name;
    private final Class<? extends RouteConfProvider> clazz;

    RouteConfProviderEnums(String name, Class<? extends RouteConfProvider> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    public Class<? extends RouteConfProvider> getClazz() {
        return clazz;
    }
}

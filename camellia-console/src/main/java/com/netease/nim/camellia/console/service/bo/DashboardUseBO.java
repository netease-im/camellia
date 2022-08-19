package com.netease.nim.camellia.console.service.bo;

import java.util.Map;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class DashboardUseBO {
    /**
     * 判断是否拥有全部的dashboard权限，如果为true，则不必关注resourceIds
     */
    private Boolean isAll;

    /**
     * 当isAll为false时，所有有读写权限的dashboard都存在里面,
     * key代表resourceId，
     * value代表权限类型，0可读，1可读写
     */
    private Map<Integer,Integer> dashboards;

    public Boolean getAll() {
        return isAll;
    }

    public void setAll(Boolean all) {
        isAll = all;
    }

    public Map<Integer, Integer> getDashboards() {
        return dashboards;
    }

    public void setDashboards(Map<Integer, Integer> dashboards) {
        this.dashboards = dashboards;
    }
}

package com.netease.nim.camellia.console.service.ao;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class IdentityDashboardBaseAO {
    private String did;


    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    @Override
    public String toString() {
        return "IdentityDashboardBaseAO{" +
                "dashboardId='" + did + '\'' +
                '}';
    }
}

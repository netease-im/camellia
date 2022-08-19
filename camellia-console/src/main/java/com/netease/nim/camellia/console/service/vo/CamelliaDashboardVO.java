package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.model.CamelliaDashboard;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaDashboardVO extends CamelliaDashboard {
    private Integer right;

    public CamelliaDashboardVO() {
    }

    public CamelliaDashboardVO(CamelliaDashboard camelliaDashboard) {
        setDid(camelliaDashboard.getDid());
        setAddress(camelliaDashboard.getAddress());
        setCreatedTime(camelliaDashboard.getCreatedTime());
        setIsOnline(camelliaDashboard.getIsOnline());
        setIsUse(camelliaDashboard.getIsUse());
        setUpdatedTime(camelliaDashboard.getUpdatedTime());
        setTag(camelliaDashboard.getTag());
    }

    public Integer getRight() {
        return right;
    }

    public void setRight(Integer right) {
        this.right = right;
    }
}

package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.model.CamelliaRefHistory;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaRefHistoryListWithNum {
    private List<CamelliaRefHistory> camelliaRefHistories;
    private Integer count;
    private Integer currentPage;

    public List<CamelliaRefHistory> getCamelliaRefHistories() {
        return camelliaRefHistories;
    }

    public void setCamelliaRefHistories(List<CamelliaRefHistory> camelliaRefHistories) {
        this.camelliaRefHistories = camelliaRefHistories;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
}

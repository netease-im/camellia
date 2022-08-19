package com.netease.nim.camellia.console.service.vo;

import com.netease.nim.camellia.console.model.CamelliaTableHistory;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaTableHistoryListWithNum {
    private List<CamelliaTableHistory> camelliaTableHistories;
    private Integer count;
    private Integer currentPage;

    public List<CamelliaTableHistory> getCamelliaTableHistories() {
        return camelliaTableHistories;
    }

    public void setCamelliaTableHistories(List<CamelliaTableHistory> camelliaTableHistories) {
        this.camelliaTableHistories = camelliaTableHistories;
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

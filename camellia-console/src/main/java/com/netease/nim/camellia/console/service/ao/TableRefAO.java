package com.netease.nim.camellia.console.service.ao;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TableRefAO extends IdentityDashboardBaseAO{
    private Long tid;
    private Long bid;
    private String bgroup;
    private String info;

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}

package com.netease.nim.camellia.console.service.ao;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TableAO extends IdentityDashboardBaseAO{
    private Long tid;
    private String info;
    private String table;
    private Integer type=0;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }


}

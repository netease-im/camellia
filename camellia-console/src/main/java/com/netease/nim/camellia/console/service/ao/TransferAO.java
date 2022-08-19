package com.netease.nim.camellia.console.service.ao;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TransferAO {
    private Integer type;//0 代表从console的协议转回dashboard；1相反
    private String table;

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
}

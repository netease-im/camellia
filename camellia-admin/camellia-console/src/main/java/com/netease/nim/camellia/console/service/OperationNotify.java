package com.netease.nim.camellia.console.service;

/**
 * Created by caojiajun on 2023/3/29
 */
public interface OperationNotify {

    void notifyCreateTable(String username, String dashboardUrl, String detail, String info,Integer type);

    void notifyDeleteTable(String username, String dashboardUrl, Long tid);

    void notifyChangeTable(String username, String dashboardUrl, Long tid, String table, String info,Integer type);

    void notifyBindBigBgroupWithTid(String username, String dashboardUrl, long bid, String bgroup, long tid, String info);

    void notifyDeleteTableRef(String username, String dashboardUrl, long bid, String bgroup);

    void notifyCreateOrUpdateResource(String username, String dashboardUrl, String url, String info);

    void notifyDeleteResource(String username, String dashboardUrl, long id);
}

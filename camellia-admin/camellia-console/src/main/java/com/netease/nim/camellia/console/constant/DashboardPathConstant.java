package com.netease.nim.camellia.console.constant;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class DashboardPathConstant {
    public final static String healthCheck="/health/check";

    public final static String basePath = "/camellia/admin";

    public final static String createResourceTablePath = basePath + "/createResourceTable";
    public final static String resourceTableTid = basePath + "/resourceTable/%s";
    public final static String resourceTableList = basePath + "/resourceTables";
    public final static String resourceTableAll = basePath + "/resourceTablesAll";
    public final static String changeResourceTable = basePath + "/changeResourceTable";

    public final static String deleteResourceTable = basePath + "/resourceTable/%s";

    public final static String createOrUpdateTableRef = basePath + "/createOrUpdateTableRef";
    public final static String getTableRefsByTid = basePath + "/getTableRefsByTid";
    public final static String getTableRefsByBidGroup = basePath + "/getTableRefByBidGroup";
    public final static String getTableRefsByBid = basePath + "/getTableRefsByBid";
    public final static String getTableRefs = basePath + "/getTableRefs";
    public final static String getTableRefAll = basePath + "/getTableRefAll";
    public final static String deleteTableRefs = basePath + "/tableRef";

    public final static String getResources = basePath + "/resources";
    public final static String getResourceQuery = basePath + "/resourcesQuery";
    public final static String deleteResource = basePath + "/resource";
    public final static String createOrUpdateResources = basePath + "/createOrUpdateResource";


}

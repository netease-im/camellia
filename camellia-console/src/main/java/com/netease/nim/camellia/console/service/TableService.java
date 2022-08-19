package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.service.bo.TableBO;
import com.netease.nim.camellia.console.service.vo.CamelliaTablePage;
import com.netease.nim.camellia.console.service.vo.TableWithTableRefs;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface TableService {

    List<TableBO> getAllTable(Long dashboardId, boolean onlyValid);

    TableBO getTable(Long dashboardId, long tid);

    TableBO createTable(Long dashboardId, String detail, String info,Integer type);

    void deleteTable(long dashboardId, long tid);

    TableWithTableRefs changeTable(Long dashboardId, Long tid, String table, String info,Integer type);

    CamelliaTablePage getAllTableTidInfo(Long did, Integer validFlag, Long tid, String info, String table,int pageNum, int pageSize);
}

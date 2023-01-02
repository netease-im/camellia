package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.model.CamelliaTableHistory;
import com.netease.nim.camellia.console.service.vo.CamelliaTableHistoryListWithNum;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface TableHistoryService {

    CamelliaTableHistoryListWithNum getTableHistoryByBidAndBgroup(Long dashboardId, Long tid,Integer pageNum, Integer pageSize);

    CamelliaTableHistory getById(Long dashboardId,Long id);
}

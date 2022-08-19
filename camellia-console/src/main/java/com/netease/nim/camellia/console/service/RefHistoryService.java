package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import com.netease.nim.camellia.console.service.vo.CamelliaRefHistoryListWithNum;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface RefHistoryService {

   CamelliaRefHistoryListWithNum getAllByDashboardId(Long dashboardId, Integer pageNum, Integer pageSize);

    CamelliaRefHistoryListWithNum getAllByBidAndBgroup(Long dashboardId, Long bid, String bgroup, Integer pageNum, Integer pageSize);

    CamelliaRefHistory getById(Long dashboardId, Long id);
}

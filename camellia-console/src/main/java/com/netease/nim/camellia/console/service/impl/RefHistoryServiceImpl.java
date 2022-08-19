package com.netease.nim.camellia.console.service.impl;

import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.dao.wrapper.CamelliaRefHistoryMapperWrapper;
import com.netease.nim.camellia.console.dao.wrapper.DashboardMapperWrapper;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import com.netease.nim.camellia.console.service.RefHistoryService;
import com.netease.nim.camellia.console.service.vo.CamelliaRefHistoryListWithNum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class RefHistoryServiceImpl  implements RefHistoryService {

    @Autowired
    DashboardMapperWrapper dashboardMapperWrapper;

    @Autowired
    CamelliaRefHistoryMapperWrapper refHistoryMapperWrapper;

    @Override
    public CamelliaRefHistoryListWithNum getAllByDashboardId(Long dashboardId, Integer pageNum, Integer pageSize) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(dashboardId);
        if(byId==null){
            throw new AppException(AppCode.DASHBOARD_CAN_NOT_USE, dashboardId + " unavailable");
        }

        Integer currentNum = (pageNum - 1) * pageSize;
        CamelliaRefHistoryListWithNum refHistoryListWithNum=new CamelliaRefHistoryListWithNum();

        List<CamelliaRefHistory> allByDashboardId = refHistoryMapperWrapper.findAllByDashboardId(dashboardId, currentNum, pageSize);
        refHistoryListWithNum.setCamelliaRefHistories(allByDashboardId);
        refHistoryListWithNum.setCurrentPage(pageNum);
        refHistoryListWithNum.setCount(refHistoryMapperWrapper.countByDashboardId(dashboardId));
        return refHistoryListWithNum;
    }

    @Override
    public CamelliaRefHistoryListWithNum getAllByBidAndBgroup(Long dashboardId, Long bid, String bgroup, Integer pageNum, Integer pageSize) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(dashboardId);
        if(byId==null){
            throw new AppException(AppCode.DASHBOARD_CAN_NOT_USE, dashboardId + " unavailable");
        }
        Integer currentNum = (pageNum - 1) * pageSize;
        CamelliaRefHistoryListWithNum refHistoryListWithNum=new CamelliaRefHistoryListWithNum();
        refHistoryListWithNum.setCamelliaRefHistories(refHistoryMapperWrapper.findAllByBidAndBgroup(dashboardId,bid,bgroup,currentNum,pageSize));
        refHistoryListWithNum.setCurrentPage(pageNum);
        refHistoryListWithNum.setCount(refHistoryMapperWrapper.countByBidAndBgroup(dashboardId,bid,bgroup));
        return refHistoryListWithNum;
    }

    @Override
    public CamelliaRefHistory getById(Long dashboardId, Long id) {

        return refHistoryMapperWrapper.findById(id, dashboardId);
    }


}

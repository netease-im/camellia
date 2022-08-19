package com.netease.nim.camellia.console.service.impl;

import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.dao.wrapper.CamelliaTableHistoryMapperWrapper;
import com.netease.nim.camellia.console.dao.wrapper.DashboardMapperWrapper;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.CamelliaTableHistory;
import com.netease.nim.camellia.console.service.TableHistoryService;
import com.netease.nim.camellia.console.service.vo.CamelliaTableHistoryListWithNum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class TableHistoryServiceImpl implements TableHistoryService {

    @Autowired
    DashboardMapperWrapper dashboardMapperWrapper;

    @Autowired
    CamelliaTableHistoryMapperWrapper tableHistoryMapperWrapper;

    @Override
    public CamelliaTableHistoryListWithNum getTableHistoryByBidAndBgroup(Long dashboardId, Long tid, Integer pageNum, Integer pageSize) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(dashboardId);
        if(byId==null){
            throw new AppException(AppCode.DASHBOARD_DO_NOT_EXIST, dashboardId + " unavailable");
        }
        Integer currentNum = (pageNum - 1) * pageSize;
        List<CamelliaTableHistory> allByTid = tableHistoryMapperWrapper.findAllByTid(dashboardId, tid, currentNum, pageSize);
        Integer count=tableHistoryMapperWrapper.countByTid(dashboardId,tid);
        CamelliaTableHistoryListWithNum camelliaTableHistoryListWithNum=new CamelliaTableHistoryListWithNum();
        camelliaTableHistoryListWithNum.setCamelliaTableHistories(allByTid);
        camelliaTableHistoryListWithNum.setCurrentPage(pageNum);
        camelliaTableHistoryListWithNum.setCount(count);
        return camelliaTableHistoryListWithNum;
    }


    @Override
    public CamelliaTableHistory getById(Long dashboardId, Long id) {

        CamelliaTableHistory byId = tableHistoryMapperWrapper.findById(id, dashboardId);
        return byId;
    }


}

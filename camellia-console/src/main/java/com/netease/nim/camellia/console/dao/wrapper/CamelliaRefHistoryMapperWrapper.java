package com.netease.nim.camellia.console.dao.wrapper;

import com.netease.nim.camellia.console.dao.mapper.CamelliaRefHistoryMapper;
import com.netease.nim.camellia.console.model.CamelliaRefHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Component
public class CamelliaRefHistoryMapperWrapper {

    @Autowired
    CamelliaRefHistoryMapper camelliaRefHistoryMapper;


    public int saveCamelliaRefHistory(CamelliaRefHistory refHistory){
        return camelliaRefHistoryMapper.saveCamelliaRefHistory(refHistory);
    }

    public List<CamelliaRefHistory> findAllByDashboardId(Long dashboardId, Integer currentNum, Integer pageSize) {
        return camelliaRefHistoryMapper.findAllByDashboardId(dashboardId,currentNum,pageSize);
    }

    public List<CamelliaRefHistory> findAllByBidAndBgroup(Long dashboardId, Long bid,String bgroup, Integer currentNum, Integer pageSize) {
        return camelliaRefHistoryMapper.findAllByBidAndBgroup(dashboardId,bid,bgroup,currentNum,pageSize);
    }

    public CamelliaRefHistory findById(Long id,Long dashboardId){
        return camelliaRefHistoryMapper.findById(id,dashboardId);
    }

    public Integer saveCamelliaRefHistories(List<CamelliaRefHistory> camelliaRefHistories) {
        camelliaRefHistoryMapper.saveCamelliaRefHistories(camelliaRefHistories);
        return camelliaRefHistories.size();
    }

    public Integer countByDashboardId(Long dashboardId) {
        return camelliaRefHistoryMapper.countById(dashboardId);
    }


    public Integer countByBidAndBgroup(Long dashboardId, Long bid, String bgroup) {
        return camelliaRefHistoryMapper.countByBidAndBgroup(dashboardId,bid,bgroup);

    }
}

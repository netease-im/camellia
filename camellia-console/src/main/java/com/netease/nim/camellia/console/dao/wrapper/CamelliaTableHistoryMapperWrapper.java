package com.netease.nim.camellia.console.dao.wrapper;

import com.netease.nim.camellia.console.dao.mapper.CamelliaTableHistoryMapper;
import com.netease.nim.camellia.console.model.CamelliaTableHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Component
public class CamelliaTableHistoryMapperWrapper {
    @Autowired
    CamelliaTableHistoryMapper camelliaTableHistoryMapper;
    public int saveCamelliaTableHistory(CamelliaTableHistory tableHistory){
        return camelliaTableHistoryMapper.saveCamelliaTableHistory(tableHistory);
    }

    public CamelliaTableHistory findById(Long id,Long dashboardId){
        return camelliaTableHistoryMapper.findById(id,dashboardId);
    }


    public List<CamelliaTableHistory> findAllByTid(Long dashboardId, Long tid, Integer currentNum, Integer pageSize) {
        return camelliaTableHistoryMapper.findAllByTid(dashboardId,tid,currentNum,pageSize);
    }


    public Integer countByTid(Long dashboardId, Long tid) {
        return camelliaTableHistoryMapper.countByTid(dashboardId,tid);
    }
}

package com.netease.nim.camellia.console.dao.wrapper;

import com.netease.nim.camellia.console.dao.mapper.DashboardMapper;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Create with IntelliJ IDEA 后续用于加缓存
 * @author ChenHongliang
 */
@Component
public class DashboardMapperWrapper {

    @Autowired
    DashboardMapper dashboardMapper;

    public List<CamelliaDashboard> findInDidsIsUseIsOnline(List<Integer> list, Integer isUse, Integer isOnline) {
        return dashboardMapper.findInDidsIsUseIsOnline(list,isUse,isOnline);

    }


    public CamelliaDashboard findByDId(Long did) {
        return dashboardMapper.findByDId(did);
    }


    public CamelliaDashboard findByTag(String tag) {
        return dashboardMapper.findByTag(tag);
    }

    public CamelliaDashboard findByAddress(String address) {
        return dashboardMapper.findByAddress(address);
    }

    public int saveCamelliaDashboard(CamelliaDashboard camelliaDashboard) {
        return dashboardMapper.saveCamelliaDashboard(camelliaDashboard);
    }


    public int updateOnlineStatus(CamelliaDashboard camelliaDashboard){
        return dashboardMapper.updateOnlineStatusByDId(camelliaDashboard);
    }

    public int updateTagByDId(CamelliaDashboard camelliaDashboard){
        return dashboardMapper.updateTagByDId(camelliaDashboard);
    }

    public int updateIsUseByDId(CamelliaDashboard camelliaDashboard){
        return dashboardMapper.updateIsUseByDId(camelliaDashboard);
    }

    public Integer deleteByDId(Long did) {
        return dashboardMapper.deleteByDId(did);
    }

    public List<CamelliaDashboard> findByUseAndOnline(Integer isUse, Integer isOnline) {
        return dashboardMapper.findByUseAndOnline(isUse,isOnline);
    }
}

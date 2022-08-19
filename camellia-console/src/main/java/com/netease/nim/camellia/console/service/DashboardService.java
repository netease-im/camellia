package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.vo.CamelliaDashboardVO;

import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface DashboardService {
    CamelliaDashboard register(String url,String tag);

    List<CamelliaDashboard> getAllDashboardByUseAndOnline(Integer isUse,Integer isOnline);

    void useStatusChange(Long id, Integer use);

    void tagDashBoard(Long id,String tag);

    List<CamelliaDashboardVO> getUserDashboard(BaseUser user,String tag,Integer use,Integer isOnline);

    CamelliaDashboard getByDId(Long did);

    void updateOnlineStatus(Long id,Integer status);

    void deleteDashboard(Long id);

    WebResult testLink(String url);
}

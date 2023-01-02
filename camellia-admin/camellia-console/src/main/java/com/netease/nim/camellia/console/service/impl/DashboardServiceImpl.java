package com.netease.nim.camellia.console.service.impl;

import com.netease.nim.camellia.console.command.DashboardHeart;
import com.netease.nim.camellia.console.command.DashboardHeartApi;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.constant.ModelConstant;
import com.netease.nim.camellia.console.dao.wrapper.DashboardMapperWrapper;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.DashboardService;
import com.netease.nim.camellia.console.service.UserAccessService;
import com.netease.nim.camellia.console.service.bo.DashboardUseBO;
import com.netease.nim.camellia.console.service.vo.CamelliaDashboardVO;
import com.netease.nim.camellia.console.util.DashboardApiUtil;
import com.netease.nim.camellia.console.util.LogBean;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.netease.nim.camellia.console.constant.ModelConstant.offLine;
import static com.netease.nim.camellia.console.constant.ModelConstant.use;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    DashboardMapperWrapper dashboardMapperWrapper;

    @Autowired
    UserAccessService userAccessService;

    @Autowired
    DashboardHeart dashboardHeart;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CamelliaDashboard register(String url, String tag) {
        CamelliaDashboard byAddress = dashboardMapperWrapper.findByAddress(url);
        if (byAddress != null) {
            throw new AppException(AppCode.DASHBOARD_URL_DUP, "url:" + url + "重复");

        }
        if (!StringUtil.isNullOrEmpty(tag)) {
            CamelliaDashboard byTag = dashboardMapperWrapper.findByTag(tag);
            if (byTag != null) {
                throw new AppException(AppCode.DASHBOARD_TAG_DUP,"tag:"+ tag + " 重复");
            }
        }
        CamelliaDashboard camelliaDashboard = new CamelliaDashboard();
        camelliaDashboard.setAddress(url);
        camelliaDashboard.setIsOnline(offLine);
        camelliaDashboard.setIsUse(use);
        camelliaDashboard.setTag(tag);
        camelliaDashboard.setCreatedTime(System.currentTimeMillis());
        camelliaDashboard.setUpdatedTime(System.currentTimeMillis());
        dashboardMapperWrapper.saveCamelliaDashboard(camelliaDashboard);
        dashboardHeart.put(camelliaDashboard.getDid(), camelliaDashboard.getAddress());
        LogBean.get().addProps("register dashboard", camelliaDashboard);

        return camelliaDashboard;

    }

    @Override
    public List<CamelliaDashboard> getAllDashboardByUseAndOnline(Integer isUse, Integer isOnline) {
        return dashboardMapperWrapper.findByUseAndOnline(isUse, isOnline);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useStatusChange(Long id, Integer use) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(id);
        if (byId == null) {
            throw new AppException(AppCode.DASHBOARD_DO_NOT_EXIST, id + " do not exists");
        }
        if (byId.getIsUse().equals(use)) {
            return;
        }

        CamelliaDashboard camelliaDashboard = new CamelliaDashboard();
        camelliaDashboard.setDid(id);
        camelliaDashboard.setIsUse(use);
        camelliaDashboard.setIsOnline(offLine);
        camelliaDashboard.setUpdatedTime(System.currentTimeMillis());
        dashboardMapperWrapper.updateIsUseByDId(camelliaDashboard);

        if (use == ModelConstant.use) {
            LogBean.get().addProps("use dashboard", byId);
            dashboardHeart.put(byId.getDid(), byId.getAddress());
        } else {
            LogBean.get().addProps("delete dashboard", byId);
            dashboardHeart.cancel(byId.getDid());
        }
    }

    @Override
    public void tagDashBoard(Long id, String tag) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(id);
        if (byId == null) {

            throw new AppException(AppCode.DASHBOARD_DO_NOT_EXIST, id + " do not exists");
        }
        CamelliaDashboard byTag = dashboardMapperWrapper.findByTag(tag);
        if (byTag != null) {
            throw new AppException(AppCode.DASHBOARD_TAG_DUP, tag + " duplicated");
        }
        CamelliaDashboard camelliaDashboard = new CamelliaDashboard();
        camelliaDashboard.setDid(id);
        camelliaDashboard.setTag(tag);
        camelliaDashboard.setUpdatedTime(System.currentTimeMillis());
        LogBean.get().addProps("tagDashboard", camelliaDashboard);
        dashboardMapperWrapper.updateTagByDId(camelliaDashboard);
    }


    @Override
    public List<CamelliaDashboardVO> getUserDashboard(BaseUser user, String tag, Integer use, Integer isOnline) {
        DashboardUseBO dashboardUseBO = userAccessService.getAllUseDashboardIdAndType(user);
        if(dashboardUseBO==null){
            return new ArrayList<>();
        }
        List<CamelliaDashboardVO> camelliaDashboardVOList = new ArrayList<>();
        if (dashboardUseBO.getAll().equals(true)) {
            List<CamelliaDashboard> all = dashboardMapperWrapper.findByUseAndOnline(use, isOnline);
            for (CamelliaDashboard camelliaDashboard : all) {
                CamelliaDashboardVO camelliaDashboardVO = new CamelliaDashboardVO(camelliaDashboard);
                camelliaDashboardVO.setRight(1);
                camelliaDashboardVOList.add(camelliaDashboardVO);
            }
            return camelliaDashboardVOList;
        } else {
            if (dashboardUseBO.getDashboards() == null || dashboardUseBO.getDashboards().isEmpty()) {
                return new ArrayList<>();
            }
            List<CamelliaDashboard> allInIds = dashboardMapperWrapper.findInDidsIsUseIsOnline(new ArrayList<>(dashboardUseBO.getDashboards().keySet()), use, isOnline);
            for (CamelliaDashboard camelliaDashboard : allInIds) {
                CamelliaDashboardVO camelliaDashboardVO = new CamelliaDashboardVO(camelliaDashboard);
                camelliaDashboardVO.setRight(dashboardUseBO.getDashboards().get(camelliaDashboard.getDid().intValue()));
                camelliaDashboardVOList.add(camelliaDashboardVO);
            }
            return camelliaDashboardVOList;
        }
    }

    @Override
    public CamelliaDashboard getByDId(Long did) {
        return dashboardMapperWrapper.findByDId(did);
    }

    @Override
    public void updateOnlineStatus(Long id, Integer status) {
        CamelliaDashboard camelliaDashboard = new CamelliaDashboard();
        camelliaDashboard.setDid(id);
        camelliaDashboard.setIsOnline(status);
        camelliaDashboard.setUpdatedTime(System.currentTimeMillis());
        dashboardMapperWrapper.updateOnlineStatus(camelliaDashboard);
    }

    @Override
    public void deleteDashboard(Long id) {
        CamelliaDashboard byId = dashboardMapperWrapper.findByDId(id);
        if (byId == null) {
            throw new AppException(AppCode.DASHBOARD_DO_NOT_EXIST, id + " do not exists");
        }
        if (byId.getIsUse() == use) {
            throw new AppException(AppCode.DASHBOARD_IS_IN_USE, id + " is use now ,can not delete");
        }
        Integer i = dashboardMapperWrapper.deleteByDId(id);
        LogBean.get().addProps("delete dashboard " + id, i);
    }

    @Override
    public WebResult testLink(String url) {
        try {
            DashboardHeartApi init = DashboardApiUtil.init(url, 1000, 2000);
            return init.check();
        } catch (Exception e) {
            throw new AppException(AppCode.DASHBOARD_CONNECT_WRONG, url + " connect wrong " + e.getMessage());
        }
    }
}

package com.netease.nim.camellia.console.service.impl;

import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.service.UserAccessService;
import com.netease.nim.camellia.console.service.ao.IdentityDashboardBaseAO;
import com.netease.nim.camellia.console.service.ao.UserLoginAO;
import com.netease.nim.camellia.console.service.bo.DashboardUseBO;
import com.netease.nim.camellia.console.service.vo.UserLoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;



/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
public class UserAccessServiceImpl implements UserAccessService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessServiceImpl.class);

    @Override
    public BaseUser chargeAccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) {
        BaseUser baseUser=new BaseUser();
        baseUser.setUsername("root");
        AppInfoContext.setUser(baseUser);
        return baseUser;

    }

    @Override
    public UserLoginVO login(UserLoginAO loginAO) {

        return null;
    }

    @Override
    public boolean authorityJudge(ActionSecurity annotation, BaseUser user, IdentityDashboardBaseAO baseAO) {
        return true;
    }

    @Override
    public DashboardUseBO getAllUseDashboardIdAndType(BaseUser user) {
        DashboardUseBO dashboardUseBO=new DashboardUseBO();
        dashboardUseBO.setAll(true);
        return dashboardUseBO;
    }

    @Override
    public boolean authorityAdmin(BaseUser user) {
        return true;
    }

}

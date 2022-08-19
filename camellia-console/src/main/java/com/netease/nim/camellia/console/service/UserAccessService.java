package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.service.ao.IdentityDashboardBaseAO;
import com.netease.nim.camellia.console.service.ao.UserLoginAO;
import com.netease.nim.camellia.console.service.bo.DashboardUseBO;
import com.netease.nim.camellia.console.service.vo.UserLoginVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public interface UserAccessService {

    /**
     * 判断用户是否登录，成功则返回BaseUser，否则返回null
     */
    BaseUser chargeAccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o);

    /**
     * 登录用
     */
    UserLoginVO login(UserLoginAO loginAO);


    boolean authorityJudge(ActionSecurity annotation, BaseUser user, IdentityDashboardBaseAO baseAO);

    DashboardUseBO getAllUseDashboardIdAndType(BaseUser user);

    boolean authorityAdmin(BaseUser user);

}

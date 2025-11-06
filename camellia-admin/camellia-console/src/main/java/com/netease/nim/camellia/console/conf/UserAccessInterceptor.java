package com.netease.nim.camellia.console.conf;

import com.alibaba.fastjson.JSON;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.UserAccessService;
import com.netease.nim.camellia.console.service.ao.IdentityDashboardBaseAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Component
public class UserAccessInterceptor implements HandlerInterceptor {

    @Autowired
    UserAccessService userAccessService;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        AppInfoContext.before();
        BaseUser baseUser = userAccessService.chargeAccess(httpServletRequest, httpServletResponse, o);
        if (baseUser == null) {
            WebResult webResult = new WebResult();
            webResult.setCode(400);
            webResult.setMsg("wrong authority");
            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write(JSON.toJSONString(webResult));
            return false;
        }
        String did = httpServletRequest.getParameter("did");
        if (did != null) {
            IdentityDashboardBaseAO baseAO = new IdentityDashboardBaseAO();
            baseAO.setDid(did);
            AppInfoContext.setIdentityDashboardBaseAO(baseAO);
        }
        AppInfoContext.setUser(baseUser);
        return true;
    }
}

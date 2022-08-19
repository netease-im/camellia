package com.netease.nim.camellia.console.samples.service;

import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.constant.ActionRole;
import com.netease.nim.camellia.console.constant.ActionType;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.service.UserAccessService;
import com.netease.nim.camellia.console.service.ao.IdentityDashboardBaseAO;
import com.netease.nim.camellia.console.service.ao.UserLoginAO;
import com.netease.nim.camellia.console.service.bo.DashboardUseBO;
import com.netease.nim.camellia.console.service.vo.UserLoginVO;
import com.netease.nim.camellia.console.util.OkhttpKit;
import com.netease.nim.camellia.console.util.RespBody;
import com.netease.nim.camellia.console.samples.constant.OpTypeEnum;
import com.netease.nim.camellia.console.samples.service.pojo.OpenAuthNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static com.netease.nim.camellia.console.samples.constant.NetEaseCenterConstant.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Service
@Primary
public class UserAccessServiceOpenAuthImpl implements UserAccessService {

    @Value("${open.auth.url}")
    private String baseUrl;

    @Value("${jwt.failureTime:86400000}")
    private Long failureTime;

    @Value("${jwt.key:camellia}")
    private String secretKey;

    @Value("${open.auth.appCode}")
    private String appCode;

    @Override
    public BaseUser chargeAccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) {
        String accessToken = httpServletRequest.getHeader("accessToken");
        try {
            if (accessToken == null) {
                return null;
            }
            JWTVerifier camellia = JWT.require(Algorithm.HMAC256(secretKey)).build();
            DecodedJWT verify = camellia.verify(accessToken);
            BaseUser baseUser = new BaseUser();
            baseUser.setUsername(verify.getClaim("username").asString());
            return baseUser;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public UserLoginVO login(UserLoginAO loginAO) {
        try {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("username", loginAO.getUsername());
            queryMap.put("password", loginAO.getPassword());
            RespBody respBody = OkhttpKit.getRequest(baseUrl + "/userLogin", queryMap, null);
            if (respBody.getHttpCode() == 200) {
                JSONObject jsonObject = JSONObject.parseObject(respBody.getData());
                Integer code = jsonObject.getInteger("code");
                if (code == 200) {
                    if (jsonObject.getJSONObject("data").getBoolean("valid")) {
                        String sign = JWT.create().withClaim("username", loginAO.getUsername())
                                .withExpiresAt(new Date(System.currentTimeMillis() + failureTime))
                                .sign(Algorithm.HMAC256(secretKey));
                        UserLoginVO userLoginVO = new UserLoginVO();
                        userLoginVO.setAccessToken(sign);
                        userLoginVO.setUsername(loginAO.getUsername());
                        userLoginVO.setTokenTtl(failureTime.intValue());
                        userLoginVO.setLoginSuccess(true);
                        return userLoginVO;
                    }
                }
            }
            UserLoginVO userLoginVO = new UserLoginVO();
            userLoginVO.setLoginSuccess(false);
            userLoginVO.setMessage("账户不存在或者密码错误");
            return userLoginVO;
        } catch (Exception e) {
            e.printStackTrace();
            UserLoginVO userLoginVO = new UserLoginVO();
            userLoginVO.setLoginSuccess(false);
            userLoginVO.setMessage(e.getMessage());
            return userLoginVO;
        }
    }

    @Override
    public boolean authorityJudge(ActionSecurity annotation, BaseUser user, IdentityDashboardBaseAO baseAO) {
        ActionType action = annotation.action();
        ActionRole role = annotation.role();
        String url = baseUrl + "/check";
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("appCode", appCode);
        queryMap.put("name", user.getUsername());
        if (role.equals(ActionRole.ADMIN)) {
            queryMap.put("resourceCode", "dashboard-admin");
        } else {
            if (baseAO == null) {
                return false;
            }
            queryMap.put("resourceCode", "dashboard-admin");
            RespBody request = OkhttpKit.getRequest(url, queryMap, null);
            if (request.getHttpCode() == 200) {
                JSONObject jsonObject = JSONObject.parseObject(request.getData());
                if (jsonObject.getInteger("code").equals(200)) {
                    if (jsonObject.getJSONObject("data").getBooleanValue("valid")) {
                        return true;
                    }
                }
            }
            String did = baseAO.getDid();
            if (action.equals(ActionType.WRITE)) {
                queryMap.put("resourceCode", "dashboard/id-" + did + "/w");
            } else {
                queryMap.put("resourceCode", "dashboard/id-" + did + "/r");
            }
        }
        try {
            RespBody request = OkhttpKit.getRequest(url, queryMap, null);
            if (request.getHttpCode() == 200) {
                JSONObject jsonObject = JSONObject.parseObject(request.getData());
                if (jsonObject.getInteger("code").equals(200)) {
                    return jsonObject.getJSONObject("data").getBooleanValue("valid");
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public DashboardUseBO getAllUseDashboardIdAndType(BaseUser user) {
        String url = baseUrl + "/rightTree";
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("appCode", appCode);
        queryMap.put("name", user.getUsername());
        try {
            RespBody request = OkhttpKit.getRequest(url, queryMap, null);
            if (request.getHttpCode() == 200) {
                JSONObject jsonObject = JSONObject.parseObject(request.getData());
                if (jsonObject.getInteger("code").equals(200)) {
                    List<OpenAuthNode> data = jsonObject.getJSONObject("data").getJSONArray("rightTree").toJavaList(OpenAuthNode.class);
                    return getDashboardUseBoFromTree(data);
                }
            }
            return null;
        } catch (AppException e) {
            return null;
        }
    }

    private DashboardUseBO getDashboardUseBoFromTree(List<OpenAuthNode> data) {
        DashboardUseBO dashboardUseBO = new DashboardUseBO();
        dashboardUseBO.setAll(false);
        if (data == null || data.isEmpty()) {
            return null;
        }
        HashMap<Integer, Integer> dashboards = new HashMap<>();
        dashboardUseBO.setDashboards(dashboards);
        Stack<OpenAuthNode> stack = new Stack<>();
        for (OpenAuthNode node : data) {
            stack.clear();
            stack.push(node);
            while (!stack.isEmpty()) {
                OpenAuthNode pop = stack.pop();
                if (pop.getResourceCode().equals(dashboard)) {
                    dashboardUseBO.setAll(true);
                    return dashboardUseBO;
                } else if (pop.getResourceCode().startsWith(dashboard)) {
                    String[] split = pop.getResourceCode().split(separator);
                    if (split.length == 2) {
                        try {
                            Integer dashboardId = getDashboardIdFromString(split[1]);
                            putDashboardToMap(dashboards, dashboardId, OpTypeEnum.WRITE.getValue());
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (split.length >= 3) {
                        try {
                            Integer dashboardId = getDashboardIdFromString(split[1]);
                            OpTypeEnum opTypeByAlias = OpTypeEnum.getOpTypeByAlias(split[2]);
                            if (opTypeByAlias != null) {
                                putDashboardToMap(dashboards, dashboardId, opTypeByAlias.getValue());
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (pop.getChild() != null)
                    for (OpenAuthNode openAuthNode : pop.getChild()) {
                        stack.push(openAuthNode);
                    }
            }
        }
        return dashboardUseBO;


    }

    private void putDashboardToMap(HashMap<Integer, Integer> dashboards, Integer dashboardId, Integer type) {
        Integer integer = dashboards.get(dashboardId);
        if (integer == null || type > integer) {
            dashboards.put(dashboardId, type);
        }
    }

    private Integer getDashboardIdFromString(String s) {
        String[] splitDashboard = s.split(dashboard_separator);
        if (splitDashboard.length < 2) {
            throw new NumberFormatException();
        }
        return Integer.parseInt(splitDashboard[1]);
    }

    @Override
    public boolean authorityAdmin(BaseUser user) {
        String url = baseUrl + "/check";
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("appCode",appCode);
        queryMap.put("name", user.getUsername());
        queryMap.put("resourceCode","dashboard-admin");
        try {
            RespBody request = OkhttpKit.getRequest(url, queryMap, null);
            if (request.getHttpCode() == 200) {
                JSONObject jsonObject = JSONObject.parseObject(request.getData());
                if (jsonObject.getInteger("code").equals(200)) {
                    return jsonObject.getJSONObject("data").getBooleanValue("valid");
                }
            }
            return false;
        } catch (AppException e) {
            return false;
        }
    }
}

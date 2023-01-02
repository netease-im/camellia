package com.netease.nim.camellia.console.controller;

import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.conf.ConsoleProperties;
import com.netease.nim.camellia.console.constant.ActionRole;
import com.netease.nim.camellia.console.constant.ActionType;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.DashboardService;
import com.netease.nim.camellia.console.service.ao.CamelliaDashboardAO;
import com.netease.nim.camellia.console.util.LogBean;
import com.netease.nim.camellia.console.util.ParaCheckUtil;
import com.netease.nim.camellia.console.util.UrlCheckUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Api(value = "有关管理员对dashboard操作的接口", tags = {"AdminDashboardController"})
@RestController
@ConditionalOnClass(ConsoleProperties.class)
@RequestMapping(value = "/camellia/console/admin/dashboard")
public class AdminDashboardController {

    public static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @Autowired
    DashboardService dashboardService;

    @ApiOperation(value = "手动添加dashboard", notes = "需要指定dashboard的地址如：http://localhost:8080 和 tag")
    @PostMapping("/add")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.ADMIN, resource = "dashboard")
    public WebResult addDashBoard(@RequestBody CamelliaDashboardAO dashboardAO) {
        String address=dashboardAO.getAddress();
        String tag=dashboardAO.getTag();
        ParaCheckUtil.checkParam(address, "address");
        ParaCheckUtil.checkParam(tag, "tag");

        LogBean.get().addProps("address",address);
        LogBean.get().addProps("tag",tag);
        if (!UrlCheckUtil.checkUrl(address)) {
            return WebResult.fail(AppCode.PARAM_WRONG, "请输入正确的url");
        }
        CamelliaDashboard register = dashboardService.register(address, tag);
        return WebResult.success(register);
    }

    @ApiOperation(value = "更改dashboard的黑名单", notes = "指定dashboard的id和 use，use为0代表加入黑名单，use为1代表从黑名单中剔除")
    @PostMapping("/useStatus")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.ADMIN, resource = "dashboard")
    public WebResult changeUseDashBoard(@RequestBody CamelliaDashboardAO dashboardAO) {
        ParaCheckUtil.checkParam(dashboardAO.getDid(), "did");
        ParaCheckUtil.checkParam(dashboardAO.getUse(),"use");
        Long did=Long.parseLong(dashboardAO.getDid());
        Integer use = dashboardAO.getUse();
        LogBean.get().addProps("did",did);
        LogBean.get().addProps("use",use);

        if (use!= 1 && use != 0) {
            throw new AppException(AppCode.PARAM_WRONG, "参数错误");
        }
        dashboardService.useStatusChange(did,use);
        return WebResult.success();
    }

    @ApiOperation(value = "更改dashboard的tag", notes = "指定dashboard的id和 tag")
    @PostMapping("/tag")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.ADMIN, resource = "dashboard")
    public WebResult tagDashBoard(@RequestBody CamelliaDashboardAO dashboardAO) {
        Long did=Long.parseLong(dashboardAO.getDid());
        String tag=dashboardAO.getTag();
        ParaCheckUtil.checkParam(tag, "tag");
        ParaCheckUtil.checkParam(dashboardAO.getDid(), "did");
        LogBean.get().addProps("did",did);
        LogBean.get().addProps("tag",tag);
        dashboardService.tagDashBoard(did,tag);
        return WebResult.success();
    }


    @ApiOperation(value = "删除dashboard（物理删除无法恢复，只能删除处于黑名单下的）", notes = "指定dashboard的id")
    @DeleteMapping()
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.ADMIN, resource = "dashboard")
    public WebResult deleteDashboard(@RequestBody CamelliaDashboardAO dashboardAO) {
        ParaCheckUtil.checkParam(dashboardAO.getDid(), "did");
        Long did=Long.parseLong(dashboardAO.getDid());
        LogBean.get().addProps("did",did);
        dashboardService.deleteDashboard(did);
        return WebResult.success();
    }



    @GetMapping("/testLink")
    public WebResult testLink(@RequestParam String url) {
        LogBean.get().addProps("url", url);
        return dashboardService.testLink(url);
    }


}

package com.netease.nim.camellia.console.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.annotation.ActionSecurity;
import com.netease.nim.camellia.console.conf.ConsoleProperties;
import com.netease.nim.camellia.console.constant.ActionRole;
import com.netease.nim.camellia.console.constant.ActionType;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.context.AppInfoContext;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.BaseUser;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.TableDetail;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.*;
import com.netease.nim.camellia.console.service.ao.TableAO;
import com.netease.nim.camellia.console.service.ao.TableRefAO;
import com.netease.nim.camellia.console.service.ao.TransferAO;
import com.netease.nim.camellia.console.service.ao.URLAO;
import com.netease.nim.camellia.console.service.bo.ResourceInfoBO;
import com.netease.nim.camellia.console.service.bo.TableBO;
import com.netease.nim.camellia.console.service.bo.TableRefBO;
import com.netease.nim.camellia.console.service.vo.*;
import com.netease.nim.camellia.console.util.LogBean;
import com.netease.nim.camellia.console.util.ParaCheckUtil;
import com.netease.nim.camellia.console.util.TableCheckUtil;
import com.netease.nim.camellia.core.api.CamelliaApiCode;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@RestController
@ConditionalOnClass(ConsoleProperties.class)
@RequestMapping(value = "/camellia/console/user/dashboard")
public class UserDashboardController {

    @Autowired
    DashboardService dashboardService;

    @Autowired
    TableService tableService;

    @Autowired
    TableRefService tableRefService;

    @Autowired
    ResourceService resourceService;

    @Autowired
    UserAccessService userAccessService;

    @Autowired(required = false)
    OperationInterceptor interceptor;

    @PostMapping("/transfer")
    public WebResult transferTable(@RequestBody TransferAO transferAO) {
        String table = transferAO.getTable();
        Integer type = transferAO.getType();
        ParaCheckUtil.checkParam(table, "table");
        ParaCheckUtil.checkParam(type, "type");
        if (type != 0 && type != 1) {
            throw new AppException(AppCode.PARAM_WRONG, "type wrong");
        }

        LogBean.get().addProps("table", table);
        LogBean.get().addProps("type", type);
        String ret;
        if (type == 0) {
            ret = TableCheckUtil.parseConsoleTableToDashboard(JSONObject.parseObject(table, TableDetail.class));
        } else {
            ret = JSON.toJSONString(TableCheckUtil.parseDashboardTableToConsole(table));
        }
        LogBean.get().addProps("ret",ret);
        return WebResult.success(ret);
    }

    @GetMapping("/byDid")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "dashboard")
    public WebResult getDashboardById(@RequestParam Long did) {
        LogBean.get().addProps("did", did);
        CamelliaDashboard byId = dashboardService.getByDId(did);
        return WebResult.success(byId);
    }


    @GetMapping()
    public WebResult getAllDashboard(@RequestParam(required = false) String tag, @RequestParam(required = false) Integer isUse, @RequestParam(required = false) Integer isOnline) {
        LogBean.get().addProps("isUse", isUse);
        if (isOnline != null) {
            LogBean.get().addProps("isOnline", isOnline);
        }

        BaseUser user = AppInfoContext.getUser();
        boolean b = userAccessService.authorityAdmin(user);
        List<CamelliaDashboardVO> dashboardVOS = new ArrayList<>();
        if (b) {
            List<CamelliaDashboard> allDashboard;
            allDashboard = dashboardService.getAllDashboardByUseAndOnline(isUse, isOnline);
            for (CamelliaDashboard dashboard : allDashboard) {
                CamelliaDashboardVO camelliaDashboardVO = new CamelliaDashboardVO(dashboard);
                camelliaDashboardVO.setRight(1);
                dashboardVOS.add(camelliaDashboardVO);
            }
        } else {
            dashboardVOS = dashboardService.getUserDashboard(user, tag, isUse, isOnline);
        }
        if (StringUtil.isNullOrEmpty(tag)) {
            return WebResult.success(dashboardVOS);
        }
        LogBean.get().addProps("tag", tag);
        List<CamelliaDashboardVO> results = new ArrayList<>();

        for (CamelliaDashboardVO dashboard : dashboardVOS) {
            if (dashboard.getTag().contains(tag)) {
                results.add(dashboard);
            }
        }
        return WebResult.success(results);
    }


    @GetMapping("/table")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "table")
    public WebResult getAllTable(@RequestParam Long did,
                                 @RequestParam(value = "validFlag", required = false) Integer validFlag,
                                 @RequestParam(value = "tid", required = false) Long tid,
                                 @RequestParam(value = "info", required = false) String info,
                                 @RequestParam(value = "table", required = false) String table,
                                 @RequestParam int pageNum,
                                 @RequestParam int pageSize) {
        if (validFlag != null && validFlag != 1 && validFlag != 0) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "valid wrong");
        }
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("validFlag", validFlag);
        LogBean.get().addProps("info", info);
        LogBean.get().addProps("pageNum", pageNum);
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("table", table);
        CamelliaTablePage tablePage = tableService.getAllTableTidInfo(did, validFlag, tid, info,table, pageNum, pageSize);
        return WebResult.success(tablePage);
    }

    @GetMapping("/table/{tid}")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "table")
    public WebResult getTable(@RequestParam Long did, @PathVariable Long tid) {
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("tid", tid);
        TableBO tableBO = tableService.getTable(did, tid);
        return WebResult.success(tableBO);
    }

    @PostMapping("/createTable")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "table")
    public WebResult createResourceTable(HttpServletRequest request, @RequestBody TableAO tableAO) {
        ParaCheckUtil.checkParam(tableAO.getDid(), "did");
        ParaCheckUtil.checkParam(tableAO.getTable(), "table");
        ParaCheckUtil.checkParam(tableAO.getInfo(), "info");

        ParaCheckUtil.checkParam(tableAO.getType(), "type");
        if (tableAO.getType() != 0 && tableAO.getType() != 1) {
            throw new AppException(AppCode.PARAM_WRONG, "type wrong");
        }

        Integer type = tableAO.getType();
        Long did = Long.parseLong(tableAO.getDid());
        String info = tableAO.getInfo();
        String table = tableAO.getTable();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("table", table);
        LogBean.get().addProps("info", info);
        LogBean.get().addProps("type", type);

        if (interceptor != null) {
            boolean pass = interceptor.createTable(request, AppInfoContext.getUser().getUsername(), tableAO);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        TableBO tableBO = tableService.createTable(did, table, info, type);
        return WebResult.success(tableBO);
    }

    @PostMapping("/changeTable")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "table")
    public WebResult changeResourceTable(HttpServletRequest request, @RequestBody TableAO tableAO) {
        ParaCheckUtil.checkParam(tableAO.getTid(), "tid");
        ParaCheckUtil.checkParam(tableAO.getDid(), "did");
        ParaCheckUtil.checkParam(tableAO.getTable(), "table");
        ParaCheckUtil.checkParam(tableAO.getInfo(), "info");
        ParaCheckUtil.checkParam(tableAO.getType(), "type");
        if (tableAO.getType() != 0 && tableAO.getType() != 1) {
            throw new AppException(AppCode.PARAM_WRONG, "type wrong");
        }
        Integer type = tableAO.getType();
        Long did = Long.parseLong(tableAO.getDid());
        Long tid = tableAO.getTid();
        String info = tableAO.getInfo();
        String table = tableAO.getTable();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("tid", tid);
        LogBean.get().addProps("table", table);
        LogBean.get().addProps("info", info);
        LogBean.get().addProps("type", type);

        if (interceptor != null) {
            TableBO oldTable = tableService.getTable(did, tableAO.getTid());
            boolean pass = interceptor.changeTable(request, AppInfoContext.getUser().getUsername(), oldTable, tableAO);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        TableWithTableRefs tableWithTableRefs = tableService.changeTable(did, tid, table, info, type);
        return WebResult.success(tableWithTableRefs);

    }

    @DeleteMapping("/table")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "table")
    public WebResult deleteResourceTable(HttpServletRequest request, @RequestBody TableAO tableAO) {
        ParaCheckUtil.checkParam(tableAO.getTid(), "tid");
        ParaCheckUtil.checkParam(tableAO.getDid(), "did");


        long did = Long.parseLong(tableAO.getDid());
        Long tid = tableAO.getTid();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("tid", tid);

        if (interceptor != null) {
            TableBO oldTable = tableService.getTable(did, tableAO.getTid());
            boolean pass = interceptor.deleteTable(request, AppInfoContext.getUser().getUsername(), oldTable, tableAO);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        tableService.deleteTable(did, tid);
        return WebResult.success();
    }


    @PostMapping("/createOrUpdateTableRef")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "tableRef")
    public WebResult createOrUpdateTableRef(HttpServletRequest request, @RequestBody TableRefAO tableRefAO) {
        ParaCheckUtil.checkParam(tableRefAO.getDid(), "did");
        ParaCheckUtil.checkParam(tableRefAO.getBid(), "bid");
        ParaCheckUtil.checkParam(tableRefAO.getBgroup(), "bgroup");
        ParaCheckUtil.checkParam(tableRefAO.getTid(), "tid");
        ParaCheckUtil.checkParam(tableRefAO.getInfo(), "info");


        Long did = Long.parseLong(tableRefAO.getDid());
        Long bid = tableRefAO.getBid();
        String bgroup = tableRefAO.getBgroup();
        String info = tableRefAO.getInfo();
        Long tid = tableRefAO.getTid();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("tid", tid);
        LogBean.get().addProps("info", info);

        if (interceptor != null) {
            boolean pass = interceptor.createOrUpdateTableRef(request, AppInfoContext.getUser().getUsername(), getTableRef(did, bid, bgroup), tableRefAO);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        TableRefBO tableRefBO = tableRefService.bindBigBgroupWithTid(did, bid, bgroup, tid, info);
        return WebResult.success(tableRefBO);
    }

    private TableRefBO getTableRef(long did, long bid, String bgroup) {
        CamelliaTableRefPage tableRef1 = tableRefService.getTableRef(did, bid, bgroup, null, null,
                1, 1, null, null);
        List<TableRefBO> tableRefs = tableRef1.getTableRefs();
        TableRefBO oldTableRef = null;
        if (tableRefs != null && !tableRefs.isEmpty()) {
            oldTableRef = tableRefs.get(0);
        }
        return oldTableRef;
    }

    @DeleteMapping("/tableRef")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "tableRef")
    public WebResult deleteTableRef(HttpServletRequest request, @RequestBody TableRefAO tableRefAO) {
        ParaCheckUtil.checkParam(tableRefAO.getDid(), "did");
        ParaCheckUtil.checkParam(tableRefAO.getBid(), "bid");
        ParaCheckUtil.checkParam(tableRefAO.getBgroup(), "bgroup");

        Long did = Long.parseLong(tableRefAO.getDid());
        Long bid = tableRefAO.getBid();
        String bgroup = tableRefAO.getBgroup();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);

        if (interceptor != null) {
            boolean pass = interceptor.deleteTableRef(request, AppInfoContext.getUser().getUsername(), getTableRef(did, bid, bgroup), tableRefAO);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        tableRefService.delete(did, bid, bgroup);
        return WebResult.success();
    }


    @GetMapping("/tableRef")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "tableRef")
    public WebResult getTableRef(
            @RequestParam Long did,
            @RequestParam(value = "tid", required = false) Long tid,
            @RequestParam(value = "bid", required = false) Long bid,
            @RequestParam(value = "bgroup", required = false) String bgroup,
            @RequestParam(value = "validFlag", required = false) Integer validFlag,
            @RequestParam(value = "info", required = false) String info,
            @RequestParam(value = "resourceInfo",required = false)String resourceInfo,
            @RequestParam int pageNum,
            @RequestParam int pageSize
    ) {
        if (validFlag != null && validFlag != 1 && validFlag != 0) {
            throw new AppException(CamelliaApiCode.PARAM_ERROR.getCode(), "valid wrong");
        }
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("tid", tid);
        LogBean.get().addProps("validFlag", validFlag);
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("info", info);
        LogBean.get().addProps("resourceInfo", resourceInfo);
        LogBean.get().addProps("pageNum", pageNum);
        LogBean.get().addProps("pageSize", pageSize);
        if (bid == null && !StringUtil.isNullOrEmpty(bgroup)) {
            throw new AppException(AppCode.PARAM_WRONG, "bid is null but bgroup is no null");
        }
        CamelliaTableRefPage tableRef = tableRefService.getTableRef(did, bid, bgroup, tid, validFlag, pageNum, pageSize, info,resourceInfo);
        return WebResult.success(tableRef);
    }


    @GetMapping("/resources")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "resource")
    public WebResult getResourceList(@RequestParam Long did,
                                     @RequestParam(value = "url", required = false, defaultValue = "") String url,
                                     @RequestParam int pageNum,
                                     @RequestParam int pageSize) {
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("pageNum", pageNum);
        LogBean.get().addProps("pageSize", pageSize);
        if (!StringUtil.isNullOrEmpty(url)) {
            LogBean.get().addProps("url", url);
        }
        CamelliaResourcePage camelliaResourcePage = resourceService.getAllUrl(did, url, pageNum, pageSize);
        return WebResult.success(camelliaResourcePage);
    }


    @GetMapping("/resourcesQuery")
    @ActionSecurity(action = ActionType.READ, role = ActionRole.NORMAL, resource = "resource")
    public WebResult getResourceQuery(@RequestParam Long did,
                                     @RequestParam(value = "url") String url,
                                     @RequestParam(value = "size" ,required = false,defaultValue = "5") int size) {
        if(size<=0){
            throw new AppException(AppCode.PARAM_WRONG, size + " 必须大于0");
        }
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("url", url);
        LogBean.get().addProps("size", size);
        List<ResourceInfoBO> resourceInfoBOList = resourceService.getResourceQuery(did, url,size);
        return WebResult.success(resourceInfoBOList);
    }


    @PostMapping("/createOrUpdateResource")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "resource")
    public WebResult createOrUpdateResource(HttpServletRequest request, @RequestBody URLAO urlao) {
        ParaCheckUtil.checkParam(urlao.getDid(), "did");
        ParaCheckUtil.checkParam(urlao.getUrl(), "url");
        long did = Long.parseLong(urlao.getDid());
        String url = urlao.getUrl();
        String info = urlao.getInfo();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("url", url);
        LogBean.get().addProps("info", info);

        if (interceptor != null) {
            boolean pass = interceptor.createOrUpdateResource(request, AppInfoContext.getUser().getUsername(), urlao);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        resourceService.createOrUpdateResource(did, url, info);
        return WebResult.success();
    }

    @DeleteMapping("/resource")
    @ActionSecurity(action = ActionType.WRITE, role = ActionRole.NORMAL, resource = "resource")
    public WebResult deleteResource(HttpServletRequest request, @RequestBody URLAO urlao) {
        ParaCheckUtil.checkParam(urlao.getDid(), "did");
        ParaCheckUtil.checkParam(urlao.getId(), "id");
        long did = Long.parseLong(urlao.getDid());
        Long id = urlao.getId();
        LogBean.get().addProps("did", did);
        LogBean.get().addProps("id", id);

        if (interceptor != null) {
            boolean pass = interceptor.deleteResource(request, AppInfoContext.getUser().getUsername(), urlao);
            if (!pass) {
                LogBean.get().addProps("interceptor.pass", false);
                throw new AppException(AppCode.FORBIDDEN, "forbidden");
            }
        }

        resourceService.delete(did, id);
        return WebResult.success();
    }


}

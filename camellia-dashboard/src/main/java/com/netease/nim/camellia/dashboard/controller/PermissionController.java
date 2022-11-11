package com.netease.nim.camellia.dashboard.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.constant.IpCheckMode;
import com.netease.nim.camellia.dashboard.dto.CreateOrUpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.dashboard.service.IIpCheckerService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Api(value = "Permission Interface", tags = {"PermissionController"})
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/permissions")
public class PermissionController {
    @Autowired
    private IIpCheckerService ipCheckerService;

    @ApiOperation(value = "Get ip checker all", notes = "This is a heartbeat interface, the client pulls it regularly (such as 5s), and judges whether there is an update through the MD5 value")
    @GetMapping("/ip-checkers/all")
    public DataWithMd5Response<List<IpChecker>> getIpCheckerList(@RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("md5", md5);
        DataWithMd5Response<List<IpChecker>> response = ipCheckerService.getList(md5);
        LogBean.get().addProps("response", response);
        return response;
    }

    @ApiOperation(value = "Find ip checker by id", notes = "Find ip checker by id")
    @GetMapping("/ip-checkers/{id}")
    public WebResult findIpCheckerById(@PathVariable("id") Long id) {
        LogBean.get().addProps("id", id);
        IpChecker ipChecker = ipCheckerService.findById(id);
        JSONObject ipCheckerJson = ipChecker.toJson();
        LogBean.get().addProps("ipChecker", ipCheckerJson);
        return WebResult.success(ipCheckerJson);
    }


    @ApiOperation(value = "Create ip checker", notes = "create ip checker")
    @PostMapping("/ip-checkers")
    public WebResult CreateChecker(@RequestBody CreateOrUpdateIpCheckerRequest request
    ) {
        LogBean.get().addProps("bid", request.getBid());
        LogBean.get().addProps("bgroup", request.getBgroup());
        LogBean.get().addProps("mode", request.getMode());
        LogBean.get().addProps("ipList", request.getIpList());

        IpChecker ipchecker = ipCheckerService.create(request);
        JSONObject ipCheckerJson = ipchecker.toJson();
        LogBean.get().addProps("ipChecker", ipCheckerJson);
        return WebResult.success(ipCheckerJson);
    }

    @ApiOperation(value = "Create ip checker", notes = "create ip checker")
    @PutMapping("/ip-checkers/{id}")
    public WebResult CreateChecker(
            @PathVariable("id") Long id,
            @RequestBody CreateOrUpdateIpCheckerRequest request
    ) {
        LogBean.get().addProps("id", id);
        LogBean.get().addProps("bid", request.getBid());
        LogBean.get().addProps("bgroup", request.getBgroup());
        LogBean.get().addProps("mode", request.getMode());
        LogBean.get().addProps("ipList", request.getIpList());

        IpChecker ipchecker = ipCheckerService.update(id, request);
        JSONObject ipCheckerJson = ipchecker.toJson();
        LogBean.get().addProps("ipChecker", ipCheckerJson);
        return WebResult.success(ipCheckerJson);
    }


    @ApiOperation(value = "Find ip checkers by conditions", notes = "Find ip checkers by conditions")
    @GetMapping("/ip-checkers")
    public WebResult findIpCheckers(
            @RequestParam(value = "bid", required = false) Long bid,
            @RequestParam(value = "bgroup", required = false) String bgroup,
            @RequestParam(value = "mode", required = false) IpCheckMode mode,
            @RequestParam(value = "ip", required = false) String ip,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("mode", mode);
        LogBean.get().addProps("ip", ip);
        LogBean.get().addProps("page", page);
        LogBean.get().addProps("limit", limit);
        PageCriteria pageCriteria = new PageCriteria(page, limit);

        Page<IpChecker> ipCheckers = ipCheckerService.findIpCheckers(bid, bgroup, mode, ip, pageCriteria);

        JSONObject response = new JSONObject();
        JSONObject paging = new JSONObject();
        paging.put("limit", pageCriteria.getLimit());
        paging.put("page", pageCriteria.getPage());
        paging.put("total", ipCheckers.getTotalElements());
        response.put("paging", paging);

        JSONArray content = new JSONArray();
        for (IpChecker ipChecker : ipCheckers.getContent()) {
            content.add(ipChecker.toJson());
        }
        response.put("content", content);

        LogBean.get().addProps("ipCheckers", content);
        LogBean.get().addProps("paging", paging);
        return WebResult.success(response);
    }

}

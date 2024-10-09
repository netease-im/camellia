package com.netease.nim.camellia.dashboard.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.PageCriteria;
import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.dto.CreateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.dto.CreateRateLimitRequest;
import com.netease.nim.camellia.dashboard.dto.UpdateIpCheckerRequest;
import com.netease.nim.camellia.dashboard.dto.UpdateRateLimitRequest;
import com.netease.nim.camellia.dashboard.model.IpChecker;
import com.netease.nim.camellia.dashboard.model.RateLimit;
import com.netease.nim.camellia.dashboard.service.IIpCheckerService;
import com.netease.nim.camellia.dashboard.service.IRateLimitService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/permissions")
public class PermissionController {
    @Autowired
    private IIpCheckerService ipCheckerService;

    @Autowired
    private IRateLimitService rateLimitService;

    @GetMapping("/ip-checkers/{id}")
    public WebResult findIpCheckerById(@PathVariable("id") Long id) {
        LogBean.get().addProps("id", id);
        IpChecker ipChecker = ipCheckerService.findById(id);
        JSONObject ipCheckerJson = ipChecker.toJson();
        LogBean.get().addProps("ipChecker", ipCheckerJson);
        return WebResult.success(ipCheckerJson);
    }

    @PostMapping("/ip-checkers")
    public WebResult createIpChecker(@RequestBody @Valid CreateIpCheckerRequest request
    ) {
        LogBean.get().addProps("bid", request.getBid());
        LogBean.get().addProps("bgroup", request.getBgroup());
        LogBean.get().addProps("mode", request.getMode());
        LogBean.get().addProps("ipList", request.getIpList());

        IpChecker ipChecker = ipCheckerService.create(request);
        JSONObject ipCheckerJson = ipChecker.toJson();
        LogBean.get().addProps("ipChecker", ipCheckerJson);
        return WebResult.success(ipCheckerJson);
    }

    @PutMapping("/ip-checkers/{id}")
    public WebResult updateIpChecker(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateIpCheckerRequest request) {
        LogBean.get().addProps("id", id);
        LogBean.get().addProps("mode", request.getMode());
        LogBean.get().addProps("ipList", request.getIpList());

        IpChecker ipChecker = ipCheckerService.update(id, request);
        JSONObject ipCheckerJson = ipChecker.toJson();
        LogBean.get().addProps("ipChecker", ipCheckerJson);
        return WebResult.success(ipCheckerJson);
    }

    @DeleteMapping("/ip-checkers/{id}")
    public WebResult deleteIpChecker(@PathVariable("id") Long id) {
        LogBean.get().addProps("id", id);
        ipCheckerService.delete(id);
        return WebResult.success();
    }

    @GetMapping("/ip-checkers")
    public WebResult findIpCheckers(
            @RequestParam(value = "bid", required = false) Long bid,
            @RequestParam(value = "bgroup", required = false) String bgroup,
            @RequestParam(value = "mode", required = false) IpCheckMode mode,
            @RequestParam(value = "ip", required = false) String ip,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
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


    /*
      ======================= Rate Limit =======================
     */
    @GetMapping("/rate-limit/{id}")
    public WebResult findRateLimitById(@PathVariable("id") Long id) {
        LogBean.get().addProps("id", id);
        RateLimit rateLimit = rateLimitService.findById(id);
        JSONObject rateLimitJson = rateLimit.toJson();
        LogBean.get().addProps("rateLimit", rateLimitJson);
        return WebResult.success(rateLimitJson);
    }

    @PostMapping("/rate-limit")
    public WebResult createRateLimit(@RequestBody @Valid CreateRateLimitRequest request
    ) {
        LogBean.get().addProps("bid", request.getBid());
        LogBean.get().addProps("bgroup", request.getBgroup());
        LogBean.get().addProps("millis", request.getCheckMillis());
        LogBean.get().addProps("maxCount", request.getMaxCount());
        RateLimit rateLimit = rateLimitService.create(request);
        JSONObject rateLimitJson = rateLimit.toJson();
        LogBean.get().addProps("rateLimit", rateLimitJson);
        return WebResult.success(rateLimitJson);
    }

    @PutMapping("/rate-limit/{id}")
    public WebResult updateRateLimit(
            @PathVariable("id") long id,
            @RequestBody @Valid UpdateRateLimitRequest request) {
        LogBean.get().addProps("id", id);
        LogBean.get().addProps("millis", request.getCheckMillis());
        LogBean.get().addProps("maxCount", request.getMaxCount());
        rateLimitService.update(id, request);
        return WebResult.success();
    }

    @DeleteMapping("/rate-limit/{id}")
    public WebResult deleteRateLimit(@PathVariable("id") long id) {
        LogBean.get().addProps("id", id);
        rateLimitService.delete(id);
        return WebResult.success();
    }

    @GetMapping("/rate-limit")
    public WebResult searchRateLimitConfigurations(
            @RequestParam(value = "bid", required = false) Long bid,
            @RequestParam(value = "bgroup", required = false) String bgroup,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("page", page);
        LogBean.get().addProps("limit", limit);
        PageCriteria pageCriteria = new PageCriteria(page, limit);

        Page<RateLimit> rateLimits = rateLimitService.searchRateLimitConfigurations(bid, bgroup, pageCriteria);

        JSONObject response = new JSONObject();
        JSONObject paging = new JSONObject();
        paging.put("limit", pageCriteria.getLimit());
        paging.put("page", pageCriteria.getPage());
        paging.put("total", rateLimits.getTotalElements());
        response.put("paging", paging);

        JSONArray content = new JSONArray();
        for (RateLimit rateLimit : rateLimits.getContent()) {
            content.add(rateLimit.toJson());
        }
        response.put("content", content);

        LogBean.get().addProps("rateLimits", content);
        LogBean.get().addProps("paging", paging);
        return WebResult.success(response);
    }

}

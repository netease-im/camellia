package com.netease.nim.camellia.dashboard.controller;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiResponse;
import com.netease.nim.camellia.core.api.ResourceStats;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.service.ResourceTableService;
import com.netease.nim.camellia.dashboard.service.StatsService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;

/**
 *
 * Created by caojiajun on 2019/5/17.
 */
@Api(value = "API接口", tags = {"ApiController"})
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/camellia/api")
public class ApiController implements CamelliaApi {

    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private StatsService statsService;

    @ApiOperation(value = "获取资源表", notes = "这是一个心跳接口，客户端定时来拉取（如5s），通过MD5值判断是否有更新")
    @GetMapping(value = "/resourceTable")
    public CamelliaApiResponse getResourceTable(@RequestParam("bid") Long bid,
                                                @RequestParam("bgroup") String bgroup,
                                                @RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("md5", md5);
        CamelliaApiResponse response = resourceTableService.get(bid, bgroup, md5);
        LogBean.get().addProps("response", response);
        return response;
    }

    @ApiOperation(value = "汇报资源读写统计数据", notes = "客户端定时汇报，服务器会将统计数据汇总存储在缓存中")
    @PostMapping("/reportStats")
    public boolean reportStats(@RequestBody ResourceStats resourceStats) {
        LogBean.get().addProps("resourceStats", resourceStats);
        String ip = LogBean.get().getIp();
        statsService.stats(ip, resourceStats);
        return true;
    }
}

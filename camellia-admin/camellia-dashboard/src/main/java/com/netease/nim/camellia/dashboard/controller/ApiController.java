package com.netease.nim.camellia.dashboard.controller;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiResponse;
import com.netease.nim.camellia.core.api.CamelliaApiV2Response;
import com.netease.nim.camellia.core.api.ResourceStats;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.service.IIpCheckerService;
import com.netease.nim.camellia.dashboard.service.ResourceTableService;
import com.netease.nim.camellia.dashboard.service.StatsService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.*;


/**
 *
 * Created by caojiajun on 2019/5/17.
 */
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/camellia/api")
public class ApiController implements CamelliaApi {

    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private IIpCheckerService ipCheckerService;

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

    @GetMapping(value = "/v2/resourceTable")
    public CamelliaApiV2Response getResourceTableV2(@RequestParam("bid") Long bid,
                                                    @RequestParam("bgroup") String bgroup,
                                                    @RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("bid", bid);
        LogBean.get().addProps("bgroup", bgroup);
        LogBean.get().addProps("md5", md5);
        CamelliaApiResponse response = resourceTableService.get(bid, bgroup, md5);
        LogBean.get().addProps("response", response);
        return ResourceTableUtil.toV2Response(response);
    }

    @PostMapping("/reportStats")
    public boolean reportStats(@RequestBody ResourceStats resourceStats) {
        LogBean.get().addProps("resourceStats", resourceStats);
        String ip = LogBean.get().getIp();
        statsService.stats(ip, resourceStats);
        return true;
    }
}

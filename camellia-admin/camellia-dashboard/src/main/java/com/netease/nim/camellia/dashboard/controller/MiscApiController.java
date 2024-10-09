package com.netease.nim.camellia.dashboard.controller;

import com.netease.nim.camellia.core.api.CamelliaMiscApi;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.core.model.RateLimitDto;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.service.IIpCheckerService;
import com.netease.nim.camellia.dashboard.service.IRateLimitService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by caojiajun on 2022/11/14
 */
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/camellia/misc/api")
public class MiscApiController implements CamelliaMiscApi {

    @Autowired
    private IIpCheckerService ipCheckerService;

    @Autowired
    private IRateLimitService rateLimitService;

    @GetMapping("/permissions/ip-checkers")
    public DataWithMd5Response<List<IpCheckerDto>> getIpCheckerList(@RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("md5", md5);
        DataWithMd5Response<List<IpCheckerDto>> response = ipCheckerService.getList(md5);
        LogBean.get().addProps("response", response);
        return response;
    }


    @GetMapping("/permissions/rate-limit")
    public DataWithMd5Response<List<RateLimitDto>> getRateLimitConfigurationList(@RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("md5", md5);
        DataWithMd5Response<List<RateLimitDto>> response = rateLimitService.getList(md5);
        LogBean.get().addProps("response", response);
        return response;
    }



}

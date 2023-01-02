package com.netease.nim.camellia.dashboard.controller;

import com.netease.nim.camellia.core.api.CamelliaMiscApi;
import com.netease.nim.camellia.core.api.DataWithMd5Response;
import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.core.model.RateLimitDto;
import com.netease.nim.camellia.dashboard.conf.DashboardProperties;
import com.netease.nim.camellia.dashboard.service.IIpCheckerService;
import com.netease.nim.camellia.dashboard.service.IRateLimitService;
import com.netease.nim.camellia.dashboard.util.LogBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(value = "API接口", tags = {"ApiController"})
@RestController
@ConditionalOnClass(DashboardProperties.class)
@RequestMapping(value = "/camellia/misc/api")
public class MiscApiController implements CamelliaMiscApi {

    @Autowired
    private IIpCheckerService ipCheckerService;

    @Autowired
    private IRateLimitService rateLimitService;

    @ApiOperation(value = "Get ip checker all", notes = "This is a heartbeat interface, the client pulls it regularly (such as 5s), and judges whether there is an update through the MD5 value")
    @GetMapping("/permissions/ip-checkers")
    public DataWithMd5Response<List<IpCheckerDto>> getIpCheckerList(@RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("md5", md5);
        DataWithMd5Response<List<IpCheckerDto>> response = ipCheckerService.getList(md5);
        LogBean.get().addProps("response", response);
        return response;
    }


    @ApiOperation(value = "Get rate limit configurations", notes = "This is a heartbeat interface, the client pulls it regularly (such as 5s), and judges whether there is an update through the MD5 value")
    @GetMapping("/permissions/rate-limit")
    public DataWithMd5Response<List<RateLimitDto>> getRateLimitConfigurationList(@RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("md5", md5);
        DataWithMd5Response<List<RateLimitDto>> response = rateLimitService.getList(md5);
        LogBean.get().addProps("response", response);
        return response;
    }
}

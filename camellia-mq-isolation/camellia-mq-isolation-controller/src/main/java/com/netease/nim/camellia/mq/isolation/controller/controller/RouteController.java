package com.netease.nim.camellia.mq.isolation.controller.controller;

import com.netease.nim.camellia.mq.isolation.controller.service.RouteService;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/6
 */
@RestController
@RequestMapping("/camellia/mq/isolation/route")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @RequestMapping(value = "/reportConsumerBizStats", method = RequestMethod.POST)
    public WebResult reportConsumerBizStats(@RequestBody ConsumerBizStatsRequest request) {
        routeService.reportConsumerBizStats(request);
        return WebResult.success();
    }

    @RequestMapping(value = "/reportSenderBizStats", method = RequestMethod.POST)
    public WebResult reportSenderBizStats(@RequestBody SenderBizStatsRequest request) {
        routeService.reportSenderBizStats(request);
        return WebResult.success();
    }

    @RequestMapping(value = "/selectMq", method = RequestMethod.GET)
    public WebResult selectMq(@RequestParam(name = "namespace") String namespace,
                              @RequestParam(name = "bizId") String bizId) {
        List<MqInfo> mqInfos = routeService.selectMqInfo(namespace, bizId);
        return WebResult.success(mqInfos);
    }

}

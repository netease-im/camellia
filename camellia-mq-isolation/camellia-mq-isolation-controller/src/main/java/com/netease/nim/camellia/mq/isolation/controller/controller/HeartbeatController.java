package com.netease.nim.camellia.mq.isolation.controller.controller;

import com.netease.nim.camellia.mq.isolation.controller.service.HeartbeatService;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by caojiajun on 2024/2/20
 */
@RestController
@RequestMapping("/camellia/mq/isolation/heartbeat")
public class HeartbeatController {

    @Autowired
    private HeartbeatService heartbeatService;

    @RequestMapping("/senderHeartbeat")
    public WebResult senderHeartbeat(@RequestBody SenderHeartbeat heartbeat) {
        heartbeatService.senderHeartbeat(heartbeat);
        return WebResult.success();
    }

    @RequestMapping("/consumerHeartbeat")
    public WebResult consumerHeartbeat(@RequestBody ConsumerHeartbeat heartbeat) {
        heartbeatService.consumerHeartbeat(heartbeat);
        return WebResult.success();
    }

    @RequestMapping("/querySenderHeartbeat")
    public WebResult querySenderHeartbeat(@RequestParam String namespace) {
        List<SenderHeartbeat> list = heartbeatService.querySenderHeartbeat(namespace);
        return WebResult.success(list);
    }

    @RequestMapping("/queryConsumerHeartbeat")
    public WebResult queryConsumerHeartbeat(@RequestParam String namespace) {
        List<ConsumerHeartbeat> list = heartbeatService.queryConsumerHeartbeat(namespace);
        return WebResult.success(list);
    }
}

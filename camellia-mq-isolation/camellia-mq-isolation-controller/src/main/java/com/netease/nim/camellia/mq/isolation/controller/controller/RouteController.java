package com.netease.nim.camellia.mq.isolation.controller.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by caojiajun on 2024/2/6
 */
@RestController
@RequestMapping("/camellia/mq/isolation/route")
public class RouteController {

    @RequestMapping(value = "/selectMq", method = RequestMethod.GET)
    public WebResult selectMq(@RequestParam String namespace, @RequestParam String bizId) {
        return WebResult.success();
    }

}

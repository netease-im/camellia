package com.netease.nim.camellia.mq.isolation.controller.controller;

import com.netease.nim.camellia.mq.isolation.controller.service.ConfigServiceWrapper;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by caojiajun on 2024/2/6
 */
@RestController
@RequestMapping("/camellia/mq/isolation/config")
public class ConfigController {

    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @RequestMapping(value = "/getMqIsolationConfig", method = RequestMethod.GET)
    public WebResult getMqIsolationConfig(@RequestParam(name = "namespace") String namespace) {
        MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
        return WebResult.success(config);
    }
}

package com.netease.nim.camellia.config.controller;

import com.netease.nim.camellia.config.conf.LogBean;
import com.netease.nim.camellia.config.service.ConfigService;
import com.netease.nim.camellia.core.api.CamelliaConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by caojiajun on 2023/3/15
 */
@RestController
@RequestMapping("/camellia/config/api")
public class ConfigApiController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/getConfig")
    public CamelliaConfigResponse getConfig(@RequestParam(value = "namespace") String namespace,
                                            @RequestParam(value = "md5", required = false) String md5) {
        LogBean.get().addProps("md5", md5);
        LogBean.get().addProps("namespace", namespace);
        CamelliaConfigResponse response = configService.getConfig(namespace, md5);
        LogBean.get().addProps("response", response);
        return response;
    }
}

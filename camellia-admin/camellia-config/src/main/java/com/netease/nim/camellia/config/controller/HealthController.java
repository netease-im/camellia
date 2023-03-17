package com.netease.nim.camellia.config.controller;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.config.conf.ConfigProperties;
import com.netease.nim.camellia.config.exception.AppException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * Created by caojiajun on 2018/5/14.
 */
@RestController
@ConditionalOnClass(ConfigProperties.class)
@RequestMapping(value = "/health")
public class HealthController {

    private static final JSONObject SUCCESS = new JSONObject();
    static {
        SUCCESS.put("code", 200);
    }

    @RequestMapping(value = "/status")
    @ResponseBody
    public JSONObject status() throws Exception {
        if (HealthStatus.status == HealthStatus.ONLINE) {
            return SUCCESS;
        } else {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @RequestMapping(value = "/check")
    @ResponseBody
    public JSONObject check() throws Exception {
        return SUCCESS;
    }

    @RequestMapping(value = "/online")
    @ResponseBody
    public JSONObject online() throws Exception {
        if (HealthStatus.status == HealthStatus.OFFLINE) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return SUCCESS;
    }

    @RequestMapping(value = "/offline")
    @ResponseBody
    public JSONObject offline() throws Exception {
        //并把status状态设置为OFFLINE，方便服务调用者通过健康检查及时发现下线节点，从而做到快速的上线下线
        HealthStatus.status = HealthStatus.OFFLINE;
        //应该反复多次调用offline接口，直到返回200，才能执行杀进程操作，以等待所有请求已经处理完毕
        if (System.currentTimeMillis() - HealthStatus.lastRequestTimestamp < 10 * 1000) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return SUCCESS;
    }
}

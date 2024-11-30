package com.netease.nim.camellia.mq.isolation.controller.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.mq.isolation.controller.service.ConfigServiceWrapper;
import com.netease.nim.camellia.mq.isolation.controller.service.HeartbeatService;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfigUtils;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/2/6
 */
@RestController
@RequestMapping("/camellia/mq/isolation/config")
public class CamelliaMqIsolationConfigController {

    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @Autowired
    private HeartbeatService heartbeatService;

    @RequestMapping(value = "/getMqIsolationConfig", method = RequestMethod.GET)
    public WebResult getMqIsolationConfig(@RequestParam(name = "namespace") String namespace) {
        CamelliaMqIsolationControllerStatus.updateLastUseTime();
        MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
        if (config == null) {
            return new WebResult(HttpStatus.NOT_FOUND.value());
        }
        return WebResult.success(config);
    }

    @RequestMapping(value = "/listNamespaces", method = RequestMethod.GET)
    public WebResult listNamespaces() {
        CamelliaMqIsolationControllerStatus.updateLastUseTime();
        List<String> list = configServiceWrapper.listNamespaces();
        return WebResult.success(list);
    }

    @RequestMapping(value = "/getNamespaceInfo", method = RequestMethod.GET)
    public WebResult getNamespaceInfo(@RequestParam("namespace") String namespace) {
        MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
        if (config == null) {
            return new WebResult(HttpStatus.NOT_FOUND.value());
        }
        JSONObject json = new JSONObject();
        json.put("config", config);

        JSONArray mqHeartbeatJsonArray = new JSONArray();
        Set<MqInfo> allMqInfo = MqIsolationConfigUtils.getAllMqInfo(config);
        allMqInfo.forEach(mqInfo -> {
            JSONObject mqHeartbeatJson = new JSONObject();
            mqHeartbeatJson.put("mq", mqInfo.getMq());
            mqHeartbeatJson.put("topic", mqInfo.getTopic());
            mqHeartbeatJson.put("active", heartbeatService.isActive(mqInfo));
            mqHeartbeatJsonArray.add(mqHeartbeatJson);
        });
        json.put("mqInfoHeartbeat", mqHeartbeatJsonArray);

        List<SenderHeartbeat> list1 = heartbeatService.querySenderHeartbeat(namespace);
        JSONArray senderHeartbeatJsonArray = new JSONArray();
        list1.forEach(heartbeat -> {
            JSONObject senderHeartbeatJson = new JSONObject();
            senderHeartbeatJson.put("instanceId", heartbeat.getInstanceId());
            senderHeartbeatJson.put("host", heartbeat.getHost());
            senderHeartbeatJsonArray.add(senderHeartbeatJson);
        });
        json.put("senderHeartbeat", senderHeartbeatJsonArray);

        List<ConsumerHeartbeat> list2 = heartbeatService.queryConsumerHeartbeat(namespace);
        JSONArray consumerHeartbeatJsonArray = new JSONArray();
        list2.forEach(heartbeat -> {
            JSONObject consumerHeartbeatJson = new JSONObject();
            consumerHeartbeatJson.put("instanceId", heartbeat.getInstanceId());
            consumerHeartbeatJson.put("host", heartbeat.getHost());
            consumerHeartbeatJson.put("mq", heartbeat.getMqInfo().getMq());
            consumerHeartbeatJson.put("topic", heartbeat.getMqInfo().getTopic());
            consumerHeartbeatJsonArray.add(consumerHeartbeatJson);
        });
        json.put("consumerHeartbeat", consumerHeartbeatJsonArray);

        return WebResult.success(json);
    }
}

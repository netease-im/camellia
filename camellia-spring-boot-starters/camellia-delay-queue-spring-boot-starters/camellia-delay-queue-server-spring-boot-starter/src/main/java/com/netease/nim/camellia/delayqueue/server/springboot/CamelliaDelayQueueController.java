package com.netease.nim.camellia.delayqueue.server.springboot;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.delayqueue.common.domain.*;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueMonitor;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueMonitorData;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServer;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueTopicInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Created by caojiajun on 2022/7/20
 */
@RestController
@RequestMapping("/camellia/delayQueue")
public class CamelliaDelayQueueController {

    @Autowired
    private CamelliaDelayQueueServer server;

    @PostMapping("/sendMsg")
    public CamelliaDelayMsgSendResponse sendMsg(@RequestParam("topic") String topic,
                                                @RequestParam("msg") String msg,
                                                @RequestParam("delayMillis") long delayMillis,
                                                @RequestParam(value = "ttlMillis", required = false, defaultValue = "-1") long ttlMillis,
                                                @RequestParam(value = "maxRetry", required = false, defaultValue = "-1") int maxRetry) {
        CamelliaDelayMsgSendRequest request = new CamelliaDelayMsgSendRequest();
        request.setTopic(topic);
        request.setMsg(msg);
        request.setDelayMillis(delayMillis);
        request.setTtlMillis(ttlMillis);
        request.setMaxRetry(maxRetry);
        return server.sendMsg(request);
    }

    @PostMapping("/deleteMsg")
    public CamelliaDelayMsgDeleteResponse deleteMsg(@RequestParam("topic") String topic,
                                                    @RequestParam("msgId") String msgId) {
        CamelliaDelayMsgDeleteRequest request = new CamelliaDelayMsgDeleteRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        return server.deleteMsg(request);
    }

    @PostMapping("/pullMsg")
    public CamelliaDelayMsgPullResponse pullMsg(@RequestParam("topic") String topic,
                                                @RequestParam(value = "ackTimeoutMillis", required = false, defaultValue = "-1") long ackTimeoutMillis,
                                                @RequestParam(value = "batch", required = false, defaultValue = "-1") int batch) {
        CamelliaDelayMsgPullRequest request = new CamelliaDelayMsgPullRequest();
        request.setTopic(topic);
        request.setAckTimeoutMillis(ackTimeoutMillis);
        request.setBatch(batch);
        return server.pullMsg(request);
    }

    @PostMapping("/ackMsg")
    public CamelliaDelayMsgAckResponse ackMsg(@RequestParam("topic") String topic,
                                              @RequestParam("msgId") String msgId,
                                              @RequestParam("ack") boolean ack) {
        CamelliaDelayMsgAckRequest request = new CamelliaDelayMsgAckRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        request.setAck(ack);
        return server.ackMsg(request);
    }

    @PostMapping("/getMsg")
    public CamelliaDelayMsgGetResponse getMsg(@RequestParam("topic") String topic,
                                              @RequestParam("msgId") String msgId) {
        CamelliaDelayMsgGetRequest request = new CamelliaDelayMsgGetRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        return server.getMsg(request);
    }

    @GetMapping("/getMonitorData")
    public JSONObject getMonitorData() {
        CamelliaDelayQueueMonitorData monitorData = CamelliaDelayQueueMonitor.getMonitorData();
        JSONObject json = new JSONObject();
        json.put("code", 200);
        json.put("data", monitorData);
        return json;
    }

    @GetMapping("/getTopicInfo")
    public JSONObject getTopicInfo(@RequestParam("topic") String topic) {
        CamelliaDelayQueueTopicInfo topicInfo = server.getTopicInfo(topic);
        JSONObject json = new JSONObject();
        json.put("code", 200);
        json.put("data", topicInfo);
        return json;
    }

    @GetMapping("/getTopicInfoList")
    public JSONObject getTopicInfoList() {
        List<CamelliaDelayQueueTopicInfo> topicInfoList = server.getTopicInfoList();
        JSONObject json = new JSONObject();
        json.put("code", 200);
        json.put("data", topicInfoList);
        return json;
    }
}

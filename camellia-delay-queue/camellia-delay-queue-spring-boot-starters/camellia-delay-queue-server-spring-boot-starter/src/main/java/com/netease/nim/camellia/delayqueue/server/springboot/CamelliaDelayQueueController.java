package com.netease.nim.camellia.delayqueue.server.springboot;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.delayqueue.common.domain.*;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueMonitor;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueMonitorData;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServer;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueTopicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;


/**
 * Created by caojiajun on 2022/7/20
 */
@RestController
@RequestMapping("/camellia/delayQueue")
public class CamelliaDelayQueueController {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDelayQueueController.class);

    @Autowired
    private CamelliaDelayQueueServer server;

    @Autowired
    private CamelliaDelayQueueLongPollingTaskExecutor executor;

    @PostMapping("/longPollingMsg")
    public void longPollingMsg(@RequestParam("topic") String topic,
                               @RequestParam(value = "ackTimeoutMillis", required = false, defaultValue = "-1") long ackTimeoutMillis,
                               @RequestParam(value = "batch", required = false, defaultValue = "-1") int batch,
                               @RequestParam(value = "longPollingTimeoutMillis", required = false, defaultValue = "-1") long longPollingTimeoutMillis,
                               HttpServletRequest httpServletRequest,
                               HttpServletResponse httpServletResponse) {
        CamelliaDelayMsgPullRequest request = new CamelliaDelayMsgPullRequest();
        request.setTopic(topic);
        request.setAckTimeoutMillis(ackTimeoutMillis);
        request.setBatch(batch);
        httpServletResponse.setCharacterEncoding("utf-8");
        //先直接取，如果取得到，直接返回，不需要异步hold连接
        try {
            CamelliaDelayMsgPullResponse response = server.pullMsg(request);
            if (response.getCode() != 200) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.getWriter().println(JSONObject.toJSONString(response));
                return;
            }
            if (response.getDelayMsgList() != null && !response.getDelayMsgList().isEmpty()) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.getWriter().println(JSONObject.toJSONString(response));
                return;
            }
        } catch (Exception e) {
            logger.error("longPollingMsg error, topic = {}", topic, e);
        }
        //开启异步长轮询
        AsyncContext asyncContext = httpServletRequest.startAsync();
        try {
            asyncContext.setTimeout(0);
            CamelliaDelayQueueLongPollingTask task = new CamelliaDelayQueueLongPollingTask(request, longPollingTimeoutMillis, asyncContext);
            executor.submit(task);
        } catch (Exception e) {
            logger.error("longPollingMsg error, topic = {}", topic, e);
            asyncContext.complete();
        }
    }

    @PostMapping("/sendMsg")
    public CamelliaDelayMsgSendResponse sendMsg(@RequestParam("topic") String topic,
                                                @RequestParam(value = "msgId", required = false) String msgId,
                                                @RequestParam("msg") String msg,
                                                @RequestParam("delayMillis") long delayMillis,
                                                @RequestParam(value = "ttlMillis", required = false, defaultValue = "-1") long ttlMillis,
                                                @RequestParam(value = "maxRetry", required = false, defaultValue = "-1") int maxRetry) {
        CamelliaDelayMsgSendRequest request = new CamelliaDelayMsgSendRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        request.setMsg(msg);
        request.setDelayMillis(delayMillis);
        request.setTtlMillis(ttlMillis);
        request.setMaxRetry(maxRetry);
        return server.sendMsg(request);
    }

    @PostMapping("/deleteMsg")
    public CamelliaDelayMsgDeleteResponse deleteMsg(@RequestParam("topic") String topic,
                                                    @RequestParam("msgId") String msgId,
                                                    @RequestParam(value = "release", required = false, defaultValue = "false") boolean release) {
        CamelliaDelayMsgDeleteRequest request = new CamelliaDelayMsgDeleteRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        request.setRelease(release);
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

    //暴露给哨兵系统的接口，按照哨兵的格式组装
    @GetMapping(value = "/getMonitorString", produces = "text/plain;charset=UTF-8")
    public String getMonitorString() {
        JSONObject monitorJson = new JSONObject();

        //monitorData
        CamelliaDelayQueueMonitorData monitorData = CamelliaDelayQueueMonitor.getMonitorData();

        JSONArray requestStatsJsonArray = new JSONArray();
        requestStatsJsonArray.addAll(monitorData.getRequestStatsList());
        monitorJson.put("requestStats", requestStatsJsonArray);

        JSONArray pullMsgTimeGapStatsJsonArray = new JSONArray();
        pullMsgTimeGapStatsJsonArray.addAll(monitorData.getPullMsgTimeGapStatsList());
        monitorJson.put("pullMsgTimeGapStats", pullMsgTimeGapStatsJsonArray);

        JSONArray readyQueueTimeGapJsonArray = new JSONArray();
        readyQueueTimeGapJsonArray.addAll(monitorData.getReadyQueueTimeGapStatsList());
        monitorJson.put("readyQueueTimeGapStats", readyQueueTimeGapJsonArray);

        //topicInfoList
        List<CamelliaDelayQueueTopicInfo> topicInfoList = server.getTopicInfoList();
        JSONArray topicInfoJsonArray = new JSONArray();
        for (CamelliaDelayQueueTopicInfo topicInfo : topicInfoList) {
            JSONObject json = new JSONObject();
            json.put("topic", topicInfo.getTopic());
            json.put("waitingQueueSize", topicInfo.getWaitingQueueSize());
            json.put("ackQueueSize", topicInfo.getAckQueueSize());
            json.put("readyQueueSize", topicInfo.getReadyQueueSize());
            topicInfoJsonArray.add(json);
        }
        monitorJson.put("topicInfoStats", topicInfoJsonArray);

        JSONArray waitingQueueStatsJsonArray = new JSONArray();
        for (CamelliaDelayQueueTopicInfo topicInfo : topicInfoList) {
            CamelliaDelayQueueTopicInfo.WaitingQueueInfo waitingQueueInfo = topicInfo.getWaitingQueueInfo();
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(waitingQueueInfo));
            json.put("topic", topicInfo.getTopic());
            waitingQueueStatsJsonArray.add(json);
        }
        monitorJson.put("waitingQueueStats", waitingQueueStatsJsonArray);

        return monitorJson.toJSONString();
    }
}

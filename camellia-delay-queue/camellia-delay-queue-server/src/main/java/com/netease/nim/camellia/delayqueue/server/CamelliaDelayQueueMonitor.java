package com.netease.nim.camellia.delayqueue.server;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.delayqueue.common.domain.*;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayMsgErrorCode;
import com.netease.nim.camellia.tools.statistic.CamelliaStatistics;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Created by caojiajun on 2022/7/21
 */
public class CamelliaDelayQueueMonitor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDelayQueueMonitor.class);
    private static final Logger statsLogger = LoggerFactory.getLogger("camellia-delay-queue-stats");

    //统计消息量
    private static ConcurrentHashMap<String, CamelliaDelayMsgCounter> msgCountMap = new ConcurrentHashMap<>();
    //统计下行消息延迟，表示消息到达触发时间，到消息实际被consumer消费的时间间隔，如果很大，说明消费者消费有瓶颈
    private static ConcurrentHashMap<String, CamelliaStatistics> pullMsgTimeGapMap = new ConcurrentHashMap<>();
    //统计ready队列消息延迟，表示消息到达触发时间，到delay-queue轮询线程把消息移动到ready队列的时间间隔，如果很大，说明服务器处理有瓶颈
    private static ConcurrentHashMap<String, CamelliaStatistics> readyQueueTimeGapMap = new ConcurrentHashMap<>();

    private static CamelliaDelayQueueMonitorData monitorData = new CamelliaDelayQueueMonitorData();
    static {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(CamelliaDelayQueueMonitor::calcMonitorData, 60, 60, TimeUnit.SECONDS);
    }

    public static CamelliaDelayQueueMonitorData getMonitorData() {
        return monitorData;
    }

    public static void sendMsg(CamelliaDelayMsgSendRequest request, CamelliaDelayMsgSendResponse response) {
        log("sendMsg", request.getTopic(), request, response);
        if (response.getCode() == CamelliaDelayMsgErrorCode.SUCCESS.getValue()) {
            CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, request.getTopic(), k -> new CamelliaDelayMsgCounter());
            counter.sendMsg(1);
        }
    }

    public static void pullMsg(CamelliaDelayMsgPullRequest request, CamelliaDelayMsgPullResponse response) {
        List<CamelliaDelayMsg> delayMsgList = response.getDelayMsgList();
        if (delayMsgList != null && !delayMsgList.isEmpty()) {
            log("pullMsg", request.getTopic(), request, response);
            for (CamelliaDelayMsg delayMsg : delayMsgList) {
                CamelliaStatistics statistics = CamelliaMapUtils.computeIfAbsent(pullMsgTimeGapMap, delayMsg.getTopic(), k -> new CamelliaStatistics());
                statistics.update(Math.abs(System.currentTimeMillis() - delayMsg.getTriggerTime()));
            }
            CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, request.getTopic(), k -> new CamelliaDelayMsgCounter());
            counter.pullMsg(delayMsgList.size());
        }
    }

    public static void deleteMsg(CamelliaDelayMsgDeleteRequest request, CamelliaDelayMsgDeleteResponse response) {
        log("deleteMsg", request.getTopic(), request, response);
        if (response.getCode() == CamelliaDelayMsgErrorCode.SUCCESS.getValue()) {
            CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, request.getTopic(), k -> new CamelliaDelayMsgCounter());
            counter.deleteMsg(1);
        }
    }

    public static void getMsg(CamelliaDelayMsgGetRequest request, CamelliaDelayMsgGetResponse response) {
        log("getMsg", request.getTopic(), request, response);
        if (response.getCode() == CamelliaDelayMsgErrorCode.SUCCESS.getValue()) {
            CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, request.getTopic(), k -> new CamelliaDelayMsgCounter());
            counter.getMsg(1);
        }
    }

    public static void ackMsg(CamelliaDelayMsgAckRequest request, CamelliaDelayMsgAckResponse response) {
        log("ackMsg", request.getTopic(), request, response);
        if (response.getCode() == CamelliaDelayMsgErrorCode.SUCCESS.getValue()) {
            CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, request.getTopic(), k -> new CamelliaDelayMsgCounter());
            counter.ackMsg(1);
        }
    }

    public static void triggerMsgReady(String topic, Map<String, CamelliaDelayMsg> msgMap) {
        log("triggerMsgReady", topic, msgMap, null);
        CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, topic, k -> new CamelliaDelayMsgCounter());
        counter.triggerMsgReady(msgCountMap.size());

        CamelliaStatistics statistics = CamelliaMapUtils.computeIfAbsent(readyQueueTimeGapMap, topic, k -> new CamelliaStatistics());
        for (CamelliaDelayMsg delayMsg : msgMap.values()) {
            if (delayMsg != null) {
                statistics.update(Math.abs(System.currentTimeMillis() - delayMsg.getTriggerTime()));
            }
        }
    }

    public static void triggerMsgEndLife(String topic, Map<String, CamelliaDelayMsg> msgMap) {
        log("triggerMsgEndLife", topic, msgMap, null);
        CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, topic, k -> new CamelliaDelayMsgCounter());
        counter.triggerMsgEndLife(msgMap.size());
    }

    public static void triggerMsgTimeout(String topic, Map<String, CamelliaDelayMsg> msgMap) {
        log("triggerMsgTimeout", topic, msgMap, null);
        CamelliaDelayMsgCounter counter = CamelliaMapUtils.computeIfAbsent(msgCountMap, topic, k -> new CamelliaDelayMsgCounter());
        counter.triggerMsgTimeout(msgMap.size());
    }

    public static void checkTopicInactive(String topic) {
        log("checkTopicInactive", topic, null, null);
    }

    public static void checkTopicRemove(String topic) {
        log("checkTopicRemove", topic, null, null);
    }

    private static void log(String event, String topic, Object request, Object response) {
        if (statsLogger.isInfoEnabled()) {
            JSONObject log = new JSONObject(true);
            log.put("event", event);
            log.put("topic", topic);
            log.put("request", request);
            log.put("response", response);
            statsLogger.info(log.toJSONString());
        }
    }

    private static void calcMonitorData() {
        try {
            CamelliaDelayQueueMonitorData monitorData = new CamelliaDelayQueueMonitorData();
            //msgCount
            List<CamelliaDelayQueueMonitorData.RequestStats> requestStatsList = new ArrayList<>();
            ConcurrentHashMap<String, CamelliaDelayMsgCounter> msgCountMap = CamelliaDelayQueueMonitor.msgCountMap;
            CamelliaDelayQueueMonitor.msgCountMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, CamelliaDelayMsgCounter> entry : msgCountMap.entrySet()) {
                CamelliaDelayMsgCounter counter = entry.getValue();
                CamelliaDelayQueueMonitorData.RequestStats requestStats = new CamelliaDelayQueueMonitorData.RequestStats(entry.getKey(),
                        counter.getSendMsg(), counter.getPullMsg(), counter.getDeleteMsg(), counter.getAckMsg(),
                        counter.getGetMsg(), counter.getTriggerMsgReady(), counter.getTriggerMsgEndLife(), counter.getTriggerMsgTimeout());
                requestStatsList.add(requestStats);
            }
            monitorData.setRequestStatsList(requestStatsList);

            //pullMsgTimeGap
            List<CamelliaDelayQueueMonitorData.TimeGapStats> pullMsgTimeGapStatsList = new ArrayList<>();
            ConcurrentHashMap<String, CamelliaStatistics> pullMsgTimeGapMap = CamelliaDelayQueueMonitor.pullMsgTimeGapMap;
            CamelliaDelayQueueMonitor.pullMsgTimeGapMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, CamelliaStatistics> entry : pullMsgTimeGapMap.entrySet()) {
                CamelliaStatsData data = entry.getValue().getStatsDataAndReset();
                CamelliaDelayQueueMonitorData.TimeGapStats timeGapStats = new CamelliaDelayQueueMonitorData.TimeGapStats(entry.getKey(),
                        data.getCount(), data.getAvg(), data.getMax());
                pullMsgTimeGapStatsList.add(timeGapStats);
            }
            monitorData.setPullMsgTimeGapStatsList(pullMsgTimeGapStatsList);

            //readyQueueTimeGap
            List<CamelliaDelayQueueMonitorData.TimeGapStats> readyQueueTimeGapStatsList = new ArrayList<>();
            ConcurrentHashMap<String, CamelliaStatistics> readyQueueTimeGapMap = CamelliaDelayQueueMonitor.readyQueueTimeGapMap;
            CamelliaDelayQueueMonitor.readyQueueTimeGapMap = new ConcurrentHashMap<>();
            for (Map.Entry<String, CamelliaStatistics> entry : readyQueueTimeGapMap.entrySet()) {
                CamelliaStatsData data = entry.getValue().getStatsDataAndReset();
                CamelliaDelayQueueMonitorData.TimeGapStats timeGapStats = new CamelliaDelayQueueMonitorData.TimeGapStats(entry.getKey(),
                        data.getCount(), data.getAvg(), data.getMax());
                readyQueueTimeGapStatsList.add(timeGapStats);
            }
            monitorData.setReadyQueueTimeGapStatsList(readyQueueTimeGapStatsList);

            CamelliaDelayQueueMonitor.monitorData = monitorData;
        } catch (Exception e) {
            logger.error("calc monitor data error", e);
        }
    }
}

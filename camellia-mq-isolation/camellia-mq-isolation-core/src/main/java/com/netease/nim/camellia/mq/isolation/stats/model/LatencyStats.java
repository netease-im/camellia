package com.netease.nim.camellia.mq.isolation.stats.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/2/7
 */
public class LatencyStats {

    private List<MqInfoLatency> mqInfoLatencyList = new ArrayList<>();
    private List<TopicTypeLatencyStats> topicTypeLatencyStatsList = new ArrayList<>();
    private List<NamespaceMsgLatencyStats> namespaceMsgLatencyStatsList = new ArrayList<>();
    private List<NamespaceBizIdMsgLatencyStats> namespaceBizIdMsgLatencyStatsList = new ArrayList<>();

    public List<MqInfoLatency> getMqInfoLatencyList() {
        return mqInfoLatencyList;
    }

    public void setMqInfoLatencyList(List<MqInfoLatency> mqInfoLatencyList) {
        this.mqInfoLatencyList = mqInfoLatencyList;
    }

    public List<TopicTypeLatencyStats> getTopicTypeLatencyStatsList() {
        return topicTypeLatencyStatsList;
    }

    public void setTopicTypeLatencyStatsList(List<TopicTypeLatencyStats> topicTypeLatencyStatsList) {
        this.topicTypeLatencyStatsList = topicTypeLatencyStatsList;
    }

    public List<NamespaceMsgLatencyStats> getNamespaceMsgLatencyStatsList() {
        return namespaceMsgLatencyStatsList;
    }

    public void setNamespaceMsgLatencyStatsList(List<NamespaceMsgLatencyStats> namespaceMsgLatencyStatsList) {
        this.namespaceMsgLatencyStatsList = namespaceMsgLatencyStatsList;
    }

    public List<NamespaceBizIdMsgLatencyStats> getNamespaceBizIdMsgLatencyStatsList() {
        return namespaceBizIdMsgLatencyStatsList;
    }

    public void setNamespaceBizIdMsgLatencyStatsList(List<NamespaceBizIdMsgLatencyStats> namespaceBizIdMsgLatencyStatsList) {
        this.namespaceBizIdMsgLatencyStatsList = namespaceBizIdMsgLatencyStatsList;
    }
}

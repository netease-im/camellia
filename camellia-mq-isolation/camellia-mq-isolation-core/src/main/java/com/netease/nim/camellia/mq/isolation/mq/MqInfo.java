package com.netease.nim.camellia.mq.isolation.mq;

import com.alibaba.fastjson.JSONObject;

import java.util.Objects;

/**
 * Created by caojiajun on 2024/2/4
 */
public class MqInfo {
    private String mq;
    private String topic;

    public MqInfo(String mq, String topic) {
        this.mq = mq;
        this.topic = topic;
    }

    public MqInfo() {
    }

    public String getMq() {
        return mq;
    }

    public void setMq(String mq) {
        this.mq = mq;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqInfo mqInfo = (MqInfo) o;
        return Objects.equals(mq, mqInfo.mq) && Objects.equals(topic, mqInfo.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mq, topic);
    }

    private String _string = null;

    @Override
    public String toString() {
        if (_string != null) {
            return _string;
        }
        JSONObject json = new JSONObject(true);
        json.put("mq", mq);
        json.put("topic", topic);
        _string = json.toJSONString();
        return _string;
    }

    public static MqInfo byString(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        String mq = jsonObject.getString("mq");
        String topic = jsonObject.getString("topic");
        return new MqInfo(mq, topic);
    }
}

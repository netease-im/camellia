package com.netease.nim.camellia.redis.proxy.mq.kafka;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例：
 * 127.0.0.1:9092,127.0.0.1:9093/camellia_multi_write_kafka
 * 表示：
 * addrs=127.0.0.1:9092,127.0.0.1:9093
 * topic=camellia_multi_write_kafka
 */
public class KafkaUrl {

    private String addrs;
    private String topic;

    public KafkaUrl(String addrs, String topic) {
        this.addrs = addrs;
        this.topic = topic;
    }

    public static KafkaUrl fromUrl(String url) {
        if (url == null || url.length() == 0) {
            throw new IllegalStateException("url is empty");
        }
        int index = url.indexOf("/");
        String addrs = url.substring(0, index);
        String topic = url.substring(index + 1);
        return new KafkaUrl(addrs, topic);
    }

    public static List<KafkaUrl> fromUrls(String urls) {
        if (urls == null || urls.length() == 0) {
            throw new IllegalStateException("urls is empty");
        }
        String[] split = urls.split("\\|");
        List<KafkaUrl> list = new ArrayList<>();
        for (String url : split) {
            list.add(fromUrl(url));
        }
        return list;
    }

    @Override
    public String toString() {
        return addrs + "/" + topic;
    }

    public String getAddrs() {
        return addrs;
    }

    public void setAddrs(String addrs) {
        this.addrs = addrs;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}

package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.netease.nim.camellia.redis.proxy.util.LockMap;

import java.util.concurrent.ConcurrentHashMap;

public class KafkaProducerHub {

    private static final ConcurrentHashMap<String, KafkaProducerWrapper> map = new ConcurrentHashMap<>();
    private static final LockMap lockMap = new LockMap();

    public static KafkaProducerWrapper getKafkaProducer(String kafkaUrl) {
        KafkaProducerWrapper producer = map.get(kafkaUrl);
        if (producer == null || !producer.isValid()) {
            synchronized (lockMap.getLockObj(kafkaUrl)) {
                producer = map.get(kafkaUrl);
                if (producer == null || !producer.isValid()) {
                    if (producer != null) {
                        producer.close();
                    }
                    producer = new KafkaProducerWrapper(kafkaUrl);
                    producer.start();
                    if (producer.isValid()) {
                        map.put(kafkaUrl, producer);
                    } else {
                        //重试一次
                        producer.close();
                        producer = new KafkaProducerWrapper(kafkaUrl);
                        producer.start();
                        if (producer.isValid()) {
                            map.put(kafkaUrl, producer);
                        } else {
                            producer.close();
                        }
                    }
                }
            }
        }
        return producer;
    }
}

package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.netease.nim.camellia.core.util.LockMap;

import java.util.concurrent.ConcurrentHashMap;

public class KafkaProducerHub {

    private static final ConcurrentHashMap<String, KafkaProducerWrapper> map = new ConcurrentHashMap<>();
    private static final LockMap lockMap = new LockMap();

    public static KafkaProducerWrapper getKafkaProducer(String url) {
        KafkaProducerWrapper producer = map.get(url);
        if (producer == null || !producer.isValid()) {
            synchronized (lockMap.getLockObj(url)) {
                producer = map.get(url);
                if (producer == null || !producer.isValid()) {
                    if (producer != null) {
                        producer.close();
                    }
                    producer = new KafkaProducerWrapper(url);
                    producer.start();
                    if (producer.isValid()) {
                        map.put(url, producer);
                    } else {
                        //重试一次
                        producer.close();
                        producer = new KafkaProducerWrapper(url);
                        producer.start();
                        if (producer.isValid()) {
                            map.put(url, producer);
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

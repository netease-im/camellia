package com.netease.nim.camellia.mq.isolation.kafka;

import com.netease.nim.camellia.mq.isolation.core.AbstractConsumerManager;
import com.netease.nim.camellia.mq.isolation.core.config.ConsumerManagerConfig;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/2/7
 */
public class KafkaConsumerManager extends AbstractConsumerManager {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerManager.class);
    private static final AtomicLong idGen = new AtomicLong();

    private final Properties properties;

    public KafkaConsumerManager(ConsumerManagerConfig config, Properties properties) {
        super(config);
        this.properties = properties;
    }

    @Override
    protected void start0(MqInfo mqInfo) {
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(initKafkaConf(mqInfo.getMq()));
        consumer.subscribe(Collections.singleton(mqInfo.getTopic()));
        new Thread(() -> {
            while (true) {
                try {
                    ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
                    if (records == null || records.isEmpty()) continue;
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        byte[] data = record.value();
                        KafkaConsumerManager.this.onMsg(mqInfo, data);
                    }
                    consumer.commitSync();
                } catch (Throwable e) {
                    logger.error("kafka consume error, mqInfo = {}", mqInfo, e);
                }
            }
        }, "camellia-mq-isolation-consumer-[" + mqInfo.getMq() + "][" + mqInfo.getTopic() + "][" + idGen.incrementAndGet() + "]").start();
        logger.info("kafka consumer start success, mqInfo = {}", mqInfo);
    }

    private Properties initKafkaConf(String mq) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", mq);
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("enable.auto.commit", "false");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("max.poll.records", "100");
        properties.put("session.timeout.ms", "25000");
        properties.put("group.id", "camellia");
        properties.putAll(this.properties);
        return properties;
    }
}

package com.netease.nim.camellia.mq.isolation.kafka;

import com.netease.nim.camellia.mq.isolation.core.CamelliaMqIsolationMsgDispatcher;
import com.netease.nim.camellia.mq.isolation.core.mq.Consumer;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/2/19
 */
public class KafkaConsumer extends Consumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private static final AtomicLong idGen = new AtomicLong();

    private final Properties properties;
    private final MqInfo mqInfo;

    private org.apache.kafka.clients.consumer.KafkaConsumer<byte[], byte[]> consumer;
    private final AtomicBoolean start = new AtomicBoolean();
    private final CountDownLatch latch = new CountDownLatch(1);

    public KafkaConsumer(MqInfo mqInfo, CamelliaMqIsolationMsgDispatcher dispatcher, Properties properties) {
        super(mqInfo, dispatcher);
        this.mqInfo = mqInfo;
        this.properties = properties;
    }

    @Override
    public synchronized void start() {
        if (start.get()) {
            throw new IllegalStateException("duplicate start");
        }
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(initKafkaConf());
        this.consumer.subscribe(Collections.singleton(mqInfo.getTopic()));
        this.start.set(true);
        new Thread(() -> {
            while (start.get()) {
                try {
                    ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
                    if (records == null || records.isEmpty()) continue;
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        byte[] data = record.value();
                        KafkaConsumer.this.onMsg(data);//因为异步，所以可能丢失，如果有更强的需求，需要自行实现
                    }
                    consumer.commitSync();
                } catch (Throwable e) {
                    logger.error("kafka consume error, mqInfo = {}", mqInfo, e);
                }
            }
            latch.countDown();
        }, "camellia-mq-isolation-kafka-consumer-[" + mqInfo.getMq() + "][" + mqInfo.getTopic() + "][" + idGen.incrementAndGet() + "]").start();
        logger.info("kafka consumer start success, mqInfo = {}", mqInfo);
    }

    @Override
    public synchronized void stop() {
        this.start.set(false);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.consumer.close();
        logger.info("kafka consumer stop success, mqInfo = {}", mqInfo);
    }

    private Properties initKafkaConf() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", mqInfo.getMq());
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

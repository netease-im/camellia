package com.netease.nim.camellia.mq.isolation.kafka;

import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.mq.MqSender;
import com.netease.nim.camellia.mq.isolation.core.mq.SenderResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/2/7
 */
public class KafkaSender implements MqSender {

    private static final Logger logger = LoggerFactory.getLogger(KafkaSender.class);

    private final ConcurrentHashMap<String, KafkaProducer<byte[], byte[]>> map = new ConcurrentHashMap<>();
    private final Properties properties;

    public KafkaSender(Properties properties) {
        this.properties = properties;
    }

    @Override
    public SenderResult send(MqInfo mqInfo, byte[] data) {
        KafkaProducer<byte[], byte[]> producer = map.get(mqInfo.getMq());
        if (producer == null) {
            producer = map.computeIfAbsent(mqInfo.getMq(), mq -> new KafkaProducer<>(initKafkaConf(mq)));
        }
        SenderResult result = new SenderResult();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        result.setResult(future);
        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(mqInfo.getTopic(), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8), data);
        producer.send(record, (recordMetadata, e) -> {
            if (e != null) {
                future.complete(false);
                logger.error("send to kafka error, mqInfo = {}, data.len = {}", mqInfo, data.length, e);
            } else {
                future.complete(true);
                if (logger.isDebugEnabled()) {
                    logger.debug("send to kafka success, mqInfo = {}, data.len = {}", mqInfo, data.length);
                }
            }
        });
        return result;
    }

    private Properties initKafkaConf(String mq) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", mq);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        properties.put("request.required.acks", "1");
        properties.put("producer.type", "async");
        properties.put("queue.buffering.max.ms", "5000");
        properties.put("queue.buffering.max.messages", "10000");
        properties.put("batch.num.messages", "200");
        properties.putAll(this.properties);
        return properties;
    }
}

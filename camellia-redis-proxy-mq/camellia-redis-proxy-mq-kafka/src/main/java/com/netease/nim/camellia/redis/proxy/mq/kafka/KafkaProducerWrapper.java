package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPack;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class KafkaProducerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerWrapper.class);

    private final List<KafkaProducer<byte[], byte[]>> list = new ArrayList<>();
    private final String kafkaUrl;
    private boolean valid;

    public KafkaProducerWrapper(String kafkaUrl) {
        this.kafkaUrl = kafkaUrl;
    }

    public void start() {
        try {
            int num = ProxyDynamicConf.getInt("mq.multi.write.kafka.producer.num", 5);
            Properties properties = initKafkaConf(kafkaUrl);
            for (int i=0; i<num; i++) {
                KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(properties);
                list.add(producer);
            }
            valid = true;
            logger.info("kafka producer start success, kafkaUrl = {}, num = {}, props = {}", kafkaUrl, num, properties);
        } catch (Exception e) {
            logger.error("kafka producer start error, kafkaUrl = {}", kafkaUrl, e);
            valid = false;
        }
    }

    public void send(ProducerRecord<byte[], byte[]> record, MqPack mqPack) {
        if (!isValid()) return;
        byte[] key = record.key();
        KafkaProducer<byte[], byte[]> producer = list.get(Math.abs(Arrays.hashCode(key)) % list.size());
        producer.send(record, (recordMetadata, e) -> {
            if (e != null) {
                valid = false;
                String errMsg = String.format("send to kafka error, kafkaUrl = %s, topic = %s, data.len = %s, " +
                                "bid = %s, bgroup = %s, command = %s, key = %s, ex = %s",
                        kafkaUrl, record.topic(), record.value().length, mqPack.getBid(), mqPack.getBgroup(),
                        mqPack.getCommand().getName(), mqPack.getCommand().getKeysStr(), e);
                ErrorLogCollector.collect(KafkaMqPackSender.class, errMsg, e);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("send to kafka success, kafkaUrl = {}, topic = {}, data.len = {}, bid = {}, bgroup = {}, command = {}, key = {}",
                            kafkaUrl, record.topic(), record.value().length, mqPack.getBid(), mqPack.getBgroup(),
                            mqPack.getCommand().getName(), mqPack.getCommand().getKeysStr());
                }
            }
        });
    }

    public boolean isValid() {
        return valid;
    }

    public void close() {
        try {
            for (KafkaProducer<byte[], byte[]> producer : list) {
                producer.close();
            }
        } catch (Exception e) {
            logger.error("close error", e);
        }
    }

    private Properties initKafkaConf(String kafkaUrl) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaUrl);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        properties.put("partitioner.class", "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        properties.put("request.required.acks", "1");
        properties.put("producer.type", "async");
        properties.put("queue.buffering.max.ms", "5000");
        properties.put("queue.buffering.max.messages", "10000");
        properties.put("batch.num.messages", "200");
        try {
            String props = ProxyDynamicConf.getString("mq.multi.write.kafka.conf.props", null);
            if (props != null) {
                JSONObject json = JSONObject.parseObject(props);
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    properties.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        } catch (Exception ignore) {
        }
        return properties;
    }
}

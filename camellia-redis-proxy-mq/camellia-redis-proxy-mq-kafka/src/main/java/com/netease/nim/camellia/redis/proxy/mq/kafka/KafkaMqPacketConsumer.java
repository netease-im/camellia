package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPack;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPackFlusher;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPackSerializer;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaMqPacketConsumer implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMqPacketConsumer.class);

    private static final AtomicLong id = new AtomicLong();

    private final ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();

    public KafkaMqPacketConsumer() {
        ProxyDynamicConf.registerCallback(cache::clear);
        start();
    }

    private void start() {
        String kafkaUrl = ProxyDynamicConf.getString("mq.multi.write.kafka.url", null);
        String topic = ProxyDynamicConf.getString("mq.multi.write.kafka.topic", "camellia_multi_write_kafka");
        if (kafkaUrl == null) {
            throw new IllegalStateException("mq.multi.write.kafka.url is null");
        }
        Properties properties = initKafkaConf(kafkaUrl);

        int num = ProxyDynamicConf.getInt("mq.multi.write.kafka.consumer.num", 1);
        for (int i=0; i<num; i++) {
            KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(properties);
            consumer.subscribe(Collections.singleton(topic));
            Thread thread = new Thread(() -> {
                logger.info(Thread.currentThread().getName() + " start");
                while (true) {
                    try {
                        ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
                        if (records == null || records.isEmpty()) continue;
                        for (ConsumerRecord<byte[], byte[]> record : records) {
                            try {
                                byte[] data = record.value();
                                MqPack mqPack = MqPackSerializer.deserialize(data);
                                List<String> redisUrls = redisUrls(mqPack.getBid(), mqPack.getBgroup());
                                if (!redisUrls.isEmpty()) {
                                    for (String redisUrl : redisUrls) {
                                        MqPackFlusher.flush(mqPack.getCommand(), redisUrl);
                                    }
                                } else {
                                    ErrorLogCollector.collect(KafkaMqPacketConsumer.class, "mq pack flush redis urls is empty");
                                }
                            } catch (Exception e) {
                                ErrorLogCollector.collect(KafkaMqPacketConsumer.class, "mq pack flush error", e);
                            }
                        }
                        consumer.commitAsync();
                    } catch (Exception e) {
                        ErrorLogCollector.collect(KafkaMqPacketConsumer.class, "kafka consume error", e);
                    }
                }
            }, "mq-multi-write-kafka-consumer-" + topic + "-" + id.incrementAndGet());
            thread.setDaemon(true);
            thread.start();
        }
        logger.info("kafka consumer start, kafkaUrl = {}, topic = {}, num = {}, props = {}", kafkaUrl, topic, num, properties);
    }

    private List<String> redisUrls(Long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        List<String> urls = cache.get(key);
        if (urls != null) {
            return urls;
        }
        String kafkaUrls = ProxyDynamicConf.getString("mq.multi.write.redis.urls", bid, bgroup, null);
        if (kafkaUrls == null) {
            urls = new ArrayList<>();
            cache.put(key, urls);
            return urls;
        }
        String[] split = kafkaUrls.split("\\|");
        urls = new ArrayList<>(Arrays.asList(split));
        cache.put(key, urls);
        return urls;
    }

    private Properties initKafkaConf(String kafkaUrl) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaUrl);
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("enable.auto.commit", "false");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("max.poll.records", "100");
        properties.put("session.timeout.ms", "25000");
        properties.put("group.id", "camellia");
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

    @Override
    public CommandInterceptResponse check(Command command) {
        return CommandInterceptResponse.SUCCESS;
    }
}

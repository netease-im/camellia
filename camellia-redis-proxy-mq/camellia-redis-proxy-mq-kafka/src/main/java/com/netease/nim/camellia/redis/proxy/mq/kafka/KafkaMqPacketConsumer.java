package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.command.async.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.RedisMonitor;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPack;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPackSerializer;
import com.netease.nim.camellia.redis.proxy.netty.ReplyEncoder;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaMqPacketConsumer implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMqPacketConsumer.class);

    private static final AtomicLong id = new AtomicLong();

    public KafkaMqPacketConsumer() {
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
                                AsyncCamelliaRedisTemplate template = ProxyInfoUtils.getAsyncCamelliaRedisTemplateChooser().choose(mqPack.getBid(), mqPack.getBgroup());
                                List<CompletableFuture<Reply>> futures = template.sendCommand(Collections.singletonList(mqPack.getCommand()));
                                for (CompletableFuture<Reply> future : futures) {
                                    future.thenAccept(reply -> {
                                        if (reply instanceof ErrorReply) {
                                            RedisMonitor.incrFail(((ErrorReply) reply).getError());
                                            ErrorLogCollector.collect(ReplyEncoder.class, "mq multi write error, msg = " + ((ErrorReply) reply).getError());
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                ErrorLogCollector.collect(KafkaMqPacketConsumer.class, "mq pack send commands error", e);
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

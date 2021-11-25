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

public class KafkaMqPackConsumer implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMqPackConsumer.class);

    private static final AtomicLong id = new AtomicLong();

    public KafkaMqPackConsumer() {
        start();
    }

    private void start() {
        String kafkaUrls = ProxyDynamicConf.getString(KafkaMqPackConstants.CONF_KEY_CONSUMER_KAFKA_URLS, null);
        List<KafkaUrl> list = KafkaUrl.fromUrls(kafkaUrls);
        if (list.isEmpty()) {
            throw new IllegalStateException(KafkaMqPackConstants.CONF_KEY_CONSUMER_KAFKA_URLS + " is null");
        }
        for (KafkaUrl kafkaUrl : list) {
            Properties properties = initKafkaConf(kafkaUrl.getAddrs());
            int num = ProxyDynamicConf.getInt("mq.multi.write.kafka.consumer.num", 1);
            String topic = kafkaUrl.getTopic();
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
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("receive command from kafka, bid = {}, bgroup = {}, command = {}, keys = {}",
                                                mqPack.getBid(), mqPack.getBgroup(), mqPack.getCommand().getName(), mqPack.getCommand().getKeysStr());
                                    }
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
                                    ErrorLogCollector.collect(KafkaMqPackConsumer.class, "mq pack send commands error", e);
                                }
                            }
                            consumer.commitAsync();
                        } catch (Exception e) {
                            ErrorLogCollector.collect(KafkaMqPackConsumer.class, "kafka consume error", e);
                        }
                    }
                }, "mq-multi-write-kafka-consumer-" + kafkaUrl + "-" + id.incrementAndGet());
                thread.setDaemon(true);
                thread.start();
            }
            logger.info("kafka consumer start, kafka = {}, topic = {}, num = {}, props = {}", kafkaUrl.getAddrs(), topic, num, properties);
        }
    }

    private Properties initKafkaConf(String url) {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", url);
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        properties.put("enable.auto.commit", "false");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("max.poll.records", "100");
        properties.put("session.timeout.ms", "25000");
        properties.put("group.id", "camellia");
        try {
            String props = ProxyDynamicConf.getString(KafkaMqPackConstants.CONF_KEY_KAFKA_CONF_PROPS, null);
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

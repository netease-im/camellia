package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPack;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPackSender;
import com.netease.nim.camellia.redis.proxy.mq.common.MqPackSerializer;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaMqPackSender implements MqPackSender {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMqPackSender.class);

    private static final AtomicLong id = new AtomicLong();
    private final ConcurrentHashMap<String, List<String>> cache = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<MqPack> queue;

    public KafkaMqPackSender() {
        ProxyDynamicConf.registerCallback(cache::clear);
        queue = new LinkedBlockingQueue<>(ProxyDynamicConf.getInt("mq.multi.write.kafka.queue.size", 100000));
        start();
    }

    @Override
    public boolean send(MqPack pack) throws IOException {
        if (ProxyDynamicConf.getBoolean("mq.multi.write.kafka.async.enable", true)) {
            boolean offer = queue.offer(pack);
            if (!offer) {
                ErrorLogCollector.collect(KafkaMqPackSender.class, "queue full");
            }
            return offer;
        } else {
            _doSend(pack);
            return true;
        }
    }
    private void start() {
        Thread thread = new Thread(() -> {
            logger.info(Thread.currentThread().getName() + " start");
            while (true) {
                try {
                    MqPack mqPack = queue.poll(3, TimeUnit.SECONDS);
                    if (mqPack != null) {
                        _doSend(mqPack);
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(KafkaMqPackSender.class, "_doSend error", e);
                }
            }
        }, "mq-multi-write-kafka-sender-" + id.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
    }

    private void _doSend(MqPack pack) throws IOException {
        Long bid = pack.getBid();
        String bgroup = pack.getBgroup();

        List<byte[]> keys = pack.getCommand().getKeys();
        byte[] kafkaHashKey;
        if (!keys.isEmpty()) {
            kafkaHashKey = keys.get(0);
        } else {
            kafkaHashKey = Utils.stringToBytes(UUID.randomUUID().toString());
        }
        byte[] data = MqPackSerializer.serialize(pack);
        String topic = kafkaTopic(bid, bgroup);

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, kafkaHashKey, data);
        List<String> kafkaUrls = kafkaUrls(bid, bgroup);
        for (String kafkaUrl : kafkaUrls) {
            KafkaProducerWrapper producer = KafkaProducerHub.getKafkaProducer(kafkaUrl);
            if (producer == null || !producer.isValid()) continue;
            producer.send(record, pack);
        }
    }

    private String kafkaTopic(Long bid, String bgroup) {
        return ProxyDynamicConf.getString("mq.multi.write.kafka.topic", bid, bgroup, "camellia_multi_write_kafka");
    }

    private List<String> kafkaUrls(Long bid, String bgroup) {
        String key = bid + "|" + bgroup;
        List<String> urls = cache.get(key);
        if (urls != null) {
            return urls;
        }
        String kafkaUrls = ProxyDynamicConf.getString("mq.multi.write.kafka.urls", bid, bgroup, null);
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
}

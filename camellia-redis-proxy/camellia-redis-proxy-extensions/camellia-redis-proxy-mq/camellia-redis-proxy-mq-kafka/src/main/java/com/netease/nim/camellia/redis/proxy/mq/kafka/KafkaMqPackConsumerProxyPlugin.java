package com.netease.nim.camellia.redis.proxy.mq.kafka;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.upstream.AsyncCamelliaRedisTemplate;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaMqPackConsumerProxyPlugin implements ProxyPlugin {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMqPackConsumerProxyPlugin.class);

    private static final AtomicLong id = new AtomicLong();

    @Override
    public void init(ProxyBeanFactory factory) {
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
                boolean syncEnable = ProxyDynamicConf.getBoolean("mq.multi.write.kafka.consumer.sync.enable", false);
                if (syncEnable) {
                    startSyncConsumer(kafkaUrl, consumer);
                } else {
                    startAsyncConsumer(kafkaUrl, consumer);
                }
            }
            logger.info("kafka consumer start, kafka = {}, topic = {}, num = {}, props = {}", kafkaUrl.getAddrs(), topic, num, properties);
        }
    }

    private void startAsyncConsumer(KafkaUrl kafkaUrl, KafkaConsumer<byte[], byte[]> consumer) {
        Thread thread = new Thread(() -> {
            logger.info(Thread.currentThread().getName() + " start");
            String key = null;
            Long bid = null;
            String bgroup = null;
            List<Command> buffer = new ArrayList<>();
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
                            String k = mqPack.getBid() + "|" + mqPack.getBgroup();
                            boolean needFlush = false;
                            if (key == null) {
                                key = k;
                                bid = mqPack.getBid();
                                bgroup = mqPack.getBgroup();
                                buffer.add(mqPack.getCommand());
                            } else {
                                if (k.equals(key)) {
                                    buffer.add(mqPack.getCommand());
                                    if (buffer.size() >= ProxyDynamicConf.getInt("mq.multi.write.commands.max.batch", 200)) {
                                        needFlush = true;
                                    }
                                } else {
                                    needFlush = true;
                                }
                            }
                            if (needFlush) {
                                try {
                                    flush(bid, bgroup, buffer);
                                } finally {
                                    bid = null;
                                    bgroup = null;
                                    buffer.clear();
                                }
                            }
                        } catch (Exception e) {
                            ErrorLogCollector.collect(KafkaMqPackConsumerProxyPlugin.class, "mq pack send commands error", e);
                        }
                    }
                    try {
                        flush(bid, bgroup, buffer);
                    } finally {
                        bid = null;
                        bgroup = null;
                        buffer.clear();
                    }
                    consumer.commitAsync();
                } catch (Exception e) {
                    ErrorLogCollector.collect(KafkaMqPackConsumerProxyPlugin.class, "kafka consume error", e);
                }
            }
        }, "mq-multi-write-kafka-consumer-" + kafkaUrl + "-" + id.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
    }

    private void flush(Long bid, String bgroup, List<Command> buffer) {
        if (buffer.isEmpty()) return;
        List<Command> commands = new ArrayList<>(buffer);
        AsyncCamelliaRedisTemplate template = GlobalRedisProxyEnv.getChooser().choose(bid, bgroup);
        List<CompletableFuture<Reply>> futures = template.sendCommand(commands);
        for (int i=0; i<futures.size(); i++) {
            Command command = commands.get(i);
            CompletableFuture<Reply> future = futures.get(i);
            future.thenAccept(reply -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("receive reply from redis, command = {}, reply = {}", command.getName(), reply.getClass().getName());
                }
                if (reply instanceof ErrorReply) {
                    CommandFailMonitor.incr(((ErrorReply) reply).getError());
                    ErrorLogCollector.collect(ReplyEncoder.class, "mq multi write error, msg = " + ((ErrorReply) reply).getError());
                }
            });
        }
    }

    private void startSyncConsumer(KafkaUrl kafkaUrl, KafkaConsumer<byte[], byte[]> consumer) {
        int queueSize = ProxyDynamicConf.getInt("mq.multi.write.kafka.consume.queue.size", 100);
        ArrayBlockingQueue<byte[]> bufferQueue = new ArrayBlockingQueue<>(queueSize);
        long threadId = id.incrementAndGet();
        Thread consumeThread = new Thread(() -> {
            while (true) {
                try {
                    byte[] data = bufferQueue.poll(1, TimeUnit.SECONDS);
                    if (data == null) {
                        continue;
                    }
                    MqPack mqPack = MqPackSerializer.deserialize(data);
                    if (logger.isDebugEnabled()) {
                        logger.debug("receive command from kafka, bid = {}, bgroup = {}, command = {}, keys = {}",
                                mqPack.getBid(), mqPack.getBgroup(), mqPack.getCommand().getName(), mqPack.getCommand().getKeysStr());
                    }
                    int retry = ProxyDynamicConf.getInt("mq.multi.write.kafka.consume.retry", 3);
                    int index = 1;
                    while (retry-- > 0) {
                        AsyncCamelliaRedisTemplate template = GlobalRedisProxyEnv.getChooser().choose(mqPack.getBid(), mqPack.getBgroup());
                        List<CompletableFuture<Reply>> futures = template.sendCommand(Collections.singletonList(mqPack.getCommand()));
                        boolean isRetry = false;
                        long timeoutSeconds = ProxyDynamicConf.getInt("mq.multi.write.kafka.consume.redis.timeout.seconds", 10);
                        Reply reply;
                        try {
                            reply = futures.get(0).get(timeoutSeconds, TimeUnit.SECONDS);
                            if (reply instanceof ErrorReply) {
                                if (((ErrorReply) reply).getError().equals(ErrorReply.NOT_AVAILABLE.getError())) {
                                    isRetry = true;
                                }
                            }
                        } catch (Exception e) {
                            ErrorLogCollector.collect(KafkaMqPackConsumerProxyPlugin.class, "mq multi write kafka consume send command error", e);
                            isRetry = true;
                        }
                        if (isRetry) {
                            TimeUnit.MILLISECONDS.sleep(1000L * index);
                            index ++;
                            continue;
                        }
                        break;
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(KafkaMqPackConsumerProxyPlugin.class, "mq multi write kafka consume error", e);
                }
            }
        });
        consumeThread.setName("mq-multi-write-kafka-consumer-" + kafkaUrl + "-" + threadId);
        consumeThread.setDaemon(true);
        consumeThread.start();
        Thread pollingThread = new Thread(() -> {
            logger.info(Thread.currentThread().getName() + " start");
            while (true) {
                try {
                    ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
                    if (records == null || records.isEmpty()) continue;
                    boolean pause = false;
                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        try {
                            byte[] data = record.value();
                            boolean offer = bufferQueue.offer(data);
                            if (!offer && !pause) {
                                try {
                                    consumer.commitSync();
                                } catch (Exception e) {
                                    logger.error(e.getMessage(), e);
                                }
                                consumer.pause(records.partitions());
                                pause = true;
                            }
                            if (!offer) {
                                do {
                                    TimeUnit.MILLISECONDS.sleep(1000L);
                                } while (!bufferQueue.offer(data));
                            }
                        } catch (Exception e) {
                            ErrorLogCollector.collect(KafkaMqPackConsumerProxyPlugin.class, "mq pack send commands error", e);
                        }
                    }
                    if (pause) {
                        consumer.resume(records.partitions());
                    }
                    if (!pause) {
                        try {
                            consumer.commitSync();
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    ErrorLogCollector.collect(KafkaMqPackConsumerProxyPlugin.class, "kafka consume error", e);
                }
            }
        });
        pollingThread.setName("mq-multi-write-kafka-polling-" + kafkaUrl + "-" + threadId);
        pollingThread.setDaemon(true);
        pollingThread.start();
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
}

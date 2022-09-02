package com.netease.nim.camellia.delayqueue.consumer.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListenerConfig;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdkConfig;
import com.netease.nim.camellia.tools.statistic.CamelliaStatistics;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2022/7/25
 */
public class PerformanceTest {

    public static void main(String[] args) throws InterruptedException {
        CamelliaStatistics statistics = new CamelliaStatistics();
        CamelliaDelayQueueSdkConfig config = new CamelliaDelayQueueSdkConfig();
        config.setUrl("http://127.0.0.1:8080");
        CamelliaDelayQueueSdk sdk = new CamelliaDelayQueueSdk(config);

        int topicNum = 10;
        int msgNum = 500;
        int pullBatch = 1;
        int pullThread = 1;
        int consumeThread = 50;
        boolean longPollingEnable = true;
        CountDownLatch totalLatch = new CountDownLatch(topicNum);
        CountDownLatch produceSpendStartLatch = new CountDownLatch(1);
        CountDownLatch produceSpendEndLatch = new CountDownLatch(topicNum);

        CountDownLatch taskLatch = new CountDownLatch(1);
        for (int i=0; i<topicNum; i++) {
            String topic = "topic-" + i;
            new Thread(() -> {
                try {
                    taskLatch.await();
                    CamelliaStatistics topicStatistics = new CamelliaStatistics();

                    AtomicLong produceNum = new AtomicLong();
                    AtomicLong consumerNum = new AtomicLong();
                    CountDownLatch latch = new CountDownLatch(msgNum);

                    AtomicLong produceSpend = new AtomicLong(0);
                    new Thread(() -> {
                        try {
                            produceSpendStartLatch.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        long time1 = System.nanoTime();
                        for (int j=0; j<msgNum; j++) {
                            //发送消息
                            int delaySeconds = ThreadLocalRandom.current().nextInt(60) + 10;
                            String msgId = UUID.randomUUID().toString();
                            while (true) {
                                try {
                                    sdk.sendMsg(topic, msgId, "abc", delaySeconds, TimeUnit.SECONDS);
                                    break;
                                } catch (Exception e) {
                                    System.out.println("【ERROR】topic=" + topic + ",ex=" + e);
                                }
                            }
                            if (produceNum.incrementAndGet() % 100 == 0) {
                                System.out.println("produce, topic=" + topic + ",count=" + produceNum.get());
                            }
                        }
                        long time2 = System.nanoTime();
                        produceSpend.set((time2 - time1)/1000000);
                        produceSpendEndLatch.countDown();
                        System.out.println("produce, topic=" + topic + ",count=" + msgNum);
                    }).start();
                    //消费消息
                    CamelliaDelayMsgListenerConfig listenerConfig = new CamelliaDelayMsgListenerConfig();
                    listenerConfig.setPullBatch(pullBatch);
                    listenerConfig.setPullThreads(pullThread);
                    listenerConfig.setConsumeThreads(consumeThread);
                    listenerConfig.setLongPollingEnable(longPollingEnable);
                    sdk.addMsgListener(topic, listenerConfig, delayMsg -> {
                        try {
                            if (consumerNum.incrementAndGet() % 100 == 0) {
                                System.out.println("consume, topic=" + topic + ",count=" + consumerNum.get());
                            }
                            long triggerTime = delayMsg.getTriggerTime();
                            long now = System.currentTimeMillis();
                            topicStatistics.update(Math.abs(triggerTime - now));
                            statistics.update(Math.abs(triggerTime - now));
                            return true;
                        } finally {
                            latch.countDown();
                        }
                    });
                    latch.await();
                    System.out.println("topic=" + topic + ",produce-spend-ms=" + produceSpend.get() + "ms,consume-time-gap=" + JSONObject.toJSONString(topicStatistics.getStatsDataAndReset()));
                    totalLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        taskLatch.countDown();
        produceSpendStartLatch.countDown();
        long produceStart = System.nanoTime();
        produceSpendEndLatch.await();
        long produceEnd = System.nanoTime();

        totalLatch.await();
        System.out.println("total statistics");
        long produceCount = msgNum*topicNum;
        double produceSpend = (produceEnd - produceStart) / 1000000.0;
        System.out.println("produce-count=" + produceCount + ",produce-spend-time=" + produceSpend + "ms");
        System.out.println("consumer-time-gap=" + JSONObject.toJSONString(statistics.getStatsDataAndReset()));
    }
}

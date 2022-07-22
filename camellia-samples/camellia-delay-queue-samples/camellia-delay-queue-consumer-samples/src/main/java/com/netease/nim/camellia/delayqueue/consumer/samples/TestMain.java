package com.netease.nim.camellia.delayqueue.consumer.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsg;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListener;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/7/22
 */
public class TestMain {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService1.class);

    public static void main(String[] args) {
        CamelliaDelayQueueSdkConfig config = new CamelliaDelayQueueSdkConfig();
        config.setUrl("http://127.0.0.1:8080");
        CamelliaDelayQueueSdk sdk = new CamelliaDelayQueueSdk(config);

        //发送消息
        sdk.sendMsg("topic1", "abc", 10, TimeUnit.SECONDS);

        //消费消息
        sdk.addMsgListener("topic1", delayMsg -> {
            try {
                logger.info("onMsg, time-gap = {}, delayMsg = {}",
                        System.currentTimeMillis() - delayMsg.getTriggerTime(), JSONObject.toJSONString(delayMsg));
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        });
    }
}

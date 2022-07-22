package com.netease.nim.camellia.delayqueue.consumer.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsg;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 手动调用sdk的addMsgListener接口去添加监听
 * Created by caojiajun on 2022/7/22
 */
@Component
public class ConsumerService2 implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService2.class);

    @Autowired
    private CamelliaDelayQueueSdk sdk;

    private boolean onMsg(CamelliaDelayMsg delayMsg) {
        try {
            logger.info("onMsg, time-gap = {}, delayMsg = {}", System.currentTimeMillis() - delayMsg.getTriggerTime(), JSONObject.toJSONString(delayMsg));
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;//返回false，则delay-queue-server会重试
        }
    }

    @Override
    public void afterPropertiesSet() {
        sdk.addMsgListener("topic2", ConsumerService2.this::onMsg);
    }
}

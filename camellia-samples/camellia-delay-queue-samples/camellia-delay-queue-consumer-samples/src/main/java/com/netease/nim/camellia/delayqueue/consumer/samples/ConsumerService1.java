package com.netease.nim.camellia.delayqueue.consumer.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsg;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayMsgListener;
import com.netease.nim.camellia.delayqueue.sdk.springboot.CamelliaDelayMsgListenerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 使用spring托管
 * 此时需要service实现CamelliaDelayMsgListener接口，并且添加@CamelliaDelayMsgListenerConfig注解设置topic以及其他参数
 * Created by caojiajun on 2022/7/21
 */
@Component
@CamelliaDelayMsgListenerConfig(topic = "topic1", pullThreads = 3)
public class ConsumerService1 implements CamelliaDelayMsgListener {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerService1.class);

    @Override
    public boolean onMsg(CamelliaDelayMsg delayMsg) {
        try {
            logger.info("onMsg, time-gap = {}, delayMsg = {}", System.currentTimeMillis() - delayMsg.getTriggerTime(), JSONObject.toJSONString(delayMsg));
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;//返回false，则delay-queue-server会重试
        }
    }
}

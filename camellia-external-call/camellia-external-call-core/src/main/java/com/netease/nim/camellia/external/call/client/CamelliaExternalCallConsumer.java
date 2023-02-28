package com.netease.nim.camellia.external.call.client;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.external.call.common.*;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 这是一个基于mq来做自动租户隔离的外部访问系统的消费端
 *
 * 业务需要启动时调用acquireMqInfo方法获取需要消费的mq地址和topic
 * 并且把收到的消息再调用CamelliaExternalCallConsumer的onMsg
 *
 * 此外，业务需要自行实现BizConsumer，表示实际的业务处理逻辑
 *
 * Created by caojiajun on 2023/2/24
 */
public class CamelliaExternalCallConsumer<R> implements ICamelliaExternalCallMqConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaExternalCallConsumer.class);

    private MqSender mqSender;
    private BizConsumer<R> bizConsumer;
    private CamelliaExternalCallRequestSerializer<R> serializer;

    @Override
    public List<MqInfo> acquireMqInfo() {
        //todo get from controller
        return new ArrayList<>();
    }

    @Override
    public boolean onMsg(MqInfo mqInfo, byte[] data) {
        ExecutorService executor = selectExecutor(mqInfo);
        try {
            Future<Boolean> future = executor.submit(() -> invoke(data));
            return future.get();
        } catch (Exception e) {
            logger.error("onMsg error", e);
            return false;
        }
    }

    private boolean invoke(byte[] data) {
        ExternalCallMqPack pack = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), ExternalCallMqPack.class);
        String isolationKey = pack.getIsolationKey();

        //isolation
        isolation(isolationKey);

        R request = serializer.deserialize(pack.getData());
        long start = System.currentTimeMillis();
        //===biz start
        boolean success = false;
        try {
            success = bizConsumer.onMsg(isolationKey, request);
        } catch (Exception e) {
            logger.error("biz onMsg error, isolationKey = {}, request = {}", isolationKey, request, e);
        }
        //===biz end
        long spendMs = System.currentTimeMillis() - start;

        //stats
        stats(isolationKey, success, spendMs);
        if (!success) {
            //retry
            retry(isolationKey, request);
        }

        return success;
    }

    private ExecutorService selectExecutor(MqInfo mqInfo) {
        return new CamelliaDynamicExecutor("", SysUtils.getCpuNum(), 10);
    }

    private void isolation(String isolationKey) {
        //todo isolation logic
    }

    private void retry(String isolationKey, R request) {
        //todo retry logic
    }

    private void stats(String isolationKey, boolean success, long spendMs) {

    }
}

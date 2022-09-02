package com.netease.nim.camellia.delayqueue.sdk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.delayqueue.common.domain.*;
import com.netease.nim.camellia.delayqueue.sdk.api.*;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayMsgErrorCode;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * delay queue sdk
 * Created by caojiajun on 2022/7/7
 */
public class CamelliaDelayQueueSdk {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDelayQueueSdk.class);

    private final CamelliaDelayQueueSdkConfig sdkConfig;
    private final CamelliaDelayQueueApi api;
    private final ConcurrentHashMap<Long, PullMsgTask> taskMap = new ConcurrentHashMap<>();

    public CamelliaDelayQueueSdk(CamelliaDelayQueueSdkConfig sdkConfig) {
        this.sdkConfig = sdkConfig;
        this.api = new CamelliaDelayQueueApi(sdkConfig);
        logger.info("CamelliaDelayQueueSdk init success, sdkConfig = {}", JSONObject.toJSONString(sdkConfig));
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msgId 消息id，topic内唯一，如果不填则由服务器生成
     * @param msg 消息
     * @param delayMillis 延迟时间，单位ms，如果小于等于0，表示立即消费
     * @param ttlMillis 过期时间，单位ms，如果小于等于0，则取服务器配置
     * @param maxRetry 消费最大重试次数，如果小于0，则取服务器配置
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msgId, String msg, long delayMillis, long ttlMillis, int maxRetry) {
        CamelliaDelayMsgSendRequest request = new CamelliaDelayMsgSendRequest();
        request.setTopic(topic);
        request.setMsg(msg);
        request.setDelayMillis(delayMillis);
        request.setTtlMillis(ttlMillis);
        request.setMaxRetry(maxRetry);
        request.setMsgId(msgId);
        CamelliaDelayMsgSendResponse response = api.sendMsg(request);
        CamelliaDelayMsgErrorCode errorCode = CamelliaDelayMsgErrorCode.getByValue(response.getCode());
        if (errorCode == CamelliaDelayMsgErrorCode.SUCCESS) {
            return response.getDelayMsg();
        }
        throw new CamelliaDelayQueueException(errorCode, response.getMsg());
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msg 数据
     * @param delayMillis 延迟时间，单位ms，如果小于等于0，表示立即消费
     * @param ttlMillis 过期时间，单位ms，如果小于等于0，则取服务器配置
     * @param maxRetry 消费最大重试次数，如果小于0，则取服务器配置
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msg, long delayMillis, long ttlMillis, int maxRetry) {
        return sendMsg(topic, null, msg, delayMillis, ttlMillis, maxRetry);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msgId 消息id，topic内唯一，如果不填则由服务器生成
     * @param msg 数据
     * @param delay 延迟的时间
     * @param delayTimeUnit 延迟的时间单位
     * @param ttl 延迟到期后消息的过期时间
     * @param ttlTimeUnit 延迟到期后消息的过期时间的单位
     * @param maxRetry 消费最大重试次数，如果小于0，则取服务器配置
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msgId, String msg, long delay, TimeUnit delayTimeUnit, long ttl, TimeUnit ttlTimeUnit, int maxRetry) {
        return sendMsg(topic, msgId, msg, delayTimeUnit.toMillis(delay), ttlTimeUnit.toMillis(ttl), maxRetry);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msg 数据
     * @param delay 延迟的时间
     * @param delayTimeUnit 延迟的时间单位
     * @param ttl 延迟到期后消息的过期时间
     * @param ttlTimeUnit 延迟到期后消息的过期时间的单位
     * @param maxRetry 消费最大重试次数，如果小于0，则取服务器配置
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msg, long delay, TimeUnit delayTimeUnit, long ttl, TimeUnit ttlTimeUnit, int maxRetry) {
        return sendMsg(topic, msg, delayTimeUnit.toMillis(delay), ttlTimeUnit.toMillis(ttl), maxRetry);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msg 数据
     * @param delayMillis 延迟时间，单位ms，如果小于等于0，表示立即消费
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msg, long delayMillis) {
        return sendMsg(topic, msg, delayMillis, -1, -1);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msg 数据
     * @param delay 延迟的时间
     * @param delayTimeUnit 延迟的时间单位
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msg, long delay, TimeUnit delayTimeUnit) {
        return sendMsg(topic, msg, delayTimeUnit.toMillis(delay), -1, -1);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msgId 消息id，topic内唯一，如果不填则由服务器生成
     * @param msg 数据
     * @param delay 延迟的时间
     * @param delayTimeUnit 延迟的时间单位
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsg(String topic, String msgId, String msg, long delay, TimeUnit delayTimeUnit) {
        return sendMsg(topic, msgId, msg, delayTimeUnit.toMillis(delay), -1, -1);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msg 数据
     * @param triggerTime 消费的时间戳，单位ms
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsgByTriggerTime(String topic, String msg, long triggerTime) {
        return sendMsg(topic, msg, triggerTime - System.currentTimeMillis(), -1, -1);
    }

    /**
     * 发送一条延迟消息
     * @param topic topic
     * @param msgId 消息id，topic内唯一，如果不填则由服务器生成
     * @param msg 数据
     * @param triggerTime 消费的时间戳，单位ms
     * @return CamelliaDelayMsg
     */
    public CamelliaDelayMsg sendMsgByTriggerTime(String topic, String msgId, String msg, long triggerTime) {
        return sendMsg(topic, msgId, msg, triggerTime - System.currentTimeMillis(), -1, -1);
    }

    /**
     * 删除一条延迟消息
     * @param topic topic
     * @param msgId 消息id
     * @return 删除结果，如果消息不存在或者已经被消费了或者已经处于丢弃状态，则会返回false
     */
    public boolean deleteMsg(String topic, String msgId) {
        CamelliaDelayMsgDeleteRequest request = new CamelliaDelayMsgDeleteRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        CamelliaDelayMsgDeleteResponse response = api.deleteMsg(request);
        CamelliaDelayMsgErrorCode errorCode = CamelliaDelayMsgErrorCode.getByValue(response.getCode());
        if (errorCode == CamelliaDelayMsgErrorCode.SUCCESS) {
            return true;
        }
        if (errorCode == CamelliaDelayMsgErrorCode.NOT_EXISTS) {
            return false;
        }
        throw new CamelliaDelayQueueException(errorCode, response.getMsg());
    }

    /**
     * 获取一条消息
     * 当消息到达终态（被消费了，或者丢弃了），服务器会维护一个较短时间（取决于服务器配置），在时间范围内去查询，可以返回消息本身并标注状态，超过这个时间，会直接返回null
     * @param topic topic
     * @param msgId 消息id
     * @return 消息
     */
    public CamelliaDelayMsg getMsg(String topic, String msgId) {
        CamelliaDelayMsgGetRequest request = new CamelliaDelayMsgGetRequest();
        request.setTopic(topic);
        request.setMsgId(msgId);
        CamelliaDelayMsgGetResponse response = api.getMsg(request);
        CamelliaDelayMsgErrorCode errorCode = CamelliaDelayMsgErrorCode.getByValue(response.getCode());
        if (errorCode == CamelliaDelayMsgErrorCode.SUCCESS) {
            return response.getDelayMsg();
        } else if (errorCode == CamelliaDelayMsgErrorCode.NOT_EXISTS) {
            return null;
        }
        throw new CamelliaDelayQueueException(errorCode, response.getMsg());
    }

    /**
     * 增加一个监听
     * @param topic topic
     * @param listener 监听器
     * @return id 可以用于移除监听
     */
    public long addMsgListener(String topic, CamelliaDelayMsgListener listener) {
        return addMsgListener(topic, sdkConfig.getListenerConfig(), listener);
    }

    /**
     * 增加一个监听
     * @param topic topic
     * @param listenerConfig 监听配置
     * @param listener 监听器
     * @return id 可以用于移除监听
     */
    public long addMsgListener(String topic, CamelliaDelayMsgListenerConfig listenerConfig, CamelliaDelayMsgListener listener) {
        if (listener == null) {
            throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "listener null");
        }
        if (listenerConfig == null) {
            listenerConfig = sdkConfig.getListenerConfig();
        }
        PullMsgTask task = new PullMsgTask(api, topic, listenerConfig, listener);
        task.start();
        taskMap.put(task.getId(), task);
        logger.info("add camellia delay queue listener success, topic = {}, listenerConfig = {}, listener = {}, id = {}",
                topic, JSONObject.toJSONString(listenerConfig), listener.getClass().getName(), task.getId());
        return task.getId();
    }

    /**
     * 移除一个监听
     * @param id id
     * @return 成功/失败
     */
    public boolean removeMsgListener(long id) {
        PullMsgTask pullMsgTask = taskMap.remove(id);
        if (pullMsgTask == null) return false;
        pullMsgTask.close();
        logger.info("remove camellia delay queue listener success, id = {}", id);
        return true;
    }

    private static class PullMsgTask {

        private static final AtomicLong idGen = new AtomicLong(0);

        private final CamelliaDelayQueueApi api;
        private final String topic;
        private final CamelliaDelayMsgListenerConfig config;
        private final CamelliaDelayMsgListener listener;
        private final long id;
        private volatile boolean running = true;
        private final ExecutorService consumerExec;

        public PullMsgTask(CamelliaDelayQueueApi api, String topic,
                           CamelliaDelayMsgListenerConfig config, CamelliaDelayMsgListener listener) {
            this.api = api;
            this.topic = topic;
            this.config = config;
            this.listener = listener;
            this.id = idGen.incrementAndGet();
            this.consumerExec = new ThreadPoolExecutor(config.getConsumeThreads(), config.getConsumeThreads(),
                    0, TimeUnit.SECONDS, new SynchronousQueue<>(),
                    new CamelliaThreadFactory("camellia-delay-queue-consume-msg[\" + topic + \"]"), new ThreadPoolExecutor.CallerRunsPolicy());
        }

        public long getId() {
            return id;
        }

        public void close() {
            running = false;
            consumerExec.shutdown();
        }

        public void start() {

            for (int i = 0; i<config.getPullThreads(); i++) {
                new Thread(() -> {
                    logger.info("camellia delay queue pull task thread start, thread = {}", Thread.currentThread().getName());
                    while (running) {
                        CamelliaDelayMsgPullRequest request = new CamelliaDelayMsgPullRequest();
                        request.setBatch(config.getPullBatch());
                        request.setTopic(topic);
                        request.setAckTimeoutMillis(config.getAckTimeoutMillis());
                        CamelliaDelayMsgPullResponse response;
                        try {
                            if (config.isLongPollingEnable()) {
                                response = api.longPollingMsg(request, config.getLongPollingTimeoutMillis());
                            } else {
                                response = api.pullMsg(request);
                            }
                        } catch (Exception e) {
                            if (config.isLongPollingEnable()) {
                                logger.error("camellia delay queue pullMsg error, topic = {}, will retry",
                                        topic, e);
                            } else {
                                logger.error("camellia delay queue pullMsg error, topic = {}, will sleep {} ms and retry",
                                        topic, config.getPullIntervalTimeMillis(), e);
                                sleepToNextTime();
                            }
                            continue;
                        }
                        CamelliaDelayMsgErrorCode errorCode = CamelliaDelayMsgErrorCode.getByValue(response.getCode());
                        if (errorCode == CamelliaDelayMsgErrorCode.SUCCESS) {
                            List<CamelliaDelayMsg> delayMsgList = response.getDelayMsgList();
                            if (delayMsgList != null && !delayMsgList.isEmpty()) {
                                for (CamelliaDelayMsg delayMsg : delayMsgList) {
                                    consumerExec.submit(() -> {
                                        try {
                                            boolean ack;
                                            try {
                                                ack = listener.onMsg(delayMsg);
                                            } catch (Exception e) {
                                                logger.error("listener onMsg error, will ack false and retry, delayMsg = {}", JSONObject.toJSONString(delayMsg), e);
                                                ack = false;
                                            }
                                            CamelliaDelayMsgAckRequest ackRequest = new CamelliaDelayMsgAckRequest();
                                            ackRequest.setTopic(delayMsg.getTopic());
                                            ackRequest.setMsgId(delayMsg.getMsgId());
                                            ackRequest.setAck(ack);
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("ack request = {}", JSONObject.toJSONString(ackRequest));
                                            }
                                            try {
                                                CamelliaDelayMsgAckResponse ackResponse = api.ackMsg(ackRequest);
                                                if (logger.isDebugEnabled()) {
                                                    logger.debug("ack response = {}", JSONObject.toJSONString(ackResponse));
                                                }
                                            } catch (Exception e) {
                                                logger.error("ack error, request = {}", JSONObject.toJSONString(request), e);
                                            }
                                        } catch (Exception e) {
                                            logger.error("onMsg error, delayMsg = {}", JSONObject.toJSONString(delayMsg), e);
                                        }
                                    });
                                }
                                continue;
                            }
                        }
                        if (!config.isLongPollingEnable()) {
                            sleepToNextTime();
                        }
                    }
                    logger.info("camellia delay queue pull task thread close, thread = {}", Thread.currentThread().getName());
                }, "camellia-delay-queue-pull-msg[" + topic + "][id=" + id + "]["+ i + "]").start();
            }
        }

        private void sleepToNextTime() {
            try {
                TimeUnit.MILLISECONDS.sleep(config.getPullIntervalTimeMillis());
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

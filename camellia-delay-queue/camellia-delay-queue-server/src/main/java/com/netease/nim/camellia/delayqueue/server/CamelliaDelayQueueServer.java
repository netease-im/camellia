package com.netease.nim.camellia.delayqueue.server;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.delayqueue.common.domain.*;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayMsgErrorCode;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayQueueException;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLockManager;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Response;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2022/7/14
 */
public class CamelliaDelayQueueServer {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDelayQueueServer.class);

    private final CamelliaDelayQueueServerConfig serverConfig;
    private final CamelliaRedisTemplate template;

    private final ExecutorService checkTriggerExecutor;
    private final ExecutorService checkTimeoutExecutor;
    private final ExecutorService checkExpireExecutor;
    private final ScheduledExecutorService msgScheduledExecutor;
    private final ScheduledExecutorService topicScheduledExecutor;
    private final CamelliaRedisLockManager lockManager;

    public CamelliaDelayQueueServer(CamelliaDelayQueueServerConfig serverConfig, CamelliaRedisTemplate template) {
        this.serverConfig = serverConfig;
        this.template = template;
        checkTriggerExecutor = new ThreadPoolExecutor(serverConfig.getCheckTriggerThreadNum(), serverConfig.getCheckTriggerThreadNum(),
                0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100000),
                new CamelliaThreadFactory("camellia-delay-msg-check-trigger"), new ThreadPoolExecutor.AbortPolicy());
        checkTimeoutExecutor = new ThreadPoolExecutor(serverConfig.getCheckTimeoutThreadNum(), serverConfig.getCheckTimeoutThreadNum(),
                0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100000),
                new CamelliaThreadFactory("camellia-delay-msg-check-timeout"), new ThreadPoolExecutor.AbortPolicy());
        checkExpireExecutor = new ThreadPoolExecutor(serverConfig.getCheckTimeoutThreadNum(), serverConfig.getCheckTimeoutThreadNum(),
                0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100000),
                new CamelliaThreadFactory("camellia-delay-msg-check-expire"), new ThreadPoolExecutor.AbortPolicy());
        msgScheduledExecutor = new ScheduledThreadPoolExecutor(serverConfig.getScheduleThreadNum(), new CamelliaThreadFactory("msg-schedule"));
        topicScheduledExecutor = new ScheduledThreadPoolExecutor(serverConfig.getScheduleThreadNum(), new CamelliaThreadFactory("topic-schedule"));

        lockManager = new CamelliaRedisLockManager(template, serverConfig.getScheduleThreadNum(), 5000, 5000);

        startSchedule();
        logger.info("CamelliaDelayQueueServer start success, config = {}", JSONObject.toJSONString(serverConfig));
        CamelliaDelayQueueMonitor.init(serverConfig.getMonitorIntervalSeconds());
    }

    /**
     * 发送一条延迟消息
     */
    public CamelliaDelayMsgSendResponse sendMsg(CamelliaDelayMsgSendRequest request) {
        try {
            String topic = request.getTopic();
            if (topic == null || topic.length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "topic is empty");
            }
            if (request.getMsg() == null || request.getMsg().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "msg is empty");
            }
            active(topic);
            CamelliaDelayMsg msg = new CamelliaDelayMsg();
            msg.setTopic(topic);
            msg.setMsg(request.getMsg());
            if (request.getMsgId() == null) {
                msg.setMsgId(genMsgId());
            } else {
                msg.setMsgId(request.getMsgId());
            }
            long now = System.currentTimeMillis();
            msg.setProduceTime(now);
            long delayMillis = request.getDelayMillis();
            if (delayMillis <= 0) {//如果delayMillis
                msg.setTriggerTime(now);
                msg.setStatus(CamelliaDelayMsgStatus.READY.getValue());
            } else {
                msg.setTriggerTime(now + delayMillis);
                if (delayMillis <= 100) {
                    msg.setStatus(CamelliaDelayMsgStatus.READY.getValue());
                } else {
                    msg.setStatus(CamelliaDelayMsgStatus.WAITING.getValue());
                }
            }
            long ttlMillis = request.getTtlMillis();
            if (ttlMillis <= 0) {
                msg.setExpireTime(now + serverConfig.getTtlMillis());
            } else {
                msg.setExpireTime(now + ttlMillis);
            }
            int maxRetry = request.getMaxRetry();
            if (maxRetry < 0) {
                msg.setMaxRetry(serverConfig.getMaxRetry());
            } else {
                msg.setMaxRetry(maxRetry);
            }
            //消息存储
            boolean ok = saveOrUpdateMsg(msg, true, false);
            if (!ok) {
                //如果已经存在了，则返回老消息，用于消息去重
                String msgKey = msgKey(topic, msg.getMsgId());
                String value = template.get(msgKey);
                CamelliaDelayMsg delayMsg = JSONObject.parseObject(value, CamelliaDelayMsg.class);
                CamelliaDelayMsgSendResponse response = new CamelliaDelayMsgSendResponse();
                response.setCode(CamelliaDelayMsgErrorCode.SUCCESS.getValue());
                response.setMsg("success");
                response.setDelayMsg(delayMsg);
                CamelliaDelayQueueMonitor.sendMsg(request, response);
                return response;
            }
            if (msg.getStatus() == CamelliaDelayMsgStatus.WAITING.getValue()) {
                //如果是等待状态，则塞到zset中
                String waitingQueueKey = waitingQueueKey(msg.getTopic());
                template.zadd(waitingQueueKey, msg.getTriggerTime(), msg.getMsgId());
            } else if (msg.getStatus() == CamelliaDelayMsgStatus.READY.getValue()) {
                //如果是就绪状态，则塞到就绪set中
                String readyQueueKey = readyQueueKey(msg.getTopic());
                template.lpush(readyQueueKey, msg.getMsgId());
            } else {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG);
            }

            //更新活跃topic列表
            String topicsKey = topicsKey();
            template.zadd(topicsKey, System.currentTimeMillis(), msg.getTopic());

            CamelliaDelayMsgSendResponse response = new CamelliaDelayMsgSendResponse();
            response.setCode(CamelliaDelayMsgErrorCode.SUCCESS.getValue());
            response.setMsg("success");
            response.setDelayMsg(msg);
            CamelliaDelayQueueMonitor.sendMsg(request, response);
            return response;
        } catch (CamelliaDelayQueueException e) {
            logger.error("sendMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgSendResponse response = new CamelliaDelayMsgSendResponse();
            response.setCode(e.getErrorCode().getValue());
            CamelliaDelayQueueMonitor.sendMsg(request, response);
            return response;
        } catch (Exception e) {
            logger.error("sendMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgSendResponse response = new CamelliaDelayMsgSendResponse();
            response.setCode(CamelliaDelayMsgErrorCode.UNKNOWN.getValue());
            CamelliaDelayQueueMonitor.sendMsg(request, response);
            return response;
        }
    }

    //从list中rpop，如果有，则zadd到zset中
    private static final String PULL_MSG_SCRIPT = "local msgid = redis.call(\"rpop\", KEYS[1])\n" +
            "if msgid then\n" +
            " redis.call(\"zadd\", KEYS[2], ARGV[1], tostring(msgid))\n" +
            "end\n" +
            "return msgid";

    /**
     * 拉消息
     */
    public CamelliaDelayMsgPullResponse pullMsg(CamelliaDelayMsgPullRequest request) {
        try {
            if (request.getTopic() == null || request.getTopic().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "topic is empty");
            }
            String topic = request.getTopic();
            active(topic);
            long ackTimeoutMillis = request.getAckTimeoutMillis();
            if (ackTimeoutMillis <= 0) {
                ackTimeoutMillis = serverConfig.getAckTimeoutMillis();
            }
            int batch = request.getBatch();
            if (batch <= 0) {
                batch = 1;
            }
            CamelliaDelayMsgPullResponse response = new CamelliaDelayMsgPullResponse();
            response.setCode(CamelliaDelayMsgErrorCode.SUCCESS.getValue());

            String readyQueueKey = readyQueueKey(topic);
            String ackQueueKey = ackQueueKey(topic);

            int retry = 2;
            while (true) {
                retry --;
                if (retry <= 0) break;
                long now = System.currentTimeMillis();
                //从ready queue中取出，并放到ack queue中
                Set<String> msgIdSet = new HashSet<>();
                List<String> keys = new ArrayList<>();
                keys.add(readyQueueKey);
                keys.add(ackQueueKey);
                List<String> args = new ArrayList<>();
                args.add(String.valueOf(now + ackTimeoutMillis));
                for (int i = 0; i < batch; i++) {
                    Object ret = template.eval(PULL_MSG_SCRIPT, keys, args);
                    if (ret == null) break;
                    String msgId = new String((byte[]) ret, StandardCharsets.UTF_8);
                    msgIdSet.add(msgId);
                }
                if (msgIdSet.isEmpty()) {
                    response.setDelayMsgList(new ArrayList<>());
                    CamelliaDelayQueueMonitor.pullMsg(request, response);
                    return response;
                }
                //校验消息生命周期
                MsgCheckStatusResult result = checkMsgLife(topic, msgIdSet, true);
                if (!result.endLifeMsgMap.isEmpty()) {
                    //生命周期已经结束的消息，直接从ackQueue中删除
                    template.zrem(ackQueueKey, result.endLifeMsgMap.keySet().toArray(new String[0]));
                }
                response.setDelayMsgList(new ArrayList<>(result.inLifeMsgMap.values()));
                //如果有无效的消息，而没有有效的消息，则重试一下
                if (!result.endLifeMsgMap.isEmpty() && result.inLifeMsgMap.isEmpty()) {
                    continue;
                }
                CamelliaDelayQueueMonitor.pullMsg(request, response);
                return response;
            }
            response.setDelayMsgList(new ArrayList<>());
            CamelliaDelayQueueMonitor.pullMsg(request, response);
            return response;
        } catch (CamelliaDelayQueueException e) {
            logger.error("pullMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgPullResponse response = new CamelliaDelayMsgPullResponse();
            response.setCode(e.getErrorCode().getValue());
            CamelliaDelayQueueMonitor.pullMsg(request, response);
            return response;
        } catch (Exception e) {
            logger.error("pullMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgPullResponse response = new CamelliaDelayMsgPullResponse();
            response.setCode(CamelliaDelayMsgErrorCode.UNKNOWN.getValue());
            CamelliaDelayQueueMonitor.pullMsg(request, response);
            return response;
        }
    }

    /**
     * 删除消息
     */
    public CamelliaDelayMsgDeleteResponse deleteMsg(CamelliaDelayMsgDeleteRequest request) {
        try {
            String topic = request.getTopic();
            if (request.getTopic() == null || request.getTopic().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "topic is empty");
            }
            String msgId = request.getMsgId();
            if (request.getMsgId() == null || request.getMsgId().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "msgId is empty");
            }
            active(topic);
            MsgCheckStatusResult result = checkMsgLife(topic, Collections.singletonList(msgId), false);
            CamelliaDelayMsgDeleteResponse response = new CamelliaDelayMsgDeleteResponse();
            if (!result.inLifeMsgMap.isEmpty()) {
                CamelliaDelayMsg delayMsg = result.inLifeMsgMap.get(msgId);
                delayMsg.setStatus(CamelliaDelayMsgStatus.DELETE.getValue());
                saveOrUpdateMsg(delayMsg, false,true);
                response.setCode(CamelliaDelayMsgErrorCode.SUCCESS.getValue());
                response.setMsg("success");
                CamelliaDelayQueueMonitor.deleteMsg(request, response);
                return response;
            }
            response.setCode(CamelliaDelayMsgErrorCode.NOT_EXISTS.getValue());
            return response;
        } catch (CamelliaDelayQueueException e) {
            logger.error("deleteMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgDeleteResponse response = new CamelliaDelayMsgDeleteResponse();
            response.setCode(e.getErrorCode().getValue());
            CamelliaDelayQueueMonitor.deleteMsg(request, response);
            return response;
        } catch (Exception e) {
            logger.error("deleteMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgDeleteResponse response = new CamelliaDelayMsgDeleteResponse();
            response.setCode(CamelliaDelayMsgErrorCode.UNKNOWN.getValue());
            CamelliaDelayQueueMonitor.deleteMsg(request, response);
            return response;
        }
    }

    /**
     * 查询消息
     */
    public CamelliaDelayMsgGetResponse getMsg(CamelliaDelayMsgGetRequest request) {
        try {
            String topic = request.getTopic();
            if (request.getTopic() == null || request.getTopic().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "topic is empty");
            }
            String msgId = request.getMsgId();
            if (request.getMsgId() == null || request.getMsgId().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "msgId is empty");
            }
            active(topic);
            MsgCheckStatusResult result = checkMsgLife(topic, Collections.singletonList(msgId), false);
            CamelliaDelayMsgGetResponse response = new CamelliaDelayMsgGetResponse();
            CamelliaDelayMsg delayMsg = result.inLifeMsgMap.get(msgId);
            if (delayMsg == null) {
                delayMsg = result.endLifeMsgMap.get(msgId);
            }
            if (delayMsg == null) {
                response.setCode(CamelliaDelayMsgErrorCode.NOT_EXISTS.getValue());
                CamelliaDelayQueueMonitor.getMsg(request, response);
                return response;
            }
            response.setCode(CamelliaDelayMsgErrorCode.SUCCESS.getValue());
            response.setMsg("success");
            response.setDelayMsg(result.inLifeMsgMap.get(msgId));
            CamelliaDelayQueueMonitor.getMsg(request, response);
            return response;
        } catch (CamelliaDelayQueueException e) {
            logger.error("getMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgGetResponse response = new CamelliaDelayMsgGetResponse();
            response.setCode(e.getErrorCode().getValue());
            CamelliaDelayQueueMonitor.getMsg(request, response);
            return response;
        } catch (Exception e) {
            logger.error("getMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgGetResponse response = new CamelliaDelayMsgGetResponse();
            response.setCode(CamelliaDelayMsgErrorCode.UNKNOWN.getValue());
            CamelliaDelayQueueMonitor.getMsg(request, response);
            return response;
        }
    }

    /**
     * ack消息
     */
    public CamelliaDelayMsgAckResponse ackMsg(CamelliaDelayMsgAckRequest request) {
        try {
            String topic = request.getTopic();
            if (request.getTopic() == null || request.getTopic().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "topic is empty");
            }
            String msgId = request.getMsgId();
            if (request.getMsgId() == null || request.getMsgId().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "msgId is empty");
            }
            active(topic);
            if (request.isAck()) {
                //如果ack成功了，则标记消息状态，并从ack队列中移除
                String msgKey = msgKey(topic, msgId);
                String value = template.get(msgKey);
                if (value != null) {
                    CamelliaDelayMsg delayMsg = JSONObject.parseObject(value, CamelliaDelayMsg.class);
                    delayMsg.setStatus(CamelliaDelayMsgStatus.CONSUME_OK.getValue());
                    saveOrUpdateMsg(delayMsg, false,true);
                }
                String ackQueueKey = ackQueueKey(topic);
                template.zrem(ackQueueKey, msgId);
            } else {
                //如果ack失败，则扔回ready队列，等待再次消费
                String ackQueueKey = ackQueueKey(topic);
                String readyQueueKey = readyQueueKey(topic);
                //使用lua脚本确保原子性
                List<String> keys = new ArrayList<>(2);
                keys.add(ackQueueKey);
                keys.add(readyQueueKey);
                List<String> args = new ArrayList<>();
                args.add(String.valueOf(1));
                args.add(msgId);
                template.eval(ZREM_LPUSH_SCRIPT, keys, args);
            }
            CamelliaDelayMsgAckResponse response = new CamelliaDelayMsgAckResponse();
            response.setCode(CamelliaDelayMsgErrorCode.SUCCESS.getValue());
            response.setMsg("success");
            CamelliaDelayQueueMonitor.ackMsg(request, response);
            return response;
        } catch (CamelliaDelayQueueException e) {
            logger.error("ackMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgAckResponse response = new CamelliaDelayMsgAckResponse();
            response.setCode(e.getErrorCode().getValue());
            CamelliaDelayQueueMonitor.ackMsg(request, response);
            return response;
        } catch (Exception e) {
            logger.error("ackMsg error, request = {}", JSONObject.toJSONString(request), e);
            CamelliaDelayMsgAckResponse response = new CamelliaDelayMsgAckResponse();
            response.setCode(CamelliaDelayMsgErrorCode.UNKNOWN.getValue());
            CamelliaDelayQueueMonitor.ackMsg(request, response);
            return response;
        }
    }

    /**
     * 获取topic信息
     */
    public CamelliaDelayQueueTopicInfo getTopicInfo(String topic) {
        String waitingQueueKey = waitingQueueKey(topic);
        String readyQueueKey = readyQueueKey(topic);
        String ackQueueKey = ackQueueKey(topic);
        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            long now = System.currentTimeMillis();
            Response<Long> min1 = pipeline.zcount(waitingQueueKey, 0, now + 60*1000L);
            Response<Long> min10 = pipeline.zcount(waitingQueueKey, now + 60*1000L, now + 10*60*1000L);
            Response<Long> min30 = pipeline.zcount(waitingQueueKey, now + 10*60*1000L, now + 30*60*1000L);
            Response<Long> hour1 = pipeline.zcount(waitingQueueKey, now + 30*60*1000L, now + 60*60*1000L);
            Response<Long> hour6 = pipeline.zcount(waitingQueueKey, now + 60*60*1000L, now + 6*60*60*1000L);
            Response<Long> day1 = pipeline.zcount(waitingQueueKey, now + 6*60*60*1000L, now + 24*60*60*1000L);
            Response<Long> day7 = pipeline.zcount(waitingQueueKey, now + 24*60*60*1000L, now + 7*24*60*60*1000L);
            Response<Long> day30 = pipeline.zcount(waitingQueueKey, now + 7*24*60*60*1000L, 30*24*60*60*1000L);
            Response<Long> dayN = pipeline.zcount(waitingQueueKey, now + 30*24*60*60*1000L, Long.MAX_VALUE);
            Response<Long> waitingQueueSize = pipeline.zcard(waitingQueueKey);
            Response<Long> readyQueueSize = pipeline.llen(readyQueueKey);
            Response<Long> ackQueueSize = pipeline.zcard(ackQueueKey);
            pipeline.sync();

            CamelliaDelayQueueTopicInfo topicInfo = new CamelliaDelayQueueTopicInfo();
            topicInfo.setTopic(topic);
            topicInfo.setWaitingQueueSize(waitingQueueSize.get());
            topicInfo.setReadyQueueSize(readyQueueSize.get());
            topicInfo.setAckQueueSize(ackQueueSize.get());
            CamelliaDelayQueueTopicInfo.WaitingQueueInfo waitingQueueInfo = new CamelliaDelayQueueTopicInfo.WaitingQueueInfo();
            waitingQueueInfo.setSizeOf0To1min(min1.get());
            waitingQueueInfo.setSizeOf1minTo10min(min10.get());
            waitingQueueInfo.setSizeOf10minTo30min(min30.get());
            waitingQueueInfo.setSizeOf30minTo1hour(hour1.get());
            waitingQueueInfo.setSizeOf1hourTo6hour(hour6.get());
            waitingQueueInfo.setSizeOf6hourTo1day(day1.get());
            waitingQueueInfo.setSizeOf1dayTo7day(day7.get());
            waitingQueueInfo.setSizeOf7dayTo30day(day30.get());
            waitingQueueInfo.setSizeOf30dayToInfinite(dayN.get());
            topicInfo.setWaitingQueueInfo(waitingQueueInfo);
            return topicInfo;
        }
    }

    /**
     * 获取topic信息
     */
    public List<CamelliaDelayQueueTopicInfo> getTopicInfoList() {
        String topicsKey = topicsKey();
        List<CamelliaDelayQueueTopicInfo> topicInfoList = new ArrayList<>();
        Long topicNum = template.zcard(topicsKey);
        for (int i = 0; i < topicNum; i += 100) {
            Set<String> topics = template.zrange(topicsKey, i, i + 99);
            for (String topic : topics) {
                CamelliaDelayQueueTopicInfo topicInfo = getTopicInfo(topic);
                topicInfoList.add(topicInfo);
            }
        }
        String topicsKeyTmp = topicsKeyTmp();
        Long topicNumTmp = template.zcard(topicsKeyTmp);
        for (int i = 0; i < topicNumTmp; i += 100) {
            Set<String> topics = template.zrange(topicsKeyTmp, i, i + 99);
            for (String topic : topics) {
                CamelliaDelayQueueTopicInfo topicInfo = getTopicInfo(topic);
                topicInfoList.add(topicInfo);
            }
        }
        return topicInfoList;
    }

    //定时任务
    private void startSchedule() {
        long msgScheduleMillis = serverConfig.getMsgScheduleMillis();
        msgScheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                //topicsKey
                String topicsKey = topicsKey();
                Long topicNum = template.zcard(topicsKey);
                scheduleMsg(topicsKey, topicNum);
                //topicsKeyTmp
                String topicsKeyTmp = topicsKeyTmp();
                Long topicNumTmp = template.zcard(topicsKeyTmp);
                scheduleMsg(topicsKeyTmp, topicNumTmp);
            } catch (Exception e) {
                logger.error("msg schedule error", e);
            }
        }, ThreadLocalRandom.current().nextLong(msgScheduleMillis), msgScheduleMillis, TimeUnit.MILLISECONDS);

        long topicScheduleSeconds = serverConfig.getTopicScheduleSeconds();
        topicScheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                //topicsKey
                String topicsKey = topicsKey();
                Long topicNum = template.zcard(topicsKey);
                scheduleTopic(topicsKey, topicNum);
                //topicsKeyTmp
                String topicsKeyTmp = topicsKeyTmp();
                Long topicNumTmp = template.zcard(topicsKeyTmp);
                scheduleTopic(topicsKeyTmp, topicNumTmp);
            } catch (Exception e) {
                logger.error("topic schedule error", e);
            }
        }, ThreadLocalRandom.current().nextLong(topicScheduleSeconds), topicScheduleSeconds, TimeUnit.SECONDS);
    }

    private void scheduleMsg(String redisKey, Long topicNum) {
        if (topicNum != null && topicNum > 0) {
            for (int i = 0; i < topicNum; i += 100) {
                Set<String> topics = template.zrange(redisKey, i, i + 99);
                for (String topic : topics) {
                    try {
                        checkTriggerExecutor.submit(() -> {
                            //通过加锁分散一下各个server的工作，提高一下效率
                            try {
                                lockManager.tryLockAndRun(checkTriggerLockKey(topic), () -> checkTriggerMsg(topic));
                            } catch (Exception e) {
                                logger.error("check trigger msg error, topic = {}", topic, e);
                            }
                        });
                    } catch (Exception e) {
                        logger.error("submit checkTriggerMsg error", e);
                    }
                    try {
                        checkTimeoutExecutor.submit(() -> {
                            //通过加锁分散一下各个server的工作，提高一下效率
                            try {
                                lockManager.tryLockAndRun(checkTimeoutLockKey(topic), () -> checkTimeoutMsg(topic));
                            } catch (Exception e) {
                                logger.error("check timeout msg error, topic = {}", topic, e);
                            }
                        });
                    } catch (Exception e) {
                        logger.error("submit checkTimeoutMsg error", e);
                    }
                    try {
                        checkExpireExecutor.submit(() -> {
                            //通过加锁分散一下各个server的工作，提高一下效率
                            try {
                                lockManager.tryLockAndRun(checkExpireLockKey(topic), () -> checkExpireMsg(topic));
                            } catch (Exception e) {
                                logger.error("check expire msg error, topic = {}", topic, e);
                            }
                        });
                    } catch (Exception e) {
                        logger.error("submit checkExpireMsg error", e);
                    }
                }
            }
        }
    }

    private void scheduleTopic(String redisKey, Long topicNum) {
        if (topicNum != null && topicNum > 0) {
            for (int i = 0; i < topicNum; i += 100) {
                Set<String> topics = template.zrange(redisKey, i, i + 99);
                for (String topic : topics) {
                    //通过加锁分散一下各个server的工作，提高一下效率
                    try {
                        lockManager.tryLockAndRun(checkActiveLockKey(topic), () -> checkActive(topic));
                    } catch (Exception e) {
                        logger.error("check active topic error, topic = {}", topic, e);
                    }
                }
            }
        }
    }

    private static final String ZREM_LPUSH_SCRIPT = "local k = tonumber(ARGV[1]) + 1 \n" +
            "for i = 2, k\n" +
            "do\n" +
            "redis.call('zrem', KEYS[1], ARGV[i])\n" +
            "redis.call('lpush', KEYS[2], ARGV[i])\n" +
            "end";
    //扫描即将到期的消息到ready队列
    private void checkTriggerMsg(String topic) {
        if (logger.isDebugEnabled()) {
            logger.debug("checkTriggerMsg, topic = {}", topic);
        }
        String waitingQueueKey = waitingQueueKey(topic);
        String readyQueueKey = readyQueueKey(topic);
        while (true) {
            Set<String> toTriggerMsgIdSet = template.zrangeByScore(waitingQueueKey, 0, System.currentTimeMillis() + 100, 0, 100);
            if (toTriggerMsgIdSet.isEmpty()) {
                return;
            }
            MsgCheckStatusResult result = checkMsgLife(topic, toTriggerMsgIdSet, false);
            //生命周期已经结束的消息直接删除
            if (!result.endLifeMsgMap.isEmpty()) {
                active(topic);
                template.zrem(waitingQueueKey, result.endLifeMsgMap.keySet().toArray(new String[0]));
            }
            //正常的消息扔到ready队列准备被消费
            if (!result.inLifeMsgMap.isEmpty()) {
                active(topic);
                //使用lua脚本确保原子性
                List<String> keys = new ArrayList<>(2);
                keys.add(waitingQueueKey);
                keys.add(readyQueueKey);
                List<String> args = new ArrayList<>(result.inLifeMsgMap.size() + 1);
                args.add(String.valueOf(result.inLifeMsgMap.size()));
                args.addAll(result.inLifeMsgMap.keySet());
                template.eval(ZREM_LPUSH_SCRIPT, keys, args);
                CamelliaDelayQueueMonitor.triggerMsgReady(topic, result.inLifeMsgMap);
            }
        }
    }

    //扫描正在消费的消息，取出timeout的去重试
    private void checkTimeoutMsg(String topic) {
        if (logger.isDebugEnabled()) {
            logger.debug("checkTimeoutMsg, topic = {}", topic);
        }
        String ackQueueKey = ackQueueKey(topic);
        String readyQueueKey = readyQueueKey(topic);
        while (true) {
            //扫描消费过期的消息
            Set<String> acKMsgIdSet = template.zrangeByScore(ackQueueKey, 0, System.currentTimeMillis(), 0, 100);
            if (acKMsgIdSet.isEmpty()) {
                return;
            }
            //校验消息生命周期
            MsgCheckStatusResult result = checkMsgLife(topic, acKMsgIdSet, false);
            //生命周期已经结束的消息直接删除
            if (!result.endLifeMsgMap.isEmpty()) {
                active(topic);
                template.zrem(ackQueueKey, result.endLifeMsgMap.keySet().toArray(new String[0]));
            }
            //正常的消息扔回ready队列重试
            if (!result.inLifeMsgMap.isEmpty()) {
                active(topic);
                //使用lua脚本确保原子性
                List<String> keys = new ArrayList<>(2);
                keys.add(ackQueueKey);
                keys.add(readyQueueKey);
                List<String> args = new ArrayList<>(result.inLifeMsgMap.size() + 1);
                args.add(String.valueOf(result.inLifeMsgMap.size()));
                args.addAll(result.inLifeMsgMap.keySet());
                template.eval(ZREM_LPUSH_SCRIPT, keys, args);
                CamelliaDelayQueueMonitor.triggerMsgTimeout(topic, result.inLifeMsgMap);
            }
        }
    }

    private void checkExpireMsg(String topic) {
        if (logger.isDebugEnabled()) {
            logger.debug("checkExpireMsg, topic = {}", topic);
        }
        String readyQueueKey = readyQueueKey(topic);
        Long len = template.llen(readyQueueKey);
        if (len == 0) return;
        for (int i=0; i<len; i+=100) {
            List<String> readyMsgIdSet = template.lrange(readyQueueKey, i, i + 99);
            //校验消息生命周期
            MsgCheckStatusResult result = checkMsgLife(topic, readyMsgIdSet, false);
            //生命周期已经结束的消息直接删除
            if (!result.endLifeMsgMap.isEmpty()) {
                active(topic);
                for (String msgId : result.endLifeMsgMap.keySet()) {
                    template.lrem(readyQueueKey, 0, msgId);
                }
            }
        }
    }

    private void checkActive(String topic) {
        if (logger.isDebugEnabled()) {
            logger.debug("checkActive, topic = {}", topic);
        }
        boolean active;
        String activeTagKey = activeTagKey(topic);
        String lastActiveTimeV = template.get(activeTagKey);
        if (lastActiveTimeV != null) {
            long lastActiveTime = Long.parseLong(lastActiveTimeV);
            active = System.currentTimeMillis() - lastActiveTime > serverConfig.getTopicActiveTagTimeoutMillis();
        } else {
            active = false;
        }
        if (!active) {
            String waitingQueueKey = waitingQueueKey(topic);
            active = template.zcard(waitingQueueKey) > 0;
        }
        if (!active) {
            String ackQueueKey = ackQueueKey(topic);
            active = template.zcard(ackQueueKey) > 0;
        }
        if (!active) {
            String readyQueueKey = readyQueueKey(topic);
            active = template.llen(readyQueueKey) > 0;
        }
        if (!active) {
            Double zscore = template.zscore(topicsKey(), topic);
            if (zscore != null) {
                //如果不活跃了，则先移动到topicsKeyTmp，此时不影响消息收发
                template.zadd(topicsKeyTmp(), System.currentTimeMillis(), topic);
                template.zrem(topicsKey(), topic);
                CamelliaDelayQueueMonitor.checkTopicInactive(topic);
                template.psetex(inactiveTag(topic), serverConfig.getTopicScheduleSeconds()*1000L*4, String.valueOf(System.currentTimeMillis()));
            } else {
                String inactiveTime = template.get(inactiveTag(topic));
                if (inactiveTime == null) {
                    template.psetex(inactiveTag(topic), serverConfig.getTopicScheduleSeconds()*1000L*4, String.valueOf(System.currentTimeMillis()));
                } else {
                    //如果下一次仍然不活跃，则从topicsKeyTmp中移除，此时彻底删除
                    if (System.currentTimeMillis() - Long.parseLong(inactiveTime) > serverConfig.getTopicScheduleSeconds()*1000L) {
                        template.zrem(topicsKeyTmp(), topic);
                        CamelliaDelayQueueMonitor.checkTopicRemove(topic);
                    }
                }
            }
        } else {
            //如果活跃，则扔回topicsKey
            template.zadd(topicsKey(), System.currentTimeMillis(), topic);
            //如果活跃，则topicsKeyTmp中不应该存在
            template.zrem(topicsKeyTmp(), topic);
            //如果活跃，则删除不活跃标记
            template.del(inactiveTag(topic));
        }
    }

    private static final String ACTIVE_TIME_TAG = "active_time";
    private final CamelliaLocalCache activeTimeMap = new CamelliaLocalCache();
    private void active(String topic) {
        //加个本地缓存，避免每次都写redis
        Long activeTime = activeTimeMap.get(ACTIVE_TIME_TAG, topic, Long.class);
        if (activeTime != null) {
            return;
        }
        String activeTagKey = activeTagKey(topic);
        template.psetex(activeTagKey, serverConfig.getTopicActiveTagTimeoutMillis() * 3, String.valueOf(System.currentTimeMillis()));
        activeTimeMap.put(ACTIVE_TIME_TAG, topic, System.currentTimeMillis(), 30);
    }

    private static class MsgCheckStatusResult {
        Map<String, CamelliaDelayMsg> endLifeMsgMap = new HashMap<>();
        Map<String, CamelliaDelayMsg> inLifeMsgMap = new HashMap<>();
    }

    private MsgCheckStatusResult checkMsgLife(String topic, Collection<String> msgIdSet, boolean pullMsg) {
        MsgCheckStatusResult result = new MsgCheckStatusResult();
        List<String> keys = new ArrayList<>();
        List<String> msgIdList = new ArrayList<>();
        for (String msgId : msgIdSet) {
            keys.add(msgKey(topic, msgId));
            msgIdList.add(msgId);
        }
        List<String> list = template.mget(keys.toArray(new String[0]));
        for (int i=0; i<list.size(); i++) {
            String msg = list.get(i);
            String msgId = msgIdList.get(i);
            //消息不存在
            if (msg == null) {
                result.endLifeMsgMap.put(msgId, null);
                if (logger.isDebugEnabled()) {
                    logger.debug("[MSG_LIFE_END] topic = {}, msgId = {} missing", topic, msgId);
                }
                continue;
            }
            //消息不存在
            CamelliaDelayMsg delayMsg = JSONObject.parseObject(msg, CamelliaDelayMsg.class);
            if (delayMsg == null) {
                result.endLifeMsgMap.put(msgId, null);
                if (logger.isDebugEnabled()) {
                    logger.debug("[MSG_LIFE_END] topic = {}, msgId = {} not found", topic, msgId);
                }
                continue;
            }
            //消息状态异常，或者已经是终态
            CamelliaDelayMsgStatus status = CamelliaDelayMsgStatus.getByValue(delayMsg.getStatus());
            if (status == null || status.isEndLife()) {
                result.endLifeMsgMap.put(delayMsg.getMsgId(), delayMsg);
                if (logger.isDebugEnabled()) {
                    logger.debug("[MSG_LIFE_END] topic = {}, msgId = {} for status = {}", topic, msgId, status);
                }
                continue;
            }
            //消息已经过期了
            if (delayMsg.getExpireTime() < System.currentTimeMillis()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("[MSG_LIFE_END] topic = {}, msgId = {} expire, expireTime = {}", topic, msgId, delayMsg.getExpireTime());
                }
                if (delayMsg.getRetry() == 0) {
                    delayMsg.setStatus(CamelliaDelayMsgStatus.EXPIRE.getValue());
                } else {
                    delayMsg.setStatus(CamelliaDelayMsgStatus.RETRY_EXHAUST.getValue());
                }
                saveOrUpdateMsg(delayMsg, false, true);
                result.endLifeMsgMap.put(delayMsg.getMsgId(), delayMsg);
                continue;
            }
            if (pullMsg) {
                int retry = delayMsg.getRetry();
                //如果超过了最大重试次数，则设置为RETRY_EXHAUST
                if (retry > delayMsg.getMaxRetry()) {
                    delayMsg.setStatus(CamelliaDelayMsgStatus.RETRY_EXHAUST.getValue());
                    saveOrUpdateMsg(delayMsg, false, true);
                    result.endLifeMsgMap.put(delayMsg.getMsgId(), delayMsg);
                    continue;
                }
                //设置为CONSUMING，并返回
                delayMsg.setStatus(CamelliaDelayMsgStatus.CONSUMING.getValue());
                delayMsg.setRetry(retry + 1);
                saveOrUpdateMsg(delayMsg, false, false);
            }
            result.inLifeMsgMap.put(delayMsg.getMsgId(), delayMsg);
        }
        if (!result.endLifeMsgMap.isEmpty()) {
            CamelliaDelayQueueMonitor.triggerMsgEndLife(topic, result.endLifeMsgMap);
        }
        return result;
    }

    private boolean saveOrUpdateMsg(CamelliaDelayMsg delayMsg, boolean checkExists, boolean endLife) {
        long now = System.currentTimeMillis();
        String msgKey = msgKey(delayMsg.getTopic(), delayMsg.getMsgId());
        if (checkExists) {
            String set = template.set(msgKey, JSONObject.toJSONString(delayMsg), "NX", "PX", delayMsg.getExpireTime() - now + serverConfig.getEndLifeMsgExpireMillis() * 3);
            return set != null && set.equalsIgnoreCase("ok");
        } else {
            if (!endLife) {
                template.psetex(msgKey, delayMsg.getExpireTime() - now + serverConfig.getEndLifeMsgExpireMillis() * 3, JSONObject.toJSONString(delayMsg));
            } else {
                template.psetex(msgKey, serverConfig.getEndLifeMsgExpireMillis(), JSONObject.toJSONString(delayMsg));
            }
            return true;
        }
    }

    private String genMsgId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    //string
    private String msgKey(String topic, String msgId) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_msg", serverConfig.getNamespace(), topic, msgId);
    }

    //zset，设置hashTag，确保redis-cluster模式下归属于同一个slot
    private String waitingQueueKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_waiting", "{", serverConfig.getNamespace(), topic, "}");
    }

    //list，设置hashTag，确保redis-cluster模式下归属于同一个slot
    private String readyQueueKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_ready", "{", serverConfig.getNamespace(), topic, "}");
    }

    //zset，设置hashTag，确保redis-cluster模式下归属于同一个slot
    private String ackQueueKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_ack", "{", serverConfig.getNamespace(), topic, "}");
    }

    //zset，用于记录活跃topic
    private String topicsKey() {
        return CacheUtil.buildCacheKey("camellia_delay_queue_topics", serverConfig.getNamespace());
    }

    //zset，用于记录清理非活跃topic时的临时存储
    private String topicsKeyTmp() {
        return CacheUtil.buildCacheKey("camellia_delay_queue_topics_tmp", serverConfig.getNamespace());
    }

    //string
    private String activeTagKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_active_tag", serverConfig.getNamespace(), topic);
    }

    //string
    public String checkExpireLockKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_check_expire", serverConfig.getNamespace(), topic, "~lock");
    }

    //string
    public String checkTriggerLockKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_check_trigger", serverConfig.getNamespace(), topic, "~lock");
    }

    //string
    public String checkTimeoutLockKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_check_timeout", serverConfig.getNamespace(), topic, "~lock");
    }

    //string
    public String checkActiveLockKey(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_check_active", serverConfig.getNamespace(), topic, "~lock");
    }

    //string
    private String inactiveTag(String topic) {
        return CacheUtil.buildCacheKey("camellia_delay_queue_inactive_tag", topic);
    }
}

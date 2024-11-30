package com.netease.nim.camellia.mq.isolation.controller.service;

import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2024/2/20
 */
@Service
public class HeartbeatService {

    private static final String sender_heartbeat = "sender_heartbeat";
    private static final String consumer_heartbeat = "consumer_heartbeat";
    private static final String mq_info_heartbeat = "mq_info_heartbeat";

    @Autowired
    private CamelliaRedisTemplate template;

    public void senderHeartbeat(SenderHeartbeat heartbeat) {
        String key = CacheUtil.buildCacheKey(sender_heartbeat, heartbeat.getNamespace());
        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            pipeline.zadd(key, heartbeat.getTimestamp(), heartbeat.getInstanceId() + "|" + heartbeat.getHost());
            pipeline.zremrangeByScore(key, 0, System.currentTimeMillis() - 60*1000);
            pipeline.expire(key, 60);
            pipeline.sync();
        }
    }

    public List<SenderHeartbeat> querySenderHeartbeat(String namespace) {
        String key = CacheUtil.buildCacheKey(sender_heartbeat, namespace);
        template.zremrangeByScore(key, 0, System.currentTimeMillis() - 60*1000);
        Set<Tuple> tuples = template.zrangeWithScores(key, 0, -1);
        List<SenderHeartbeat> list = new ArrayList<>();
        for (Tuple tuple : tuples) {
            SenderHeartbeat heartbeat = new SenderHeartbeat();
            heartbeat.setNamespace(namespace);
            heartbeat.setTimestamp((long)tuple.getScore());
            String[] split = tuple.getElement().split("\\|");
            heartbeat.setInstanceId(split[0]);
            heartbeat.setHost(split[1]);
            list.add(heartbeat);
        }
        return list;
    }

    public void consumerHeartbeat(ConsumerHeartbeat heartbeat) {
        String key = CacheUtil.buildCacheKey(consumer_heartbeat, heartbeat.getNamespace());
        try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
            pipeline.zadd(key, heartbeat.getTimestamp(), heartbeat.getInstanceId() + "|" + heartbeat.getHost() + "|" + heartbeat.getMqInfo().toString());
            pipeline.zremrangeByScore(key, 0, System.currentTimeMillis() - 60*1000);
            pipeline.expire(key, 60);
            String mqInfoKey = CacheUtil.buildCacheKey(mq_info_heartbeat, heartbeat.getMqInfo().toString());
            pipeline.setex(mqInfoKey, 60, "1");
            pipeline.sync();
        }
    }

    public boolean isActive(MqInfo mqInfo) {
        String mqInfoKey = CacheUtil.buildCacheKey(mq_info_heartbeat, mqInfo.toString());
        return template.exists(mqInfoKey);
    }

    public List<ConsumerHeartbeat> queryConsumerHeartbeat(String namespace) {
        String key = CacheUtil.buildCacheKey(consumer_heartbeat, namespace);
        template.zremrangeByScore(key, 0, System.currentTimeMillis() - 60*1000);
        Set<Tuple> tuples = template.zrangeWithScores(key, 0, -1);
        List<ConsumerHeartbeat> list = new ArrayList<>();
        for (Tuple tuple : tuples) {
            ConsumerHeartbeat heartbeat = new ConsumerHeartbeat();
            heartbeat.setNamespace(namespace);
            heartbeat.setTimestamp((long)tuple.getScore());
            String[] split = tuple.getElement().split("\\|");
            heartbeat.setInstanceId(split[0]);
            heartbeat.setHost(split[1]);
            heartbeat.setMqInfo(MqInfo.byString(split[2]));
            list.add(heartbeat);
        }
        return list;
    }
}

package com.netease.nim.camellia.mq.isolation.controller.service;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CacheUtil;
import com.netease.nim.camellia.mq.isolation.core.config.*;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStats;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.pipeline.ICamelliaRedisPipeline;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 * Created by caojiajun on 2024/2/6
 */
@Service
public class RouteService {

    private static final Logger logger = LoggerFactory.getLogger(RouteService.class);

    private static final String sender_instance_list = "sender_instance_list";
    private static final String sender_stats = "sender_stats";
    private static final String consumer_instance_list = "consumer_instance_list";
    private static final String consumer_stats = "consumer_stats";

    @Autowired
    private CamelliaRedisTemplate template;

    @Autowired
    private ConfigServiceWrapper configServiceWrapper;

    @Autowired
    private HeartbeatService heartbeatService;

    private final CamelliaLocalCache cache = new CamelliaLocalCache();

    public List<MqInfo> selectMqInfo(String namespace, String bizId) {
        CamelliaLocalCache.ValueWrapper valueWrapper = cache.get(namespace, bizId);
        if (valueWrapper != null) {
            return (List<MqInfo>) valueWrapper.get();
        } else {
            List<MqInfo> mqInfos = selectMqInfo0(namespace, bizId);
            cache.put(namespace, bizId, mqInfos, 1);
            return mqInfos;
        }
    }

    private List<MqInfo> selectMqInfo0(String namespace, String bizId) {
        //get config
        MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);

        try {
            //check manual config
            List<ManualConfig> manualConfigs = config.getManualConfigs();
            if (manualConfigs != null && !manualConfigs.isEmpty()) {
                for (ManualConfig manualConfig : manualConfigs) {
                    MatchType matchType = manualConfig.getMatchType();
                    if (matchType == MatchType.exact_match) {
                        if (manualConfig.getBizId().equals(bizId)) {
                            if (heartbeatService.isActive(manualConfig.getMqInfo())) {
                                return Collections.singletonList(manualConfig.getMqInfo());
                            }
                        }
                    } else if (matchType == MatchType.prefix_match) {
                        if (bizId.startsWith(manualConfig.getBizId())) {
                            if (heartbeatService.isActive(manualConfig.getMqInfo())) {
                                return Collections.singletonList(manualConfig.getMqInfo());
                            }
                        }
                    }
                }
            }

            //check sender stats
            int senderHeavyTrafficThreshold1 = config.getSenderHeavyTrafficThreshold1();
            int senderHeavyTrafficThreshold2 = config.getSenderHeavyTrafficThreshold2();
            double senderHeavyTrafficPercent = config.getSenderHeavyTrafficPercent();

            Map<Long, List<SenderBizStats>> senderStatsMap = querySenderStats(namespace, bizId);

            int dataSize = 0;
            int senderHeavyTrafficCount = 0;
            for (Map.Entry<Long, List<SenderBizStats>> entry : senderStatsMap.entrySet()) {
                //说明数据可能不完整，跳过
                if (System.currentTimeMillis() - entry.getKey() < config.getSenderStatsIntervalSeconds() * 1000L) {
                    continue;
                }
                List<SenderBizStats> subList = entry.getValue();
                long count = 0;
                for (SenderBizStats stats : subList) {
                    count += stats.getCount();
                }
                //最近一个周期如果超过一个较大阈值，则直接自动隔离
                //说明突发流量
                if (count > senderHeavyTrafficThreshold1) {
                    List<MqInfo> mqInfos = checkActive(config.getAutoIsolationLevel0());
                    if (mqInfos != null) {
                        return mqInfos;
                    }
                }
                if (count > senderHeavyTrafficThreshold2) {
                    senderHeavyTrafficCount ++;
                }
                dataSize ++;
            }
            //如果最近n个周期里有超过一定比例的超过较小阈值，则自动隔离
            //说明持续高水位
            if (senderHeavyTrafficCount > dataSize * senderHeavyTrafficPercent) {
                List<MqInfo> mqInfos = checkActive(config.getAutoIsolationLevel0());
                if (mqInfos != null) {
                    return mqInfos;
                }
            }

            //check consumer stats
            double failRateThreshold = config.getConsumerFailRateThreshold();

            long success = 0;
            long fail = 0;
            double spendMsAll = 0;
            Map<Long, List<ConsumerBizStats>> consumerStatsMap = queryConsumerStats(namespace, bizId);
            for (Map.Entry<Long, List<ConsumerBizStats>> entry : consumerStatsMap.entrySet()) {
                //说明数据可能不完整
                if (System.currentTimeMillis() - entry.getKey() < config.getConsumerStatsIntervalSeconds() * 1000L) {
                    continue;
                }
                List<ConsumerBizStats> subList = entry.getValue();

                for (ConsumerBizStats stats : subList) {
                    success += stats.getSuccess();
                    fail += stats.getFail();
                    spendMsAll += stats.getSpendAvg() * (stats.getSuccess() + stats.getFail());
                }
            }
            //没有统计数据，则走默认
            if (success + fail <= 0) {
                return config.getLevelInfoList().get(0).getMqInfoList();
            }
            double failRate = fail / (1.0 * (success + fail));
            double spendMsAvg = spendMsAll / (success + fail);
            boolean error = failRate >= failRateThreshold;
            List<MqInfo> mqInfoList;
            if (error) {
                //错误序列
                mqInfoList = checkTimeRange(config.getErrorLevelInfoList(), spendMsAvg);
            } else {
                mqInfoList = checkTimeRange(config.getLevelInfoList(), spendMsAvg);
            }
            if (mqInfoList != null) {
                return mqInfoList;
            }
            //兜底
            return config.getLevelInfoList().get(0).getMqInfoList();
        } catch (Exception e) {
            List<MqInfo> mqInfoList = config.getLevelInfoList().get(0).getMqInfoList();
            logger.error("select mq info error, namespace = {}, bizId = {}, return default config = {}", namespace, bizId, mqInfoList);
            return mqInfoList;
        }
    }

    private List<MqInfo> checkTimeRange(List<MqLevelInfo> mqLevelInfoList, double spendMsAvg) {
        for (MqLevelInfo mqLevelInfo : mqLevelInfoList) {
            TimeRange timeRange = mqLevelInfo.getTimeRange();
            if (spendMsAvg >= timeRange.getMin() && spendMsAvg < timeRange.getMax()) {
                List<MqInfo> mqInfoList = mqLevelInfo.getMqInfoList();
                return checkActive(mqInfoList);
            }
        }
        return null;
    }

    private List<MqInfo> checkActive(List<MqInfo> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<MqInfo> ret = new ArrayList<>();
        for (MqInfo mqInfo : list) {
            if (heartbeatService.isActive(mqInfo)) {
                ret.add(mqInfo);
            }
        }
        if (ret.isEmpty()) {
            return null;
        }
        return ret;
    }

    public void reportConsumerBizStats(ConsumerBizStatsRequest request) {
        List<ConsumerBizStats> list = request.getList();
        if (list == null) return;
        String instanceId = request.getInstanceId();
        for (ConsumerBizStats consumerBizStats : list) {
            String namespace = consumerBizStats.getNamespace();
            long timestamp = consumerBizStats.getTimestamp();
            MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
            int expireSeconds = config.getConsumerStatsExpireSeconds();
            String bizId = consumerBizStats.getBizId();

            String key = CacheUtil.buildCacheKey(consumer_stats, namespace, bizId, instanceId);
            String instanceKey = CacheUtil.buildCacheKey(consumer_instance_list, namespace, bizId);
            try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
                //instance
                pipeline.zadd(instanceKey, timestamp, request.getInstanceId());
                pipeline.zremrangeByScore(instanceKey, 0, System.currentTimeMillis() - expireSeconds * 1000L);
                pipeline.expire(instanceKey, expireSeconds * 2);
                //stats
                pipeline.zadd(key, timestamp, JSONObject.toJSONString(consumerBizStats));
                pipeline.zremrangeByScore(key, 0, System.currentTimeMillis() - expireSeconds * 1000L);
                pipeline.expire(key, expireSeconds * 2);
                pipeline.sync();
            }
        }
    }

    private Map<Long, List<ConsumerBizStats>> queryConsumerStats(String namespace, String bizId) {
        MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
        int expireSeconds = config.getConsumerStatsExpireSeconds();
        String instanceKey = CacheUtil.buildCacheKey(consumer_instance_list, namespace, bizId);
        template.zremrangeByScore(instanceKey, 0, System.currentTimeMillis() - expireSeconds * 1000L);
        Set<String> set = template.zrange(instanceKey, 0, -1);
        Map<Long, List<ConsumerBizStats>> map = new HashMap<>();
        for (String instanceId : set) {
            String key = CacheUtil.buildCacheKey(consumer_stats, namespace, bizId, instanceId);
            template.zremrangeByScore(key, 0, System.currentTimeMillis() - expireSeconds * 1000L);
            Set<String> statsSet = template.zrange(key, 0, -1);
            for (String str : statsSet) {
                ConsumerBizStats stats = JSONObject.parseObject(str, ConsumerBizStats.class);
                List<ConsumerBizStats> list = map.computeIfAbsent(stats.getTimestamp(), t -> new ArrayList<>());
                list.add(stats);
            }
        }
        return map;
    }

    public void reportSenderBizStats(SenderBizStatsRequest request) {
        List<SenderBizStats> list = request.getList();
        if (list == null) return;
        String instanceId = request.getInstanceId();
        for (SenderBizStats senderBizStats : list) {
            String namespace = senderBizStats.getNamespace();
            long timestamp = senderBizStats.getTimestamp();
            MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
            int expireSeconds = config.getSenderStatsExpireSeconds();

            String bizId = senderBizStats.getBizId();
            String key = CacheUtil.buildCacheKey(sender_stats, namespace, bizId, instanceId);
            String instanceKey = CacheUtil.buildCacheKey(sender_instance_list, namespace, bizId);
            try (ICamelliaRedisPipeline pipeline = template.pipelined()) {
                //instance
                pipeline.zadd(instanceKey, timestamp, request.getInstanceId());
                pipeline.zremrangeByScore(instanceKey, 0, System.currentTimeMillis() - expireSeconds * 1000L);
                pipeline.expire(instanceKey, expireSeconds * 2);
                //stats
                pipeline.zadd(key, timestamp, JSONObject.toJSONString(senderBizStats));
                pipeline.zremrangeByScore(key, 0, System.currentTimeMillis() - expireSeconds * 1000L);
                pipeline.expire(key, expireSeconds * 2);
                pipeline.sync();
            }
        }
    }

    private Map<Long, List<SenderBizStats>> querySenderStats(String namespace, String bizId) {
        MqIsolationConfig config = configServiceWrapper.getMqIsolationConfig(namespace);
        int expireSeconds = config.getSenderStatsExpireSeconds();
        String instanceKey = CacheUtil.buildCacheKey(sender_instance_list, namespace, bizId);
        template.zremrangeByScore(instanceKey, 0, System.currentTimeMillis() - expireSeconds * 1000L);
        Set<String> set = template.zrange(instanceKey, 0, -1);
        Map<Long, List<SenderBizStats>> map = new HashMap<>();
        for (String instanceId : set) {
            String key = CacheUtil.buildCacheKey(sender_stats, namespace, bizId, instanceId);
            template.zremrangeByScore(key, 0, System.currentTimeMillis() - expireSeconds * 1000L);
            Set<String> statsSet = template.zrange(key, 0, -1);
            for (String str : statsSet) {
                SenderBizStats stats = JSONObject.parseObject(str, SenderBizStats.class);
                List<SenderBizStats> list = map.computeIfAbsent(stats.getTimestamp(), t -> new ArrayList<>());
                list.add(stats);
            }
        }
        return map;
    }
}

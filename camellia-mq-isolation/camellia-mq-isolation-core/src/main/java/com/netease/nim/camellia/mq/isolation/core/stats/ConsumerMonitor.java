package com.netease.nim.camellia.mq.isolation.core.stats;

import com.netease.nim.camellia.mq.isolation.core.executor.MsgHandlerResult;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.*;
import com.netease.nim.camellia.mq.isolation.core.mq.TopicType;
import com.netease.nim.camellia.mq.isolation.stats.model.*;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2024/2/7
 */
public class ConsumerMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerMonitor.class);

    private static final CamelliaStatisticsManager topicTypeMqLatency = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager mqInfoMqLatency = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager namespaceMsgLatency = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager namespaceBizIdMsgLatency = new CamelliaStatisticsManager();

    private static final CamelliaStatisticsManager namespaceSpend = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager namespaceBizIdSpend = new CamelliaStatisticsManager();

    private static final AtomicBoolean initOk = new AtomicBoolean(false);

    public static void init(int intervalSeconds) {
        if (initOk.compareAndSet(false, true)) {
            Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-mq-isolation-consumer-monitor"))
                    .scheduleAtFixedRate(ConsumerMonitor::calc, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            logger.info("camellia mq isolation consumer monitor init success");
        }
    }

    private static ConsumerStats consumerStats = new ConsumerStats();

    //获取stats
    public static ConsumerStats getStats() {
        return consumerStats;
    }

    //延迟监控
    public static void latency(MqInfo mqInfo, TopicType topicType, String namespace, String bizId, long mqLatency, long msgLatency) {
        try {
            mqInfoMqLatency.update(mqInfo.toString(), mqLatency);
            topicTypeMqLatency.update(namespace + "|" + topicType.name(), mqLatency);
            namespaceMsgLatency.update(namespace, msgLatency);
            namespaceBizIdMsgLatency.update(new BizKey(namespace, bizId).toString(), msgLatency);
        } catch (Exception e) {
            logger.error("latency error", e);
        }
    }

    //耗时监控
    public static void spend(String namespace, String bizId, MsgHandlerResult result, long spendMs) {
        try {
            namespaceSpend.update(namespace + "|" + result.getValue(), spendMs);
            namespaceBizIdSpend.update(new BizResultKey(namespace, bizId, result).toString(), spendMs);
        } catch (Exception e) {
            logger.error("spend error", e);
        }
    }

    private static void calc() {
        try {
            ConsumerStats consumerStats = new ConsumerStats();

            LatencyStats latencyStats = new LatencyStats();
            {
                Map<String, CamelliaStatsData> map = topicTypeMqLatency.getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
                    String key = entry.getKey();
                    int index = key.lastIndexOf("|");
                    String namespace = key.substring(0, index);
                    TopicType topicType = TopicType.valueOf(key.substring(index + 1));
                    TopicTypeLatencyStats stats = new TopicTypeLatencyStats();
                    stats.setNamespace(namespace);
                    stats.setTopicType(topicType);
                    CamelliaStatsData data = entry.getValue();
                    stats.setCount(data.getCount());
                    stats.setAvg(data.getCount());
                    stats.setMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    latencyStats.getTopicTypeLatencyStatsList().add(stats);
                }
            }
            {
                Map<String, CamelliaStatsData> map = mqInfoMqLatency.getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
                    MqInfo mqInfo = MqInfo.byString(entry.getKey());
                    MqInfoLatency stats = new MqInfoLatency();
                    stats.setMqInfo(mqInfo);
                    CamelliaStatsData data = entry.getValue();
                    stats.setCount(data.getCount());
                    stats.setAvg(data.getCount());
                    stats.setMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    latencyStats.getMqInfoLatencyList().add(stats);
                }
            }

            {
                Map<String, CamelliaStatsData> map = namespaceBizIdMsgLatency.getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
                    NamespaceBizIdMsgLatencyStats stats = new NamespaceBizIdMsgLatencyStats();
                    stats.setNamespace(entry.getKey());
                    CamelliaStatsData data = entry.getValue();
                    stats.setCount(data.getCount());
                    stats.setAvg(data.getCount());
                    stats.setMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    latencyStats.getNamespaceBizIdMsgLatencyStatsList().add(stats);
                }
            }

            {
                Map<String, CamelliaStatsData> map = namespaceMsgLatency.getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
                    BizKey bizKey = BizKey.byString(entry.getKey());
                    NamespaceBizIdMsgLatencyStats stats = new NamespaceBizIdMsgLatencyStats();
                    stats.setNamespace(bizKey.getNamespace());
                    stats.setBizId(bizKey.getBidId());
                    CamelliaStatsData data = entry.getValue();
                    stats.setCount(data.getCount());
                    stats.setAvg(data.getCount());
                    stats.setMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    latencyStats.getNamespaceBizIdMsgLatencyStatsList().add(stats);
                }
            }
            consumerStats.setLatencyStats(latencyStats);

            SpendStats spendStats = new SpendStats();
            {
                Map<String, CamelliaStatsData> map = namespaceSpend.getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {

                    String key = entry.getKey();
                    int index = key.lastIndexOf("|");
                    String namespace = key.substring(0, index);
                    MsgHandlerResult result = MsgHandlerResult.byValue(Integer.parseInt(key.substring(index + 1)));
                    NamespaceSpendStats stats = new NamespaceSpendStats();
                    stats.setNamespace(namespace);
                    stats.setResult(result);
                    CamelliaStatsData data = entry.getValue();
                    stats.setCount(data.getCount());
                    stats.setAvg(data.getCount());
                    stats.setMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    spendStats.getSpendStatsList().add(stats);
                }
            }
            {
                Map<String, CamelliaStatsData> map = namespaceBizIdSpend.getStatsDataAndReset();
                for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
                    BizResultKey bizResultKey = BizResultKey.byString(entry.getKey());
                    NamespaceBizIdSpendStats stats = new NamespaceBizIdSpendStats();
                    stats.setNamespace(bizResultKey.getNamespace());
                    stats.setBizId(bizResultKey.getBidId());
                    stats.setResult(bizResultKey.getResult());
                    CamelliaStatsData data = entry.getValue();
                    stats.setCount(data.getCount());
                    stats.setAvg(data.getCount());
                    stats.setMax(data.getMax());
                    stats.setP50(data.getP50());
                    stats.setP90(data.getP90());
                    stats.setP99(data.getP99());
                    spendStats.getBizIdSpendStatsList().add(stats);
                }
            }
            consumerStats.setSpendStats(spendStats);

            ConsumerMonitor.consumerStats = consumerStats;
        } catch (Exception e) {
            logger.error("mq isolation consumer monitor calc error", e);
        }
    }
}

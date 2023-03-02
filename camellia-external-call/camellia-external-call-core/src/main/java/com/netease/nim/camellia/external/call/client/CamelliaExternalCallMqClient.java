package com.netease.nim.camellia.external.call.client;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.external.call.common.*;
import com.netease.nim.camellia.tools.cache.CamelliaLoadingCache;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 这是一个基于mq来做自动租户隔离的外部访问系统的客户端
 *
 * 业务任务生产方调用submit方法提交任务，消费端由CamelliaExternalCallConsumer处理
 *
 * Created by caojiajun on 2023/2/24
 */
public class CamelliaExternalCallMqClient<R> implements ICamelliaExternalCallMqClient<R> {

    private final String instanceId = UUID.randomUUID().toString().replaceAll("-", "");

    private final CamelliaLoadingCache<String, ExternalCallSelectInfo> cache = new CamelliaLoadingCache.Builder<String, ExternalCallSelectInfo>()
            .initialCapacity(1024)
            .maxCapacity(10240)
            .build(new CamelliaLoadingCache.CacheLoader<String, ExternalCallSelectInfo>() {
                @Override
                public ExternalCallSelectInfo load(String key) {
                    return controller.select(key);
                }
            });

    private final CamelliaStatisticsManager manager = new CamelliaStatisticsManager();

    private final CamelliaExternalCallMqClientConfig<R> clientConfig;
    private final MqSender mqSender;
    private final CamelliaExternalCallRequestSerializer<R> serializer;
    private ICamelliaExternalCallController controller;

    public CamelliaExternalCallMqClient(CamelliaExternalCallMqClientConfig<R> clientConfig) {
        this.clientConfig = clientConfig;
        this.mqSender = clientConfig.getMqSender();
        this.serializer = clientConfig.getSerializer();
        clientConfig.getScheduledExecutor().scheduleAtFixedRate(this::schedule, clientConfig.getReportIntervalSeconds(),
                clientConfig.getReportIntervalSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public boolean submit(String isolationKey, R request) {
        MqInfo mqInfo = selectMqInfo(isolationKey);
        if (mqInfo == null) {
            return false;
        }
        stats(isolationKey);
        ExternalCallMqPack pack = new ExternalCallMqPack();
        pack.setIsolationKey(isolationKey);
        pack.setData(serializer.serialize(request));
        pack.setCreateTime(System.currentTimeMillis());
        return mqSender.send(mqInfo, JSONObject.toJSONString(pack).getBytes(StandardCharsets.UTF_8));
    }

    private MqInfo selectMqInfo(String isolationKey) {
        List<MqInfo> mqInfoList = cache.get(isolationKey).getMqInfoList();
        if (mqInfoList == null || mqInfoList.isEmpty()) {
            return null;
        }
        if (mqInfoList.size() == 1) {
            return mqInfoList.get(0);
        }
        int index = ThreadLocalRandom.current().nextInt(mqInfoList.size());
        return mqInfoList.get(index);
    }

    private void stats(String isolationKey) {
        manager.update(isolationKey, 1);
    }

    private void schedule() {
        ExternalCallInputStats inputStats = new ExternalCallInputStats();
        inputStats.setInstanceId(instanceId);
        inputStats.setNamespace(clientConfig.getNamespace());
        inputStats.setTimestamp(System.currentTimeMillis());
        List<ExternalCallInputStats.Stats> statsList = new ArrayList<>();
        Map<String, CamelliaStatsData> dataMap = manager.getStatsDataAndReset();
        for (Map.Entry<String, CamelliaStatsData> entry : dataMap.entrySet()) {
            String isolationKey = entry.getKey();
            CamelliaStatsData data = entry.getValue();
            ExternalCallInputStats.Stats stats = new ExternalCallInputStats.Stats();
            stats.setIsolationKey(isolationKey);
            stats.setInput(data.getCount());
            statsList.add(stats);
        }
        inputStats.setStatsList(statsList);
        controller.reportInputStats(inputStats);
    }
}

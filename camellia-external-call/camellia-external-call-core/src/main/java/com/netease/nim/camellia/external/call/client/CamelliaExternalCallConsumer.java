package com.netease.nim.camellia.external.call.client;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.external.call.common.*;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicExecutor;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicExecutorConfig;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private final String instanceId = UUID.randomUUID().toString().replaceAll("-", "");

    private final CamelliaExternalCallMqConsumerConfig<R> consumerConfig;

    private final MqSender mqSender;
    private final BizConsumer<R> bizConsumer;
    private final CamelliaExternalCallRequestSerializer<R> serializer;

    private final MqInfoGenerator generator;

    private ICamelliaExternalCallController controller;

    private final CamelliaStatisticsManager manager = new CamelliaStatisticsManager();
    private final CamelliaStatisticsManager successManager = new CamelliaStatisticsManager();
    private final CamelliaStatisticsManager failManager = new CamelliaStatisticsManager();

    private final ConcurrentHashMap<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CamelliaDynamicExecutor> executorMap = new ConcurrentHashMap<>();

    public CamelliaExternalCallConsumer(CamelliaExternalCallMqConsumerConfig<R> consumerConfig) {
        this.consumerConfig = consumerConfig;
        this.mqSender = consumerConfig.getMqSender();
        this.bizConsumer = consumerConfig.getBizConsumer();
        this.serializer = consumerConfig.getSerializer();
        this.generator = consumerConfig.getGenerator();
        consumerConfig.getScheduledExecutor().scheduleAtFixedRate(this::schedule, consumerConfig.getReportIntervalSeconds(),
                consumerConfig.getReportIntervalSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public List<MqInfo> acquireMqInfo() {
        List<MqInfo> list = new ArrayList<>();
        list.addAll(generator.fast());
        list.addAll(generator.fastError());
        list.addAll(generator.slow());
        list.addAll(generator.slowError());
        list.addAll(generator.retry());
        list.addAll(generator.retryHighPriority());
        list.addAll(generator.heavyTraffic());
        list.addAll(generator.isolation());
        list.addAll(generator.degradation());
        return list;
    }

    @Override
    public BizResponse onMsg(MqInfo mqInfo, byte[] data) {
        CamelliaDynamicExecutor executor = selectExecutor(mqInfo);
        try {
            Future<BizResponse> future = executor.submit(() -> invoke(executor, mqInfo, data));
            return future.get();
        } catch (Exception e) {
            logger.error("onMsg error", e);
            return BizResponse.FAIL_NO_RETRY;
        }
    }

    private BizResponse invoke(CamelliaDynamicExecutor currentExecutor, MqInfo mqInfo, byte[] data) {
        ExternalCallMqPack pack = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8), ExternalCallMqPack.class);
        String isolationKey = pack.getIsolationKey();

        if (isDegradation(mqInfo) && System.currentTimeMillis() - pack.getCreateTime() > consumerConfig.getDegradationTimeThreshold()) {
            return BizResponse.FAIL_NO_RETRY;
        }

        // acquire permits
        // same isolationKey should not acquire too many permits
        Semaphore semaphore = getIsolationSemaphore(currentExecutor, isolationKey);
        boolean permission = true;
        if (semaphore != null) {
            permission = semaphore.tryAcquire();
        }
        if (!permission) {
            mqSender.send(selectRetryMqInfo(false), data);
            return BizResponse.FAIL_RETRY;
        }
        //

        try {
            R request = serializer.deserialize(pack.getData());
            long start = System.currentTimeMillis();
            //===biz start
            BizResponse response;
            BizContext context = new BizContext();
            context.setIsolationKey(isolationKey);
            context.setRetry(pack.getRetry());
            try {
                response = bizConsumer.onMsg(context, request);
            } catch (Throwable e) {
                logger.error("biz onMsg error, isolationKey = {}, request = {}", isolationKey, request, e);
                response = BizResponse.FAIL_NO_RETRY;
            }
            //===biz end
            long spendMs = System.currentTimeMillis() - start;

            //stats
            stats(isolationKey, response.isSuccess(), spendMs);
            if (!response.isSuccess() && response.isRetry()) {
                //retry
                pack.setRetry(pack.getRetry() + 1);
                mqSender.send(selectRetryMqInfo(response.isHighPriority()), JSONObject.toJSONString(pack).getBytes(StandardCharsets.UTF_8));
            }
            return response;
        } finally {
            //release the permit
            if (semaphore != null) {
                semaphore.release();
            }
            //
        }
    }

    private boolean isDegradation(MqInfo mqInfo) {
        List<MqInfo> list = generator.degradation();
        if (list.isEmpty()) {
            return false;
        }
        return list.contains(mqInfo);
    }

    private MqInfo selectRetryMqInfo(boolean highPriority) {
        List<MqInfo> list;
        if (highPriority) {
            list = generator.retryHighPriority();
        } else {
            list = generator.retry();
        }
        if (list.isEmpty()) {
            logger.warn("selectRetryMqInfo null");
            return null;
        }
        if (list.size() == 1) {
            return list.get(0);
        } else {
            int index = ThreadLocalRandom.current().nextInt(list.size());
            return list.get(index);
        }
    }

    private CamelliaDynamicExecutor selectExecutor(MqInfo mqInfo) {
        String key = mqInfo.getServer() + "|" + mqInfo.getTopic();
        return CamelliaMapUtils.computeIfAbsent(executorMap, key, k -> {
            CamelliaDynamicExecutorConfig config = new CamelliaDynamicExecutorConfig(k, consumerConfig::getWorkThreadPerTopic, consumerConfig::getWorkThreadPerTopic);
            config.setQueueSize(() -> 10);
            config.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy::new);
            return new CamelliaDynamicExecutor(config);
        });
    }

    private Semaphore getIsolationSemaphore(CamelliaDynamicExecutor executor, String isolationKey) {
        double percent = consumerConfig.getPermitMaxRatio();
        return CamelliaMapUtils.computeIfAbsent(semaphoreMap,
                executor.getName() + "|" + isolationKey, key -> new Semaphore((int)(executor.getMaximumPoolSize() * percent)));
    }

    private void stats(String isolationKey, boolean success, long spendMs) {
        manager.update(isolationKey, spendMs);
        if (success) {
            successManager.update(isolationKey, spendMs);
        } else {
            failManager.update(isolationKey, spendMs);
        }
    }

    private void schedule() {
        ExternalCallConsumeStats inputStats = new ExternalCallConsumeStats();
        inputStats.setInstanceId(instanceId);
        inputStats.setNamespace(consumerConfig.getNamespace());
        List<ExternalCallConsumeStats.Stats> statsList = new ArrayList<>();
        Map<String, CamelliaStatsData> dataMap = manager.getStatsDataAndReset();
        Map<String, CamelliaStatsData> successMap = successManager.getStatsDataAndReset();
        Map<String, CamelliaStatsData> failMap = failManager.getStatsDataAndReset();
        for (Map.Entry<String, CamelliaStatsData> entry : dataMap.entrySet()) {
            String isolationKey = entry.getKey();
            CamelliaStatsData data = entry.getValue();
            ExternalCallConsumeStats.Stats stats = new ExternalCallConsumeStats.Stats();
            stats.setIsolationKey(isolationKey);
            stats.setCount(data.getCount());
            stats.setSpendAvg(data.getAvg());
            stats.setSpendMax(data.getMax());
            stats.setP50(data.getP50());
            stats.setP75(data.getP75());
            stats.setP90(data.getP90());
            stats.setP95(data.getP95());
            stats.setP99(data.getP99());
            stats.setP999(data.getP999());
            CamelliaStatsData successData = successMap.get(isolationKey);
            if (successData != null) {
                stats.setSuccess(successData.getCount());
            }
            CamelliaStatsData failData = failMap.get(isolationKey);
            if (failData != null) {
                stats.setFail(failData.getCount());
            }
            statsList.add(stats);
        }
        inputStats.setStatsList(statsList);
        controller.reportConsumeStats(inputStats);
    }
}

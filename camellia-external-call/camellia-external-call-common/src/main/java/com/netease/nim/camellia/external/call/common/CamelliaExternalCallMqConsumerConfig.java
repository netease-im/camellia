package com.netease.nim.camellia.external.call.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2023/2/28
 */
public class CamelliaExternalCallMqConsumerConfig<R> {

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private String namespace = "default";
    private ScheduledExecutorService scheduledExecutor = scheduledExecutorService;

    private int reportIntervalSeconds = 5;

    private MqSender mqSender;
    private BizConsumer<R> bizConsumer;
    private int workThreadPerTopic = 200;
    private CamelliaExternalCallRequestSerializer<R> serializer;

    private MqInfoGenerator generator;

    private double permitMaxRatio = 0.5;//单个租户最多占用的工作线程的比例

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public void setScheduledExecutor(ScheduledExecutorService scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }

    public int getReportIntervalSeconds() {
        return reportIntervalSeconds;
    }

    public void setReportIntervalSeconds(int reportIntervalSeconds) {
        this.reportIntervalSeconds = reportIntervalSeconds;
    }

    public MqSender getMqSender() {
        return mqSender;
    }

    public void setMqSender(MqSender mqSender) {
        this.mqSender = mqSender;
    }

    public BizConsumer<R> getBizConsumer() {
        return bizConsumer;
    }

    public void setBizConsumer(BizConsumer<R> bizConsumer) {
        this.bizConsumer = bizConsumer;
    }

    public int getWorkThreadPerTopic() {
        return workThreadPerTopic;
    }

    public void setWorkThreadPerTopic(int workThreadPerTopic) {
        this.workThreadPerTopic = workThreadPerTopic;
    }

    public CamelliaExternalCallRequestSerializer<R> getSerializer() {
        return serializer;
    }

    public void setSerializer(CamelliaExternalCallRequestSerializer<R> serializer) {
        this.serializer = serializer;
    }

    public MqInfoGenerator getGenerator() {
        return generator;
    }

    public void setGenerator(MqInfoGenerator generator) {
        this.generator = generator;
    }

    public double getPermitMaxRatio() {
        return permitMaxRatio;
    }

    public void setPermitMaxRatio(double permitMaxRatio) {
        this.permitMaxRatio = permitMaxRatio;
    }
}

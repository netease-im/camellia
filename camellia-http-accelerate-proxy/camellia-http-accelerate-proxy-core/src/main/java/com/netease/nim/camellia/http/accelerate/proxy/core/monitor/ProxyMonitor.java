package com.netease.nim.camellia.http.accelerate.proxy.core.monitor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/7/10
 */
public class ProxyMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ProxyMonitor.class);

    private static final CamelliaStatisticsManager manager1 = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager manager2 = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager manager3 = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager manager4 = new CamelliaStatisticsManager();
    private static final CamelliaStatisticsManager manager5 = new CamelliaStatisticsManager();

    private static JSONObject monitorJson = new JSONObject();

    static {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("proxy-monitor"))
                .scheduleAtFixedRate(ProxyMonitor::calc, 60, 60, TimeUnit.SECONDS);
    }

    public static void updateSpendTime(String host, int code, long spendTime) {
        manager1.update(host + "|" + code, spendTime);
    }

    public static void updateTransportSpendTime1(String host, int code, long transportSpendTime1) {
        manager2.update(host + "|" + code, transportSpendTime1);
    }

    public static void updateUpstreamSpendTime(String host, int code, long upstreamSpendTime) {
        manager3.update(host + "|" + code, upstreamSpendTime);
    }

    public static void updateTransportSpendTime2(String host, int code, long transportSpendTime2) {
        manager4.update(host + "|" + code, transportSpendTime2);
    }

    public static void updateError(String host, ErrorReason reason) {
        manager5.update(host + "|" + reason.getValue(), 1);
    }

    public static JSONObject getMonitorJson() {
        return monitorJson;
    }

    private static void calc() {
        try {
            Map<String, CamelliaStatsData> dataMap1 = manager1.getStatsDataAndReset();
            Map<String, CamelliaStatsData> dataMap2 = manager2.getStatsDataAndReset();
            Map<String, CamelliaStatsData> dataMap3 = manager3.getStatsDataAndReset();
            Map<String, CamelliaStatsData> dataMap4 = manager4.getStatsDataAndReset();
            Map<String, CamelliaStatsData> dataMap5 = manager5.getStatsDataAndReset();

            JSONObject monitorJson = new JSONObject();

            JSONArray spendTime = new JSONArray();
            for (Map.Entry<String, CamelliaStatsData> entry : dataMap1.entrySet()) {
                JSONObject json = new JSONObject();
                CamelliaStatsData data = entry.getValue();
                String[] split = entry.getKey().split("\\|");
                json.put("host", split[0]);
                json.put("code", split[1]);
                json.put("count", data.getCount());
                json.put("avg", data.getAvg());
                json.put("max", data.getMax());
                json.put("p50", data.getP50());
                json.put("p75", data.getP75());
                json.put("p90", data.getP90());
                json.put("p95", data.getP95());
                json.put("p99", data.getP99());
                json.put("p999", data.getP999());
                spendTime.add(json);
            }
            monitorJson.put("spendStats", spendTime);

            JSONArray transportSpendTime1 = new JSONArray();
            for (Map.Entry<String, CamelliaStatsData> entry : dataMap2.entrySet()) {
                JSONObject json = new JSONObject();
                CamelliaStatsData data = entry.getValue();
                String[] split = entry.getKey().split("\\|");
                json.put("host", split[0]);
                json.put("code", split[1]);
                json.put("count", data.getCount());
                json.put("avg", data.getAvg());
                json.put("max", data.getMax());
                json.put("p50", data.getP50());
                json.put("p75", data.getP75());
                json.put("p90", data.getP90());
                json.put("p95", data.getP95());
                json.put("p99", data.getP99());
                json.put("p999", data.getP999());
                transportSpendTime1.add(json);
            }
            monitorJson.put("transportSpendStats", transportSpendTime1);


            JSONArray upstreamSpendTime = new JSONArray();
            for (Map.Entry<String, CamelliaStatsData> entry : dataMap3.entrySet()) {
                JSONObject json = new JSONObject();
                CamelliaStatsData data = entry.getValue();
                String[] split = entry.getKey().split("\\|");
                json.put("host", split[0]);
                json.put("code", split[1]);
                json.put("count", data.getCount());
                json.put("avg", data.getAvg());
                json.put("max", data.getMax());
                json.put("p50", data.getP50());
                json.put("p75", data.getP75());
                json.put("p90", data.getP90());
                json.put("p95", data.getP95());
                json.put("p99", data.getP99());
                json.put("p999", data.getP999());
                upstreamSpendTime.add(json);
            }
            monitorJson.put("upstreamSpendTime", upstreamSpendTime);

            JSONArray transportSpendTime2 = new JSONArray();
            for (Map.Entry<String, CamelliaStatsData> entry : dataMap4.entrySet()) {
                JSONObject json = new JSONObject();
                CamelliaStatsData data = entry.getValue();
                String[] split = entry.getKey().split("\\|");
                json.put("host", split[0]);
                json.put("code", split[1]);
                json.put("count", data.getCount());
                json.put("avg", data.getAvg());
                json.put("max", data.getMax());
                json.put("p50", data.getP50());
                json.put("p75", data.getP75());
                json.put("p90", data.getP90());
                json.put("p95", data.getP95());
                json.put("p99", data.getP99());
                json.put("p999", data.getP999());
                transportSpendTime2.add(json);
            }
            monitorJson.put("transportSpend2Stats", transportSpendTime2);

            JSONArray errorReason = new JSONArray();
            for (Map.Entry<String, CamelliaStatsData> entry : dataMap5.entrySet()) {
                JSONObject json = new JSONObject();
                CamelliaStatsData data = entry.getValue();
                String[] split = entry.getKey().split("\\|");
                json.put("host", split[0]);
                json.put("reason", ErrorReason.getByValue(Integer.parseInt(split[1])));
                json.put("count", data.getCount());
                errorReason.add(json);
            }
            monitorJson.put("errorReasonStats", errorReason);
            ProxyMonitor.monitorJson = monitorJson;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

package com.netease.nim.camellia.tools.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.statistic.CamelliaStatistics;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/1/27
 */
public class StatisticsSamples {

    public static void main(String[] args) {
        test1();
        test2();
    }

    public static void test1() {
        CamelliaStatistics statistics = new CamelliaStatistics();
        for (int i=0; i<200; i++) {
            statistics.update(ThreadLocalRandom.current().nextInt(10));
        }
        for (int i=0; i<100; i++) {
            statistics.update(ThreadLocalRandom.current().nextInt(100));
        }

        CamelliaStatsData data = statistics.getStatsDataAndReset();
        System.out.println(data.getCount());
        System.out.println(data.getSum());
        System.out.println(data.getMax());
        System.out.println(data.getAvg());
        System.out.println(data.getP50());
        System.out.println(data.getP75());
        System.out.println(data.getP90());
        System.out.println(data.getP95());
        System.out.println(data.getP99());
        System.out.println(data.getP999());
    }

    public static void test2() {
        CamelliaStatisticsManager manager = new CamelliaStatisticsManager();
        for (int i=0; i<200; i++) {
            manager.update("path1", ThreadLocalRandom.current().nextInt(10));
        }
        for (int i=0; i<200; i++) {
            manager.update("path2", ThreadLocalRandom.current().nextInt(10));
        }
        Map<String, CamelliaStatsData> dataMap = manager.getStatsDataAndReset();
        System.out.println(JSONObject.toJSONString(dataMap));
    }
}

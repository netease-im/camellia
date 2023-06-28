package com.netease.nim.camellia.tools.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.statistic.CamelliaSegmentStatistics;
import com.netease.nim.camellia.tools.statistic.CamelliaSegmentStatisticsData;

import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/6/28
 */
public class CamelliaSegmentStatisticsSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaSegmentStatistics statistics = new CamelliaSegmentStatistics();
        TimeUnit.MILLISECONDS.sleep(10);
        statistics.update("step1");
        TimeUnit.MILLISECONDS.sleep(20);
        statistics.update("step2");
        TimeUnit.MILLISECONDS.sleep(50);
        CamelliaSegmentStatisticsData data = statistics.end("step3");

        System.out.println(JSONObject.toJSON(data));
    }
}

package com.netease.nim.camellia.redis.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.mergetask.*;
import com.netease.nim.camellia.tools.statistic.CamelliaStatisticsManager;
import com.netease.nim.camellia.tools.statistic.CamelliaStatsData;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/11/7
 */
public class MergeTaskSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaMergeTaskExecutor executor = new CamelliaMergeTaskExecutor(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        CamelliaStatisticsManager statisticsManager = new CamelliaStatisticsManager();
        int c = 10000;
        CountDownLatch latch = new CountDownLatch(c);
        for (int i=0; i<c; i++) {
            long start = System.currentTimeMillis();
            CamelliaMergeTaskFuture<String> future = executor.submit(new BusinessMergeTask(ThreadLocalRandom.current().nextInt(5)));
            future.thenAccept(result -> {
                statisticsManager.update(result.getType().name(), (System.currentTimeMillis() - start));
                System.out.println("type=" + result.getType() + ",result=" + result.getResult() + ",spend=" + (System.currentTimeMillis() - start));
                latch.countDown();
            });
        }
        latch.await();
        Map<String, CamelliaStatsData> map = statisticsManager.getStatsDataAndReset();
        for (Map.Entry<String, CamelliaStatsData> entry : map.entrySet()) {
            System.out.println("key=" + entry.getKey() + ",stats=" + JSONObject.toJSONString(entry.getValue()));
        }
    }

    private static class BusinessMergeTask implements CamelliaMergeTask<BusinessMergeTaskRequest, String> {

        private final int num;

        public BusinessMergeTask(int num) {
            this.num = num;
        }

        @Override
        public CamelliaMergeTaskType getType() {
            return CamelliaMergeTaskType.CLUSTER;
        }

        @Override
        public long resultCacheMillis() {
            return 1000;
        }

        @Override
        public CamelliaMergeTaskResultSerializer<String> getResultSerializer() {
            return CamelliaMergeTaskResultStringSerializer.INSTANCE;
        }

        @Override
        public BusinessMergeTaskRequest getKey() {
            return new BusinessMergeTaskRequest(num);
        }

        @Override
        public String getTag() {
            return "test";
        }

        @Override
        public String execute(BusinessMergeTaskRequest key) throws Exception {
            return businessMethod(key.getNum());
        }
    }

    private static class BusinessMergeTaskRequest implements CamelliaMergeTaskKey {

        private final int num;

        public BusinessMergeTaskRequest(int num) {
            this.num = num;
        }

        public int getNum() {
            return num;
        }

        @Override
        public String serialize() {
            return String.valueOf(num);
        }
    }

    private static String businessMethod(int num) throws InterruptedException {
        int c = 0;
        for (int i=0; i<num; i++) {
            c += i;
            TimeUnit.MILLISECONDS.sleep(i * 100L);
        }
        return String.valueOf(c);
    }
}

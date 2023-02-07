
# CamelliaMergeTask/CamelliaMergeTaskExecutor

## 简介
* 适用场景：有相同请求参数的查询请求，高并发或者高tps查询，对于数据一致性要求不是那么高
* 此时为了避免每次请求都落到底层（DB或者复杂的cache计算），CamelliaMergeTask会控制相同查询请求的并发，穿透过去一个请求，并把结果分发给等待队列中的其他请求
* 此外，还可以对结果进行短暂的缓存，从而提高请求merge的效果
* 支持单机合并，也支持集群合并（需要redis）
* 集群任务合并基于CamelliaRedisTemplate实现

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-toolkit</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```

## 示例

```java
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
```
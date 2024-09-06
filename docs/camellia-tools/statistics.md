# CamelliaStatistics/CamelliaStatisticsManager

## 简介
* 一个用于统计的工具类
* 支持计数、求和、平均值、最大值、p50/p75/p90/p95/p99/p999

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.2.29</version>
</dependency>
```

## 示例
```java
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

```
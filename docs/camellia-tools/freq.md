
# CamelliaFreq
## 简介
* 支持单机频控，也支持集群频控，还支持混合
* 频控参数详见CamelliaFreqConfig
* 集群频控基于CamelliaRedisTemplate实现

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-toolkit</artifactId>
    <version>1.3.4</version>
</dependency>
```

## 示例
```java
public enum CamelliaFreqType {
    //单机模式
    STANDALONE,
    //集群模式，走redis
    CLUSTER,
    //混合，先过单机，再过集群，主要是用于输入qps非常高，但是频控后的目标qps又很低的场景
    //假设输入10w的QPS，目标是20的QPS
    // 如果是普通的集群模式，则10w的QPS都会打到redis
    // 如果用混合模式，且一共有10个节点在处理，则穿透到redis最多是20*10=200QPS，最终通过的也只有20QPS，可以极大的降低redis的压力
    MISC,
    ;
}
```

```java
/**
 * 示例一：
 * checkTime=1000，threshold=10，banTime=2000，delayBanEnable=true
 * 表示1s内最多10次请求，如果超过了，则2s内不允许请求，如果还有请求，2s会一直顺延，直到连续2s内没有新的请求进来，频控才会取消
 * 
 * 示例二：
 * checkTime=1000，threshold=10，banTime=2000，delayBanEnable=false
 * 表示1s内最多10次请求，如果超过了，则2s内不允许请求，2s之后直接频控自动取消
 *
 * 示例三：
 * checkTime=1000，threshold=10，banTime=0
 * 表示1s内最多10次请求，如果超过了，则返回失败，等当前这个周期（1s）过去了，则频控自动取消
 * Created by caojiajun on 2022/8/1
 */
public class CamelliaFreqConfig {

    private long checkTime;//检查周期，单位ms
    private long threshold;//阈值，一个周期内的最大请求数
    private long banTime;//超过阈值后的惩罚屏蔽时间
    private boolean delayBanEnable;//超过阈值后进入屏蔽时间，此时如果有新请求过来，是否要顺延屏蔽时间
}
```

```java
public class FreqSamples {

    //need same CamelliaFreq instance to enable local freq
    private static final CamelliaFreq freq = new CamelliaFreq(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===test1===");
        test1();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test2===");
        test2();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test3===");
        test3();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test4===");
        test4();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test5===");
        test5();
        TimeUnit.SECONDS.sleep(2);
        System.out.println("===test6===");
        test6();
        System.out.println("===END====");
    }

    private static void test1() throws InterruptedException {
        String freqKey = "k1";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }

    private static void test2() throws InterruptedException {
        String freqKey = "k1";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        config.setDelayBanEnable(true);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }

    private static void test3() throws InterruptedException {
        String freqKey = "k2";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(0);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.CLUSTER, config);
        System.out.println(response.isPass());
    }

    private static void test4() throws InterruptedException {
        String freqKey = "k3";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
        System.out.println(response.isPass());
    }

    private static void test5() throws InterruptedException {
        String freqKey = "k3";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(2000);
        config.setDelayBanEnable(true);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
        System.out.println(response.isPass());
    }

    private static void test6() throws InterruptedException {
        String freqKey = "k4";
        CamelliaFreqConfig config = new CamelliaFreqConfig();
        config.setThreshold(2);
        config.setCheckTime(1000);
        config.setBanTime(0);
        for (int i=0; i<20; i++) {
            CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
            System.out.println(response.isPass());
            TimeUnit.MILLISECONDS.sleep(200);
        }
        TimeUnit.SECONDS.sleep(3);
        CamelliaFreqResponse response = freq.checkFreqPass(freqKey, CamelliaFreqType.STANDALONE, config);
        System.out.println(response.isPass());
    }
}

```

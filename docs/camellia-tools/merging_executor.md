
# CamelliaMergingExecutor

## 简介
CamelliaMergingExecutor是"**按hashKey合并 + 时间窗批量提交**"的执行器  
提交任务时额外提供hashKey参数：相同hashKey的任务先被合并成一个槽位（同一个key在一个窗口内只保留合并后的一条），每个队列维护一个滚动窗口，窗口到期后把该队列累积的所有槽位交给BatchHandler处理  

它和[CamelliaHashedBatchExecutor](/docs/camellia-tools/hashed_batch_executor.md)（流式批量）的区别：

| | CamelliaHashedBatchExecutor | CamelliaMergingExecutor|
|:---:|:---:|:---:|
|攒批方式|提交即入队，按批次取出|提交时按hashKey合并成槽位|
|去重时机|需要业务写在BatchHandler里|执行器内部完成，同一个key一个窗口内只出现一次|
|批次触发|够量就走（受"同时非空队列数/批次上限"影响）|窗口到期（由时间保证，不随吞吐上升而压缩）|
|合并比|高负载下可能退化为1|≈"每key在窗口内的提交次数"|

两者都是"hashKey路由到固定队列 + 队列级别互斥"，都不保证任务落在固定线程上  

典型场景：同一主体的状态上报（比如已读位点msgid），业务只关心"最终落库最大的那个值"，窗口内的大量重复上报可以合并成一次落库  

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>a.b.c</version>
</dependency>
```

## 示例
```java
public class CamelliaMergingExecutorSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaMergingExecutorConfig<UpdateMsgBean> config = new CamelliaMergingExecutorConfig<>("sample",
                //相同hashKey的合并规则：取msgid大的那个
                (oldValue, newValue) -> newValue.getMsgid() > oldValue.getMsgid() ? newValue : oldValue,
                //一个批次里的每个hashKey只出现一次，直接逐条落库即可
                tasks -> {
                    for (UpdateMsgBean task : tasks) {
                        System.out.println("handle task = " + task);
                    }
                });
        config.setQueueCount(512);
        config.setWorkerCount(32);
        config.setMaxBatchSize(4096);
        //滚动窗口：窗口越长合并比越高、写入延迟越大
        config.setWindowMillis(3000);
        //提前刷新阈值 + 单队列槽位硬上限
        config.setMaxAccumulateSize(100000);
        CamelliaMergingExecutor<UpdateMsgBean> executor = new CamelliaMergingExecutor<>(config);

        //同一个key在一个窗口内提交多次，最终只会落库一条（合并结果）
        executor.submit("key1", new UpdateMsgBean(1));
        executor.submit("key1", new UpdateMsgBean(2));
        executor.submit("key1", new UpdateMsgBean(3));

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

## 合并效果
设"某个key在窗口内提交次数"为r，则合并比约为r（每个不同的key每窗口最多落一次）  
而流式批量在"同时非空队列数 &gt;= maxBatchSize"时合并比为1，即每次提交产生一条  

实测（1024个活跃key、queueCount=512、workerCount=32、流式maxBatchSize=128、窗口1000ms）：

|提交总次数|流式批量合并比|本执行器合并比|
|:---:|:---:|:---:|
|20480（每key 20次）|1.0|20.0|
|204800（每key 200次）|1.0|200.0|

## 配置项

|参数|默认值|含义|
|:---:|:---:|:---:|
|name|-|名称，用于监控和日志|
|queueCount|32|队列数量，hashKey对queueCount取模决定进入哪个队列|
|workerCount|CPU核数|工作线程数，要求workerCount &lt;= queueCount|
|merger|-|相同hashKey的合并规则|
|batchHandler|-|批次处理器，接收合并后的任务列表|
|windowMillis|1000|单队列的滚动窗口长度，窗口越长合并比越高、写入延迟越大|
|maxBatchSize|100|单次交给BatchHandler的合并后条数上限|
|maxAccumulateSize|100000|提前刷新阈值 + 单队列槽位硬上限（见下节）|
|skipWaitPendingThreshold|10000|待合并的不同key总数达到该值时所有队列立即刷新；&lt;=0表示不启用|
|idleWaitMillis|10|没有到期队列时工作线程的最大休眠时间，用于控制空转唤醒频率|
|workThreadInitCallback|-|工作线程启动时的回调|
|rejectedExecutionHandler|-|拒绝策略，默认CallerRunsPolicy|

## 内存上界与背压
maxAccumulateSize同时承担两个职责：

1、**提前刷新阈值**：某个队列累积的不同key数达到该值，即把该队列标记为立即刷新（正常行走这条路，不丢数据）  
2、**单队列槽位硬上限**：如果工作线程因为下游（DB等）变慢而来不及刷新、槽位继续增长，则**新增hashKey的提交会走拒绝策略**（背压），从而保证内存上界为queueCount * maxAccumulateSize  

两个关键性质：

* **同一个hashKey的重复提交只是合并、不新增槽位，任何时候都不会被拒绝**；只有"新增hashKey且槽位已达上限"才会触发拒绝
* 默认拒绝策略是CallerRunsPolicy，即在**调用方线程同步执行**（等价于"队列满就同步写"的常见做法），因此默认不会丢数据；也可以显式配置AbortPolicy（抛RejectedExecutionException）或DiscardPolicy（丢弃并打日志）

因此配置时应保证 queueCount * maxAccumulateSize &gt;= 峰值不同key速率 * windowMillis，否则会频繁提前刷新、窗口被压缩  
例如峰值2万个不同key/秒、窗口3000ms，需要约6万个槽位（512队列 × maxAccumulateSize 200即足够），内存约9MB  

## 其他需要知道的点
* **超时提交**：窗口是"每队列独立、首次提交开窗"的滚动窗口，同一队列相邻两个窗口可能由不同工作线程刷新，写入时间上是分散的，不是全局统一时刻齐刷
* **延迟上界**约为windowMillis
* **停机不丢**：shutdown时会做一次最终刷新，把窗口内残留的槽位交给BatchHandler
* 拒绝策略在调用方同步执行时，会自旋等待队列令牌，因此调用方线程可能被阻塞

## 监控
CamelliaMergingExecutor在创建时会自动注册到CamelliaExecutorMonitor，通过executor.getStats()可以获取活跃线程数、线程数、已完成任务数、待刷新槽位数等指标  
```
CamelliaExecutorStats stats = executor.getStats();
```

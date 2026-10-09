
# CamelliaHashedBatchExecutor

## 简介
常规线程池按提交顺序执行，线程数超过1个时，相同业务主体的任务可能乱序并发  
CamelliaHashedBatchExecutor在提交任务时额外提供hashKey参数：相同hashKey的任务被路由到同一个队列、串行执行；工作线程按批次从队列中收集任务，聚合成一个批次后交给BatchHandler处理，从而支持批量操作（批量读写DB、批量mget等）  

注意：本执行器是**流式批量**——任务提交即入队、按批次取出执行，**批次内的去重/合并需要业务自己写在BatchHandler里**  
如果一个批次可能包含同一个hashKey的多条任务，且希望"同一个key在一段时间内只处理一次"，请使用[CamelliaMergingExecutor](/docs/camellia-tools/merging_executor.md)  

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>a.b.c</version>
</dependency>
```

## 配置项
通过CamelliaHashedBatchExecutorConfig进行配置，各参数含义如下：  

|参数|默认值|含义|
|:---:|:---:|:---:|
|name|-|线程池名称，用于监控和日志|
|queueCount|32|队列数量，hashKey对queueCount取模决定进入哪个队列|
|workerCount|CPU核数|工作线程数，要求workerCount &lt;= queueCount|
|dynamicQueueCapacity|100000|每个队列的容量，支持动态调整|
|maxBatchSize|100|一个批次最多包含的任务数|
|minBatchSize|1|一个批次期望的最小任务数，配合maxBatchWaitMillis使用|
|maxBatchWaitMillis|0|当批次任务数小于minBatchSize时的最大等待时间，0表示不等待|
|perQueueMaxDrainSize|16|单次从同一个队列最多拉取的任务数（不超过maxBatchSize）|
|skipWaitPendingThreshold|10000|待处理任务数达到该阈值时跳过等待，立即处理，避免延迟|
|idleWaitMillis|10|队列空闲时的轮询等待时间|
|workThreadInitCallback|-|工作线程启动时的回调|
|batchHandler|-|批次处理器，接收一个任务列表|
|rejectedExecutionHandler|-|拒绝策略，支持动态获取|

## 批次的组成
一个批次由drainTo决定：**第一轮是"每个非空队列各取1条，凑满maxBatchSize即停止"**，第二轮才按perQueueMaxDrainSize从每个队列深挖  
因此当"同时非空队列数 &gt;= maxBatchSize"时，一个批次恰好是maxBatchSize条来自不同队列的任务，也就是maxBatchSize个不同的hashKey，**业务写在BatchHandler里的去重逻辑会完全失效**  
另外：同一个hashKey在一个批次里不会出现两次；同一个hashKey的相对顺序保持不变；跨队列的先后顺序没有业务含义  

## 示例
```java
public class CamelliaHashedBatchExecutorSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaHashedBatchExecutor<String> executor = new CamelliaHashedBatchExecutor<>("sample", 32, 8, 128,
                tasks -> {
                    //一个批次可能包含多个不同hashKey的任务
                    for (String task : tasks) {
                        System.out.println("handle task = " + task);
                    }
                });

        //相同hashKey的任务确保串行执行，顺序与提交顺序一致
        executor.submit("key1", "task1-1");
        executor.submit("key2", "task2-1");
        executor.submit("key1", "task1-2");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

## 监控
CamelliaHashedBatchExecutor在创建时会自动注册到CamelliaExecutorMonitor，通过executor.getStats()可以获取活跃线程数、线程数、已完成任务数、排队任务数等指标  
如果需要定期汇总输出，可以调用CamelliaExecutorMonitor.init(intervalSeconds)启动监控  
```
CamelliaExecutorStats stats = executor.getStats();
```

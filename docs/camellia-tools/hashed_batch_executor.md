
# CamelliaHashedBatchExecutor

## 简介
在某些使用线程池的场景下，我们希望其中部分任务是顺序执行的，而常规的线程池是按照提交顺序执行的，如果线程池线程数超过1个，则可能导致乱序或者并发  
CamelliaHashedExecutor通过hashKey把任务路由到固定的线程，可以解决这个问题，但是每个任务仍然是单独执行的，无法做批量处理  
CamelliaHashedBatchExecutor在CamelliaHashedExecutor的基础上做了增强：提交任务时额外提供hashKey参数，相同hashKey的任务会被路由到同一个队列并串行执行；同时工作线程会一次性从队列中收集多个任务，聚合成一个批次后交给BatchHandler处理，从而支持批量操作（如批量读写DB、批量mget等）  

同时要注意和CamelliaHashedExecutor的差异：
* 相同hashKey的任务保证串行执行，且执行顺序与提交顺序一致，但不保证始终由同一个线程执行（不同批次可能由不同工作线程处理）
* 一个批次可能包含多个不同hashKey的任务，同一个hashKey在一个批次内不会出现两次

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

## 示例
```java
public class CamelliaHashedBatchExecutorSamples {

    public static void main(String[] args) throws InterruptedException {
        String name = "sample";
        int queueCount = 32;
        int workerCount = 8;
        int maxBatchSize = 100;

        CamelliaHashedBatchExecutor<String> executor = new CamelliaHashedBatchExecutor<>(name, queueCount, workerCount, maxBatchSize,
                //BatchHandler：接收一个批次的任务列表，可能来自不同的hashKey
                tasks -> {
                    System.out.println("batch handle, size = " + tasks.size() + ", thread = " + Thread.currentThread().getName());
                    for (String task : tasks) {
                        System.out.println("handle task = " + task);
                    }
                });

        //相同hashKey的任务确保是串行执行的，且顺序与提交顺序一致
        executor.submit("key1", "task1-1");
        executor.submit("key2", "task2-1");
        executor.submit("key1", "task1-2");
        executor.submit("key2", "task2-2");

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
```

如果需要更精细地控制批次行为，可以使用CamelliaHashedBatchExecutorConfig进行配置：  
```java
public class CamelliaHashedBatchExecutorConfigSamples {

    public static void main(String[] args) throws InterruptedException {
        CamelliaHashedBatchExecutorConfig<String> config = new CamelliaHashedBatchExecutorConfig<>("sample", 32, 8, 100, tasks -> {
            //批量处理
        });
        //批次期望的最小任务数，以及等待时间，任务数不足时会等待一小段时间以凑批
        config.setMinBatchSize(10);
        config.setMaxBatchWaitMillis(10);
        //每个队列的容量，支持动态调整
        config.setDynamicQueueCapacity(() -> 100000);
        //拒绝策略，支持动态获取
        config.setRejectedExecutionHandler(() -> new CamelliaHashedBatchExecutor.CallerRunsPolicy<>());

        CamelliaHashedBatchExecutor<String> executor = new CamelliaHashedBatchExecutor<>(config);

        executor.submit("key1", "task1");

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

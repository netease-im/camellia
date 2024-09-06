
# CamelliaHashedExecutor

## 简介
在某些使用线程池的场景下，我们希望其中部分任务是顺序执行的，而常规的线程池是按照提交顺序执行的，如果线程池线程数超过1个，则可能导致乱序或者并发     
CamelliaHashedExecutor在提交任务时额外提供了hashKey这样的参数，当hashKey参数是一样的时候，会确保所有任务都是相同线程执行的  

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
public class CamelliaHashedExecutorSamples {
    public static void main(String[] args) {
        String name = "sample";
        int poolSize = Runtime.getRuntime().availableProcessors() * 2;
        int queueSize = 100000;
        CamelliaHashedExecutor.RejectedExecutionHandler rejectedExecutionHandler = new CamelliaHashedExecutor.CallerRunsPolicy();
        CamelliaHashedExecutor executor = new CamelliaHashedExecutor(name, poolSize, queueSize, rejectedExecutionHandler);

        //相同hashKey的两个任务确保是单线程顺序执行的
        
        executor.submit("key1", () -> {
            System.out.println("key1 start1, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key1 end1, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key2", () -> {
            System.out.println("key2 start1, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key2 end1, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key1", () -> {
            System.out.println("key1 start2, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key1 end2, thread=" + Thread.currentThread().getName());
        });

        executor.submit("key2", () -> {
            System.out.println("key2 start2, thread=" + Thread.currentThread().getName());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("key2 end2, thread=" + Thread.currentThread().getName());
        });
    }
}
```

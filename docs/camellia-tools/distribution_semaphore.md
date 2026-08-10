

# CamelliaRedisSemaphore

## 简介
* 分布式信号量（限定 N 个 permit），可用作限流、熔断保护等并发许可控制
* 基于CamelliaRedisTemplate实现
* 基于lua实现原子加锁/释放/续期，避免并发下的超卖与计数错乱
* permit 带租约（TTL），持有者宕机或忘记 release 时到期自动回收，不会永久泄漏
* 支持 acquire 阻塞等待 / tryAcquire 快速失败两种获取方式
* 支持运行时动态调整 permit 上限（setPermits）
* 通过 PermitsMode 指定 permit 上限的确定方式：
    * SEED：上限持久化到 redis（key:max），首次 acquire 播种，之后由 setPermits/外部主导
    * LOCAL：上限不持久化，每次 acquire 用本实例 permits 对全局持有数判断，改配置即生效

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-toolkit</artifactId>
    <version>a.b.c</version>
</dependency>
```

## 示例
```java
public class SemaphoreSamples {

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://abc@127.0.0.1:6379");

        String key = "semaphoreKey123";//信号量key
        int permits = 10;//permit 上限 N
        long acquireTimeoutMillis = 3000;//阻塞获取许可的最大等待时间
        long expireTimeoutMillis = 3000;//单个 permit 的租约过期时间

        //SEED 模式：上限持久化到 redis（默认推荐）
        CamelliaRedisSemaphore redisSemaphore = CamelliaRedisSemaphore.newSemaphore(
                template, key, permits, acquireTimeoutMillis, expireTimeoutMillis, CamelliaRedisSemaphore.PermitsMode.SEED);

        //1. 阻塞获取 permit，若获取失败，则会重试直到 acquireTimeoutMillis
        boolean acquire = redisSemaphore.acquire();
        if (acquire) {
            try {
                System.out.println("do some thing");
            } finally {
                redisSemaphore.release();
            }
        }

        //2. 阻塞获取 permit，若获取成功则执行 Runnable，若获取失败则会阻塞直到 acquireTimeoutMillis
        boolean ok = redisSemaphore.acquireAndRun(() -> System.out.println("do some thing"));
        System.out.println("acquireAndRun = " + ok);

        //3. 尝试获取 permit，若获取失败立即返回（不等待）
        boolean tryAcquire = redisSemaphore.tryAcquire();
        if (tryAcquire) {
            try {
                System.out.println("do some thing");
            } finally {
                redisSemaphore.release();
            }
        }

        //4. 快速失败获取 permit，若获取成功则执行 Runnable，若获取失败立即返回
        boolean tryOk = redisSemaphore.tryAcquireAndRun(() -> System.out.println("do some thing"));
        System.out.println("tryAcquireAndRun = " + tryOk);

        //5. 续期自己持有的 permit（任务耗时较长、可能超过 expireTimeoutMillis 时调用）
        boolean renew = redisSemaphore.renew();
        System.out.println("renew = " + renew);

        //6. 运行时调整 permit 上限 N（软缩容：不强制回收已发放的 permit）
        boolean setPermits = redisSemaphore.setPermits(20);
        System.out.println("setPermits = " + setPermits);

        //7. 清空信号量相关的 key，会释放不是自己获取到的 permit
        redisSemaphore.clear();

        //////LOCAL 模式：上限不持久化，改配置即生效（适合应用配置驱动）
        //共享同一 key 的实例应使用相同的 permits（通常来自同一份配置），此时等效全局上限 N
        CamelliaRedisSemaphore localSemaphore = CamelliaRedisSemaphore.newSemaphore(
                template, key, permits, acquireTimeoutMillis, expireTimeoutMillis, CamelliaRedisSemaphore.PermitsMode.LOCAL);
        boolean localAcquire = localSemaphore.acquire();
        if (localAcquire) {
            try {
                System.out.println("do some thing");
            } finally {
                localSemaphore.release();
            }
        }
    }
}
```

## 注意事项
* redis 数据结构：`key`（ZSET，member=permitId，score=过期时间戳）；`key:max`（string，permit 上限 N，仅 SEED 模式使用）。
* redis cluster 部署时，`key` 需包含 hash tag（例如 `{rate-limit}:foo`），使 `key` 与 `key:max` 落在同一 slot；通过 camellia-redis-proxy / 单机 / 主从部署则无此要求。
* 一个 `CamelliaRedisSemaphore` 实例最多持有 1 个 permit，`acquire`/`tryAcquire` 幂等（已持有则直接返回 true）；如需并发持有多个 permit，请使用多个实例。
* `release`/`renew` 仅作用于自己持有的 permit。
* 长任务需在 `expireTimeoutMillis` 到期前调用 `renew` 续期，建议 renew 间隔显著小于 `expireTimeoutMillis`，否则 permit 可能在续期前被回收。
* SEED 模式下 `getPermits()` 读取 redis `key:max` 实际值；LOCAL 模式返回本实例 `permits`。
* 默认非公平排队（非 FIFO），高并发下可能出现饥饿。
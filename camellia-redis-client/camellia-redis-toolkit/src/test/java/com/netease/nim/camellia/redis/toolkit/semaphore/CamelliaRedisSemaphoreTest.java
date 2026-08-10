package com.netease.nim.camellia.redis.toolkit.semaphore;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.redis.toolkit.lock.LockTaskResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CamelliaRedisSemaphore 测试。
 *
 * <p>沿用 camellia 测试约定：默认通过系统属性 {@code test_enable}（默认 {@code false}）门控，
 * 无 redis 时 {@code mvn test} 直接 no-op；开启后通过 {@code test_redis_url}（默认 {@code redis://@127.0.0.1:6379}）
 * 连接真实 redis 执行。参数校验用例不依赖 redis，始终执行。
 *
 * Created by caojiajun on 2026/8/7.
 */
public class CamelliaRedisSemaphoreTest {

    private static final boolean enable = Boolean.parseBoolean(System.getProperty("test_enable", "false"));
    private static final String redis_url = System.getProperty("test_redis_url", "redis://@127.0.0.1:6379");

    private CamelliaRedisTemplate template() {
        return new CamelliaRedisTemplate(redis_url);
    }

    private String newKey() {
        return "test:sem:" + UUID.randomUUID();
    }

    private CamelliaRedisSemaphore semaphore(String key, int permits, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return CamelliaRedisSemaphore.newSemaphore(template(), key, permits, acquireTimeoutMillis, expireTimeoutMillis, CamelliaRedisSemaphore.PermitsMode.SEED);
    }

    private CamelliaRedisSemaphore semaphoreLocal(String key, int permits, long acquireTimeoutMillis, long expireTimeoutMillis) {
        return CamelliaRedisSemaphore.newSemaphore(template(), key, permits, acquireTimeoutMillis, expireTimeoutMillis, 5, CamelliaRedisSemaphore.PermitsMode.LOCAL);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 参数校验：不依赖 redis，始终执行
    @Test
    public void testValidation() {
        try {
            CamelliaRedisSemaphore.newSemaphore(template(), "test:sem:invalid", 0, 1000, 1000, CamelliaRedisSemaphore.PermitsMode.SEED);
            Assert.fail("permits=0 should throw");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            CamelliaRedisSemaphore.newSemaphore(template(), "test:sem:invalid", 1, 1000, 0, CamelliaRedisSemaphore.PermitsMode.SEED);
            Assert.fail("expireTimeoutMillis=0 should throw");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            CamelliaRedisSemaphore.newSemaphore(template(), "test:sem:invalid", 1, -1, 1000, CamelliaRedisSemaphore.PermitsMode.SEED);
            Assert.fail("acquireTimeoutMillis<0 should throw");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            CamelliaRedisSemaphore.newSemaphore(template(), "test:sem:invalid", 1, 1000, 1000, 0, CamelliaRedisSemaphore.PermitsMode.SEED);
            Assert.fail("tryIntervalMillis=0 should throw");
        } catch (IllegalArgumentException ignored) {
        }
        try {
            CamelliaRedisSemaphore.newSemaphore(template(), "test:sem:invalid", 1, 1000, 1000, 5, null);
            Assert.fail("permitsMode=null should throw");
        } catch (IllegalArgumentException ignored) {
        }
        CamelliaRedisSemaphore semaphore = semaphore(newKey(), 1, 1000, 1000);
        try {
            semaphore.setPermits(0);
            Assert.fail("setPermits(0) should throw");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testTryAcquireAndRelease() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 1, 1000, 10000);

        Assert.assertTrue(s1.tryAcquire());
        Assert.assertFalse(s2.tryAcquire());
        Assert.assertTrue(s1.isAcquireOk());

        Assert.assertTrue(s1.release());
        Assert.assertFalse(s1.isAcquireOk());
        Assert.assertTrue(s2.tryAcquire());
        Assert.assertTrue(s2.release());
        s1.clear();
    }

    @Test
    public void testTryAcquireIdempotent() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 1, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        String permitId = s1.getPermitId();
        // 同实例重复 tryAcquire 幂等，不额外占用 permit
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertEquals(permitId, s1.getPermitId());
        Assert.assertFalse(s2.tryAcquire());
        s1.clear();
    }

    @Test
    public void testAcquireTimeout() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 1, 300, 10000);
        Assert.assertTrue(s1.tryAcquire());
        long start = System.currentTimeMillis();
        Assert.assertFalse(s2.acquire());
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue("acquire should wait until timeout, cost=" + cost, cost >= 250);
        s1.clear();
    }

    @Test
    public void testAcquireBlockThenSuccess() throws Exception {
        if (!enable) return;
        final String key = newKey();
        final CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        final CamelliaRedisSemaphore s2 = semaphore(key, 1, 2000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        // 延迟释放，使 s2 阻塞等待后成功
        new Thread(() -> {
            sleep(300);
            s1.release();
        }).start();
        long start = System.currentTimeMillis();
        Assert.assertTrue(s2.acquire());
        long cost = System.currentTimeMillis() - start;
        Assert.assertTrue("acquire should block until release, cost=" + cost, cost >= 200);
        s2.clear();
    }

    @Test
    public void testRenew() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 1000);
        Assert.assertTrue(s1.tryAcquire());
        long before = s1.getExpireTimestamp();
        sleep(50);
        Assert.assertTrue(s1.renew());
        long after = s1.getExpireTimestamp();
        Assert.assertTrue("renew should extend expire timestamp", after > before);
        Assert.assertTrue(s1.isAcquireOk());
        s1.clear();
    }

    @Test
    public void testReleaseOnlyOwn() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 1, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        // s2 未持有，release 应失败，且不影响 s1
        Assert.assertFalse(s2.release());
        Assert.assertTrue(s1.isAcquireOk());
        Assert.assertFalse(s2.tryAcquire());
        s1.clear();
    }

    @Test
    public void testSetPermitsExpand() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 1, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertFalse(s2.tryAcquire());
        Assert.assertTrue(s1.setPermits(2));
        Assert.assertTrue(s2.tryAcquire());
        s1.clear();
    }

    @Test
    public void testSetPermitsShrinkSoft() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 2, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 2, 1000, 10000);
        CamelliaRedisSemaphore s3 = semaphore(key, 2, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertTrue(s2.tryAcquire());
        // 软缩容到 1：已发放的两个 permit 不被强制回收
        Assert.assertTrue(s1.setPermits(1));
        Assert.assertTrue(s1.isAcquireOk());
        Assert.assertTrue(s2.isAcquireOk());
        // 新上限立即对后续 acquire 生效：cap=1 且已持有 2，s3 拿不到
        Assert.assertFalse(s3.tryAcquire());
        // s1 归还后仍有 s2 持有 1（=cap），s3 仍拿不到
        Assert.assertTrue(s1.release());
        Assert.assertFalse(s3.tryAcquire());
        // s2 也归还后才空出名额，s3 可获取
        Assert.assertTrue(s2.release());
        Assert.assertTrue(s3.tryAcquire());
        s1.clear();
    }

    @Test
    public void testClear() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 2, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphore(key, 2, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertTrue(s2.tryAcquire());
        Assert.assertTrue(s1.clear());
        // clear 后信号量重置，可重新获取
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertTrue(s2.tryAcquire());
        s1.clear();
    }

    @Test
    public void testTtlLazyReclaim() {
        if (!enable) return;
        String key = newKey();
        // 短租约，持有者不 release/renew，到期后 permit 重新可用（key 自过期或下次 acquire 回收）
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 300);
        CamelliaRedisSemaphore s2 = semaphore(key, 1, 1000, 300);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertFalse(s2.tryAcquire());
        sleep(500); // > 300ms TTL
        // 持有者宕机到期后，permit 不再被占用，s2 可获取（不永久泄漏）
        Assert.assertTrue(s2.tryAcquire());
        s2.clear();
    }

    @Test
    public void testKeyAutoExpireWithoutSubsequentAcquire() {
        if (!enable) return;
        CamelliaRedisTemplate t = template();
        String key = "test:sem:expire:" + UUID.randomUUID();
        CamelliaRedisSemaphore s1 = CamelliaRedisSemaphore.newSemaphore(t, key, 1, 1000, 300, CamelliaRedisSemaphore.PermitsMode.SEED);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertTrue("key should exist right after acquire", t.exists(key));
        // 模拟持有者宕机：既不 release/renew，也不再有任何人 acquire
        sleep(600); // > 300ms TTL
        // key 被 redis 自动过期删除，不依赖后续 tryAcquire（对齐 CamelliaRedisLock 的 PX 行为）
        Assert.assertFalse("key should auto-expire without subsequent acquire", t.exists(key));
        // 新实例首次 acquire 会重新初始化（key:max 也已自过期）
        CamelliaRedisSemaphore s2 = CamelliaRedisSemaphore.newSemaphore(t, key, 1, 1000, 1000, CamelliaRedisSemaphore.PermitsMode.SEED);
        Assert.assertTrue(s2.tryAcquire());
        s2.clear();
    }

    @Test
    public void testIsAcquireOkScorePrecision() {
        if (!enable) return;
        CamelliaRedisTemplate t = template();
        String key = newKey();
        CamelliaRedisSemaphore s1 = CamelliaRedisSemaphore.newSemaphore(t, key, 1, 1000, 200, CamelliaRedisSemaphore.PermitsMode.SEED);
        Assert.assertTrue(s1.tryAcquire());
        String permitId = s1.getPermitId();
        Assert.assertNotNull(permitId);
        sleep(400); // 本地 expireTimestamp 已过，key 也已自过期
        // 手动塞回一个 score 在过去的成员，模拟「成员仍在但逻辑过期」
        long past = System.currentTimeMillis() - 1000;
        t.eval(SafeEncoder.encode("return redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])"), 1,
                SafeEncoder.encode(key), SafeEncoder.encode(String.valueOf(past)), SafeEncoder.encode(permitId));
        // isAcquireOk 按 score 判定逻辑过期（score < now），即使成员仍在也返回 false
        Assert.assertFalse("isAcquireOk should be false when score < now even if member exists", s1.isAcquireOk());
        s1.clear();
    }

    // ===== LOCAL 模式：上限不持久化，按本实例 permits 对全局计数判断 =====

    @Test
    public void testLocalModeNoKeyMax() {
        if (!enable) return;
        CamelliaRedisTemplate t = template();
        String key = newKey();
        CamelliaRedisSemaphore s1 = CamelliaRedisSemaphore.newSemaphore(t, key, 2, 1000, 10000, 5, CamelliaRedisSemaphore.PermitsMode.LOCAL);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertFalse("LOCAL 模式不应创建 key:max", t.exists(SafeEncoder.encode(key + ":max")));
        s1.clear();
    }

    @Test
    public void testLocalModeSamePermitsNoOversell() {
        if (!enable) return;
        String key = newKey();
        // 各实例 permits 相同（同配置）→ 等效全局上限 N
        CamelliaRedisSemaphore s1 = semaphoreLocal(key, 2, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphoreLocal(key, 2, 1000, 10000);
        CamelliaRedisSemaphore s3 = semaphoreLocal(key, 2, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertTrue(s2.tryAcquire());
        Assert.assertFalse("全局已达 2，s3 不应获取", s3.tryAcquire());
        s1.clear();
    }

    @Test
    public void testLocalModePermitsChangeTakesEffect() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphoreLocal(key, 1, 1000, 10000);
        CamelliaRedisSemaphore s2 = semaphoreLocal(key, 1, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertFalse(s2.tryAcquire()); // N=1 已占
        // 改配置（新实例 N=2），立即生效，无需 setPermits
        CamelliaRedisSemaphore s3 = semaphoreLocal(key, 2, 1000, 10000);
        Assert.assertTrue("LOCAL 模式改 permits 立即生效", s3.tryAcquire());
        s1.clear();
    }

    @Test
    public void testLocalModeRenewAndClear() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphoreLocal(key, 1, 1000, 1000);
        Assert.assertTrue(s1.tryAcquire());
        long before = s1.getExpireTimestamp();
        sleep(50);
        Assert.assertTrue("LOCAL 模式 renew 应成功", s1.renew());
        Assert.assertTrue("renew 应延长过期时间", s1.getExpireTimestamp() > before);
        // LOCAL 模式 clear 不涉及 key:max，但应清空 holders 并允许重新获取
        Assert.assertTrue(s1.clear());
        Assert.assertTrue("clear 后可重新获取", s1.tryAcquire());
        s1.clear();
    }

    @Test
    public void testLocalModeReleaseNotHeld() {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphoreLocal(key, 1, 1000, 10000);
        // 未持有，release 应返回 false
        Assert.assertFalse(s1.release());
    }

    @Test
    public void testSeedCreatesKeyMax() {
        if (!enable) return;
        CamelliaRedisTemplate t = template();
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 3, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertTrue("SEED 模式应创建 key:max", t.exists(SafeEncoder.encode(key + ":max")));
        s1.clear();
    }

    @Test
    public void testSeedGetPermitsReadsRedis() {
        if (!enable) return;
        CamelliaRedisTemplate t = template();
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 3, 1000, 10000);
        Assert.assertTrue(s1.tryAcquire());
        Assert.assertEquals(3, s1.getPermits());
        // 外部把 key:max 改成 7，getPermits 应返回 redis 实际值，而非本地种子 3
        t.eval(SafeEncoder.encode("return redis.call('SET', KEYS[1], ARGV[1])"), 1,
                SafeEncoder.encode(key + ":max"), SafeEncoder.encode("7"));
        Assert.assertEquals("SEED 模式 getPermits 应读取 redis 实际值", 7, s1.getPermits());
        s1.clear();
    }

    @Test
    public void testConcurrencyNoOversell() throws Exception {
        if (!enable) return;
        final int permits = 5;
        final int threads = 30;
        final String key = "test:sem:conc:" + UUID.randomUUID();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicInteger success = new AtomicInteger(0);
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    CamelliaRedisSemaphore s = CamelliaRedisSemaphore.newSemaphore(template(), key,
                            permits, 1000, 10000, CamelliaRedisSemaphore.PermitsMode.SEED);
                    start.await();
                    if (s.tryAcquire()) {
                        success.incrementAndGet();
                        sleep(50);
                        s.release();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();
        // 30 个线程对 5 个 permit 各做一次 tryAcquire，因 Lua 原子化，成功数恰为 permits
        Assert.assertEquals("concurrent acquires must not oversell", permits, success.get());
        CamelliaRedisSemaphore cleaner = CamelliaRedisSemaphore.newSemaphore(template(), key, permits, 1000, 10000, CamelliaRedisSemaphore.PermitsMode.SEED);
        cleaner.clear();
    }

    @Test
    public void testAcquireAndRun() throws Exception {
        if (!enable) return;
        String key = newKey();
        CamelliaRedisSemaphore s1 = semaphore(key, 1, 1000, 10000);
        final AtomicInteger ran = new AtomicInteger(0);
        Assert.assertTrue(s1.acquireAndRun((Runnable) ran::incrementAndGet));
        Assert.assertEquals(1, ran.get());
        // 执行完毕后 permit 已归还，可再次获取
        Assert.assertFalse(s1.isAcquireOk());

        LockTaskResult<Integer> result = s1.acquireAndRun(() -> 42);
        Assert.assertTrue(result.isExecute());
        Assert.assertEquals(Integer.valueOf(42), result.getResult());
        s1.clear();
    }
}

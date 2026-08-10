package com.netease.nim.camellia.redis.toolkit.semaphore;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.redis.toolkit.lock.LockTaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于redis的分布式信号量。
 *
 * <p>限定 {@code permits} 个许可（permit），支持阻塞等待获取、快速失败获取、显式归还、租约续期，
 * 以及运行时动态调整上限。持有者异常退出或忘记 {@link #release()} 时，已占用的 permit 会在租约到期后自动回收，
 * 不会永久泄漏。可用作限流、熔断保护等并发许可控制的通用构建块。
 *
 * <p>接口风格对齐 {@code CamelliaRedisLock}：静态工厂构造 + try/acquire/release/renew/clear + 便捷执行方法。
 * 一个 {@code CamelliaRedisSemaphore} 实例最多持有 1 个 permit，{@code acquire} 幂等（已持有则直接返回 {@code true}）；
 * 如需并发持有多个 permit，请使用多个实例。
 *
 * <p>redis 数据结构：
 * <ul>
 *     <li>{@code key}（ZSET）：member = permitId，score = 过期时间戳(ms)</li>
 *     <li>{@code key:max}（string）：permit 上限 N，仅 {@link PermitsMode#SEED} 模式使用；{@link PermitsMode#LOCAL} 模式不持久化上限</li>
 * </ul>
 * redis cluster 部署时，{@code key} 需包含 hash tag（例如 {@code {rate-limit}:foo}），使两个 key 落在同一 slot；
 * 通过 camellia-redis-proxy / 单机 / 主从部署则无此要求。
 *
 * <p>permit 上限的确定方式由 {@link PermitsMode} 控制（构造时必填）：{@code SEED} 将上限持久化到 {@code key:max}，
 * 首次 acquire 播种（先到先得），之后由 {@link #setPermits(int)} 或外部主导；{@code LOCAL} 不持久化上限，每次 acquire
 * 用本实例 {@code permits} 对全局持有数判断，改配置即生效。共享同一 {@code key} 的实例应使用相同的 {@code PermitsMode}
 * （以及相同的 {@code permits}），混用不同模式行为未定义。
 *
 * <p>所有 permit 操作通过 Lua 脚本原子化，避免并发下的超卖与计数错乱。
 *
 * Created by caojiajun on 2026/8/7.
 */
public class CamelliaRedisSemaphore {

    /**
     * permit 上限的确定方式。
     * <ul>
     *     <li>{@link #SEED}：上限持久化到 redis 的 {@code key:max}，首次 acquire 时播种（先到先得），
     *         之后由 {@link #setPermits(int)} 或外部设置主导。适合中心化/运维集中管理上限。</li>
     *     <li>{@link #LOCAL}：上限不持久化，每次 acquire 用本实例的 {@code permits} 对全局持有数判断。
     *         共享同一 key 的实例应使用相同的 {@code permits}（通常来自同一份配置），此时等效于全局上限 N；
     *         若各实例 {@code permits} 不同，则按最宽松者生效。适合应用配置驱动、希望改配置即生效的场景。</li>
     * </ul>
     */
    public enum PermitsMode {
        /**
         * 上限持久化到 redis（key:max），首次播种，先到先得，之后由 setPermits/外部主导
         */
        SEED,
        /**
         * 上限不持久化，每次 acquire 用本实例 permits 对全局持有数判断
         */
        LOCAL
    }

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisSemaphore.class);

    private static final byte[] MAX_SUFFIX = SafeEncoder.encode(":max");

    private final CamelliaRedisTemplate template;//redis客户端
    private final byte[] key;//信号量key，ZSET 存放 permit 持有者
    private final byte[] keyMax;//上限 key，string 存放 permit 上限 N
    private int permits;//permit 上限（初始值，仅在上限 key 缺失时播种；setPermits 可调整）
    private final long acquireTimeoutMillis;//阻塞获取许可的最大等待时间
    private final long expireTimeoutMillis;//单个 permit 的租约过期时间
    private final long tryIntervalMillis;//阻塞获取时的重试间隔
    private final PermitsMode permitsMode;//permit 上限的确定方式
    private String permitId = null;//当前持有 permit 的唯一标识
    private boolean acquireOk = false;//是否已获取到 permit
    private long expireTimestamp = -1;//当前持有 permit 的过期时间戳
    private final ReentrantLock lock = new ReentrantLock();

    private CamelliaRedisSemaphore(CamelliaRedisTemplate template, byte[] key, int permits,
                                   long acquireTimeoutMillis, long expireTimeoutMillis, long tryIntervalMillis,
                                   PermitsMode permitsMode) {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must > 0");
        }
        if (expireTimeoutMillis <= 0) {
            throw new IllegalArgumentException("expireTimeoutMillis must > 0");
        }
        if (tryIntervalMillis <= 0) {
            throw new IllegalArgumentException("tryIntervalMillis must > 0");
        }
        if (acquireTimeoutMillis < 0) {
            throw new IllegalArgumentException("acquireTimeoutMillis must >= 0");
        }
        if (permitsMode == null) {
            throw new IllegalArgumentException("permitsMode is null");
        }
        this.template = template;
        this.key = key;
        this.keyMax = append(key, MAX_SUFFIX);
        this.permits = permits;
        this.acquireTimeoutMillis = acquireTimeoutMillis;
        this.expireTimeoutMillis = expireTimeoutMillis;
        this.tryIntervalMillis = tryIntervalMillis;
        this.permitsMode = permitsMode;
    }

    /**
     * 获取一个信号量对象
     * @param template redis客户端
     * @param key 信号量key
     * @param permits permit上限
     * @param acquireTimeoutMillis 阻塞获取许可的最大等待时间
     * @param expireTimeoutMillis 单个 permit 的租约过期时间
     * @param permitsMode permit 上限的确定方式（{@link PermitsMode#SEED} 或 {@link PermitsMode#LOCAL}）
     * @return 信号量对象
     */
    public static CamelliaRedisSemaphore newSemaphore(CamelliaRedisTemplate template, String key, int permits,
                                                      long acquireTimeoutMillis, long expireTimeoutMillis, PermitsMode permitsMode) {
        return new CamelliaRedisSemaphore(template, SafeEncoder.encode(key), permits, acquireTimeoutMillis, expireTimeoutMillis, 5, permitsMode);
    }

    /**
     * 获取一个信号量对象
     * @param template redis客户端
     * @param key 信号量key
     * @param permits permit上限
     * @param acquireTimeoutMillis 阻塞获取许可的最大等待时间
     * @param expireTimeoutMillis 单个 permit 的租约过期时间
     * @param permitsMode permit 上限的确定方式（{@link PermitsMode#SEED} 或 {@link PermitsMode#LOCAL}）
     * @return 信号量对象
     */
    public static CamelliaRedisSemaphore newSemaphore(CamelliaRedisTemplate template, byte[] key, int permits,
                                                      long acquireTimeoutMillis, long expireTimeoutMillis, PermitsMode permitsMode) {
        return new CamelliaRedisSemaphore(template, key, permits, acquireTimeoutMillis, expireTimeoutMillis, 5, permitsMode);
    }

    /**
     * 获取一个信号量对象
     * @param template redis客户端
     * @param key 信号量key
     * @param permits permit上限
     * @param acquireTimeoutMillis 阻塞获取许可的最大等待时间
     * @param expireTimeoutMillis 单个 permit 的租约过期时间
     * @param tryIntervalMillis 阻塞获取时的重试间隔
     * @param permitsMode permit 上限的确定方式（{@link PermitsMode#SEED} 或 {@link PermitsMode#LOCAL}）
     * @return 信号量对象
     */
    public static CamelliaRedisSemaphore newSemaphore(CamelliaRedisTemplate template, String key, int permits,
                                                      long acquireTimeoutMillis, long expireTimeoutMillis, long tryIntervalMillis, PermitsMode permitsMode) {
        return new CamelliaRedisSemaphore(template, SafeEncoder.encode(key), permits, acquireTimeoutMillis, expireTimeoutMillis, tryIntervalMillis, permitsMode);
    }

    /**
     * 获取一个信号量对象
     * @param template redis客户端
     * @param key 信号量key
     * @param permits permit上限
     * @param acquireTimeoutMillis 阻塞获取许可的最大等待时间
     * @param expireTimeoutMillis 单个 permit 的租约过期时间
     * @param tryIntervalMillis 阻塞获取时的重试间隔
     * @param permitsMode permit 上限的确定方式（{@link PermitsMode#SEED} 或 {@link PermitsMode#LOCAL}）
     * @return 信号量对象
     */
    public static CamelliaRedisSemaphore newSemaphore(CamelliaRedisTemplate template, byte[] key, int permits,
                                                      long acquireTimeoutMillis, long expireTimeoutMillis, long tryIntervalMillis, PermitsMode permitsMode) {
        return new CamelliaRedisSemaphore(template, key, permits, acquireTimeoutMillis, expireTimeoutMillis, tryIntervalMillis, permitsMode);
    }

    // 先回收过期 permit，再按 permitsMode 确定上限后分配；ARGV[6]=permitsMode(seed/local)
    private static final byte[] ACQUIRE_SCRIPT = SafeEncoder.encode(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1]) " +
                    "local max " +
                    "if ARGV[6] == 'local' then max = tonumber(ARGV[4]) else if redis.call('EXISTS', KEYS[2]) == 0 then redis.call('SET', KEYS[2], ARGV[4]) end max = tonumber(redis.call('GET', KEYS[2])) end " +
                    "local current = redis.call('ZCARD', KEYS[1]) " +
                    "if current < max then redis.call('ZADD', KEYS[1], ARGV[3], ARGV[2]) redis.call('PEXPIRE', KEYS[1], ARGV[5]) if ARGV[6] ~= 'local' then redis.call('PEXPIRE', KEYS[2], ARGV[5]) end return 1 else return 0 end");
    /**
     * 尝试获取一个 permit，若获取不到，则立即返回
     * @return 成功/失败
     */
    public boolean tryAcquire() {
        lock.lock();
        try {
            if (isAcquireOk()) return true;
            String permitId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            long expireTimestamp = now + expireTimeoutMillis;
            try {
                Object result = template.eval(ACQUIRE_SCRIPT, 2, key, keyMax,
                        SafeEncoder.encode(String.valueOf(now)),
                        SafeEncoder.encode(permitId),
                        SafeEncoder.encode(String.valueOf(expireTimestamp)),
                        SafeEncoder.encode(String.valueOf(permits)),
                        SafeEncoder.encode(String.valueOf(expireTimeoutMillis)),
                        SafeEncoder.encode(permitsMode == PermitsMode.LOCAL ? "local" : "seed"));
                boolean ok = result != null && String.valueOf(result).equals("1");
                if (ok) {
                    this.permitId = permitId;
                    this.acquireOk = true;
                    this.expireTimestamp = expireTimestamp;
                }
                return ok;
            } catch (Exception e) {
                logger.error("tryAcquire error, key = {}, permitId = {}", key, permitId, e);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 当前实例是否仍持有有效 permit
     * @return 成功/失败
     */
    public boolean isAcquireOk() {
        lock.lock();
        try {
            if (!acquireOk) return false;
            if (System.currentTimeMillis() < this.expireTimestamp) {
                return true;
            }
            //本地判过期时，再向 redis 确认 permit 是否仍有效（按 score 判断逻辑过期）
            try {
                Object result = template.eval(CHECK_SCRIPT, 1, key, SafeEncoder.encode(permitId));
                long now = System.currentTimeMillis();
                boolean ok = false;
                if (result != null) {
                    try {
                        double score = Double.parseDouble(String.valueOf(result));
                        if (score > now) {
                            ok = true;
                            this.expireTimestamp = (long) score;
                        }
                    } catch (NumberFormatException ignore) {
                    }
                }
                if (!ok) {
                    acquireOk = false;
                    permitId = null;
                    expireTimestamp = -1;
                }
                return ok;
            } catch (Exception e) {
                logger.error("isAcquireOk error, key = {}, permitId = {}", key, permitId, e);
                acquireOk = false;
                permitId = null;
                expireTimestamp = -1;
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    // 返回自己持有 permit 的 score（不存在返回 0），用于本地过期后判断是否逻辑过期
    private static final byte[] CHECK_SCRIPT = SafeEncoder.encode(
            "local s = redis.call('ZSCORE', KEYS[1], ARGV[1]) if s then return s else return 0 end");

    /**
     * 尝试获取一个 permit，若没有，则会等待重试，直到 acquireTimeoutMillis 超时
     * @return 成功/失败
     */
    public boolean acquire() {
        lock.lock();
        try {
            if (isAcquireOk()) return true;
            long start = System.currentTimeMillis();
            while (true) {
                boolean ok = tryAcquire();
                if (ok) {
                    return true;
                }
                try {
                    Thread.sleep(tryIntervalMillis);
                } catch (InterruptedException e) {
                    logger.error("sleep error", e);
                }
                if (System.currentTimeMillis() - start > acquireTimeoutMillis) {
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // 仅续期自己持有的 permit（permitId 为持有方私有凭据，他人无法续期），并刷新 key 租约
    private static final byte[] RENEW_SCRIPT = SafeEncoder.encode(
            "if redis.call('ZSCORE', KEYS[1], ARGV[1]) then redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1]) redis.call('PEXPIRE', KEYS[1], ARGV[3]) redis.call('PEXPIRE', KEYS[2], ARGV[3]) return 1 else return 0 end");
    /**
     * 尝试对自己持有的 permit 进行续期，刷新租约过期时间
     * @return 成功/失败
     */
    public boolean renew() {
        lock.lock();
        try {
            if (!acquireOk) {
                return false;
            }
            try {
                long newExpireTimestamp = System.currentTimeMillis() + expireTimeoutMillis;
                Object result = template.eval(RENEW_SCRIPT, 2, key, keyMax,
                        SafeEncoder.encode(permitId), SafeEncoder.encode(String.valueOf(newExpireTimestamp)),
                        SafeEncoder.encode(String.valueOf(expireTimeoutMillis)));
                if (result != null && String.valueOf(result).equals("1")) {
                    this.expireTimestamp = newExpireTimestamp;
                    return true;
                }
                this.acquireOk = false;
                this.permitId = null;
                this.expireTimestamp = -1;
                return false;
            } catch (Exception e) {
                logger.error("renew error, key = {}, permitId = {}", key, permitId, e);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    // 仅归还自己持有的 permit
    private static final byte[] RELEASE_SCRIPT = SafeEncoder.encode(
            "return redis.call('ZREM', KEYS[1], ARGV[1])");
    /**
     * 释放自己持有的 permit
     * @return 成功/失败
     */
    public boolean release() {
        lock.lock();
        try {
            if (!acquireOk) return false;
            try {
                Object eval = template.eval(RELEASE_SCRIPT, 1, key, SafeEncoder.encode(permitId));
                boolean ok = eval != null && String.valueOf(eval).equals("1");
                acquireOk = false;
                permitId = null;
                expireTimestamp = -1;
                return ok;
            } catch (Exception e) {
                logger.error("release error, key = {}, permitId = {}", key, permitId, e);
                acquireOk = false;
                permitId = null;
                expireTimestamp = -1;
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    // 先回收过期 permit，再更新上限，并刷新 key 租约
    private static final byte[] SET_PERMITS_SCRIPT = SafeEncoder.encode(
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[2]) " +
                    "redis.call('SET', KEYS[2], ARGV[1]) redis.call('PEXPIRE', KEYS[1], ARGV[3]) redis.call('PEXPIRE', KEYS[2], ARGV[3]) return 1");
    /**
     * 运行时调整 permit 上限 N（软缩容：不强制回收已发放的 permit，新上限对后续 acquire 立即生效）
     * @param permits 新的 permit 上限，必须大于 0
     * @return 成功/失败
     */
    public boolean setPermits(int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must > 0");
        }
        lock.lock();
        try {
            if (permitsMode == PermitsMode.LOCAL) {
                // LOCAL 模式上限不持久化，仅更新本实例的 permits
                this.permits = permits;
                return true;
            }
            long now = System.currentTimeMillis();
            try {
                Object result = template.eval(SET_PERMITS_SCRIPT, 2, key, keyMax,
                        SafeEncoder.encode(String.valueOf(permits)),
                        SafeEncoder.encode(String.valueOf(now)),
                        SafeEncoder.encode(String.valueOf(expireTimeoutMillis)));
                boolean ok = result != null && String.valueOf(result).equals("1");
                if (ok) {
                    this.permits = permits;
                }
                return ok;
            } catch (Exception e) {
                logger.error("setPermits error, key = {}", key, e);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清空信号量相关的 key，会释放不是自己获取到的 permit
     * @return 成功/失败
     */
    public boolean clear() {
        lock.lock();
        try {
            try {
                Long del = template.del(key);
                if (permitsMode != PermitsMode.LOCAL) {
                    template.del(keyMax);
                }
                acquireOk = false;
                permitId = null;
                expireTimestamp = -1;
                return del != null && del > 0;
            } catch (Exception e) {
                logger.error("clear error, key = {}", key, e);
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 尝试获取一个 permit，并且执行一个任务，获取失败时会等待直到超时失败
     * @param runnable 任务
     * @return 成功/失败
     */
    public boolean acquireAndRun(Runnable runnable) {
        boolean acquire = acquire();
        if (!acquire) return false;
        try {
            runnable.run();
            return true;
        } finally {
            release();
        }
    }

    /**
     * 尝试获取一个 permit，并且执行一个带返回值的任务，获取失败时会等待直到超时失败
     * @param callable 任务
     * @param <T> 任务返回值类型
     * @return 任务返回值
     * @throws Exception 异常
     */
    public <T> LockTaskResult<T> acquireAndRun(Callable<T> callable) throws Exception {
        boolean acquire = acquire();
        if (!acquire) return new LockTaskResult<>(false, null);
        try {
            T result = callable.call();
            return new LockTaskResult<>(true, result);
        } finally {
            release();
        }
    }

    /**
     * 尝试获取一个 permit，并且执行一个任务，获取失败时会立即返回
     * @param runnable 任务
     * @return 成功/失败
     */
    public boolean tryAcquireAndRun(Runnable runnable) {
        boolean acquire = tryAcquire();
        if (!acquire) return false;
        try {
            runnable.run();
            return true;
        } finally {
            release();
        }
    }

    /**
     * 尝试获取一个 permit，并且执行一个带返回值的任务，获取失败时会立即返回
     * @param callable 任务
     * @param <T> 任务返回值类型
     * @return 任务返回值
     * @throws Exception 异常
     */
    public <T> LockTaskResult<T> tryAcquireAndRun(Callable<T> callable) throws Exception {
        boolean acquire = tryAcquire();
        if (!acquire) return new LockTaskResult<>(false, null);
        try {
            T result = callable.call();
            return new LockTaskResult<>(true, result);
        } finally {
            release();
        }
    }

    /**
     * 获取当前 permit 的过期时间戳，毫秒
     * @return 当前 permit 的过期时间戳，未持有时返回 -1
     */
    public long getExpireTimestamp() {
        if (!acquireOk) return -1;
        return expireTimestamp;
    }

    /**
     * 获取信号量 key
     * @return 信号量 key
     */
    public byte[] getKey() {
        return key;
    }

    /**
     * 获取当前持有的 permitId
     * @return 当前持有的 permitId，未持有时返回 null
     */
    public String getPermitId() {
        return permitId;
    }

    /**
     * 获取 permit 上限 N。
     * <p>SEED 模式下实际上限以 redis {@code key:max} 为准（读取一次 redis）；{@code key:max} 不存在或读取失败时回退本实例配置。
     * LOCAL 模式直接返回本实例 {@code permits}。
     * @return permit 上限 N
     */
    public int getPermits() {
        lock.lock();
        try {
            if (permitsMode == PermitsMode.LOCAL) {
                return permits;
            }
            try {
                byte[] value = template.get(keyMax);
                if (value != null) {
                    return Integer.parseInt(SafeEncoder.encode(value));
                }
            } catch (Exception e) {
                logger.error("getPermits error, key = {}", key, e);
            }
            return permits;
        } finally {
            lock.unlock();
        }
    }

    private static byte[] append(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}

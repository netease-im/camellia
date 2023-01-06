package com.netease.nim.camellia.tools.circuitbreaker;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 一个简单的熔断器计算器
 * Created by caojiajun on 2022/3/25
 */
public class CamelliaCircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCircuitBreaker.class);

    private static final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            new DefaultThreadFactory("camellia-circuit-breaker-schedule"));
    private static final AtomicLong idGen = new AtomicLong();

    private final CircuitBreakerConfig config;
    private final String name;

    private final int bucketSize;
    private final LongAdder[] successBuckets;
    private final LongAdder[] failBuckets;
    private int index;

    private final AtomicBoolean circuitBreakerOpen = new AtomicBoolean(false);

    private volatile long openTimestamp = 0;//熔断器打开的时间戳
    private final AtomicLong lastSingleTestTimestamp = new AtomicLong(0L);//上一次探测的时间戳（半开）

    public CamelliaCircuitBreaker() {
        this(new CircuitBreakerConfig());
    }

    public CamelliaCircuitBreaker(CircuitBreakerConfig config) {
        this.config = config;
        this.name = "[" + config.getName() + "][id=" + idGen.incrementAndGet() + "]";
        this.bucketSize = config.getStatisticSlidingWindowBucketSize();
        this.successBuckets = new LongAdder[bucketSize];
        this.failBuckets = new LongAdder[bucketSize];
        for (int i=0; i<bucketSize; i++) {
            this.successBuckets[i] = new LongAdder();
            this.failBuckets[i] = new LongAdder();
        }
        this.index = 0;
        long slidePeriodMillis = config.getStatisticSlidingWindowTime() / bucketSize;
        scheduledExecutorService.scheduleAtFixedRate(this::slideToNextBucket, slidePeriodMillis, slidePeriodMillis, TimeUnit.MILLISECONDS);
        logger.info("camellia-circuit-breaker init success, name = {}", name);
    }

    /**
     * 获取名字
     */
    public String getName() {
        return name;
    }

    /**
     * 当前的熔断器是否是打开状态
     * @return 结果
     */
    public boolean isOpen() {
        try {
            if (!config.getEnable().get()) {
                if (circuitBreakerOpen.get()) {
                    circuitBreakerOpen.set(false);
                }
                return false;
            }
            if (config.getForceOpen().get()) return true;
            return circuitBreakerOpen.get();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 是否允许发起请求
     */
    public boolean allowRequest() {
        try {
            if (!config.getEnable().get()) {
                if (circuitBreakerOpen.get()) {
                    circuitBreakerOpen.set(false);
                }
                return true;
            }
            if (config.getForceOpen().get()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("camellia circuit breaker return allowRequest=false for forceOpen, name = {}", name);
                }
                return false;
            }
            //如果熔断器没有打开，直接返回true
            if (!circuitBreakerOpen.get()) {
                return true;
            }
            //如果熔断器已经打开了，则判定是否需要漏过去一个请求（也就是半开状态）
            if (System.currentTimeMillis() - openTimestamp > config.getSingleTestIntervalMillis().get()) {
                long lastSingleTestTime = lastSingleTestTimestamp.get();
                if (System.currentTimeMillis() - lastSingleTestTime > config.getSingleTestIntervalMillis().get()) {
                    boolean allow = lastSingleTestTimestamp.compareAndSet(lastSingleTestTime, System.currentTimeMillis());
                    if (allow && config.getLogEnable().get()) {
                        logger.info("camellia circuit breaker half-open, name = {}", name);
                    }
                    return allow;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("camellia circuit breaker return allowRequest=false for open, name = {}", name);
            }
            return false;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return true;
        }
    }

    /**
     * 增加一次成功
     */
    public void incrementSuccess() {
        try {
            if (!config.getEnable().get()) return;
            incrementSuccess(1L);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 增加若干次成功
     * @param count 次数
     */
    public void incrementSuccess(long count) {
        try {
            if (!config.getEnable().get()) return;
            successBuckets[index].add(count);
            if (logger.isDebugEnabled()) {
                logger.debug("camellia circuit breaker incrementSuccess, count = {}, name = {}", count, name);
            }
            //如果此时熔断器是打开的，则说明探测成功了，则需要重置一下状态
            if (circuitBreakerOpen.get() && circuitBreakerOpen.compareAndSet(true, false)) {
                if (config.getLogEnable().get()) {
                    logger.info("camellia circuit breaker close, name = {}", name);
                }
                for (LongAdder adder : failBuckets) {
                    adder.reset();
                }
                for (LongAdder adder : successBuckets) {
                    adder.reset();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 增加一次失败
     */
    public void incrementFail() {
        try {
            if (!config.getEnable().get()) return;
            incrementFail(1L);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 增加若干次失败
     * @param count 次数
     */
    public void incrementFail(long count) {
        try {
            if (!config.getEnable().get()) return;
            failBuckets[index].add(count);
            if (logger.isDebugEnabled()) {
                logger.debug("camellia circuit breaker incrementFail, count = {}, name = {}", count, name);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }


    //滑动到下一个时间窗口
    private void slideToNextBucket() {
        try {
            //计算下一个游标
            int nextIndex;
            if (index == bucketSize - 1) {
                nextIndex = 0;
            } else {
                nextIndex = index + 1;
            }
            //先清空老数据
            successBuckets[nextIndex].reset();
            failBuckets[nextIndex].reset();
            //再移动游标
            index = nextIndex;
            //计算统计数据
            long totalSuccess = 0;
            for (LongAdder adder : successBuckets) {
                totalSuccess += adder.sum();
            }
            long totalFail = 0;
            for (LongAdder adder : failBuckets) {
                totalFail += adder.sum();
            }
            //如果总的请求数超过了基准值，才会尝试计算失败率
            if (totalSuccess + totalFail > config.getRequestVolumeThreshold().get() && totalFail > 0) {
                //如果失败率超过了阈值，则断路器打开
                double failRate = ((double)totalFail) / (totalSuccess + totalFail);
                if (failRate > config.getFailThresholdPercentage().get()) {
                    boolean open = circuitBreakerOpen.compareAndSet(false, true);
                    if (open) {
                        openTimestamp = System.currentTimeMillis();
                    }
                    if (open && config.getLogEnable().get()) {
                        logger.info("camellia circuit breaker open, name = {}, success = {}, fail = {}, fail-rate = {}, fail-threshold-percentage = {}",
                                name, totalSuccess, totalFail, failRate, config.getFailThresholdPercentage().get());
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

package com.netease.nim.camellia.id.gen.snowflake;

import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.lock.CamelliaRedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis生成workerId的实现
 * Created by caojiajun on 2021/9/18
 */
public class RedisWorkerIdGen implements WorkerIdGen {

    private static final Logger logger = LoggerFactory.getLogger(RedisWorkerIdGen.class);

    private final CamelliaRedisTemplate template;
    private final String namespace;
    private final long lockExpireMillis;
    private final long renewIntervalMillis;
    private final boolean exitIfRenewFail;

    public RedisWorkerIdGen(CamelliaRedisTemplate template) {
        this(template, CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.namespace);
    }

    public RedisWorkerIdGen(CamelliaRedisTemplate template, String namespace) {
        this(template, namespace, CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.lockExpireMillis,  CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.renewIntervalMillis,  CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.exitIfRenewFail);
    }

    public RedisWorkerIdGen(CamelliaRedisTemplate template, String namespace, long lockExpireMillis, long renewIntervalMillis) {
        this(template, namespace, lockExpireMillis, renewIntervalMillis,  CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.exitIfRenewFail);
    }

    public RedisWorkerIdGen(CamelliaRedisTemplate template, String namespace, long lockExpireMillis, long renewIntervalMillis, boolean exitIfRenewFail) {
        this.template = template;
        this.namespace = namespace;
        this.lockExpireMillis = lockExpireMillis;
        this.renewIntervalMillis = renewIntervalMillis;
        this.exitIfRenewFail = exitIfRenewFail;
        logger.info("RedisWorkerIdGen init success, namespace = {}, lockExpireMillis = {}, renewIntervalMillis = {}, exitIfRenewFail = {}",
                namespace, lockExpireMillis, renewIntervalMillis, exitIfRenewFail);
    }

    @Override
    public long genWorkerId(long maxWorkerId) {
        String hostAddress;
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            hostAddress = UUID.randomUUID().toString();
        }
        long initialId = Math.abs(hostAddress.hashCode()) % (maxWorkerId + 1);
        long workerId = initialId;
        do {
            CamelliaRedisLock redisLock = CamelliaRedisLock.newLock(template, lockKey(workerId), lockExpireMillis, lockExpireMillis);
            boolean tryLock = redisLock.tryLock();
            if (tryLock) {
                startRenewThread(workerId, redisLock);
                return workerId;
            }
            workerId++;
            workerId = Math.abs(workerId) % (maxWorkerId + 1);
        } while (workerId != initialId);

        throw new CamelliaIdGenException("workerId gen fail");
    }

    private String lockKey(long workerId) {
        return  "camellia_snowflake|" + namespace + "|" + workerId + "~lock";
    }

    private void startRenewThread(long workerId, CamelliaRedisLock redisLock) {
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-snowflake-redis-worker-id-renew"))
                .scheduleAtFixedRate(() -> {
                    boolean renew = redisLock.renew();
                    if (renew) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("renew namespace = {}, workerId = {} success", namespace, workerId);
                        }
                    } else {
                        logger.error("renew namespace = {}, workerId = {} fail", namespace, workerId);
                        if (exitIfRenewFail) {
                            logger.error("[WARNING] System will exit for renew namespace = {}, workerId = {} fail", namespace, workerId);
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException e) {
                                logger.error(e.getMessage(), e);
                            }
                            System.exit(-1);
                        }
                    }
                }, renewIntervalMillis, renewIntervalMillis, TimeUnit.MILLISECONDS);
    }
}

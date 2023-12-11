package com.netease.nim.camellia.id.gen.springboot.snowflake;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by caojiajun on 2021/9/26
 */
@ConfigurationProperties(prefix = "camellia-id-gen-snowflake")
public class CamelliaIdGenSnowflakeProperties {

    private long twepoch = CamelliaIdGenConstants.Snowflake.twepoch;

    private int regionBits = CamelliaIdGenConstants.Snowflake.regionBits;

    private int workerIdBits = CamelliaIdGenConstants.Snowflake.workerIdBits;

    private int sequenceBits = CamelliaIdGenConstants.Snowflake.sequenceBits;

    private int regionId;

    private long workerId = -1;

    private RedisWorkerIdGenConf redisWorkerIdGenConf = new RedisWorkerIdGenConf();

    public static class RedisWorkerIdGenConf {
        private String namespace = CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.namespace;
        private long lockExpireMillis = CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.lockExpireMillis;
        private long renewIntervalMillis = CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.renewIntervalMillis;
        private boolean exitIfRenewFail = CamelliaIdGenConstants.Snowflake.RedisWorkerIdGen.exitIfRenewFail;

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public long getLockExpireMillis() {
            return lockExpireMillis;
        }

        public void setLockExpireMillis(long lockExpireMillis) {
            this.lockExpireMillis = lockExpireMillis;
        }

        public long getRenewIntervalMillis() {
            return renewIntervalMillis;
        }

        public void setRenewIntervalMillis(long renewIntervalMillis) {
            this.renewIntervalMillis = renewIntervalMillis;
        }

        public boolean isExitIfRenewFail() {
            return exitIfRenewFail;
        }

        public void setExitIfRenewFail(boolean exitIfRenewFail) {
            this.exitIfRenewFail = exitIfRenewFail;
        }
    }

    public long getTwepoch() {
        return twepoch;
    }

    public void setTwepoch(long twepoch) {
        this.twepoch = twepoch;
    }

    public int getRegionBits() {
        return regionBits;
    }

    public void setRegionBits(int regionBits) {
        this.regionBits = regionBits;
    }

    public int getWorkerIdBits() {
        return workerIdBits;
    }

    public void setWorkerIdBits(int workerIdBits) {
        this.workerIdBits = workerIdBits;
    }

    public int getSequenceBits() {
        return sequenceBits;
    }

    public void setSequenceBits(int sequenceBits) {
        this.sequenceBits = sequenceBits;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workerId) {
        this.workerId = workerId;
    }

    public RedisWorkerIdGenConf getRedisWorkerIdGenConf() {
        return redisWorkerIdGenConf;
    }

    public void setRedisWorkerIdGenConf(RedisWorkerIdGenConf redisWorkerIdGenConf) {
        this.redisWorkerIdGenConf = redisWorkerIdGenConf;
    }
}

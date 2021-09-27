package com.netease.nim.camellia.id.gen.snowflake;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 雪花算法生成id，趋势递增
 * 64位数字
 * 1位（不用）+ 41位（时间戳）+ r位（单元id）+ w位（机器id）+ s位（序列号）
 * 其中r+w+s<=22位
 * Created by caojiajun on 2021/9/18
 */
public class CamelliaSnowflakeIdGen implements ICamelliaSnowflakeIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaSnowflakeIdGen.class);

    private final long twepoch;//起止时间戳
    private final long regionId;//regionId
    private final long workerId;//workerId

    private final int regionBits;//单元id占用的位数，大于等于0
    private final int workerIdBits;//机器id占用的位数，大于0
    private final int sequenceBits;//序列号占用的位数，大于0

    private final long maxSequence;

    private volatile long lastTimestamp = -1;

    private long sequence;

    public CamelliaSnowflakeIdGen(CamelliaSnowflakeConfig config) {
        this.twepoch = config.getTwepoch();
        this.regionId = config.getRegionId();
        this.regionBits = config.getRegionBits();
        this.sequenceBits = config.getSequenceBits();
        this.workerIdBits = config.getWorkerIdBits();
        if (this.regionBits + workerIdBits + this.sequenceBits > 22) {
            throw new CamelliaIdGenException("regionBits + workIdBits + sequenceBits should <= 22");
        }
        if (this.regionBits < 0) {
            throw new CamelliaIdGenException("regionBits should >= 0");
        }
        if (this.workerIdBits <= 0) {
            throw new CamelliaIdGenException("workerIdBits should > 0");
        }
        if (this.sequenceBits < 0) {
            throw new CamelliaIdGenException("sequenceBits should > 0");
        }
        if (this.twepoch >= System.currentTimeMillis()) {
            throw new CamelliaIdGenException("twepoch should < now");
        }

        long maxWorkerId = (1L << config.getWorkerIdBits()) - 1;
        long maxRegionId = (1L << config.getRegionBits()) - 1;
        this.maxSequence = (1L << config.getSequenceBits()) - 1;

        this.workerId = config.getWorkerIdGen().genWorkerId(maxWorkerId);
        if (workerId > maxWorkerId) {
            throw new CamelliaIdGenException("workerId too long");
        }
        if (this.regionId > maxRegionId) {
            throw new CamelliaIdGenException("regionId too long");
        }
        logger.info("CamelliaSnowflakeIdGen init success, twepoch = {}, regionId = {}, workerId = {}, " +
                "regionBits = {}, workerIdBits = {}, sequenceBits = {}", twepoch, regionId, workerId, regionBits, workerIdBits, sequenceBits);
    }

    @Override
    public synchronized long genId() {
        long now = System.currentTimeMillis();
        //处理时间戳回退问题
        if (now < lastTimestamp) {
            logger.warn("timestamp back to the past, try sleep and retry");
            //计算回退了多少ms
            long backMs = lastTimestamp - now;
            //等待一下回退的ms数*2
            try {
                TimeUnit.MILLISECONDS.sleep(backMs * 2);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            now = System.currentTimeMillis();
            if (now < lastTimestamp) {
                throw new CamelliaIdGenException("timestamp back to the past");
            }
        }
        if (lastTimestamp == now) {
            sequence ++;
            if (sequence > maxSequence) {
                if (logger.isDebugEnabled()) {
                    logger.debug("ids exhaust in current ms, wait to next ms");
                }
                //1ms内的id都取完了，则等到下一ms再取
                while (now <= lastTimestamp) {
                    now = System.currentTimeMillis();
                }
                sequence = sequenceInit();
                lastTimestamp = now;
                if (logger.isDebugEnabled()) {
                    logger.debug("sequence init, timestamp = {}, sequence = {}", lastTimestamp, sequence);
                }
            }
        } else {
            //本ms内的第一个id
            sequence = sequenceInit();
            lastTimestamp = now;
            if (logger.isDebugEnabled()) {
                logger.debug("sequence init, timestamp = {}, sequence = {}", lastTimestamp, sequence);
            }
        }
        return ((now - twepoch) << (regionBits + workerIdBits + sequenceBits)) | (regionId << (workerIdBits + sequenceBits)) | (workerId << sequenceBits) | sequence;
    }

    //选择一个随机值，而不是每次都从0开始
    private long sequenceInit() {
        if (maxSequence > 128L) {
            return ThreadLocalRandom.current().nextLong(64L);
        } else if (maxSequence <= 3) {
            return 0;
        } else {
            return ThreadLocalRandom.current().nextLong((maxSequence / 2));
        }
    }
}

package com.netease.nim.camellia.id.gen.snowflake;


/**
 * Created by caojiajun on 2021/9/18
 */
public class CamelliaSnowflakeConfig {

    private long twepoch = 1631203200000L;//默认：2021-09-10 00:00:00

    /**
     * regionBits + workerIdBits + sequenceBits <= 22
     */
    //单元id所占的位数
    //默认为0，表示不需要单元id
    //如果为4，则表示最多支持16个单元id
    private int regionBits = 0;
    //workerId所占的位数
    //默认10，表示最多支持1024个workerId
    private int workerIdBits = 10;//默认10位，表示最多支持1024个发号器进程
    //sequence所占的位数
    //默认12，表示每ms最多支持生成4096个序号
    private int sequenceBits = 12;

    private long regionId;//regionId，位数不超过regionBits

    private WorkerIdGen workerIdGen;//workerId生成器

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

    public long getRegionId() {
        return regionId;
    }

    public void setRegionId(long regionId) {
        this.regionId = regionId;
    }

    public WorkerIdGen getWorkerIdGen() {
        return workerIdGen;
    }

    public void setWorkerIdGen(WorkerIdGen workerIdGen) {
        this.workerIdGen = workerIdGen;
    }
}

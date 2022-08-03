package com.netease.nim.camellia.redis.toolkit.freq;

/**
 * Created by caojiajun on 2022/8/3
 */
public class CamelliaFreqResponse {

    public static CamelliaFreqResponse DEFAULT_PASS = new CamelliaFreqResponse(true, -1, null);

    private boolean pass;
    private long current;
    private CamelliaFreqType freqType;

    public CamelliaFreqResponse() {
    }

    public CamelliaFreqResponse(boolean pass, long current, CamelliaFreqType freqType) {
        this.pass = pass;
        this.current = current;
        this.freqType = freqType;
    }

    public boolean isPass() {
        return pass;
    }

    public long getCurrent() {
        return current;
    }

    public CamelliaFreqType getFreqType() {
        return freqType;
    }
}

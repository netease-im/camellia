package com.netease.nim.camellia.redis.toolkit.freq;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2022/8/1
 */
public class CamelliaFreq {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaFreq.class);

    private final CamelliaStandaloneFreq standaloneFreq;
    private CamelliaClusterFreq clusterFreq;

    public CamelliaFreq(CamelliaRedisTemplate template, int standaloneCapacity) {
        this.standaloneFreq = new CamelliaStandaloneFreq(standaloneCapacity);
        this.clusterFreq = new CamelliaClusterFreq(template);
    }

    public CamelliaFreq(CamelliaRedisTemplate template) {
        this.standaloneFreq = new CamelliaStandaloneFreq();
        this.clusterFreq = new CamelliaClusterFreq(template);
    }

    public CamelliaFreq(int standaloneCapacity) {
        this.standaloneFreq = new CamelliaStandaloneFreq(standaloneCapacity);
    }

    public CamelliaFreq() {
        this.standaloneFreq = new CamelliaStandaloneFreq();
    }

    public CamelliaFreqResponse checkFreqPass(String freqKey, CamelliaFreqType freqType, CamelliaFreqConfig freqConfig) {
        return checkFreqPass(freqKey, 1, freqType, freqConfig);
    }

    public CamelliaFreqResponse checkFreqPass(String freqKey, int delta, CamelliaFreqType freqType, CamelliaFreqConfig freqConfig) {
        try {
            if (delta <= 0) {
                logger.warn("point is null, return pass, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig));
                return CamelliaFreqResponse.DEFAULT_PASS;
            }
            if (freqType == null) {
                logger.warn("freqType is null, return pass, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig));
                return CamelliaFreqResponse.DEFAULT_PASS;
            }
            if (freqType == CamelliaFreqType.STANDALONE) {
                return standaloneFreq.checkFreqPass(freqKey, delta, freqConfig);
            } else if (freqType == CamelliaFreqType.CLUSTER) {
                if (clusterFreq == null) {
                    logger.warn("CamelliaClusterFreq not init, return pass, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig));
                    return CamelliaFreqResponse.DEFAULT_PASS;
                }
                return clusterFreq.checkFreqPass(freqKey, delta, freqConfig);
            } else if (freqType == CamelliaFreqType.MISC) {
                if (clusterFreq == null) {
                    logger.warn("CamelliaClusterFreq not init, return pass, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig));
                    return CamelliaFreqResponse.DEFAULT_PASS;
                }
                CamelliaFreqResponse response = standaloneFreq.checkFreqPass(freqKey, delta, freqConfig);
                if (!response.isPass()) {
                    return response;
                }
                return clusterFreq.checkFreqPass(freqKey, delta, freqConfig);
            }
            return CamelliaFreqResponse.DEFAULT_PASS;
        } catch (Throwable e) {
            logger.error("checkFreqPass error, return pass, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig), e);
            return CamelliaFreqResponse.DEFAULT_PASS;
        }
    }
}

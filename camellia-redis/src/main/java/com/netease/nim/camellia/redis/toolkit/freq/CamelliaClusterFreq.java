package com.netease.nim.camellia.redis.toolkit.freq;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2022/8/2
 */
public class CamelliaClusterFreq {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaClusterFreq.class);
    private final CamelliaRedisTemplate template;

    public CamelliaClusterFreq(CamelliaRedisTemplate template) {
        this.template = template;
    }

    public CamelliaFreqResponse checkFreqPass(String freqKey, CamelliaFreqConfig freqConfig) {
        return checkFreqPass(freqKey, 1, freqConfig);
    }

    public CamelliaFreqResponse checkFreqPass(String freqKey, int delta, CamelliaFreqConfig freqConfig) {
        try {
            Object curObj = template.eval("local x = redis.call('incrBy', KEYS[1], ARGV[1])\n" +
                    "if x == tonumber(ARGV[1]) then\n" +
                    "\tredis.call('pexpire', KEYS[1], ARGV[2])\n" +
                    "end\n" +
                    "return x", 1, freqKey, String.valueOf(delta), String.valueOf(freqConfig.getCheckTime()));
            long current = Long.parseLong(String.valueOf(curObj));
            boolean pass = current <= freqConfig.getThreshold();
            if (!pass) {
                if (freqConfig.getBanTime() > 0) {
                    template.pexpire(freqKey, freqConfig.getBanTime());
                }
            }
            return new CamelliaFreqResponse(pass, current, CamelliaFreqType.CLUSTER);
        } catch (Exception e) {
            logger.error("checkFreqPass error, freqKey = {}, delta = {}, freqConfig = {}", freqKey, delta, JSONObject.toJSONString(freqConfig), e);
        }
        return CamelliaFreqResponse.DEFAULT_PASS;
    }
}

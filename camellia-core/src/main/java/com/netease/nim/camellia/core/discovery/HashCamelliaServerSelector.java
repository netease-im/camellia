package com.netease.nim.camellia.core.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class HashCamelliaServerSelector<T> implements CamelliaServerSelector<T> {

    private static final Logger logger = LoggerFactory.getLogger(HashCamelliaServerSelector.class);

    @Override
    public T pick(List<T> list, Object loadBalanceKey) {
        try {
            if (list == null || list.isEmpty()) {
                return null;
            }
            int size = list.size();
            if (size == 1) {
                if (GlobalDiscoveryEnv.logInfoEnable) {
                    logger.info("pick server by hash, only one, loadBalanceKey = {}, server = {}", loadBalanceKey, list);
                }
                return list.get(0);
            }
            int index;
            if (loadBalanceKey == null) {
                index = ThreadLocalRandom.current().nextInt(list.size());
            } else {
                index = Math.abs(loadBalanceKey.hashCode()) % list.size();
            }
            if (GlobalDiscoveryEnv.logInfoEnable) {
                logger.info("pick server by hash, loadBalanceKey = {}, index = {}, list = {}", loadBalanceKey, index, list);
            }
            return list.get(index);
        } catch (Exception e) {
            return null;
        }
    }

}

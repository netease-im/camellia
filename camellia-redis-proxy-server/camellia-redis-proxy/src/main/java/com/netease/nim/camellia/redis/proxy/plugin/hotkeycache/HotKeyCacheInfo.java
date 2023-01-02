package com.netease.nim.camellia.redis.proxy.plugin.hotkeycache;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/11/5
 */
public class HotKeyCacheInfo {

    private List<Stats> statsList = new ArrayList<>();

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public static class Stats {
        private byte[] key;
        private long hitCount;

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }

        public long getHitCount() {
            return hitCount;
        }

        public void setHitCount(long hitCount) {
            this.hitCount = hitCount;
        }
    }
}

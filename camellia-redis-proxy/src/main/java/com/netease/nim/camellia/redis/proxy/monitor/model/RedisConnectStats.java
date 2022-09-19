package com.netease.nim.camellia.redis.proxy.monitor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2022/9/16
 */
public class RedisConnectStats {
    private int connectCount;
    private List<Detail> detailList = new ArrayList<>();

    public int getConnectCount() {
        return connectCount;
    }

    public void setConnectCount(int connectCount) {
        this.connectCount = connectCount;
    }

    public List<Detail> getDetailList() {
        return detailList;
    }

    public void setDetailList(List<Detail> detailList) {
        this.detailList = detailList;
    }

    public static class Detail {
        private String addr;
        private int connectCount;

        public String getAddr() {
            return addr;
        }

        public void setAddr(String addr) {
            this.addr = addr;
        }

        public int getConnectCount() {
            return connectCount;
        }

        public void setConnectCount(int connectCount) {
            this.connectCount = connectCount;
        }
    }
}

package com.netease.nim.camellia.hot.key.server.monitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/5/11
 */
public class TrafficStats {

    public long total;
    private List<Stats> statsList = new ArrayList<>();

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public static class Stats {
        private String namespace;
        private Type type;
        private long count;

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }

    public static enum Type {
        RULE_NOT_MATCH(1),
        NORMAL(2),
        HOT(3),
        ;
        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Type getByValue(int value) {
            for (Type type : Type.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return null;
        }

    }
}

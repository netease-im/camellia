package com.netease.nim.camellia.external.call.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/2/27
 */
public class ExternalCallInputStats {

    private String instanceId;
    private String namespace;
    private List<Stats> statsList = new ArrayList<>();

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public static class Stats {
        private String isolationKey;
        private long input;

        public String getIsolationKey() {
            return isolationKey;
        }

        public void setIsolationKey(String isolationKey) {
            this.isolationKey = isolationKey;
        }

        public long getInput() {
            return input;
        }

        public void setInput(long input) {
            this.input = input;
        }
    }
}

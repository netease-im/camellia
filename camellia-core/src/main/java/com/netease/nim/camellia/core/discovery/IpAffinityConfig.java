package com.netease.nim.camellia.core.discovery;

import com.netease.nim.camellia.tools.utils.IPMatcher;

import java.util.List;

/**
 * Created by caojiajun on 2025/3/25
 */
public class IpAffinityConfig {

    private Type type;
    private List<Config> configList;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<Config> getConfigList() {
        return configList;
    }

    public void setConfigList(List<Config> configList) {
        this.configList = configList;
    }

    public static enum Type {
        affinity,
        anti_affinity,
        ;
    }

    public static class Config {
        private IPMatcher source;
        private IPMatcher target;
        public IPMatcher getSource() {
            return source;
        }

        public void setSource(IPMatcher source) {
            this.source = source;
        }

        public IPMatcher getTarget() {
            return target;
        }

        public void setTarget(IPMatcher target) {
            this.target = target;
        }
    }
}

package com.netease.nim.camellia.core.discovery;

import com.netease.nim.camellia.tools.utils.IPMatcher;

/**
 * Created by caojiajun on 2025/3/25
 */
public class IpAffinityConfig {
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

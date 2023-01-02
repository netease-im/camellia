package com.netease.nim.camellia.redis.proxy.plugin.permission.model;

import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.redis.proxy.discovery.common.IPMatcher;

import java.util.HashSet;
import java.util.Set;

public class IpCheckInfo {
    private final IpCheckMode mode;
    private final Set<IPMatcher> ipMatcherSet = new HashSet<>();
    private final Set<String> ipSet = new HashSet<>();

    public IpCheckInfo(IpCheckMode mode) {
        this.mode = mode;
    }

    public void addIp(String ip) {
        ipSet.add(ip);
    }

    public void addIpWithMask(String ip, String mask) {
        ipMatcherSet.add(new IPMatcher(ip, mask));
    }

    public boolean check(String ip) {
        if (mode == IpCheckMode.WHITE) {
            if (!ipSet.isEmpty()) {
                for (String whiteIp : ipSet) {
                    if (ip.equals(whiteIp)) {
                        return true;
                    }
                }
            }
            if (!ipMatcherSet.isEmpty()) {
                for (IPMatcher ipMatcher : ipMatcherSet) {
                    if (ipMatcher.match(ip)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (mode == IpCheckMode.BLACK) {
            if (!ipSet.isEmpty()) {
                for (String whiteIp : ipSet) {
                    if (ip.equals(whiteIp)) {
                        return false;
                    }
                }
            }
            if (!ipMatcherSet.isEmpty()) {
                for (IPMatcher ipMatcher : ipMatcherSet) {
                    if (ipMatcher.match(ip)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return true;
    }

    public IpCheckMode getMode() {
        return mode;
    }
}

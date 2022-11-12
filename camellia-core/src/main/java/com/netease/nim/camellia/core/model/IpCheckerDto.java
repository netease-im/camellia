package com.netease.nim.camellia.core.model;

import com.netease.nim.camellia.core.enums.IpCheckMode;

import java.util.HashSet;
import java.util.Set;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */

public class IpCheckerDto {
    private Long bid;
    private String bgroup;
    private IpCheckMode mode;
    public Set<String> ipList = new HashSet<>();

    public Long getBid() {
        return bid;
    }

    public void setBid(Long bid) {
        this.bid = bid;
    }

    public String getBgroup() {
        return bgroup;
    }

    public void setBgroup(String bgroup) {
        this.bgroup = bgroup;
    }

    public IpCheckMode getMode() {
        return mode;
    }

    public void setMode(IpCheckMode ipCheckMode) {
        this.mode = ipCheckMode;
    }

    public Set<String> getIpList() {
        return ipList;
    }

    public void setIpList(Set<String> ipList) {
        this.ipList = ipList;
    }
}

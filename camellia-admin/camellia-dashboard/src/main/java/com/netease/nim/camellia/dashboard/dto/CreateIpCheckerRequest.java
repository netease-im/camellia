package com.netease.nim.camellia.dashboard.dto;


import com.netease.nim.camellia.core.enums.IpCheckMode;

import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class CreateIpCheckerRequest {

    @NotNull
    private Long bid;
    @NotNull
    private String bgroup;
    @NotNull
    private IpCheckMode mode;
    @NotNull
    private Set<String> ipList;

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

    public Set<String> getIpList() {
        return ipList;
    }

    public void setIpList(Set<String> ipList) {
        this.ipList = ipList;
    }

    public IpCheckMode getMode() {
        return mode;
    }

    public void setMode(IpCheckMode mode) {
        this.mode = mode;
    }
}

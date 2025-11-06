package com.netease.nim.camellia.dashboard.dto;


import com.netease.nim.camellia.core.enums.IpCheckMode;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class UpdateIpCheckerRequest {
    @NotNull
    private IpCheckMode mode;
    @NotNull
    private Set<String> ipList;

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

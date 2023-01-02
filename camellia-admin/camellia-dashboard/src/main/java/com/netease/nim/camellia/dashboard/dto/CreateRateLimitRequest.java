package com.netease.nim.camellia.dashboard.dto;


import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class CreateRateLimitRequest {

    @Min(value = -2, message = "bid must be equal -1,-2 or greater than 0")
    private Long bid;
    @NotBlank
    private String bgroup;
    @Min(value = 1, message = "checkMillis must be greater than 0")
    private Long checkMillis = 1000L;
    private Long maxCount;

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

    public Long getCheckMillis() {
        return checkMillis;
    }

    public void setCheckMillis(Long checkMillis) {
        this.checkMillis = checkMillis;
    }

    public Long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Long maxCount) {
        this.maxCount = maxCount;
    }
}

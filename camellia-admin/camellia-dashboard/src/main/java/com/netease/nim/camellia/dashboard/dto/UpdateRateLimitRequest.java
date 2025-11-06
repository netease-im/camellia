package com.netease.nim.camellia.dashboard.dto;


import jakarta.validation.constraints.Min;

/**
 * @author tasszz2k
 * @since 11/11/2022
 */
public class UpdateRateLimitRequest {
    @Min(value = 1, message = "checkMillis must be greater than 0")
    private Long checkMillis;
    private Long maxCount;

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

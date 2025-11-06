package com.netease.nim.camellia.dashboard.model;

import com.alibaba.fastjson.JSONObject;
import jakarta.persistence.*;


/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Entity(name = "camellia_rate_limit")
public class RateLimit extends BaseEntity {
    @Column
    private Long bid;
    @Column
    private String bgroup;
    @Column
    private Long checkMillis;
    @Column
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

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("bid", bid);
        jsonObject.put("bgroup", bgroup);
        jsonObject.put("checkMillis", checkMillis);
        jsonObject.put("maxCount", maxCount);
        jsonObject.put("createTime", createTime);
        jsonObject.put("updateTime", updateTime);
        return jsonObject;
    }
}

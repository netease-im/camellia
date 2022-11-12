package com.netease.nim.camellia.dashboard.model;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.enums.IpCheckMode;
import com.netease.nim.camellia.dashboard.util.StringCollectionConverter;

import javax.persistence.Table;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tasszz2k
 * @since 09/11/2022
 */
@Entity
@Table(name = "camellia_ip_checker")
public class IpChecker extends BaseEntity {
    @Column
    private Long bid;
    @Column
    private String bgroup;

    @Column(name = "mode")
    @Basic
    private Integer modeValue;

    @Transient
    private IpCheckMode mode;

    @Convert(converter = StringCollectionConverter.class)
    @Column
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

    public Integer getModeValue() {
        return modeValue;
    }

    public void setModeValue(Integer modeValue) {
        this.modeValue = modeValue;
        this.mode = IpCheckMode.getByValue(modeValue);
    }

    public IpCheckMode getMode() {
        return mode;
    }

    public void setMode(IpCheckMode mode) {
        this.mode = mode;
        this.modeValue = mode.getValue();
    }

    public Set<String> getIpList() {
        return ipList;
    }

    public void setIpList(Set<String> ipList) {
        this.ipList = ipList;
    }

    @PostLoad
    void fillTransient() {
        if (modeValue != null) {
            this.mode = IpCheckMode.getByValue(modeValue);
        }
    }

    @PrePersist
    void fillPersistent() {
        if (mode != null) {
            this.modeValue = mode.getValue();
        }
    }


    @Override
    public String toString() {
        return "IpChecker{" +
                "bid=" + bid +
                ", bgroup='" + bgroup + '\'' +
                ", mode=" + mode +
                ", ipList='" + ipList + '\'' +
                ", id=" + id +
                '}';
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("bid", bid);
        jsonObject.put("bgroup", bgroup);
        jsonObject.put("mode", mode);
        jsonObject.put("ipList", ipList);
        jsonObject.put("createTime", createTime);
        jsonObject.put("updateTime", updateTime);
        return jsonObject;
    }
}

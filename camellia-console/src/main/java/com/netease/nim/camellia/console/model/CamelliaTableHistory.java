package com.netease.nim.camellia.console.model;

import com.alibaba.fastjson.JSONObject;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class CamelliaTableHistory {
    private Long id;
    private String address;
    private Long dashboardId;
    private String username;
    private Long tid;
    private String resourceInfo;
    private String detail;
    private Integer opType;
    private Long createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(Long dashboardId) {
        this.dashboardId = dashboardId;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public String getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(String resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Integer getOpType() {
        return opType;
    }

    public void setOpType(Integer opType) {
        this.opType = opType;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("address", address);
        jsonObject.put("dashboardId", dashboardId);
        jsonObject.put("username", username);
        jsonObject.put("tid", tid);
        jsonObject.put("resourceInfo", resourceInfo);
        jsonObject.put("opType", opType);
        jsonObject.put("createdTime", createdTime);
        jsonObject.put("table", detail);
        return jsonObject;
    }

    @Override
    public String toString() {
        return "CamelliaTableHistory{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", dashboardId=" + dashboardId +
                ", username='" + username + '\'' +
                ", tid=" + tid +
                ", resourceInfo='" + resourceInfo + '\'' +
                ", detail='" + detail + '\'' +
                ", opType=" + opType +
                ", createdTime=" + createdTime +
                '}';
    }
}

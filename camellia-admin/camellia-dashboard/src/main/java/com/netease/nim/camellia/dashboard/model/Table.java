package com.netease.nim.camellia.dashboard.model;

import com.alibaba.fastjson.JSONObject;
import jakarta.persistence.*;

/**
 *
 * Created by caojiajun on 2019/5/28.
 */
@Entity(name = "camellia_table")
public class Table {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tid;

    @Column
    private String detail;

    @Column
    private String info;

    @Column
    private Integer validFlag;

    @Column
    private Long createTime;

    @Column
    private Long updateTime;

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Integer getValidFlag() {
        return validFlag;
    }

    public void setValidFlag(Integer validFlag) {
        this.validFlag = validFlag;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tid", tid);
        try {
            jsonObject.put("table", JSONObject.parseObject(detail));
        } catch (Exception e) {
            jsonObject.put("table", detail);
        }
        jsonObject.put("info", info);
        jsonObject.put("validFlag", ValidFlag.getByValue(validFlag));
        jsonObject.put("createTime", createTime);
        jsonObject.put("updateTime", updateTime);
        return jsonObject;
    }
}

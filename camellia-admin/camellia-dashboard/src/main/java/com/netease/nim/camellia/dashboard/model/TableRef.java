package com.netease.nim.camellia.dashboard.model;

import com.alibaba.fastjson.JSONObject;

import javax.persistence.*;


/**
 *
 * Created by caojiajun on 2019/5/28.
 */
@Entity
@javax.persistence.Table(name = "camellia_table_ref")
public class TableRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long tid;

    @Column
    private Long bid;

    @Column
    private String bgroup;

    @Column
    private String info;

    @Column
    private Integer validFlag;

    @Column
    private Long createTime;

    @Column
    private Long updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTid() {
        return tid;
    }

    public void setTid(Long tid) {
        this.tid = tid;
    }

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
        jsonObject.put("bid", bid);
        jsonObject.put("bgroup", bgroup);
        jsonObject.put("tid", tid);
        jsonObject.put("info", info);
        jsonObject.put("validFlag", ValidFlag.getByValue(validFlag));
        jsonObject.put("createTime", createTime);
        jsonObject.put("updateTime", updateTime);
        return jsonObject;
    }
}

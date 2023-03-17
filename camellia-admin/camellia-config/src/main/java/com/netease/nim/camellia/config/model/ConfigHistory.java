package com.netease.nim.camellia.config.model;

/**
 * Created by caojiajun on 2023/3/15
 */
public class ConfigHistory {

    private Long id;

    private Integer type;

    private String namespace;

    private Long configId;

    private String oldConfig;

    private String newConfig;

    private String operatorType;

    private String operator;//操作者

    private Long createTime;//创建时间

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getOldConfig() {
        return oldConfig;
    }

    public void setOldConfig(String oldConfig) {
        this.oldConfig = oldConfig;
    }

    public String getNewConfig() {
        return newConfig;
    }

    public void setNewConfig(String newConfig) {
        this.newConfig = newConfig;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}

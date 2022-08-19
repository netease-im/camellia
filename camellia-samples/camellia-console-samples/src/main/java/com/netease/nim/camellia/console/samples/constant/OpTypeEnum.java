package com.netease.nim.camellia.console.samples.constant;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public enum OpTypeEnum {
    READ("r",0),
    WRITE("w",1)
    ;
    private final String alias;
    private Integer value;

    public String getAlias() {
        return alias;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    OpTypeEnum(String alias, Integer value) {
        this.alias = alias;
        this.value = value;
    }

    public static OpTypeEnum getOpTypeByAlias(String alias){
        for(OpTypeEnum opTypeEnum:OpTypeEnum.values()){
            if(opTypeEnum.alias.equals(alias)){
                return opTypeEnum;
            }
        }
        return null;
    }
}

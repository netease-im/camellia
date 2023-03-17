package com.netease.nim.camellia.config.model;

/**
 * Created by caojiajun on 2023/3/15
 */
public enum OperatorType {
    CREATE,//创建
    UPDATE,//更新
    INVALID,//从valid改成invalid
    VALID,//从invalid改成valid
    DELETE,//删除
    ;
}

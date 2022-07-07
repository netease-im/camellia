package com.netease.nim.camellia.feign.naked;


/**
 * CamelliaNakedClient 失败监听回调
 * Created by caojiajun on 2022/7/4
 */
public interface CamelliaNakedClientFailureListener<R> {

    /**
     * 失败监听
     * @param context 上下文信息
     */
    void onFailure(CamelliaNakedClientFailureContext<R> context);

}

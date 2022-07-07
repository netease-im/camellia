package com.netease.nim.camellia.feign;


/**
 * Created by caojiajun on 2022/7/5
 */
public interface CamelliaFeignFailureListener {

    /**
     * 失败监听
     * @param context 上下文信息
     */
    void onFailure(CamelliaFeignFailureContext context);
}

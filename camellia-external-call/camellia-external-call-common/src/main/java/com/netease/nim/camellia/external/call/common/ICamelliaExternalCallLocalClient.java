package com.netease.nim.camellia.external.call.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface ICamelliaExternalCallLocalClient<R, T> {

    /**
     * 发送一个外部调用任务
     * @param isolationKey 外部调用任务归属的租户id
     * @param request 任务请求参数
     * @return 任务响应
     */
    CompletableFuture<T> submit(String isolationKey, R request);

    /**
     * 发送一个外部调用任务，并获得结果
     * @param isolationKey 外部调用任务归属的租户id
     * @param request 任务请求参数
     * @param timeout 超时时间
     * @param timeUnit 超时时间单位
     * @return 结果
     */
    T execute(String isolationKey, R request, long timeout, TimeUnit timeUnit);

}

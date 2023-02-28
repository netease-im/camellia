package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface ICamelliaExternalCallMqClient<R> {

    /**
     * 提交一个任务，任务会动态的投递给不同的mq，从而自动隔离
     * @param isolationKey 租户id
     * @param request 请求
     * @return 投递结果
     */
    boolean submit(String isolationKey, R request);

}

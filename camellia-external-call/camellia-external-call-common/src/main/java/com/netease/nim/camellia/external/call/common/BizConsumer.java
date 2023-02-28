package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/24
 */
public interface BizConsumer<R> {

    /**
     * 业务方自己实现业务逻辑（也就是外部访问逻辑）
     * @param isolationKey 租户id
     * @param request 请求
     * @return 结果
     */
    boolean onMsg(String isolationKey, R request);
}

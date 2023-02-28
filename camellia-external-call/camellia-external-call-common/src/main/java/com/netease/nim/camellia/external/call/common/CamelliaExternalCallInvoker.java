package com.netease.nim.camellia.external.call.common;

/**
 *
 * Created by caojiajun on 2023/2/24
 */
public interface CamelliaExternalCallInvoker<R, T> {

    T invoke(R request);
}

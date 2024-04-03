package com.netease.nim.camellia.feign.samples;

import com.netease.nim.camellia.core.client.annotation.RetryPolicy;
import com.netease.nim.camellia.tools.utils.ExceptionUtils;

/**
 * Created by caojiajun on 2024/4/3
 */
public class CustomRetryPolicy2 implements RetryPolicy {
    @Override
    public RetryAction onError(Throwable t) {
        System.out.println("CustomRetryPolicy2: error=" + t.toString() + ",isConnectError=" + ExceptionUtils.isConnectError(t));
        return RetryAction.RETRY_NEXT;
    }
}

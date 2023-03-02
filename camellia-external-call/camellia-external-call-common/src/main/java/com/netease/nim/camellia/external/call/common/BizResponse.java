package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/28
 */
public class BizResponse {

    public static final BizResponse SUCCESS = new BizResponse(true, false);
    public static final BizResponse FAIL_RETRY = new BizResponse(false, true);
    public static final BizResponse FAIL_NO_RETRY = new BizResponse(false, false);

    private boolean success;
    private boolean retry;

    public BizResponse(boolean success, boolean retry) {
        this.success = success;
        this.retry = retry;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

}

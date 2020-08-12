package com.netease.nim.camellia.redis.proxy.command.async;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class CommandInterceptResponse {

    public static final CommandInterceptResponse SUCCESS = new CommandInterceptResponse(true, null);
    public static final CommandInterceptResponse DEFAULT_FAIL = new CommandInterceptResponse(false, "ERR command intercept no pass");

    private final boolean pass;
    private final String errorMsg;

    public CommandInterceptResponse(boolean pass, String errorMsg) {
        this.pass = pass;
        this.errorMsg = errorMsg;
    }

    public boolean isPass() {
        return pass;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

}

package com.netease.nim.camellia.redis.proxy.command.async;

/**
 *
 * Created by caojiajun on 2020/8/10
 */
public class CommandFilterResponse {

    public static final CommandFilterResponse SUCCESS = new CommandFilterResponse(true, null);
    public static final CommandFilterResponse DEFAULT_FAIL = new CommandFilterResponse(false, "ERR command filter no pass");

    private final boolean pass;
    private final String errorMsg;

    public CommandFilterResponse(boolean pass, String errorMsg) {
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

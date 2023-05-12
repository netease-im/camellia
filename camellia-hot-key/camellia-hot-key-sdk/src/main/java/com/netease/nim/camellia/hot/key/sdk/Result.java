package com.netease.nim.camellia.hot.key.sdk;

/**
 * Created by caojiajun on 2023/5/12
 */
public class Result {

    public static final Result HOT = new Result(true);
    public static final Result NOT_HOT = new Result(false);

    private final boolean hot;

    public Result(boolean hot) {
        this.hot = hot;
    }

    public boolean isHot() {
        return hot;
    }
}


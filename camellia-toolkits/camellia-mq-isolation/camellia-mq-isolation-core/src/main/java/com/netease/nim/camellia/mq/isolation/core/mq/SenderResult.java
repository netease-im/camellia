package com.netease.nim.camellia.mq.isolation.core.mq;

import java.util.concurrent.CompletableFuture;

/**
 * Created by caojiajun on 2024/2/4
 */
public class SenderResult {
    private CompletableFuture<Boolean> result;

    public CompletableFuture<Boolean> getResult() {
        return result;
    }

    public void setResult(CompletableFuture<Boolean> result) {
        this.result = result;
    }
}

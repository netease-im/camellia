package com.netease.nim.camellia.delayqueue.server.springboot;

import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgPullRequest;

import javax.servlet.AsyncContext;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/9/1
 */
public class CamelliaDelayQueueLongPollingTask {

    private final CamelliaDelayMsgPullRequest request;
    private final long longPollingTimeoutMillis;
    private final AsyncContext asyncContext;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private ScheduledFuture<?> future;

    public CamelliaDelayQueueLongPollingTask(CamelliaDelayMsgPullRequest request, long longPollingTimeoutMillis, AsyncContext asyncContext) {
        this.request = request;
        this.longPollingTimeoutMillis = longPollingTimeoutMillis;
        this.asyncContext = asyncContext;
    }

    public CamelliaDelayMsgPullRequest getRequest() {
        return request;
    }

    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    public long getLongPollingTimeoutMillis() {
        return longPollingTimeoutMillis;
    }

    public boolean tryLock() {
        return done.compareAndSet(false, true);
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    public void cancel() {
        if (future != null) {
            future.cancel(false);
        }
    }
}

package com.netease.nim.camellia.delayqueue.server.springboot;

import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgPullRequest;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.AsyncContext;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/9/1
 */
public class CamelliaDelayQueueLongPollingTask {

    private final CamelliaDelayMsgPullRequest request;
    private final long longPollingTimeoutMillis;
    private final DeferredResult<String> result;
    private final AtomicBoolean done = new AtomicBoolean(false);
    private Runnable cancelCallback;

    public CamelliaDelayQueueLongPollingTask(CamelliaDelayMsgPullRequest request, long longPollingTimeoutMillis, DeferredResult<String> result) {
        this.request = request;
        this.longPollingTimeoutMillis = longPollingTimeoutMillis;
        this.result = result;
    }

    public CamelliaDelayMsgPullRequest getRequest() {
        return request;
    }

    public DeferredResult<String> getResult() {
        return result;
    }

    public long getLongPollingTimeoutMillis() {
        return longPollingTimeoutMillis;
    }

    public boolean tryLock() {
        return done.compareAndSet(false, true);
    }

    public void setCancelCallback(Runnable cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    public boolean isDone() {
        return done.get();
    }

    public void cancel() {
        if (cancelCallback != null) {
            cancelCallback.run();
        }
    }
}

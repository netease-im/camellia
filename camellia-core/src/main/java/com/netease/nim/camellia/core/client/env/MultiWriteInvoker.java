package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.CRC16Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2024/8/1
 */
public class MultiWriteInvoker {

    private static final Logger logger = LoggerFactory.getLogger(MultiWriteInvoker.class);

    public static interface Invoker {
        Object invoke(Resource resource, int index) throws Throwable;
    }

    public static interface FailedCallback {
        void failed(Throwable t, Resource resource, int index, FailedReason failedReason);
    }

    public static Object invoke(ProxyEnv proxyEnv, List<Resource> writeResources, Invoker invoker) throws Throwable {
        return invoke(proxyEnv, writeResources, invoker, null);
    }

    public static Object invoke(ProxyEnv proxyEnv, List<Resource> writeResources, Invoker invoker, FailedCallback failedCallback) throws Throwable {
        if (writeResources.size() == 1) {
            return invoke0(invoker, writeResources.get(0), failedCallback, 0, null);
        }
        MultiWriteType multiWriteType = proxyEnv.getMultiWriteType();
        if (multiWriteType == MultiWriteType.SINGLE_THREAD) {
            Object result = null;
            for (int i=0; i<writeResources.size(); i++) {
                Resource resource = writeResources.get(i);
                if (i == 0) {
                    result = invoke0(invoker, resource, failedCallback, i, null);
                } else {
                    invoke0(invoker, resource, failedCallback, i, null);
                }
            }
            return result;
        }
        if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {
            ThreadContextSwitchStrategy strategy = proxyEnv.getThreadContextSwitchStrategy();
            List<Future<Object>> futureList = new ArrayList<>(writeResources.size());
            for (int i=0; i<writeResources.size(); i++) {
                Resource resource = writeResources.get(i);
                final int index = i;
                AtomicBoolean lock = new AtomicBoolean(false);
                Future<Object> future;
                try {
                    future = proxyEnv.getMultiWriteConcurrentExec()
                            .submit(strategy.wrapperCallable(() -> invoke0(invoker, resource, failedCallback, index, lock)));
                } catch (Exception e) {
                    onFailedCallback(resource, index, failedCallback, e, FailedReason.DISCARD, lock);
                    throw e;
                }
                futureList.add(future);
            }
            Object result = null;
            for (int i = 0; i < futureList.size(); i++) {
                Future<Object> future = futureList.get(i);
                Object obj = future.get();
                if (i == 0) {
                    result = obj;
                }
            }
            return result;
        }
        if (multiWriteType == MultiWriteType.ASYNC_MULTI_THREAD) {
            ThreadContextSwitchStrategy strategy = proxyEnv.getThreadContextSwitchStrategy();
            Future<Object> targetFuture = null;
            boolean first;
            for (int i=0; i<writeResources.size(); i++) {
                Resource resource = writeResources.get(i);
                final int index = i;
                first = i == 0;
                Future<Object> future = null;
                AtomicBoolean lock = new AtomicBoolean(false);
                try {
                    String hashKey = threadHashKey();
                    future = proxyEnv.getMultiWriteAsyncExec().submit(hashKey,
                            strategy.wrapperCallable(() -> invoke0(invoker, resource, failedCallback, index, lock)));
                } catch (Exception e) {
                    onFailedCallback(resource, index, failedCallback, e, FailedReason.DISCARD, lock);
                    if (first) {
                        throw e;
                    } else {
                        logger.error("submit async multi thread task error", e);
                    }
                }
                if (first) {
                    targetFuture = future;
                }
            }
            Object result;
            if (targetFuture != null) {
                result = targetFuture.get();
            } else {
                throw new IllegalStateException("wil not invoke here");
            }
            return result;
        }
        if (multiWriteType == MultiWriteType.MISC_ASYNC_MULTI_THREAD) {
            ThreadContextSwitchStrategy strategy = proxyEnv.getThreadContextSwitchStrategy();
            Object result = null;
            for (int i=0; i<writeResources.size(); i++) {
                Resource resource = writeResources.get(i);
                final int index = i;
                if (i == 0) {
                    result = invoke0(invoker, resource, failedCallback, index, null);
                    continue;
                }
                AtomicBoolean lock = new AtomicBoolean(false);
                try {
                    String hashKey = threadHashKey();
                    proxyEnv.getMultiWriteAsyncExec().submit(hashKey,
                            strategy.wrapperCallable(() -> invoke0(invoker, resource, failedCallback, index, lock)));
                } catch (Exception e) {
                    onFailedCallback(resource, index, failedCallback, e, FailedReason.DISCARD, lock);
                    logger.error("submit async multi thread task error", e);
                }
            }
            return result;
        }
        throw new IllegalArgumentException("unknown multiWriteType");
    }

    private static Object invoke0(Invoker invoker, Resource resource, FailedCallback failedCallback, int index, AtomicBoolean lock) throws Exception {
        try {
            return invoker.invoke(resource, index);
        } catch (Throwable e) {
            onFailedCallback(resource, index, failedCallback, e, FailedReason.EXCEPTION, lock);
            if (e instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new ExecutionException(e);
            }
        }
    }

    private static void onFailedCallback(Resource resource, int index, FailedCallback failedCallback, Throwable e, FailedReason failedReason, AtomicBoolean lock) {
        if (failedCallback == null) return;
        if (lock == null) {
            failedCallback.failed(e, resource, index, failedReason);
        } else {
            if (lock.compareAndSet(false, true)) {
                failedCallback.failed(e, resource, index, failedReason);
            }
        }
    }


    private static String threadHashKey() {
        return String.valueOf(CRC16Utils.getCRC16(String.valueOf(Thread.currentThread().getId()).getBytes(StandardCharsets.UTF_8)));
    }
}

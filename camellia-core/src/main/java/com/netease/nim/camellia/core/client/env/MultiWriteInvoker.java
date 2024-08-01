package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.CRC16Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by caojiajun on 2024/8/1
 */
public class MultiWriteInvoker {

    private static final Logger logger = LoggerFactory.getLogger(MultiWriteInvoker.class);

    public static interface Invoker {
        Object invoke(Resource resource);
    }

    public static Object invoke(ProxyEnv proxyEnv, List<Resource> writeResources, Invoker invoker) throws Exception {
        MultiWriteType multiWriteType = proxyEnv.getMultiWriteType();
        if (multiWriteType == MultiWriteType.SINGLE_THREAD) {
            Object result = null;
            for (int i=0; i<writeResources.size(); i++) {
                Resource resource = writeResources.get(i);
                if (i == 0) {
                    result = invoker.invoke(resource);
                } else {
                    invoker.invoke(resource);
                }
            }
            return result;
        }
        if (multiWriteType == MultiWriteType.MULTI_THREAD_CONCURRENT) {
            ThreadContextSwitchStrategy strategy = proxyEnv.getThreadContextSwitchStrategy();
            List<Future<Object>> futureList = new ArrayList<>(writeResources.size());
            for (Resource resource : writeResources) {
                Future<Object> future = proxyEnv.getMultiWriteConcurrentExec()
                        .submit(strategy.wrapperCallable(() -> invoker.invoke(resource)));
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
                first = i == 0;
                Future<Object> future = null;
                try {
                    String hashKey = threadHashKey();
                    future = proxyEnv.getMultiWriteAsyncExec().submit(hashKey,
                            strategy.wrapperCallable(() -> invoker.invoke(resource)));
                } catch (Exception e) {
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
                if (i == 0) {
                    result = invoker.invoke(resource);
                    continue;
                }
                try {
                    String hashKey = threadHashKey();
                    proxyEnv.getMultiWriteAsyncExec().submit(hashKey,
                            strategy.wrapperCallable(() -> invoker.invoke(resource)));
                } catch (Exception e) {
                    logger.error("submit async multi thread task error", e);
                }
            }
            return result;
        }
        throw new IllegalArgumentException("unknown multiWriteType");
    }

    private static String threadHashKey() {
        return String.valueOf(CRC16Utils.getCRC16(String.valueOf(Thread.currentThread().getId()).getBytes(StandardCharsets.UTF_8)));
    }
}

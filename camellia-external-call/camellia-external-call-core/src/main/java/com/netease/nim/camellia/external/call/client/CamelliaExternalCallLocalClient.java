package com.netease.nim.camellia.external.call.client;

import com.netease.nim.camellia.external.call.common.CamelliaExternalCallInvoker;
import com.netease.nim.camellia.external.call.common.ICamelliaExternalCallLocalClient;
import com.netease.nim.camellia.external.call.common.CamelliaExternalCallException;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicIsolationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 这是一个本地调用的外部调用执行客户端
 * 需要设置任务执行的invoker
 * 每次请求时传入isolationKey和Request，可以获取结果result
 *
 * 一般把isolationKey设置为租户
 *
 * 可以自动隔离不同响应速度的租户的外部调用任务，避免慢租户阻塞快租户
 *
 * Created by caojiajun on 2023/2/24
 */
public class CamelliaExternalCallLocalClient<R, T> implements ICamelliaExternalCallLocalClient<R, T> {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaExternalCallLocalClient.class);

    private final CamelliaDynamicIsolationExecutor executor;
    private final CamelliaExternalCallInvoker<R, T> invoker;

    public CamelliaExternalCallLocalClient(CamelliaDynamicIsolationExecutor executor,
                                           CamelliaExternalCallInvoker<R, T> invoker) {
        this.executor = executor;
        this.invoker = invoker;
    }

    @Override
    public CompletableFuture<T> submit(String isolationKey, R request) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.submit(isolationKey, () -> {
            T result;
            try {
                result = invoker.invoke(request);
            } catch (Exception e) {
                future.completeExceptionally(e);
                return;
            }
            try {
                future.complete(result);
            } catch (Exception e) {
                logger.error("complete error, isolationKey = {}, request = {}, result = {}", isolationKey, request, result, e);
            }
        }, (key, reason) -> future.completeExceptionally(new CamelliaExternalCallException(reason.name())));
        return future;
    }

    @Override
    public T execute(String isolationKey, R request, long timeout, TimeUnit timeUnit) {
        CompletableFuture<T> future = submit(isolationKey, request);
        try {
            return future.get(timeout, timeUnit);
        } catch (Exception e) {
            throw new CamelliaExternalCallException(e);
        }
    }

}

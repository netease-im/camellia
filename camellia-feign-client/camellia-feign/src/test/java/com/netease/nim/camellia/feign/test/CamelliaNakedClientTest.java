package com.netease.nim.camellia.feign.test;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.feign.CamelliaFeignFallbackFactory;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClient;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClientFailureContext;
import com.netease.nim.camellia.feign.naked.CamelliaNakedClientFailureListener;
import com.netease.nim.camellia.feign.naked.CamelliaNakedRequestInvoker;
import com.netease.nim.camellia.feign.naked.exception.CamelliaNakedClientRetriableException;
import com.netease.nim.camellia.feign.resource.FeignResource;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by caojiajun on 2026/4/29
 */
public class CamelliaNakedClientTest {

    @Test
    public void testReadAndWriteRequestUseConfiguredFeignResource() {
        RecordingInvoker invoker = new RecordingInvoker();
        CamelliaNakedClient<String, String> client = new CamelliaNakedClient.Builder()
                .name("unit")
                .resourceTable("feign#http://naked.example")
                .build(invoker);

        String readResponse = client.sendRequest(CamelliaNakedClient.OperationType.READ, "read-body");
        String writeResponse = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, "write-body");

        Assert.assertEquals("http://naked.example|read-body", readResponse);
        Assert.assertEquals("http://naked.example|write-body", writeResponse);
        Assert.assertEquals("unit", client.getName());
        Assert.assertEquals(2, invoker.resources.size());
        Assert.assertEquals("http://naked.example", invoker.resources.get(0).getFeignUrl());
        Assert.assertEquals("http://naked.example", invoker.resources.get(1).getFeignUrl());
    }

    @Test
    public void testRetriableExceptionRetriesUntilSuccess() {
        AtomicInteger attempts = new AtomicInteger();
        CamelliaNakedRequestInvoker<String, String> invoker = (feignResource, request) -> {
            int attempt = attempts.incrementAndGet();
            if (attempt <= 2) {
                throw new CamelliaNakedClientRetriableException("retry " + attempt);
            }
            return feignResource.getFeignUrl() + "|" + request + "|" + attempt;
        };
        CamelliaNakedClient<String, String> client = new CamelliaNakedClient.Builder()
                .resourceTable("feign#http://retry.example")
                .build(invoker);

        String response = client.sendRequest(CamelliaNakedClient.OperationType.READ, "body", null, null, 2);

        Assert.assertEquals("http://retry.example|body|3", response);
        Assert.assertEquals(3, attempts.get());
    }

    @Test
    public void testFallbackAndFailureListenerAfterInvokeFailure() {
        List<CamelliaNakedClientFailureContext<String>> contexts = new ArrayList<>();
        CamelliaNakedRequestInvoker<String, String> invoker = (feignResource, request) -> {
            throw new CamelliaNakedClientRetriableException("mock failure");
        };
        CamelliaFeignFallbackFactory<String> fallbackFactory = t -> "fallback:" + t.getClass().getSimpleName();
        CamelliaNakedClientFailureListener<String> failureListener = contexts::add;
        CamelliaNakedClient<String, String> client = new CamelliaNakedClient.Builder()
                .bid(1001)
                .bgroup("unit")
                .resourceTable("feign#http://fallback.example")
                .build(invoker, fallbackFactory, failureListener);

        String response = client.sendRequest(CamelliaNakedClient.OperationType.WRITE, "payload", "route-key", "lb-key", 0);

        Assert.assertEquals("fallback:CamelliaNakedClientRetriableException", response);
        Assert.assertEquals(1, contexts.size());
        CamelliaNakedClientFailureContext<String> context = contexts.get(0);
        Assert.assertEquals(1001, context.getBid());
        Assert.assertEquals("unit", context.getBgroup());
        Assert.assertEquals(CamelliaNakedClient.OperationType.WRITE, context.getOperationType());
        Assert.assertEquals("payload", context.getRequest());
        Assert.assertEquals("lb-key", context.getLoadBalanceKey());
        Assert.assertEquals("feign#http://fallback.example", context.getResource().getUrl());
        Assert.assertTrue(context.getException() instanceof CamelliaNakedClientRetriableException);
    }

    @Test
    public void testSendRetryUsesFailureContextResource() {
        RecordingInvoker invoker = new RecordingInvoker();
        CamelliaNakedClient<String, String> client = new CamelliaNakedClient.Builder()
                .resourceTable("feign#http://default.example")
                .build(invoker);
        CamelliaNakedClientFailureContext<String> context = new CamelliaNakedClientFailureContext<>(
                1002, "retry-bgroup", CamelliaNakedClient.OperationType.READ, "retry-body", "lb-key",
                new Resource("feign#http://retry-context.example"), new RuntimeException("previous failure"));

        String response = client.sendRetry(context, 0);

        Assert.assertEquals("http://retry-context.example|retry-body", response);
        Assert.assertEquals(1, invoker.resources.size());
        Assert.assertEquals("http://retry-context.example", invoker.resources.get(0).getFeignUrl());
    }

    private static class RecordingInvoker implements CamelliaNakedRequestInvoker<String, String> {

        private final List<FeignResource> resources = new ArrayList<>();

        @Override
        public String invoke(FeignResource feignResource, String request) {
            resources.add(feignResource);
            return feignResource.getFeignUrl() + "|" + request;
        }
    }
}

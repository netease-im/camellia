package com.netease.nim.camellia.feign.test;

import com.netease.nim.camellia.core.client.annotation.RetryPolicy;
import com.netease.nim.camellia.feign.CamelliaFeign;
import com.netease.nim.camellia.feign.CamelliaFeignClient;
import com.netease.nim.camellia.feign.CamelliaFeignFailureContext;
import com.netease.nim.camellia.feign.CamelliaFeignFailureListener;
import com.netease.nim.camellia.feign.CamelliaFeignFallbackFactory;
import feign.Client;
import feign.Param;
import feign.Request;
import feign.RequestLine;
import feign.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2026/4/29
 */
public class CamelliaFeignClientAnnotationTest {

    private static final String ROUTE = "feign#http://annotation.example";

    @Before
    public void before() {
        AnnotationFailureListener.contexts.clear();
    }

    @Test
    public void testAnnotationRouteAndRetry() {
        RecordingClient client = new RecordingClient();
        client.failTimes = 1;
        client.responseBody = "pong";

        AnnotationApi api = CamelliaFeign.builder()
                .client(client)
                .target(AnnotationApi.class, (CamelliaFeignFallbackFactory<AnnotationApi>) null);

        String response = api.ping("alice");

        Assert.assertEquals("pong", response);
        Assert.assertEquals(2, client.urls.size());
        Assert.assertEquals("http://annotation.example/ping/alice", client.urls.get(0));
        Assert.assertEquals("http://annotation.example/ping/alice", client.urls.get(1));
        Assert.assertTrue(AnnotationFailureListener.contexts.isEmpty());
    }

    @Test
    public void testAnnotationFallbackAndFailureListener() {
        RecordingClient client = new RecordingClient();
        client.failTimes = Integer.MAX_VALUE;

        AnnotationApi api = CamelliaFeign.builder()
                .client(client)
                .target(AnnotationApi.class, (CamelliaFeignFallbackFactory<AnnotationApi>) null);

        String response = api.ping("bob");

        Assert.assertEquals("fallback:bob", response);
        Assert.assertEquals(2, client.urls.size());
        Assert.assertEquals(1, AnnotationFailureListener.contexts.size());
        CamelliaFeignFailureContext context = AnnotationFailureListener.contexts.get(0);
        Assert.assertEquals(AnnotationApi.class, context.getApiType());
        Assert.assertEquals(ROUTE, context.getResource().getUrl());
        Assert.assertEquals("bob", context.getObjects()[0]);
        Assert.assertNotNull(context.getException());
    }

    @CamelliaFeignClient(route = ROUTE, fallback = AnnotationFallback.class,
            failureListener = AnnotationFailureListener.class, retry = 1,
            retryPolicy = RetryPolicy.AlwaysRetryCurrentPolicy.class)
    public interface AnnotationApi {
        @RequestLine("GET /ping/{name}")
        String ping(@Param("name") String name);
    }

    public static class AnnotationFallback implements AnnotationApi {
        @Override
        public String ping(String name) {
            return "fallback:" + name;
        }
    }

    public static class AnnotationFailureListener implements CamelliaFeignFailureListener {
        private static final List<CamelliaFeignFailureContext> contexts = new ArrayList<>();

        @Override
        public void onFailure(CamelliaFeignFailureContext context) {
            contexts.add(context);
        }
    }

    private static class RecordingClient implements Client {

        private final List<String> urls = new ArrayList<>();
        private int failTimes;
        private String responseBody = "ok";

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            urls.add(request.url());
            if (failTimes > 0) {
                failTimes--;
                throw new IOException("mock request failure");
            }
            return Response.builder()
                    .status(200)
                    .reason("OK")
                    .request(request)
                    .headers(Collections.emptyMap())
                    .body(responseBody, StandardCharsets.UTF_8)
                    .build();
        }
    }
}

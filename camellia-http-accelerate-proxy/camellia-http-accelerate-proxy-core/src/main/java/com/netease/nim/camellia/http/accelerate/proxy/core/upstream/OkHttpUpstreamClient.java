package com.netease.nim.camellia.http.accelerate.proxy.core.upstream;

import com.netease.nim.camellia.http.accelerate.proxy.core.context.ErrorReason;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LogBean;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.constants.Constants;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.config.DynamicUpstreamAddrs;
import com.netease.nim.camellia.tools.base.DynamicValueGetter;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2023/7/6
 */
public class OkHttpUpstreamClient implements IUpstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(OkHttpUpstreamClient.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
            new CamelliaThreadFactory("okhttp-upstream-client-heartbeat"));

    private final OkHttpClient okHttpClient;
    private final DynamicUpstreamAddrs dynamicUpstreamAddrs;
    private final DynamicValueGetter<String> healthUri;
    private final DynamicValueGetter<Long> healthTimeout;

    private List<String> validAddrs = new ArrayList<>();

    public OkHttpUpstreamClient(DynamicUpstreamAddrs dynamicUpstreamAddrs,
                                DynamicValueGetter<String> healthUri,
                                DynamicValueGetter<Long> healthTimeout) {
        this.dynamicUpstreamAddrs = dynamicUpstreamAddrs;
        this.healthUri = healthUri;
        this.healthTimeout = healthTimeout;
        int maxRequests = DynamicConf.getInt("okhttp.max.requests", 4096);
        int maxRequestsPerHost = DynamicConf.getInt("okhttp.max.requests.per.host", 2048);
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
        int maxIdleConnections = DynamicConf.getInt("okhttp.max.idle.connections", 256);
        int keepAliveSeconds = DynamicConf.getInt("okhttp.keep.alive.seconds", 3);
        int connectTimeoutMillis = DynamicConf.getInt("okhttp.connect.timeout.millis", 60 * 1000);
        int readTimeoutMillis = DynamicConf.getInt("okhttp.read.timeout.millis", 60 * 1000);
        int writeTimeoutMillis = DynamicConf.getInt("okhttp.write.timeout.millis", 60 * 1000);
        int callTimeoutMillis = DynamicConf.getInt("okhttp.call.timeout.millis", 60 * 1000);
        this.okHttpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveSeconds, TimeUnit.SECONDS))
                .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)
                .callTimeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .build();
        check();
        int intervalSeconds = DynamicConf.getInt("okhttp.health.check.interval.seconds", 5);
        scheduler.scheduleAtFixedRate(this::check, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private synchronized void check() {
        try {
            String uri = healthUri.get();
            List<String> validAddrs = new ArrayList<>();
            List<String> addrs = dynamicUpstreamAddrs.getAddrs();
            Long timeout = healthTimeout.get();
            OkHttpClient client = okHttpClient.newBuilder()
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .readTimeout(timeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                    .callTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();
            for (String addr : addrs) {
                if (uri == null) {
                    validAddrs.add(addr);
                    continue;
                }
                Request request = new Request.Builder().url(addr + uri).get().build();
                try (Response response = client.newCall(request).execute()) {
                    String string = response.body().string();
                    if (response.isSuccessful()) {
                        validAddrs.add(addr);
                        if (logger.isDebugEnabled()) {
                            logger.debug("heartbeat success, addr = {}, uri = {}, code = {}, response = {}",
                                    addr, uri, response.code(), string);
                        }
                    } else {
                        if (logger.isWarnEnabled()) {
                            logger.warn("heartbeat fail, addr = {}, uri = {}, code = {}, response = {}",
                                    addr, uri, response.code(), string);
                        }
                    }
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error("heartbeat error, addr = {}, uri = {}", addr, uri, e);
                    }
                }
            }
            this.validAddrs = validAddrs;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<ProxyResponse> send(ProxyRequest proxyRequest) {
        CompletableFuture<ProxyResponse> future = new CompletableFuture<>();
        if (validAddrs.isEmpty()) {
            proxyRequest.getLogBean().setErrorReason(ErrorReason.UPSTREAM_SERVER_SELECT_FAIL);
            future.complete(new ProxyResponse(Constants.BAD_GATEWAY, proxyRequest.getLogBean()));
            return future;
        }
        FullHttpRequest request = proxyRequest.getRequest();
        int index = ThreadLocalRandom.current().nextInt(validAddrs.size());
        String upstreamAddr = validAddrs.get(index);
        proxyRequest.getLogBean().setUpstreamAddr(upstreamAddr);
        String target = upstreamAddr +  request.uri();
        Request.Builder builder = new Request.Builder().url(target);
        String contentType = "application/json; charset=utf-8";
        for (Map.Entry<String, String> header : request.headers()) {
            if (header.getKey().equalsIgnoreCase("content-length")) continue;
            builder.addHeader(header.getKey(), header.getValue());
            if (header.getKey().equals("Content-Type")) {
                contentType = header.getValue();
            }
        }
        HttpMethod method = request.method();
        if (method == HttpMethod.GET) {
            builder.get();
        } else if (method == HttpMethod.POST) {
            byte[] requestBody = request.content().array();
            RequestBody body = RequestBody.create(requestBody, MediaType.get(contentType));
            builder.post(body);
        } else if (method == HttpMethod.PUT) {
            byte[] requestBody = request.content().array();
            RequestBody body = RequestBody.create(requestBody, MediaType.get(contentType));
            builder.put(body);
        } else if (method == HttpMethod.HEAD) {
            builder.head();
        } else if (method == HttpMethod.DELETE) {
            byte[] requestBody = request.content().array();
            RequestBody body = RequestBody.create(requestBody, MediaType.get(contentType));
            builder.delete(body);
        } else if (method == HttpMethod.PATCH) {
            byte[] requestBody = request.content().array();
            RequestBody body = RequestBody.create(requestBody, MediaType.get(contentType));
            builder.patch(body);
        } else {
            byte[] requestBody = request.content().array();
            RequestBody body = RequestBody.create(requestBody, MediaType.get(contentType));
            builder.method(method.name(), body);
        }
        proxyRequest.getLogBean().setUpstreamSendTime(System.currentTimeMillis());
        okHttpClient.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                proxyRequest.getLogBean().setUpstreamReplyTime(System.currentTimeMillis());
                DefaultFullHttpResponse response;
                if (e instanceof ConnectException) {
                    proxyRequest.getLogBean().setErrorReason(ErrorReason.UPSTREAM_CONNECT_FAIL);
                    response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.BAD_GATEWAY);
                } else {
                    proxyRequest.getLogBean().setErrorReason(ErrorReason.UPSTREAM_ERROR);
                    response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
                }
                future.complete(new ProxyResponse(response, proxyRequest.getLogBean()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                proxyRequest.getLogBean().setUpstreamReplyTime(System.currentTimeMillis());
                Headers headers = response.headers();
                HttpHeaders httpHeaders = new DefaultHttpHeaders();
                for (String name : headers.names()) {
                    String value = headers.get(name);
                    httpHeaders.set(name, value);
                }
                ByteBuf byteBuf = Unpooled.wrappedBuffer(response.body().bytes());
                int code = response.code();
                if (!(code >= 200 && code <= 299)) {
                    proxyRequest.getLogBean().setErrorReason(ErrorReason.UPSTREAM_NOT_2XX_CODE);
                }
                DefaultFullHttpResponse rep = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.valueOf(code), byteBuf, httpHeaders, new DefaultHttpHeaders());
                future.complete(new ProxyResponse(rep, proxyRequest.getLogBean()));
            }
        });
        return future;
    }
}

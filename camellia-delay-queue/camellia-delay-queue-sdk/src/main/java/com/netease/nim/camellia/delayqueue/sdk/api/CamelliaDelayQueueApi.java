package com.netease.nim.camellia.delayqueue.sdk.api;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.delayqueue.common.domain.*;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayMsgErrorCode;
import com.netease.nim.camellia.delayqueue.common.exception.CamelliaDelayQueueException;
import com.netease.nim.camellia.delayqueue.sdk.CamelliaDelayQueueSdkConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/**
 * 不使用REST，统一使用POST请求
 * Created by caojiajun on 2022/7/11
 */
public class CamelliaDelayQueueApi {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDelayQueueApi.class);

    private final DelayQueueServerDiscovery discovery;
    private final OkHttpClient okHttpClient;

    private List<DelayQueueServer> all;
    private List<DelayQueueServer> dynamic;
    private final Object lock = new Object();

    public CamelliaDelayQueueApi(CamelliaDelayQueueSdkConfig sdkConfig) {
        this.okHttpClient = initOkHttpClient(sdkConfig);

        DelayQueueServerDiscovery discovery = sdkConfig.getDiscovery();
        if (discovery != null) {
            this.discovery = discovery;
        } else {
            String url = sdkConfig.getUrl();
            if (url == null || url.trim().length() == 0) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "url/discovery is empty");
            }
            this.discovery = new LocalConfDelayQueueServerDiscovery(sdkConfig.getUrl());
        }
        this.all = new ArrayList<>(this.discovery.findAll());
        if (all.isEmpty()) {
            throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.PARAM_WRONG, "delay queue server is empty");
        }
        this.dynamic = new ArrayList<>(all);
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("delay-queue-sdk", true))
                .scheduleAtFixedRate(this::reload, sdkConfig.getDiscoveryReloadIntervalSeconds(), sdkConfig.getDiscoveryReloadIntervalSeconds(), TimeUnit.SECONDS);
        this.discovery.setCallback(new CamelliaDiscovery.Callback<DelayQueueServer>() {
            @Override
            public void add(DelayQueueServer server) {
                try {
                    synchronized (lock) {
                        all.add(server);
                        dynamic = new ArrayList<>(all);
                    }
                } catch (Exception e) {
                    logger.error("add error", e);
                }
            }

            @Override
            public void remove(DelayQueueServer server) {
                try {
                    synchronized (lock) {
                        ArrayList<DelayQueueServer> list = new ArrayList<>(all);
                        list.remove(server);
                        if (list.isEmpty()) {
                            logger.warn("last delay queue server, skip remove");
                            return;
                        }
                        all = list;
                        dynamic = new ArrayList<>(all);
                    }
                } catch (Exception e) {
                    logger.error("remove error", e);
                }
            }
        });
    }

    private OkHttpClient initOkHttpClient(CamelliaDelayQueueSdkConfig sdkConfig) {
        Dispatcher dispatcher = new Dispatcher();
        CamelliaDelayQueueSdkConfig.CamelliaDelayMsgHttpConfig config = sdkConfig.getHttpConfig();
        dispatcher.setMaxRequests(config.getMaxRequests());
        dispatcher.setMaxRequestsPerHost(config.getMaxRequestsPerHost());
        return new OkHttpClient.Builder()
                .readTimeout(config.getReadTimeoutMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(config.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS)
                .connectTimeout(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(config.getMaxIdleConnections(), config.getKeepAliveSeconds(), TimeUnit.SECONDS))
                .build();
    }

    private void reload() {
        try {
            List<DelayQueueServer> all = discovery.findAll();
            if (!all.isEmpty()) {
                synchronized (lock) {
                    this.all = new ArrayList<>(all);
                    this.dynamic = new ArrayList<>(all);
                }
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    private DelayQueueServer nextDelayQueueServer() {
        try {
            if (all.size() == 1) {
                return all.get(0);
            }
            if (dynamic.isEmpty()) {
                dynamic = new ArrayList<>(all);
            }
            int index = ThreadLocalRandom.current().nextInt(dynamic.size());
            return dynamic.get(index);
        } catch (Exception e) {
            return all.get(0);
        }
    }

    private void onError(DelayQueueServer server) {
        try {
            dynamic.remove(server);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private <T> T invoke(OkHttpClient okHttpClient, String url, Map<String, Object> params, Class<T> clazz) {
        try {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.append(URLEncoder.encode(entry.getKey(), "utf-8")).append("=")
                        .append(URLEncoder.encode(String.valueOf(entry.getValue()), "utf-8")).append("&");
            }
            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), builder.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            int httpCode = response.code();
            if (httpCode != 200) {
                throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.UNKNOWN, "http.code=" + httpCode);
            }
            String string = response.body().string();
            return JSONObject.parseObject(string, clazz);
        } catch (CamelliaDelayQueueException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaDelayQueueException(CamelliaDelayMsgErrorCode.UNKNOWN, e);
        }
    }

    public CamelliaDelayMsgSendResponse sendMsg(CamelliaDelayMsgSendRequest request) {
        DelayQueueServer server = nextDelayQueueServer();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("topic", request.getTopic());
            params.put("msg", request.getMsg());
            params.put("delayMillis", request.getDelayMillis());
            params.put("ttlMillis", request.getTtlMillis());
            params.put("maxRetry", request.getMaxRetry());
            if (request.getMsgId() != null) {
                params.put("msgId", request.getMsgId());
            }
            return invoke(okHttpClient, server.getUrl() + "/camellia/delayQueue/sendMsg",
                    params, CamelliaDelayMsgSendResponse.class);
        } catch (Exception e) {
            onError(server);
            throw e;
        }
    }

    public CamelliaDelayMsgDeleteResponse deleteMsg(CamelliaDelayMsgDeleteRequest request) {
        DelayQueueServer server = nextDelayQueueServer();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("topic", request.getTopic());
            params.put("msgId", request.getMsgId());
            return invoke(okHttpClient, server.getUrl() + "/camellia/delayQueue/deleteMsg",
                    params, CamelliaDelayMsgDeleteResponse.class);
        } catch (Exception e) {
            onError(server);
            throw e;
        }
    }

    public CamelliaDelayMsgPullResponse pullMsg(CamelliaDelayMsgPullRequest request) {
        DelayQueueServer server = nextDelayQueueServer();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("topic", request.getTopic());
            params.put("batch", request.getBatch());
            params.put("timeoutMillis", request.getAckTimeoutMillis());
            return invoke(okHttpClient, server.getUrl() + "/camellia/delayQueue/pullMsg",
                    params, CamelliaDelayMsgPullResponse.class);
        } catch (Exception e) {
            onError(server);
            throw e;
        }
    }

    public CamelliaDelayMsgAckResponse ackMsg(CamelliaDelayMsgAckRequest request) {
        DelayQueueServer server = nextDelayQueueServer();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("topic", request.getTopic());
            params.put("msgId", request.getMsgId());
            params.put("ack", request.isAck());
            return invoke(okHttpClient, server.getUrl() + "/camellia/delayQueue/ackMsg",
                    params, CamelliaDelayMsgAckResponse.class);
        } catch (Exception e) {
            onError(server);
            throw e;
        }
    }

    public CamelliaDelayMsgGetResponse getMsg(CamelliaDelayMsgGetRequest request) {
        DelayQueueServer server = nextDelayQueueServer();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("topic", request.getTopic());
            params.put("msgId", request.getMsgId());
            return invoke(okHttpClient, server.getUrl() + "/camellia/delayQueue/getMsg",
                    params, CamelliaDelayMsgGetResponse.class);
        } catch (Exception e) {
            onError(server);
            throw e;
        }
    }
}

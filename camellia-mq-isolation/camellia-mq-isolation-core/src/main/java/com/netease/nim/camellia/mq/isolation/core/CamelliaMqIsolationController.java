package com.netease.nim.camellia.mq.isolation.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.mq.isolation.core.config.MqIsolationConfig;
import com.netease.nim.camellia.mq.isolation.core.domain.ConsumerHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.domain.SenderHeartbeat;
import com.netease.nim.camellia.mq.isolation.core.mq.MqInfo;
import com.netease.nim.camellia.mq.isolation.core.stats.model.ConsumerBizStatsRequest;
import com.netease.nim.camellia.mq.isolation.core.stats.model.SenderBizStatsRequest;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2024/2/20
 */
public class CamelliaMqIsolationController implements MqIsolationController {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaMqIsolationController.class);

    private final String url;
    private final OkHttpClient okHttpClient;

    public CamelliaMqIsolationController(String url, int timeoutSeconds) {
        this.url = url;
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(4096);
        dispatcher.setMaxRequestsPerHost(1024);
        this.okHttpClient =  new OkHttpClient.Builder()
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(256, 3, TimeUnit.SECONDS))
                .build();
    }

    public CamelliaMqIsolationController(String url, OkHttpClient okHttpClient) {
        this.url = url;
        this.okHttpClient = okHttpClient;
    }

    @Override
    public MqIsolationConfig getMqIsolationConfig(String namespace) {
        try {
            String url = this.url + "/camellia/mq/isolation/config/getMqIsolationConfig?namespace=" + URLEncoder.encode(namespace, "utf-8");
            JSONObject result = invokeGet(okHttpClient, url);
            return result.getObject("data", MqIsolationConfig.class);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void reportConsumerBizStats(ConsumerBizStatsRequest request) {
        invokePost(okHttpClient, url + "/camellia/mq/isolation/route/reportConsumerBizStats", JSONObject.toJSONString(request));
    }

    @Override
    public void reportSenderBizStats(SenderBizStatsRequest request) {
        invokePost(okHttpClient, url + "/camellia/mq/isolation/route/reportSenderBizStats", JSONObject.toJSONString(request));
    }

    @Override
    public List<MqInfo> selectMqInfo(String namespace, String bizId) {
        try {
            String url = this.url + "/camellia/mq/isolation/config/selectMq?namespace="
                    + URLEncoder.encode(namespace, "utf-8") + "&bizId=" + URLEncoder.encode(bizId, "utf-8");
            JSONObject result = invokeGet(okHttpClient, url);
            JSONArray array = result.getJSONArray("data");
            List<MqInfo> list = new ArrayList<>();
            for (Object o : array) {
                list.add(JSONObject.parseObject(o.toString(), MqInfo.class));
            }
            return list;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void consumerHeartbeat(ConsumerHeartbeat heartbeat) {
        invokePost(okHttpClient, url + "/camellia/mq/isolation/heartbeat/consumerHeartbeat", JSONObject.toJSONString(heartbeat));
    }

    @Override
    public void senderHeartbeat(SenderHeartbeat heartbeat) {
        invokePost(okHttpClient, url + "/camellia/mq/isolation/heartbeat/senderHeartbeat", JSONObject.toJSONString(heartbeat));
    }

    public static JSONObject invokeGet(OkHttpClient okHttpClient, String url) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            response = okHttpClient.newCall(request).execute();
            int httpCode = response.code();
            if (httpCode != 200) {
                throw new IllegalStateException("http.code=" + httpCode);
            }
            String string = response.body().string();
            JSONObject json = JSONObject.parseObject(string);
            Integer code = json.getInteger("code");
            if (code == null || code != 200) {
                throw new IllegalStateException("code=" + code);
            }
            return json;
        } catch (IllegalStateException e) {
            logger.error("get error, url = {}", url, e);
            throw e;
        } catch (Exception e) {
            logger.error("get error, url = {}", url, e);
            throw new IllegalStateException(e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public static JSONObject invokePost(OkHttpClient okHttpClient, String url, String body) {
        Response response = null;
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), body);
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            response = okHttpClient.newCall(request).execute();
            int httpCode = response.code();
            if (httpCode != 200) {
                throw new IllegalStateException("http.code=" + httpCode);
            }
            String string = response.body().string();
            JSONObject json = JSONObject.parseObject(string);
            Integer code = json.getInteger("code");
            if (code == null || code != 200) {
                throw new IllegalStateException("code=" + code);
            }
            return json;
        } catch (IllegalStateException e) {
            logger.error("post error, url = {}, body = {}", url, body, e);
            throw e;
        } catch (Exception e) {
            logger.error("post error, url = {}, body = {}", url, body, e);
            throw new IllegalStateException(e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}

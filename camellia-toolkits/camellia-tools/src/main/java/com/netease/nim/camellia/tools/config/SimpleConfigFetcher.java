package com.netease.nim.camellia.tools.config;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.http.ParamBuilder;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class SimpleConfigFetcher {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfigFetcher.class);

    private final static OkHttpClient okHttpClient;
    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(4096);
        dispatcher.setMaxRequestsPerHost(256);
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5000L, TimeUnit.MILLISECONDS)
                .readTimeout(5000L, TimeUnit.MILLISECONDS)
                .writeTimeout(5000L, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(256, 3, TimeUnit.SECONDS))
                .build();
    }

    private final String url;
    private final String biz;

    private String md5 = null;
    private long nextPullTime = -1L;
    private String config = null;

    public SimpleConfigFetcher(String url, String biz) {
        this.biz = biz;
        this.url = url;
    }

    public String getConfig() {
        if (nextPullTime > 0 && nextPullTime <= System.currentTimeMillis()) {
            return config;
        }
        JSONObject response = null;
        try {
            ParamBuilder builder = new ParamBuilder();
            builder.addParam("biz", biz);
            if (md5 != null) {
                builder.addParam("md5", md5);
            }
            response = get(url + "?" + builder.build());
            int code = response.getIntValue("code");
            if (code == 304) {
                return config;
            }
            if (code != 200) {
                logger.error("fetch simple config error, biz = {}, response = {}", biz, response);
                return config;
            }
            this.md5 = response.getString("md5");
            this.nextPullTime = response.getLongValue("nextPullTime");
            String config = response.getString("config");
            logger.info("simple config updated, biz = {}, md5 = {}, config = {}", biz, md5, config);
            this.config = config;
            return config;
        } catch (Exception e) {
            logger.error("fetch simple config error, biz = {}, response = {}", biz, response, e);
            return config;
        }
    }

    private static JSONObject get(String url) {
        Request.Builder builder = new Request.Builder().get().url(url);
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return JSONObject.parseObject(response.body().string());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.netease.nim.camellia.core.discovery;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2022/3/8
 */
public abstract class SimpleHealthChecker<T> implements CamelliaServerHealthChecker<T> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleHealthChecker.class);

    private final OkHttpClient okHttpClient;
    private final Map<String, String> headers;

    public SimpleHealthChecker(OkHttpClient okHttpClient, Map<String, String> headers) {
        this.okHttpClient = okHttpClient;
        this.headers = headers;
    }

    public SimpleHealthChecker(OkHttpClient okHttpClient) {
        this(okHttpClient, new HashMap<String, String>());
    }

    public SimpleHealthChecker() {
        this(new OkHttpClient(), new HashMap<String, String>());
    }

    public abstract String toUrl(T server);

    @Override
    public boolean healthCheck(T server) {
        String url = toUrl(server);
        Response response = null;
        try {
            Request.Builder builder = new Request.Builder().url(url);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = builder.build();
            response = okHttpClient.newCall(request).execute();
            String string = response.body().string();
            if (logger.isTraceEnabled()) {
                logger.trace("ping response = {}", string);
            }
            boolean alive = response.isSuccessful();
            if (!alive) {
                logger.warn("{} not alive! code = {}", url, response.code());
            }
            return alive;
        } catch (Exception e) {
            logger.warn("{} not alive! ex = {}", url, e.toString());
            return false;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
}

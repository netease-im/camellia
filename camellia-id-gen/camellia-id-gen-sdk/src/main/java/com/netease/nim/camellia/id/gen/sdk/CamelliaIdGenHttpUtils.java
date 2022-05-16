package com.netease.nim.camellia.id.gen.sdk;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import okhttp3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaIdGenHttpUtils {

    public static OkHttpClient initOkHttpClient(CamelliaIdGenSdkConfig config) {
        Dispatcher dispatcher = new Dispatcher();
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

    public static long genId(OkHttpClient okHttpClient, String url) {
        try {
            JSONObject json = invokeGet(okHttpClient, url);
            return json.getLongValue("data");
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }

    public static List<Long> genIds(OkHttpClient okHttpClient, String url) {
        try {
            JSONObject json = invokeGet(okHttpClient, url);
            JSONArray data = json.getJSONArray("data");
            if (data != null) {
                List<Long> ids = new ArrayList<>();
                for (Object item : data) {
                    ids.add(Long.parseLong(item.toString()));
                }
                if (!ids.isEmpty()) {
                    return ids;
                }
                throw new CamelliaIdGenException("empty data");
            }
            throw new CamelliaIdGenException(json.getString("msg"));
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }

    public static JSONObject invokeGet(OkHttpClient okHttpClient, String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            int httpCode = response.code();
            if (httpCode != 200) {
                throw new CamelliaIdGenException(CamelliaIdGenException.NETWORK_ERROR, "http.code=" + httpCode);
            }
            String string = response.body().string();
            JSONObject json = JSONObject.parseObject(string);
            Integer code = json.getInteger("code");
            if (code == null || code != 200) {
                throw new CamelliaIdGenException("code=" + code);
            }
            return json;
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(CamelliaIdGenException.NETWORK_ERROR, e);
        }
    }

    public static JSONObject invokePost(OkHttpClient okHttpClient, String url, String body) {
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), body);
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            int httpCode = response.code();
            if (httpCode != 200) {
                throw new CamelliaIdGenException(CamelliaIdGenException.NETWORK_ERROR, "http.code=" + httpCode);
            }
            String string = response.body().string();
            JSONObject json = JSONObject.parseObject(string);
            Integer code = json.getInteger("code");
            if (code == null || code != 200) {
                throw new CamelliaIdGenException("code=" + code);
            }
            return json;
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(CamelliaIdGenException.NETWORK_ERROR, e);
        }
    }
}

package com.netease.nim.camellia.tools.http;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/10/20
 */
public class HttpClientUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    private static OkHttpClient okHttpClient;
    private static long timeoutMillis;
    private static boolean init = false;

    public static synchronized void init(HttpClientConfig httpClientConfig) {
        if (init) {
            return;
        }
        timeoutMillis = httpClientConfig.getReadTimeoutMillis();
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(httpClientConfig.getMaxRequests());
        dispatcher.setMaxRequestsPerHost(httpClientConfig.getMaxRequestsPerHost());

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(httpClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(httpClientConfig.getReadTimeoutMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(httpClientConfig.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(httpClientConfig.getMaxIdleConnections(), httpClientConfig.getKeepAliveSeconds(), TimeUnit.SECONDS));

        if (httpClientConfig.isSkipHostNameVerifier()) {
            builder.hostnameVerifier((s, sslSession) -> true);
            builder.sslSocketFactory(createUnsafeSSLSocketFactory(), new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }
                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            });
        }

        okHttpClient = builder.build();

        init = true;
    }


    private static SSLSocketFactory createUnsafeSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    //client

    public static OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    //get

    public static HttpResponse get(String url, Map<String, String> headers) {
        return get(okHttpClient, url, headers);
    }

    public static HttpResponse get(String url, Map<String, String> headers, long timeout, TimeUnit timeUnit) {
        return get(okHttpClient(timeout, timeUnit), url, headers);
    }

    public static HttpResponse get(String url) {
        return get(url, null);
    }

    //post form

    public static HttpResponse postForm(String url, Map<String, String> headers, Map<String, String> params) {
        return postForm(okHttpClient, url, headers, params);
    }

    public static HttpResponse postForm(String url, Map<String, String> headers, Map<String, String> params, long timeout, TimeUnit timeUnit) {
        return postForm(okHttpClient(timeout, timeUnit), url, headers, params);
    }

    public static HttpResponse postForm(String url, Map<String, String> params) {
        return postForm(okHttpClient, url, null, params);
    }

    //post json

    public static HttpResponse postJson(String url, Map<String, String> headers, String body) {
        return postJson(okHttpClient, url, headers, body);
    }

    public static HttpResponse postJson(String url, Map<String, String> headers, String body, long timeout, TimeUnit timeUnit) {
        return postJson(okHttpClient(timeout, timeUnit), url, headers, body);
    }

    public static HttpResponse postJson(String url, String body) {
        return postJson(okHttpClient, url, null, body);
    }

    //post bytes

    public static HttpResponse postBytes(String url, Map<String, String> headers, byte[] body) {
        return postBytes(okHttpClient, url, headers, body);
    }

    public static HttpResponse postBytes(String url, Map<String, String> headers, byte[] body, long timeout, TimeUnit timeUnit) {
        return postBytes(okHttpClient(timeout, timeUnit), url, headers, body);
    }

    public static HttpResponse postBytes(String url, byte[] body) {
        return postBytes(okHttpClient, url, null, body);
    }

    //post text

    public static HttpResponse postText(String url, Map<String, String> headers, String body) {
        return postText(okHttpClient, url, headers, body);
    }

    public static HttpResponse postText(String url, Map<String, String> headers, String body, long timeout, TimeUnit timeUnit) {
        return postText(okHttpClient(timeout, timeUnit), url, headers, body);
    }

    public static HttpResponse postText(String url, String body) {
        return postText(okHttpClient, url, null, body);
    }

    // delete json

    public static HttpResponse deleteJson(String url, Map<String, String> headers, String body) {
        return deleteJson(okHttpClient, url, headers, body);
    }

    public static HttpResponse deleteJson(String url, Map<String, String> headers, String body, long timeout, TimeUnit timeUnit) {
        return deleteJson(okHttpClient(timeout, timeUnit), url, headers, body);
    }

    public static HttpResponse deleteJson(String url, String body) {
        return deleteJson(okHttpClient, url, null, body);
    }

    // put json

    public static HttpResponse putJson(String url, Map<String, String> headers, String body) {
        return putJson(okHttpClient, url, headers, body);
    }

    public static HttpResponse putJson(String url, Map<String, String> headers, String body, long timeout, TimeUnit timeUnit) {
        return putJson(okHttpClient(timeout, timeUnit), url, headers, body);
    }

    public static HttpResponse putJson(String url, String body) {
        return putJson(okHttpClient, url, null, body);
    }

    //

    private static OkHttpClient okHttpClient(long timeout, TimeUnit timeUnit) {
        if (timeUnit.toMillis(timeout) == timeoutMillis) {
            return okHttpClient;
        } else {
            return okHttpClient.newBuilder()
                    .readTimeout(timeout, timeUnit)
                    .writeTimeout(timeout, timeUnit)
                    .connectTimeout(timeout, timeUnit)
                    .build();
        }
    }

    private static HttpResponse get(OkHttpClient okHttpClient, String url, Map<String, String> headers) {
        if (logger.isDebugEnabled()) {
            logger.debug("get, url = {}, headers = {}", url, headers);
        }
        Request.Builder builder = new Request.Builder().get().url(url);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return new HttpResponse(response.code(), response.body().bytes());
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static HttpResponse deleteJson(OkHttpClient okHttpClient, String url, Map<String, String> headers, String body) {
        if (logger.isDebugEnabled()) {
            logger.debug("delete, url = {}, headers = {}, body = {}", url, headers, body);
        }
        Request.Builder builder = new Request.Builder()
                .delete(RequestBody.create(MediaType.parse("application/json"), body))
                .url(url);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return new HttpResponse(response.code(), response.body().bytes());
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static HttpResponse putJson(OkHttpClient okHttpClient, String url, Map<String, String> headers, String body) {
        if (logger.isDebugEnabled()) {
            logger.debug("put, url = {}, headers = {}, body = {}", url, headers, body);
        }
        Request.Builder builder = new Request.Builder()
                .put(RequestBody.create(MediaType.parse("application/json"), body))
                .url(url);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(builder::addHeader);
        }
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            return new HttpResponse(response.code(), response.body().bytes());
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static HttpResponse postBytes(OkHttpClient okHttpClient, String url, Map<String, String> headers, byte[] body) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("postBytes, url = {}, headers = {}, body = {}", url, headers, body);
            }
            Request.Builder post = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/octet-stream"), body));
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = post.build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                return new HttpResponse(response.code(), response.body().bytes());
            }
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static HttpResponse postJson(OkHttpClient okHttpClient, String url, Map<String, String> headers, String body) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("postJson, url = {}, headers = {}, body = {}", url, headers, body);
            }
            Request.Builder post = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), body));
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = post.build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                return new HttpResponse(response.code(), response.body().bytes());
            }
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static HttpResponse postText(OkHttpClient okHttpClient, String url, Map<String, String> headers, String body) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("postText, url = {}, headers = {}, body = {}", url, headers, body);
            }
            Request.Builder post = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/text;charset=utf-8"), body));
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = post.build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                return new HttpResponse(response.code(), response.body().bytes());
            }
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }

    private static HttpResponse postForm(OkHttpClient okHttpClient, String url, Map<String, String> headers, Map<String, String> params) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("postForm, url = {}, headers = {}, params = {}", url, headers, params);
            }
            ParamBuilder builder = new ParamBuilder();
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        builder.addParam(entry.getKey(), entry.getValue());
                    }
                }
            }
            Request.Builder post = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded;charset=utf-8"), builder.build()));
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            Request request = post.build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                return new HttpResponse(response.code(), response.body().bytes());
            }
        } catch (Exception e) {
            throw new HttpException(e);
        }
    }
}

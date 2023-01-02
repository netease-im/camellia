package com.netease.nim.camellia.console.util;


import com.alibaba.fastjson.JSON;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.exception.AppException;
import okhttp3.*;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class OkhttpKit {
    //以KB为基础单位进行换算
    public static final long ONE_KILOBYTE = 1024;
    public static final long ONE_MEGABYTE = 1024 * ONE_KILOBYTE;
    public static final long ONE_GIGABYTE = 1024 * ONE_MEGABYTE;
    public static final long ONE_TERABYTE = 1024 * ONE_GIGABYTE;
    public static final String protocol_https = "https://";
    public static final String url_param_tag = "?";
    private static final Logger logger = LoggerFactory.getLogger(OkhttpKit.class);

    private static OkHttpClient client ;

    static {
        X509TrustManager manager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        };
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{manager}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            client= new OkHttpClient.Builder()
                    .connectTimeout(3000, TimeUnit.MILLISECONDS)
                    .readTimeout(4000, TimeUnit.MILLISECONDS)
                    .sslSocketFactory(sslSocketFactory, manager)
                    .connectionPool(new ConnectionPool(32, 5, TimeUnit.MINUTES))
                    .hostnameVerifier((s, sslSession) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }


    }

    public static RespBody postJson(String url, String data, Map<String, String> headers) {
        return postJson(url, null, data, headers);
    }

    public static RespBody getRequest(String url, Map<String, String> queryParas, Map<String, String> headersMap) {
        Call call = null;
        ResponseBody responseBody = null;
        try {
            String realUrl = buildUrlWithQueryString(url, queryParas);
            Headers headers = GetHeaders(headersMap);
            Request request = new Request.Builder().url(realUrl)
                    .headers(headers)
                    .build();

            if (logger.isDebugEnabled()) {
                logger.debug("request :" + request.toString());
                logger.debug("request header:" + request.headers().toString());
            }

            call = client.newCall(request);
            Response execute = call.execute();
            responseBody = execute.body();
            RespBody respBody = new RespBody();
            respBody.setHttpCode(execute.code());
            respBody.setHttpMessage(execute.message());
            if (responseBody != null) {
                respBody.setData(responseBody.string());
                if (logger.isDebugEnabled()) {
                    logger.debug("result:{}", JSON.toJSONString(respBody));
                }
            }
            return respBody;
        } catch (IOException e) {
            logger.error("url:{},connect wrong", url);
            throw new AppException(AppCode.DASHBOARD_CONNECT_WRONG, url + " connect wrong");
        } finally {
            if (call != null) {
                call.cancel();
            }
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }


    public static RespBody postJson(String url, Map<String, String> queryParas, String data, Map<String, String> headersMap) {
        Call call = null;
        ResponseBody responseBody = null;
        try {
            String realUrl = buildUrlWithQueryString(url, queryParas);
            Headers headers = GetHeaders(headersMap);
            Request request = new Request.Builder().url(realUrl)
                    .headers(headers)
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), data))
                    .build();


            //用于打印http请求的信息
            if (logger.isDebugEnabled()) {
                Buffer buffer = new Buffer();
                try {
                    request.body().writeTo(buffer);
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = request.body().contentType();
                    String requestString = buffer.readString(charset);

                    logger.debug("request :" + request.toString());
                    logger.debug("request header:" + request.headers().toString());
                    logger.debug("body:" + requestString);
                } catch (IOException e) {
                    logger.error("requestBody:", e);
                }
            }

            call = client.newCall(request);
            Response execute = call.execute();
            responseBody = execute.body();
            RespBody respBody = new RespBody();
            respBody.setHttpCode(execute.code());
            respBody.setHttpMessage(execute.message());
            if (responseBody != null) {
                respBody.setData(responseBody.string());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("response:" + respBody.toString());
            }
            return respBody;
        } catch (IOException e) {
            logger.error("url:{},connect wrong", url);
            throw new AppException(AppCode.DASHBOARD_CONNECT_WRONG, url + " connect wrong");
        } finally {
            if (call != null) {
                call.cancel();
            }
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }

    public static RespBody postForm(String url, Map<String, String> formBodyMap, Map<String, String> headersMap) {
        return postForm(url, null, formBodyMap, headersMap);
    }

    public static RespBody postForm(String url, Map<String, String> queryParas, Map<String, String> formBodyMap, Map<String, String> headersMap) {
        Call call = null;
        ResponseBody responseBody = null;
        try {
            String realUrl = buildUrlWithQueryString(url, queryParas);
            Headers headers = GetHeaders(headersMap);
            FormBody formBody = GetFormBody(formBodyMap);
            Request request = new Request.Builder().url(realUrl)
                    .headers(headers)
                    .post(formBody)
                    .build();

            //用于打印http请求的信息
            if (logger.isDebugEnabled()) {
                Buffer buffer = new Buffer();
                try {
                    request.body().writeTo(buffer);
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = request.body().contentType();
                    String requestString = buffer.readString(charset);

                    logger.debug("request :" + request.toString());
                    logger.debug("request header:" + request.headers().toString());
                    logger.debug("body:" + requestString);
                } catch (IOException e) {
                    logger.error("requestBody:", e);
                }
            }

            call = client.newCall(request);
            Response execute = call.execute();
            responseBody = execute.body();
            RespBody respBody = new RespBody();
            respBody.setHttpCode(execute.code());
            respBody.setHttpMessage(execute.message());
            if (responseBody != null) {
                respBody.setData(responseBody.string());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("response:" + respBody.toString());
            }
            return respBody;
        } catch (IOException e) {
            logger.error("url:{},connect wrong", url);
            throw new AppException(AppCode.DASHBOARD_CONNECT_WRONG, url + " connect wrong");
        } finally {
            if (call != null) {
                call.cancel();
            }
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }

    public static RespBody deleteRequest(String url, Map<String, String> formBodyMap, Map<String, String> headersMap) {
        return deleteRequest(url, null, formBodyMap, headersMap);
    }

    public static RespBody deleteRequest(String url, Map<String, String> queryParas, Map<String, String> formBodyMap, Map<String, String> headersMap) {
        Call call = null;
        ResponseBody responseBody = null;
        try {
            String realUrl = buildUrlWithQueryString(url, queryParas);
            Headers headers = GetHeaders(headersMap);
            FormBody formBody = GetFormBody(formBodyMap);
            Request request = new Request.Builder().url(realUrl)
                    .headers(headers)
                    .delete(formBody)
                    .build();
            //用于打印http请求的信息
            if (logger.isDebugEnabled()) {
                Buffer buffer = new Buffer();
                try {
                    request.body().writeTo(buffer);
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = request.body().contentType();
                    String requestString = buffer.readString(charset);

                    logger.debug("request :" + request.toString());
                    logger.debug("request header:" + request.headers().toString());
                    logger.debug("body:" + requestString);
                } catch (IOException e) {
                    logger.error("requestBody:", e);
                }
            }
            call = client.newCall(request);
            Response execute = call.execute();
            responseBody = execute.body();
            RespBody respBody = new RespBody();
            respBody.setHttpCode(execute.code());
            respBody.setHttpMessage(execute.message());
            if (responseBody != null) {
                respBody.setData(responseBody.string());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("response:" + respBody.toString());
            }
            return respBody;
        } catch (IOException e) {
            logger.error("url:{},connect wrong", url);
            throw new AppException(AppCode.DASHBOARD_CONNECT_WRONG, url + " connect wrong");
        } finally {
            if (call != null) {
                call.cancel();
            }
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }


    private static FormBody GetFormBody(Map<String, String> formBodyMap) {
        FormBody.Builder builder = new FormBody.Builder();
        if (formBodyMap == null || formBodyMap.isEmpty())
            return builder.build();
        for (Map.Entry<String, String> entry : formBodyMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            builder.add(key, value);
        }
        return builder.build();
    }


    private static Headers GetHeaders(Map<String, String> headersParams) {
        Headers.Builder headersBuilder = new Headers.Builder();
        if (headersParams == null || headersParams.isEmpty())
            return headersBuilder.build();
        for (Map.Entry<String, String> entry : headersParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            headersBuilder.add(key, value);
        }
        return headersBuilder.build();
    }


    /**
     * Build queryString of the url
     */
    private static String buildUrlWithQueryString(String url, Map<String, String> queryParas) {
        if (queryParas == null || queryParas.isEmpty()) {
            return url;
        }
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();

        for (Map.Entry<String, String> entry : queryParas.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            urlBuilder.addQueryParameter(key, value);
        }

        return urlBuilder.build().toString();
    }

}

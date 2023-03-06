package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.conf.FileBasedCamelliaConfig;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

import java.util.Map;

/**
 * Created by caojiajun on 2019/11/25.
 */
public class CamelliaApiUtil {

    public static CamelliaApi init(String url) {
        return init(url, null);
    }

    public static CamelliaMiscApi initMiscApi(String url) {
        return initMiscApi(url, null);
    }

    public static CamelliaApi init(String url, Map<String, String> headerMap) {
        return init(url, 10000, 60000, headerMap);
    }

    public static CamelliaMiscApi initMiscApi(String url, Map<String, String> headerMap) {
        return initMiscApi(url, 10000, 60000, headerMap);
    }


    public static CamelliaApi init(String url, int connectTimeoutMillis, int readTimeoutMillis) {
        return init(url, connectTimeoutMillis, readTimeoutMillis, null);
    }

    public static CamelliaMiscApi initMiscApi(String url, int connectTimeoutMillis, int readTimeoutMillis) {
        return initMiscApi(url, connectTimeoutMillis, readTimeoutMillis, null);
    }

    public static CamelliaApi init(String url, int connectTimeoutMillis, int readTimeoutMillis, Map<String, String> headerMap) {
        return init(CamelliaApi.class, url, connectTimeoutMillis, readTimeoutMillis, headerMap);
    }

    public static CamelliaMiscApi initMiscApi(String url, int connectTimeoutMillis, int readTimeoutMillis, Map<String, String> headerMap) {
        return init(CamelliaMiscApi.class, url, connectTimeoutMillis, readTimeoutMillis, headerMap);
    }

    private static <T> T init(Class<T> clazz, String url, int connectTimeoutMillis, int readTimeoutMillis, Map<String, String> headerMap) {
        if (CamelliaApi.class.isAssignableFrom(clazz)) {
            if (url.startsWith("file://")) {
                String[] split = url.split("//");
                if (split.length == 2) {
                    String fileName = split[1];
                    FileBasedCamelliaConfig camelliaConfig = new FileBasedCamelliaConfig(fileName);
                    return (T) new FileBasedCamelliaApi(camelliaConfig);
                }
            }
        }
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .requestInterceptor(template -> {
                    if (CamelliaApiEnv.source != null) {
                        template.header(CamelliaApiEnv.REQUEST_SOURCE, CamelliaApiEnv.source);
                    }
                    if (headerMap != null && !headerMap.isEmpty()) {
                        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                            template.header(entry.getKey(), entry.getValue());
                        }
                    }
                })
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(clazz, url);
    }
}

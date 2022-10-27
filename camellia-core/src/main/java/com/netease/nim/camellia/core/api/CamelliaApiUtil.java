package com.netease.nim.camellia.core.api;

import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

import java.util.Map;

/**
 * Created by caojiajun on 2019/11/25.
 */
public class CamelliaApiUtil {

    public static CamelliaApi init(String url, Map<String, String> headerMap) {
        return init(url, 10000, 60000, headerMap);
    }

    public static CamelliaApi init(String url, int connectTimeoutMillis, int readTimeoutMillis, Map<String, String> headerMap) {
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
                .target(CamelliaApi.class, url);
    }
}

package com.netease.nim.camellia.core.api;

import feign.Feign;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 *
 * Created by caojiajun on 2019/11/25.
 */
public class CamelliaApiUtil {

    public static CamelliaApi init(String url) {
        return init(url, 10000, 60000);
    }

    public static CamelliaApi init(String url, int connectTimeoutMillis, int readTimeoutMillis) {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .requestInterceptor(template -> {
                    if (CamelliaApiEnv.source != null) {
                        template.header(CamelliaApiEnv.REQUEST_SOURCE, CamelliaApiEnv.source);
                    }
                })
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(CamelliaApi.class, url);
    }
}

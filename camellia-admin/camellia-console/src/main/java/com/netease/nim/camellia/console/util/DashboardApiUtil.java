package com.netease.nim.camellia.console.util;

import com.netease.nim.camellia.console.command.DashboardHeartApi;
import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class DashboardApiUtil {
    public static DashboardHeartApi init(String url) {
        return init(url, 10000, 60000);
    }
    public static DashboardHeartApi init(String url, int connectTimeoutMillis, int readTimeoutMillis) {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .requestInterceptor(template -> {
                    if (CamelliaApiEnv.source != null) {
                        template.header(CamelliaApiEnv.REQUEST_SOURCE, CamelliaApiEnv.source);
                    }
                })
                .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
                .target(DashboardHeartApi.class, url);
    }
}

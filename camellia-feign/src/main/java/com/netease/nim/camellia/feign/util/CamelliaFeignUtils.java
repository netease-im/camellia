package com.netease.nim.camellia.feign.util;

import com.netease.nim.camellia.feign.CamelliaFeignProps;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import com.netease.nim.camellia.feign.resource.FeignResource;
import feign.Feign;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignUtils {

    public static <T> T generate(CamelliaFeignProps feignProps, Class<T> apiType, String feignUrl) {
        Feign.Builder builder = Feign.builder()
                    .requestInterceptors(feignProps.getRequestInterceptors())
                    .logLevel(feignProps.getLogLevel())
                    .contract(feignProps.getContract())
                    .client(feignProps.getClient())
                    .retryer(feignProps.getRetryer())
                    .logger(feignProps.getLogger())
                    .encoder(feignProps.getEncoder())
                    .decoder(feignProps.getDecoder())
                    .errorDecoder(feignProps.getErrorDecoder())
                    .options(feignProps.getOptions())
                    .invocationHandlerFactory(feignProps.getInvocationHandlerFactory());
        if (feignProps.isDecode404()) {
            builder.decode404();
        }
        return builder.target(apiType, feignUrl);
    }

    public static FeignResource toFeignResource(FeignServerInfo feignServerInfo) {
        String feignUrl = "http://" + feignServerInfo.getHost() + ":" + feignServerInfo.getPort();
        return new FeignResource(feignUrl);
    }
}

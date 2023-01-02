package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.feign.util.CamelliaFeignUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/3/1
 */
public interface FeignClientFactory<T> {

    T get(FeignResource feignResource);

    static class Default<T> implements FeignClientFactory<T> {

        private final Class<T> clazz;
        private final CamelliaFeignProps feignProps;
        private final DynamicOption dynamicOption;
        private final ConcurrentHashMap<String, T> map = new ConcurrentHashMap<>();

        public Default(Class<T> clazz, CamelliaFeignProps feignProps, DynamicOption dynamicOption) {
            this.clazz = clazz;
            this.feignProps = feignProps;
            this.dynamicOption = dynamicOption;
        }

        @Override
        public T get(FeignResource feignResource) {
            String feignUrl = feignResource.getFeignUrl();
            T client = map.get(feignUrl);
            if (client == null) {
                synchronized (map) {
                    client = map.get(feignUrl);
                    if (client == null) {
                        client = CamelliaFeignUtils.generate(feignProps, dynamicOption, clazz, feignUrl);
                        map.put(feignUrl, client);
                    }
                }
            }
            return client;
        }
    }
}

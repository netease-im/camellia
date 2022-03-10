package com.netease.nim.camellia.feign;

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
        private final ConcurrentHashMap<String, T> map = new ConcurrentHashMap<>();

        public Default(Class<T> clazz, CamelliaFeignProps feignProps) {
            this.clazz = clazz;
            this.feignProps = feignProps;
        }

        @Override
        public T get(FeignResource feignResource) {
            String feignUrl = feignResource.getFeignUrl();
            T t = map.get(feignUrl);
            if (t == null) {
                synchronized (map) {
                    t = map.get(feignUrl);
                    if (t == null) {
                        t = CamelliaFeignUtils.generate(feignProps, clazz, feignUrl);
                        map.put(feignUrl, t);
                    }
                }
            }
            return t;
        }
    }
}

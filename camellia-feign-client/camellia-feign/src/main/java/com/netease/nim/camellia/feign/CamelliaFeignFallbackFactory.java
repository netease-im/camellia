package com.netease.nim.camellia.feign;

/**
 * Created by caojiajun on 2022/4/2
 */
public interface CamelliaFeignFallbackFactory<T> {

    T getFallback(Throwable t);

    public static class Default<T> implements CamelliaFeignFallbackFactory<T> {

        private final T fallback;

        public Default(T fallback) {
            this.fallback = fallback;
        }

        @Override
        public T getFallback(Throwable t) {
            return fallback;
        }
    }
}

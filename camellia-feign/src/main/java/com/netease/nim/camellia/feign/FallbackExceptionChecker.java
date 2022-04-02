package com.netease.nim.camellia.feign;

/**
 * Created by caojiajun on 2022/4/2
 */
public interface FallbackExceptionChecker {

    /**
     * 判断一个异常是否需要触发熔断or触发fallback
     * @param e 异常
     * @return 是否需要跳过
     */
    boolean isSkipError(Throwable e);

    public static class Default implements FallbackExceptionChecker {

        @Override
        public boolean isSkipError(Throwable e) {
            return false;
        }
    }

}

package com.netease.nim.camellia.feign;

/**
 * Created by caojiajun on 2024/4/1
 */
public interface RetryPolicy {

    RetryInfo retryError(Throwable t);

    final class NeverRetryPolicy implements RetryPolicy {
        @Override
        public RetryInfo retryError(Throwable t) {
            return RetryInfo.NO_RETRY;
        }
    }

    final class AlwaysRetryCurrentPolicy implements RetryPolicy {
        @Override
        public RetryInfo retryError(Throwable t) {
            return RetryInfo.RETRY_CURRENT;
        }
    }

    final class AlwaysRetryNextPolicy implements RetryPolicy {
        @Override
        public RetryInfo retryError(Throwable t) {
            return RetryInfo.RETRY_NEXT;
        }
    }


    class RetryInfo {

        public static RetryInfo NO_RETRY = new RetryInfo(false, false);
        public static RetryInfo RETRY_CURRENT = new RetryInfo(true, false);
        public static RetryInfo RETRY_NEXT = new RetryInfo(true, true);

        private final boolean retry;
        private final boolean nextServer;

        public RetryInfo(boolean retry, boolean nextServer) {
            this.retry = retry;
            this.nextServer = nextServer;
        }

        public boolean isRetry() {
            return retry;
        }

        public boolean isNextServer() {
            return nextServer;
        }
    }
}

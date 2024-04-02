package com.netease.nim.camellia.core.client.annotation;

/**
 * Created by caojiajun on 2024/4/1
 */
public interface RetryPolicy {

    RetryAction onError(Throwable t);

    final class NeverRetryPolicy implements RetryPolicy {
        @Override
        public RetryAction onError(Throwable t) {
            return RetryAction.NO_RETRY;
        }
    }

    final class AlwaysRetryCurrentPolicy implements RetryPolicy {
        @Override
        public RetryAction onError(Throwable t) {
            return RetryAction.RETRY_CURRENT;
        }
    }

    final class AlwaysRetryNextPolicy implements RetryPolicy {
        @Override
        public RetryAction onError(Throwable t) {
            return RetryAction.RETRY_NEXT;
        }
    }


    class RetryAction {

        public static RetryAction NO_RETRY = new RetryAction(false, false);
        public static RetryAction RETRY_CURRENT = new RetryAction(true, false);
        public static RetryAction RETRY_NEXT = new RetryAction(true, true);

        private final boolean retry;
        private final boolean nextServer;

        public RetryAction(boolean retry, boolean nextServer) {
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

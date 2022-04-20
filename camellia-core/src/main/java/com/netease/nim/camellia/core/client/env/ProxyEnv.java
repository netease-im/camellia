package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.client.callback.OperationCallback;
import com.netease.nim.camellia.core.client.callback.ShardingCallback;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;

import java.util.concurrent.*;

/**
 *
 * Created by caojiajun on 2019/8/14.
 */
public class ProxyEnv {

    //批量操作是否并发进行，默认true，即针对多个分片的操作同时进行，而不是顺序执行
    private boolean shardingConcurrentEnable = ProxyConstants.shardingConcurrentEnable;
    //批量操作的线程池大小
    private int shardingConcurrentExecPoolSize = ProxyConstants.shardingConcurrentExecPoolSize;
    //批量操作的并发线程池
    private ExecutorService shardingConcurrentExec;

    //多写操作类型
    private MultiWriteType multiWriteType = MultiWriteType.MULTI_THREAD_CONCURRENT;

    //多写操作（多线程并发）的线程池大小
    private int multiWriteConcurrentExecPoolSize = ProxyConstants.multiWriteConcurrentExecPoolSize;
    //多写操作（多线程并发）的线程池
    private ExecutorService multiWriteConcurrentExec;

    //多写操作（异步）的线程池大小
    private int multiWriteAsyncExecPoolSize = ProxyConstants.multiWriteAsyncExecPoolSize;
    //多写操作（异步）的队列大小
    private int multiWriteAsyncExecQueueSize = ProxyConstants.multiWriteAsyncExecQueueSize;
    //多写操作（异步）的线程池
    private ExecutorService multiWriteAsyncExec;

    //监控bean，若为null，表示不监控
    private Monitor monitor;

    private ThreadContextSwitchStrategy threadContextSwitchStrategy = new ThreadContextSwitchStrategy.Default();

    //分片函数
    private ShardingFunc shardingFunc = new DefaultShardingFunc();

    private ProxyEnv() {
        initExec();
    }

    private ProxyEnv(boolean shardingConcurrentEnable, int shardingConcurrentExecPoolSize, MultiWriteType multiWriteType,
                    int multiWriteConcurrentExecPoolSize, int multiWriteAsyncExecPoolSize, int multiWriteAsyncExecQueueSize, Monitor monitor, ShardingFunc shardingFunc) {
        this.shardingConcurrentEnable = shardingConcurrentEnable;
        this.shardingConcurrentExecPoolSize = shardingConcurrentExecPoolSize;
        this.multiWriteType = multiWriteType;
        this.multiWriteConcurrentExecPoolSize = multiWriteConcurrentExecPoolSize;
        this.multiWriteAsyncExecPoolSize = multiWriteAsyncExecPoolSize;
        this.multiWriteAsyncExecQueueSize = multiWriteAsyncExecQueueSize;
        this.monitor = monitor;
        this.shardingFunc = shardingFunc;
        initExec();
    }

    private void initExec() {
        shardingConcurrentExec = new ThreadPoolExecutor(shardingConcurrentExecPoolSize, shardingConcurrentExecPoolSize, 0, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new CamelliaThreadFactory(ShardingCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
        multiWriteConcurrentExec = new ThreadPoolExecutor(multiWriteConcurrentExecPoolSize, multiWriteConcurrentExecPoolSize, 0, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new CamelliaThreadFactory(OperationCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
        multiWriteAsyncExec = new ThreadPoolExecutor(multiWriteAsyncExecPoolSize, multiWriteAsyncExecPoolSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(multiWriteAsyncExecQueueSize), new CamelliaThreadFactory(OperationCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static ProxyEnv defaultProxyEnv() {
        return new ProxyEnv();
    }

    public boolean isShardingConcurrentEnable() {
        return shardingConcurrentEnable;
    }

    public ExecutorService getShardingConcurrentExec() {
        return shardingConcurrentExec;
    }

    public MultiWriteType getMultiWriteType() {
        return multiWriteType;
    }

    public ExecutorService getMultiWriteConcurrentExec() {
        return multiWriteConcurrentExec;
    }

    public ExecutorService getMultiWriteAsyncExec() {
        return multiWriteAsyncExec;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public ShardingFunc getShardingFunc() {
        return shardingFunc;
    }

    public ThreadContextSwitchStrategy getThreadContextSwitchStrategy() {
        return threadContextSwitchStrategy;
    }

    public static class Builder {

        private final ProxyEnv proxyEnv;

        public Builder() {
            proxyEnv = new ProxyEnv();
        }

        public Builder(ProxyEnv proxyEnv) {
            this.proxyEnv = new ProxyEnv(proxyEnv.shardingConcurrentEnable, proxyEnv.shardingConcurrentExecPoolSize, proxyEnv.multiWriteType,
                    proxyEnv.multiWriteConcurrentExecPoolSize, proxyEnv.multiWriteAsyncExecPoolSize, proxyEnv.multiWriteAsyncExecQueueSize,
                    proxyEnv.monitor, proxyEnv.shardingFunc);
            this.proxyEnv.shardingConcurrentExec = proxyEnv.shardingConcurrentExec;
            this.proxyEnv.multiWriteConcurrentExec = proxyEnv.multiWriteConcurrentExec;
            this.proxyEnv.threadContextSwitchStrategy = proxyEnv.threadContextSwitchStrategy;
        }

        public Builder shardingConcurrentEnable(boolean shardingConcurrentEnable) {
            proxyEnv.shardingConcurrentEnable = shardingConcurrentEnable;
            return this;
        }

        public Builder shardingConcurrentExecPoolSize(int shardingConcurrentExecPoolSize) {
            if (shardingConcurrentExecPoolSize > 0) {
                proxyEnv.shardingConcurrentExecPoolSize = shardingConcurrentExecPoolSize;
            }
            return this;
        }

        public Builder multiWriteType(MultiWriteType multiWriteType) {
            proxyEnv.multiWriteType = multiWriteType;
            return this;
        }

        public Builder multiWriteConcurrentExecPoolSize(int multiWriteConcurrentExecPoolSize) {
            if (multiWriteConcurrentExecPoolSize > 0) {
                proxyEnv.multiWriteConcurrentExecPoolSize = multiWriteConcurrentExecPoolSize;
            }
            return this;
        }

        public Builder multiWriteAsyncExecPoolSize(int multiWriteAsyncExecPoolSize) {
            if (multiWriteAsyncExecPoolSize > 0) {
                proxyEnv.multiWriteAsyncExecPoolSize = multiWriteAsyncExecPoolSize;
            }
            return this;
        }

        public Builder multiWriteAsyncExecQueueSize(int multiWriteAsyncExecQueueSize) {
            if (multiWriteAsyncExecQueueSize > 0) {
                proxyEnv.multiWriteAsyncExecQueueSize = multiWriteAsyncExecQueueSize;
            }
            return this;
        }

        public Builder monitor(Monitor monitor) {
            if (monitor != null) {
                proxyEnv.monitor = monitor;
            }
            return this;
        }

        public Builder shardingFunc(ShardingFunc shardingFunc) {
            if (shardingFunc != null) {
                proxyEnv.shardingFunc = shardingFunc;
            }
            return this;
        }

        public Builder threadContextSwitchStrategy(ThreadContextSwitchStrategy threadContextSwitchStrategy) {
            if (threadContextSwitchStrategy != null) {
                proxyEnv.threadContextSwitchStrategy = threadContextSwitchStrategy;
            }
            return this;
        }

        public ProxyEnv build() {
            return proxyEnv;
        }
    }
}

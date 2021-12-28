package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.client.callback.OperationCallback;
import com.netease.nim.camellia.core.client.callback.ShardingCallback;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    //多写操作是否并发进行，默认true，即针对多个资源的多写操作
    private boolean multiWriteConcurrentEnable = ProxyConstants.multiWriteConcurrentEnable;
    //多写操作的线程池大小
    private int multiWriteConcurrentExecPoolSize = ProxyConstants.multiWriteConcurrentExecPoolSize;
    //多写操作的并发线程池
    private ExecutorService multiWriteConcurrentExec;

    //监控bean，若为null，表示不监控
    private Monitor monitor;

    //分片函数
    private ShardingFunc shardingFunc = new DefaultShardingFunc();

    private ProxyEnv() {
        initExec();
    }

    private ProxyEnv(boolean shardingConcurrentEnable, int shardingConcurrentExecPoolSize, boolean multiWriteConcurrentEnable,
                    int multiWriteConcurrentExecPoolSize, Monitor monitor, ShardingFunc shardingFunc) {
        this.shardingConcurrentEnable = shardingConcurrentEnable;
        this.shardingConcurrentExecPoolSize = shardingConcurrentExecPoolSize;
        this.multiWriteConcurrentEnable = multiWriteConcurrentEnable;
        this.multiWriteConcurrentExecPoolSize = multiWriteConcurrentExecPoolSize;
        this.monitor = monitor;
        this.shardingFunc = shardingFunc;
        initExec();
    }

    private void initExec() {
        shardingConcurrentExec = new ThreadPoolExecutor(shardingConcurrentExecPoolSize, shardingConcurrentExecPoolSize, 0, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new CamelliaThreadFactory(ShardingCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
        multiWriteConcurrentExec = new ThreadPoolExecutor(multiWriteConcurrentExecPoolSize, multiWriteConcurrentExecPoolSize, 0, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new CamelliaThreadFactory(OperationCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
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

    public boolean isMultiWriteConcurrentEnable() {
        return multiWriteConcurrentEnable;
    }

    public ExecutorService getMultiWriteConcurrentExec() {
        return multiWriteConcurrentExec;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public ShardingFunc getShardingFunc() {
        return shardingFunc;
    }

    public static class Builder {

        private final ProxyEnv proxyEnv;

        public Builder() {
            proxyEnv = new ProxyEnv();
        }

        public Builder(ProxyEnv proxyEnv) {
            this.proxyEnv = new ProxyEnv(proxyEnv.shardingConcurrentEnable, proxyEnv.shardingConcurrentExecPoolSize, proxyEnv.multiWriteConcurrentEnable,
                    proxyEnv.multiWriteConcurrentExecPoolSize, proxyEnv.monitor, proxyEnv.shardingFunc);
            this.proxyEnv.shardingConcurrentExec = proxyEnv.shardingConcurrentExec;
            this.proxyEnv.multiWriteConcurrentExec = proxyEnv.multiWriteConcurrentExec;
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

        public Builder multiWriteConcurrentEnable(boolean multiWriteConcurrentEnable) {
            proxyEnv.multiWriteConcurrentEnable = multiWriteConcurrentEnable;
            return this;
        }

        public Builder multiWriteConcurrentExecPoolSize(int multiWriteConcurrentExecPoolSize) {
            if (multiWriteConcurrentExecPoolSize > 0) {
                proxyEnv.multiWriteConcurrentExecPoolSize = multiWriteConcurrentExecPoolSize;
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

        public ProxyEnv build() {
            return proxyEnv;
        }
    }
}

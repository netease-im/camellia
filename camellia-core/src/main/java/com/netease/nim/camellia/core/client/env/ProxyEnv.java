package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.client.callback.OperationCallback;
import com.netease.nim.camellia.core.client.callback.ShadingCallback;
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
    private boolean shadingConcurrentEnable = ProxyConstants.shadingConcurrentEnable;
    //批量操作的线程池大小
    private int shadingConcurrentExecPoolSize = ProxyConstants.shadingConcurrentExecPoolSize;
    //批量操作的并发线程池
    private ExecutorService shadingConcurrentExec;

    //多写操作是否并发进行，默认true，即针对多个资源的多写操作
    private boolean multiWriteConcurrentEnable = ProxyConstants.multiWriteConcurrentEnable;
    //多写操作的线程池大小
    private int multiWriteConcurrentExecPoolSize = ProxyConstants.multiWriteConcurrentExecPoolSize;
    //多写操作的并发线程池
    private ExecutorService multiWriteConcurrentExec;

    //监控bean，若为null，表示不监控
    private Monitor monitor;

    //分片函数
    private ShadingFunc shadingFunc = new DefaultShadingFunc();

    private ProxyEnv() {
        initExec();
    }

    private ProxyEnv(boolean shadingConcurrentEnable, int shadingConcurrentExecPoolSize, boolean multiWriteConcurrentEnable,
                    int multiWriteConcurrentExecPoolSize, Monitor monitor, ShadingFunc shadingFunc) {
        this.shadingConcurrentEnable = shadingConcurrentEnable;
        this.shadingConcurrentExecPoolSize = shadingConcurrentExecPoolSize;
        this.multiWriteConcurrentEnable = multiWriteConcurrentEnable;
        this.multiWriteConcurrentExecPoolSize = multiWriteConcurrentExecPoolSize;
        this.monitor = monitor;
        this.shadingFunc = shadingFunc;
        initExec();
    }

    private void initExec() {
        shadingConcurrentExec = new ThreadPoolExecutor(shadingConcurrentExecPoolSize, shadingConcurrentExecPoolSize, 0, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new CamelliaThreadFactory(ShadingCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
        multiWriteConcurrentExec = new ThreadPoolExecutor(multiWriteConcurrentExecPoolSize, multiWriteConcurrentExecPoolSize, 0, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new CamelliaThreadFactory(OperationCallback.class), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static ProxyEnv defaultProxyEnv() {
        return new ProxyEnv();
    }

    public boolean isShadingConcurrentEnable() {
        return shadingConcurrentEnable;
    }

    public ExecutorService getShadingConcurrentExec() {
        return shadingConcurrentExec;
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

    public ShadingFunc getShadingFunc() {
        return shadingFunc;
    }

    public static class Builder {

        private final ProxyEnv proxyEnv;

        public Builder() {
            proxyEnv = new ProxyEnv();
        }

        public Builder(ProxyEnv proxyEnv) {
            this.proxyEnv = new ProxyEnv(proxyEnv.shadingConcurrentEnable, proxyEnv.shadingConcurrentExecPoolSize, proxyEnv.multiWriteConcurrentEnable,
                    proxyEnv.multiWriteConcurrentExecPoolSize, proxyEnv.monitor, proxyEnv.shadingFunc);
            this.proxyEnv.shadingConcurrentExec = proxyEnv.shadingConcurrentExec;
            this.proxyEnv.multiWriteConcurrentExec = proxyEnv.multiWriteConcurrentExec;
        }

        public Builder shadingConcurrentEnable(boolean shadingConcurrentEnable) {
            proxyEnv.shadingConcurrentEnable = shadingConcurrentEnable;
            return this;
        }

        public Builder shadingConcurrentExecPoolSize(int shadingConcurrentExecPoolSize) {
            if (shadingConcurrentExecPoolSize > 0) {
                proxyEnv.shadingConcurrentExecPoolSize = shadingConcurrentExecPoolSize;
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

        public Builder shadingFunc(ShadingFunc shadingFunc) {
            if (shadingFunc != null) {
                proxyEnv.shadingFunc = shadingFunc;
            }
            return this;
        }

        public ProxyEnv build() {
            return proxyEnv;
        }
    }
}

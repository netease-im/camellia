package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.client.callback.ProxyClientFactory;
import com.netease.nim.camellia.core.client.callback.DynamicProxyCallback;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.standard.StandardProxyGenerator;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *
 * Created by caojiajun on 2019/5/20.
 */
public class ReloadableProxyFactory<T> {

    private static final Logger logger = LoggerFactory.getLogger(ReloadableProxyFactory.class);

    private final Class<T> clazz;
    private final Long bid;//业务类型
    private final String bgroup;//业务分组
    private final CamelliaApi service;
    private final Resource defaultResource;
    private CamelliaApiResponse response;
    private T proxy;
    private StandardProxyGenerator<T> generator;
    private final ProxyEnv env;
    private ResourceChooser resourceChooser;

    private final AtomicBoolean reloading = new AtomicBoolean(false);

    private ReloadableProxyFactory(Class<T> clazz, ProxyEnv proxyEnv, Long bid, String bgroup, CamelliaApi service,
                                   Resource defaultResource, long checkIntervalMillis, boolean monitorEnable) {
        this.clazz = clazz;
        this.bid = bid;
        this.bgroup = bgroup;
        this.defaultResource = defaultResource;
        this.service = service;
        if (monitorEnable) {
            Monitor monitor = new RemoteMonitor(bid, bgroup, service);
            env = new ProxyEnv.Builder(proxyEnv).monitor(monitor).build();
        } else {
            env = proxyEnv;
        }
        reload(true);
        Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(ReloadableProxyFactory.class)).scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                reload(false);
            }
        }, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public void reload(boolean throwError) {
        if (reloading.compareAndSet(false, true)) {
            try {
                String md5 = this.response == null ? null : this.response.getMd5();
                CamelliaApiResponse response = service.getResourceTable(bid, bgroup, md5);
                if (response.getCode() == CamelliaApiCode.NOT_MODIFY.getCode()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("not modify, bid = {}, bgroup = {}, md5 = {}", bid, bgroup, md5);
                    }
                    if (throwError) {
                        throw new IllegalArgumentException("api.code=" + response.getCode() + ",bid=" + bid + ",bgroup=" + bgroup);
                    }
                    return;
                }
                if (response.getCode() != CamelliaApiCode.SUCCESS.getCode()) {
                    logger.error("api code = {}, bid = {}, bgroup = {}", response.getCode(), bid, bgroup);
                    if (throwError) {
                        throw new IllegalArgumentException("api.code=" + response.getCode() + ",bid=" + bid + ",bgroup=" + bgroup);
                    }
                    return;
                }
                if (logger.isInfoEnabled()) {
                    logger.info("getOrUpdate resourceTable success, bid = {}, bgroup = {}, md5 = {}, resourceTable = {}",
                            bid, bgroup, response.getMd5(), ReadableResourceTableUtil.readableResourceTable(response.getResourceTable()));
                }
                this.response = response;
                this.resourceChooser = new ResourceChooser(response.getResourceTable(), env);
                this.generator = new StandardProxyGenerator<>(clazz, response.getResourceTable(), defaultResource, env);
                T proxy = generator.generate();
                if (logger.isInfoEnabled()) {
                    logger.info("initOrReload success, bid = {}, bgroup = {}, proxy = {}", bid, bgroup, clazz.getName());
                }
                this.proxy = proxy;
            } catch (Exception e) {
                logger.error("initOrReload error, bid = {}, bgroup = {}, clazz = {}", bid, bgroup, clazz.getName(), e);
                if (throwError) {
                    throw e;
                }
            } finally {
                reloading.compareAndSet(true, false);
            }
        }
    }

    /**
     * 获取代理对象
     * @return 代理对象
     */
    public T getProxy() {
        return proxy;
    }

    /**
     * 获取会自动更新的动态代理对象
     * @return 代理对象
     */
    public T getDynamicProxy() {
        return ProxyClientFactory.createProxy(clazz, new Class[] {Resource.class},
                new Object[] {null}, new DynamicProxyCallback<>(this));
    }

    /**
     * 暴露给上层一个通过shadingKey选择Proxy的方法，一般用不上
     * @param key shadingKey
     * @return 代理对象
     */
    public T chooseProxy(byte[]... key) {
        return this.generator.getProxyHub().chooseProxy(key);
    }

    /**
     * 获取来自CamelliaApi的response
     * @return CamelliaApiResponse
     */
    public CamelliaApiResponse getResponse() {
        return response;
    }

    /**
     * 获取ResourceChooser
     * @return ResourceChooser
     */
    public ResourceChooser getResourceChooser() {
        return resourceChooser;
    }

    /**
     * 获取当前本地代理的环境配置
     * @return ProxyEnv
     */
    public ProxyEnv getEnv() {
        return env;
    }

    public static class Builder<T> {
        private Class<T> clazz;
        private Long bid;
        private String bgroup;
        private CamelliaApi service;
        private Resource defaultResource;
        private long checkIntervalMillis = 5000;
        private boolean monitorEnable = true;
        private ProxyEnv proxyEnv = ProxyEnv.defaultProxyEnv();

        public Builder() {
        }

        public Builder<T> clazz(Class<T> clazz) {
            if (clazz == null) {
                throw new IllegalArgumentException("clazz is null");
            }
            try {
                //必须包括一个这样的构造方法
                clazz.getConstructor(Resource.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(e);
            }
            this.clazz = clazz;
            return this;
        }

        public Builder<T> bid(Long bid) {
            if (bid == null) {
                throw new IllegalArgumentException("bid is null");
            }
            this.bid = bid;
            return this;
        }

        public Builder<T> bgroup(String bgroup) {
            if (bgroup == null) {
                throw new IllegalArgumentException("group is null");
            }
            this.bgroup = bgroup;
            return this;
        }

        public Builder<T> service(CamelliaApi service) {
            if (service == null) {
                throw new IllegalArgumentException("service is null");
            }
            this.service = service;
            return this;
        }

        public Builder<T> defaultResource(Resource resource) {
            this.defaultResource = resource;
            return this;
        }

        public Builder<T> checkIntervalMillis(long checkIntervalMillis) {
            if (checkIntervalMillis < 500) {
                throw new IllegalArgumentException("checkIntervalMillis should > 500");
            }
            this.checkIntervalMillis = checkIntervalMillis;
            return this;
        }

        public Builder<T> monitorEnable(boolean monitorEnable) {
            this.monitorEnable = monitorEnable;
            return this;
        }

        public Builder<T> proxyEnv(ProxyEnv proxyEnv) {
            this.proxyEnv = proxyEnv;
            return this;
        }

        public ReloadableProxyFactory<T> build() {
            return new ReloadableProxyFactory<>(clazz, proxyEnv, bid, bgroup, service, defaultResource,
                    checkIntervalMillis, monitorEnable);
        }
    }
}

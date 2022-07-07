package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.RemoteMonitor;
import com.netease.nim.camellia.core.client.annotation.RouteKey;
import com.netease.nim.camellia.core.client.callback.ProxyClientFactory;
import com.netease.nim.camellia.core.client.env.Monitor;
import com.netease.nim.camellia.core.util.AnnotationValueGetterCache;
import com.netease.nim.camellia.core.util.ExceptionUtils;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.conf.CamelliaFeignDynamicOptionGetter;
import com.netease.nim.camellia.feign.route.CamelliaDashboardFeignResourceTableUpdater;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2022/3/28
 */
public class FeignDynamicRouteCallback<T> implements MethodInterceptor {

    private final ConcurrentHashMap<String, T> clientMap = new ConcurrentHashMap<>();

    private final T defaultFeignClient;
    private final CamelliaFeignBuildParam<T> buildParam;
    private final AnnotationValueGetterCache annotationValueGetterCache = new AnnotationValueGetterCache();
    private final long bid;
    private final String defaultBgroup;
    private final CamelliaFeignDynamicOptionGetter camelliaFeignDynamicOptionGetter;
    private final CamelliaApi camelliaApi;
    private final long checkIntervalMillis;

    public FeignDynamicRouteCallback(T defaultFeignClient, CamelliaFeignBuildParam<T> buildParam, CamelliaFeignDynamicOptionGetter camelliaFeignDynamicOptionGetter,
                                     long bid, String defaultBgroup, CamelliaApi camelliaApi, long checkIntervalMillis) {
        this.defaultFeignClient = defaultFeignClient;
        this.buildParam = buildParam;
        this.bid = bid;
        this.defaultBgroup = defaultBgroup;
        this.camelliaApi = camelliaApi;
        this.checkIntervalMillis = checkIntervalMillis;
        this.camelliaFeignDynamicOptionGetter = camelliaFeignDynamicOptionGetter;
        annotationValueGetterCache.preheatAnnotationValueByParameterField(buildParam.getApiType(), RouteKey.class);
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        try {
            Object routeKey = annotationValueGetterCache.getAnnotationValueByParameterField(RouteKey.class, method, objects);
            String bgroup = null;
            if (routeKey != null) {
                bgroup = camelliaFeignDynamicOptionGetter.getDynamicRouteConfGetter(bid).bgroup(routeKey);
            }
            T feignClient = getFeignClient(bgroup);
            return method.invoke(feignClient, objects);
        } catch (Exception e) {
            throw ExceptionUtils.onError(e);
        }
    }

    private T getFeignClient(String bgroup) {
        if (bgroup == null || bgroup.equals(defaultBgroup)) {
            return defaultFeignClient;
        }
        T client = clientMap.get(bgroup);
        if (client == null) {
            synchronized (clientMap) {
                client = clientMap.get(bgroup);
                if (client == null) {
                    CamelliaFeignBuildParam<T> param = buildParam.duplicate();
                    Monitor monitor = buildParam.getFeignEnv().getProxyEnv().getMonitor();
                    DynamicOption dynamicOption = camelliaFeignDynamicOptionGetter.getDynamicOption(bid, bgroup);
                    if (dynamicOption != null) {
                        Boolean monitorEnable = dynamicOption.isMonitorEnable();
                        if (monitorEnable != null && monitorEnable && camelliaApi != null) {
                            if (monitor == null) {
                                monitor = new RemoteMonitor(bid, bgroup, camelliaApi);
                            }
                        } else {
                            monitor = null;
                        }
                    }
                    param.setMonitor(monitor);
                    param.setDynamicOption(camelliaFeignDynamicOptionGetter.getDynamicOption(bid, bgroup));
                    param.setUpdater(new CamelliaDashboardFeignResourceTableUpdater(camelliaApi, bid, bgroup, checkIntervalMillis));
                    param.setBgroup(bgroup);
                    client = ProxyClientFactory.createProxy(param.getApiType(), new FeignCallback<>(param));
                    clientMap.put(bgroup, client);
                }
            }
        }
        return client;
    }
}

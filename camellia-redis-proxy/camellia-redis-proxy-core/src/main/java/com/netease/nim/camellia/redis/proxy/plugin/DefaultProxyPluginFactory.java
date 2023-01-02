package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2022/9/16
 */
public class DefaultProxyPluginFactory implements ProxyPluginFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyPluginFactory.class);

    private static final String CONF_KEY = "proxy.plugin.list";
    private final List<String> defaultPlugins;

    private final ProxyBeanFactory beanFactory;
    private String pluginConf;
    private final List<Runnable> callbackSet = new ArrayList<>();

    private final ConcurrentHashMap<String, ProxyPlugin> pluginMap = new ConcurrentHashMap<>();

    public DefaultProxyPluginFactory(List<String> defaultPlugins, ProxyBeanFactory beanFactory) {
        this.defaultPlugins = defaultPlugins;
        this.beanFactory = beanFactory;
        reload();
        int seconds = ProxyDynamicConf.getInt("proxy.plugin.update.interval.seconds", 60);
        ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
    }

    private void reload() {
        String pluginConf = ProxyDynamicConf.getString(CONF_KEY, "");
        if (!Objects.equals(pluginConf, this.pluginConf)) {
            this.pluginConf = pluginConf;
            for (Runnable runnable : callbackSet) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    logger.error("proxy plugin callback error", e);
                }
            }
        }
    }

    @Override
    public ProxyPluginInitResp initPlugins() {
        Set<String> pluginSet = new HashSet<>(defaultPlugins);
        if (pluginConf != null && pluginConf.trim().length() != 0) {
            String[] split = pluginConf.trim().split(",");
            pluginSet.addAll(Arrays.asList(split));
        }
        if (pluginSet.isEmpty()) {
            return new ProxyPluginInitResp(new ArrayList<>(), new ArrayList<>());
        }
        List<ProxyPlugin> plugins = new ArrayList<>();
        for (String classOrAlias : pluginSet) {
            ProxyPlugin proxyPlugin = getOrInitProxyPlugin(classOrAlias);
            plugins.add(proxyPlugin);
        }
        plugins.sort((p1, p2) -> Integer.compare(p1.order().request(), p2.order().request()) * -1);
        List<ProxyPlugin> requestPlugins = new ArrayList<>(plugins);
        if (logger.isInfoEnabled()) {
            logger.info("###request-plugins-start");
            for (int i=0; i<requestPlugins.size(); i++) {
                ProxyPlugin plugin = requestPlugins.get(i);
                logger.info("index = {}, order = {}, plugin = {}", i, plugin.order().request(), plugin.getClass().getName());
            }
            logger.info("###requst-plugins-end");
        }
        plugins.sort((p1, p2) -> Integer.compare(p1.order().reply(), p2.order().reply()) * -1);
        List<ProxyPlugin> replyPlugins = new ArrayList<>(plugins);
        if (logger.isInfoEnabled()) {
            logger.info("###reply-plugins-start");
            for (int i=0; i<replyPlugins.size(); i++) {
                ProxyPlugin plugin = replyPlugins.get(i);
                logger.info("index = {}, order = {}, plugin = {}", i, plugin.order().reply(), plugin.getClass().getName());
            }
            logger.info("###reply-plugins-end");
        }
        return new ProxyPluginInitResp(requestPlugins, replyPlugins);
    }

    //只初始化一次
    private ProxyPlugin getOrInitProxyPlugin(String classOrAlias) {
        ProxyPlugin plugin = pluginMap.get(classOrAlias);
        if (plugin == null) {
            synchronized (pluginMap) {
                plugin = pluginMap.get(classOrAlias);
                if (plugin == null) {
                    plugin = initProxyPlugin(classOrAlias);
                    plugin.init(beanFactory);
                    pluginMap.put(classOrAlias, plugin);
                }
            }
        }
        return plugin;
    }

    @Override
    public ProxyPlugin initProxyPlugin(String classOrAlias) {
        try {
            Class<?> clazz;
            BuildInProxyPluginEnum pluginEnum = BuildInProxyPluginEnum.getByAlias(classOrAlias);
            if (pluginEnum != null) {
                clazz = pluginEnum.getClazz();
            } else {
                clazz = BeanInitUtils.parseClass(classOrAlias);
            }
            return (ProxyPlugin) beanFactory.getBean(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("init ProxyPlugin[" + classOrAlias + "] error", e);
        }
    }

    @Override
    public synchronized void registerPluginUpdate(Runnable callback) {
        callbackSet.add(callback);
    }
}

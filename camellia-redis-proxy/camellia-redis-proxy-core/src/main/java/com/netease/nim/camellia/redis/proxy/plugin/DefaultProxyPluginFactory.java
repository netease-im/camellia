package com.netease.nim.camellia.redis.proxy.plugin;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
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

    /**
     * 插件配置的字符串,也就是配置中{@link DefaultProxyPluginFactory#CONF_KEY}的值
     */
    private String pluginConf;
    /**
     * 回调接口，这里主要是通过注册进来，配合线程池每隔多久进行一次reload刷新插件用的
     */
    private final List<Runnable> callbackSet = new ArrayList<>();

    /**
     * 插件map
     */
    private final ConcurrentHashMap<String, ProxyPlugin> pluginMap = new ConcurrentHashMap<>();

    public DefaultProxyPluginFactory() {
        reload();
        int seconds = ProxyDynamicConf.getInt("proxy.plugin.update.interval.seconds", 60);
        // 按照配置的频率去调用reload方法
        ExecutorUtils.scheduleAtFixedRate(this::reload, seconds, seconds, TimeUnit.SECONDS);
    }

    /**
     * 依次调用callbackSet里面的方法，{@link DefaultProxyPluginFactory#CONF_KEY} 配置字符串发生改变的时候才会进行调用
     */
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
        // 根据配置中的名称构建插件
        Set<String> pluginSet = new HashSet<>();
        if (pluginConf != null && !pluginConf.trim().isEmpty()) {
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
        // 插件排序，先按request排再按reply排，越大约优先
        plugins.sort((p1, p2) -> Integer.compare(p1.order().request(), p2.order().request()) * -1);
        List<ProxyPlugin> requestPlugins = new ArrayList<>(plugins);
        if (logger.isInfoEnabled()) {
            logger.info("###request-plugins-start");
            for (int i = 0; i < requestPlugins.size(); i++) {
                ProxyPlugin plugin = requestPlugins.get(i);
                logger.info("index = {}, order = {}, plugin = {}", i, plugin.order().request(), plugin.getClass().getName());
            }
            logger.info("###requst-plugins-end");
        }
        plugins.sort((p1, p2) -> Integer.compare(p1.order().reply(), p2.order().reply()) * -1);
        List<ProxyPlugin> replyPlugins = new ArrayList<>(plugins);
        if (logger.isInfoEnabled()) {
            logger.info("###reply-plugins-start");
            for (int i = 0; i < replyPlugins.size(); i++) {
                ProxyPlugin plugin = replyPlugins.get(i);
                logger.info("index = {}, order = {}, plugin = {}", i, plugin.order().reply(), plugin.getClass().getName());
            }
            logger.info("###reply-plugins-end");
        }
        return new ProxyPluginInitResp(requestPlugins, replyPlugins);
    }

    /**
     * 用的DCL，保证插件的单例
     *
     * @param classOrAlias 类的全限定名
     * @return 插件 {@link ProxyPlugin}对象
     */
    private ProxyPlugin getOrInitProxyPlugin(String classOrAlias) {
        ProxyPlugin plugin = pluginMap.get(classOrAlias);
        if (plugin == null) {
            synchronized (pluginMap) {
                plugin = pluginMap.get(classOrAlias);
                if (plugin == null) {
                    plugin = initProxyPlugin(classOrAlias);
                    plugin.init(ServerConf.getProxyBeanFactory());
                    pluginMap.put(classOrAlias, plugin);
                }
            }
        }
        return plugin;
    }

    /**
     * 根据类的全限定名或者别名获取插件。
     *
     * @param classOrAlias 类的全限定名或者内建的插件的别名,内建插件别名必须是{@link BuildInProxyPluginEnum} 不然会报错
     * @return 插件 {@link ProxyPlugin}对象
     */
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
            return (ProxyPlugin) ServerConf.getProxyBeanFactory().getBean(clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("init ProxyPlugin[" + classOrAlias + "] error", e);
        }
    }

    /**
     * 将callback方法注册进去
     *
     * @param callback callback
     */
    @Override
    public synchronized void registerPluginUpdate(Runnable callback) {
        callbackSet.add(callback);
    }
}

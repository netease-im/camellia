package com.netease.nim.camellia.redis.proxy.conf;

import com.netease.nim.camellia.redis.proxy.command.QueueFactory;
import com.netease.nim.camellia.redis.proxy.enums.ProxyMode;
import com.netease.nim.camellia.redis.proxy.plugin.DefaultBeanFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by caojiajun on 2026/2/9
 */
public class ServerConf {

    private static int port;
    private static String applicationName = "camellia-redis-proxy";
    private static ProxyBeanFactory proxyBeanFactory = new DefaultBeanFactory();
    private static QueueFactory queueFactory;
    private static String password;
    private static String cportPassword;

    public static void init(CamelliaServerProperties serverProperties, int port, String applicationName, ProxyBeanFactory proxyBeanFactory) {
        if (port > 0) {
            ServerConf.port = port;
        }
        if (applicationName != null) {
            ServerConf.applicationName = applicationName;
        }
        if (proxyBeanFactory != null) {
            ServerConf.proxyBeanFactory = proxyBeanFactory;
        }
        //
        ProxyDynamicConfLoader confLoader;
        String className = serverProperties.getProxyDynamicConfLoaderClassName();
        if (className != null) {
            confLoader = (ProxyDynamicConfLoader) ServerConf.proxyBeanFactory.getBean(BeanInitUtils.parseClass(className));
        } else {
            confLoader = new FileBasedProxyDynamicConfLoader();
        }
        ProxyDynamicConf.init(serverProperties.getConfig(), confLoader);
        //
        password = ProxyDynamicConf.getString("password", null);
        cportPassword = ProxyDynamicConf.getString("cport.password", null);
        queueFactory = ConfigInitUtil.initQueueFactory();
    }

    public static String getApplicationName() {
        return ProxyDynamicConf.getString("application.name", applicationName);
    }

    public static int port() {
        return ProxyDynamicConf.getInt("port", port);
    }

    public static int consolePort() {
        return ProxyDynamicConf.getInt("console.port", Constants.Server.consolePort);
    }

    public static int cport() {
        return ProxyDynamicConf.getInt("cport", -1);
    }

    public static int tlsPort() {
        return ProxyDynamicConf.getInt("tls.port", -1);
    }

    public static int httpPort() {
        return ProxyDynamicConf.getInt("http.port", -1);
    }

    public static String udsPath() {
        return ProxyDynamicConf.getString("uds.path", null);
    }

    public static String password() {
        return password;
    }

    public static String cportPassword() {
        return cportPassword;
    }

    public static ProxyMode proxyMode() {
        return ProxyMode.valueOf(ProxyDynamicConf.getString("proxy.mode", ProxyMode.standalone.name()));
    }

    public static boolean isProxyProtocolEnable() {
        return ProxyDynamicConf.getBoolean("proxy.protocol.enable", false);
    }

    public static Set<Integer> proxyProtocolPorts(int port, int tlsPort) {
        Set<Integer> set = new HashSet<>();
        String ports = ProxyDynamicConf.getString("proxy.protocol.ports", null);
        if (ports == null || ports.trim().isEmpty()) {
            if (port > 0) {
                set.add(port);
            }
            if (tlsPort > 0) {
                set.add(tlsPort);
            }
        } else {
            String[] split = ports.trim().split(",");
            for (String str : split) {
                if (port > 0 && str.trim().equalsIgnoreCase(String.valueOf(port))) {
                    set.add(port);
                }
                if (tlsPort > 0 && str.trim().equalsIgnoreCase(String.valueOf(tlsPort))) {
                    set.add(tlsPort);
                }
            }
        }
        return set;
    }

    public static ProxyBeanFactory getProxyBeanFactory() {
        return proxyBeanFactory;
    }

    public static QueueFactory getQueueFactory() {
        return queueFactory;
    }
}

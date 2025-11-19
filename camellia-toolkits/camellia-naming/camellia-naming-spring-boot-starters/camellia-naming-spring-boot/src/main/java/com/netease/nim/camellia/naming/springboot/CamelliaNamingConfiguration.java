package com.netease.nim.camellia.naming.springboot;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;
import com.netease.nim.camellia.naming.nacos.CamelliaNacosNamingService;
import com.netease.nim.camellia.naming.zk.CamelliaZkNamingService;
import com.netease.nim.camellia.tools.utils.InetUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


/**
 *
 * Created by caojiajun on 2020/3/31.
 */
@Configuration
@EnableConfigurationProperties({CamelliaNamingProperties.class})
public class CamelliaNamingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNamingConfiguration.class);

    @Value("${server.port:-1}")
    private int port;

    @Value("${spring.application.name:}")
    private String applicationName;

    private CamelliaNamingProperties properties;
    private ICamelliaNamingService service;

    @Bean
    public ICamelliaNamingService camelliaNamingService(CamelliaNamingProperties properties) {
        try {
            String serviceName = properties.getServiceName();
            if (serviceName == null || serviceName.trim().isEmpty()) {
                serviceName = System.getProperty("camellia.naming.service.name");
                if (serviceName == null || serviceName.trim().isEmpty()) {
                    properties.setServiceName(this.applicationName);
                } else {
                    properties.setServiceName(serviceName);
                }
            }
            if (properties.getPort() <= 0) {
                String portStr = System.getProperty("camellia.naming.port");
                if (portStr != null) {
                    int port = Integer.parseInt(portStr);
                    if (port > 0) {
                        properties.setPort(port);
                    } else {
                        properties.setPort(this.port);
                    }
                } else {
                    properties.setPort(this.port);
                }
            }
            if (properties.getHost() == null) {
                Map<String, String> config = properties.getConfig();
                String ignoredInterfaces = config.get("register.ignored.interfaces");
                String preferredNetworks = config.get("register.preferred.interfaces");
                InetAddress address = InetUtils.findFirstNonLoopbackAddress(ignoredInterfaces, preferredNetworks);
                properties.setHost(address.getHostAddress());
            }
            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setHost(properties.getHost());
            instanceInfo.setPort(properties.getPort());
            ICamelliaNamingService service;
            CamelliaNamingProperties.Type type = properties.getType();
            if (type == CamelliaNamingProperties.Type.ZK) {
                String zkUrl = properties.getConfig().get("zk.url");
                if (zkUrl == null) {
                    throw new CamelliaNamingException("'zk.url' is empty");
                }
                String zkBasePath = properties.getConfig().get("zk.base.path");
                if (zkBasePath == null) {
                    throw new CamelliaNamingException("'zk.base.path' is empty");
                }
                int sessionTimeoutMs = getInt(properties.getConfig(), "zk.session.timeout.ms", 60 * 1000);
                int connectTimeoutMs = getInt(properties.getConfig(), "zk.connect.timeout.ms", 15 * 1000);
                int baseSleepTimeoutMs = getInt(properties.getConfig(), "zk.base.sleep.timeout.ms", 1000);
                int maxRetry = getInt(properties.getConfig(), "zk.max.retry", 3);
                CuratorFramework client = CuratorFrameworkFactory.builder()
                        .connectString(zkUrl)
                        .sessionTimeoutMs(sessionTimeoutMs)
                        .connectionTimeoutMs(connectTimeoutMs)
                        .retryPolicy(new ExponentialBackoffRetry(baseSleepTimeoutMs, maxRetry))
                        .build();
                client.start();
                service = new CamelliaZkNamingService(client, zkBasePath, properties.getServiceName(), instanceInfo);
            } else if (type == CamelliaNamingProperties.Type.NACOS) {
                Properties nacosProps = new Properties();
                String prefix = "nacos.";
                for (Map.Entry<String, String> entry : properties.getConfig().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key.startsWith(prefix)) {
                        key = key.substring(prefix.length());
                        nacosProps.put(key, value);
                    }
                }
                NamingService namingService = NacosFactory.createNamingService(nacosProps);
                service = new CamelliaNacosNamingService(namingService, properties.getServiceName(), instanceInfo);
            } else {
                throw new CamelliaNamingException("illegal type");
            }
            logger.info("camellia naming service init success, type = {}", type);
            this.properties = properties;
            this.service = service;
            return service;
        } catch (CamelliaNamingException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaNamingException(e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register(ApplicationReadyEvent readyEvent) {
        if (properties == null || service == null) {
            return;
        }
        if (properties.isRegisterEnable()) {
            String enable = properties.getConfig().get("register.pre.check.enable");
            if (Boolean.parseBoolean(enable)) {
                String type = properties.getConfig().get("register.pre.check.type");
                int timeout = getInt(properties.getConfig(), "register.pre.check.timeout.ms", 1000);
                int maxRetry = getInt(properties.getConfig(), "register.pre.check.max.retry", 3);
                String host = properties.getConfig().get("register.pre.check.host");
                if (host == null) {
                    host = properties.getHost();
                }
                int port = getInt(properties.getConfig(), "register.pre.check.port", properties.getPort());
                boolean pass = false;
                if ("tcp".equalsIgnoreCase(type)) {
                    for (int i=0; i<maxRetry; i++) {
                        pass = checkTcp(host, port, timeout);
                        if (pass) {
                            break;
                        }
                        sleep((i + 1) * 1000L);
                    }
                } else if ("http".equalsIgnoreCase(type)) {
                    String uri = properties.getConfig().get("register.pre.check.http.uri");
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .readTimeout(timeout, TimeUnit.MILLISECONDS)
                            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                    for (int i=0; i<maxRetry; i++) {
                        pass = checkHttp(okHttpClient, host, port, timeout, uri);
                        if (pass) {
                            break;
                        }
                        sleep((i + 1) * 1000L);
                    }
                } else {
                    pass = true;
                }
                if (!pass) {
                    throw new CamelliaNamingException("register pre check failed");
                }
            }
            service.register();
        }
    }

    @EventListener({ContextStoppedEvent.class, ContextClosedEvent.class})
    public void close(ApplicationEvent event) {
        if (properties == null || service == null) {
            return;
        }
        if (properties.isRegisterEnable()) {
            service.deregister();
        }
    }

    private boolean checkTcp(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress(host, port), timeout);
                logger.info("register pre check, check tcp success, host = {}, port = {}, timeout = {}", host, port, timeout);
                return true;
            } catch (Exception e) {
                logger.warn("register pre check, check tcp fail on connect, host = {}, port = {}, timeout = {}, e = {}", host, port, timeout, e.toString());
                return false;
            }
        } catch (Exception e) {
            logger.warn("register pre check, check tcp fail, host = {}, port = {}, timeout = {}, e = {}", host, port, timeout, e.toString());
            return false;
        }
    }

    private boolean checkHttp(OkHttpClient okHttpClient, String host, int port, int timeout, String uri) {
        String url = "http://" + host + ":" + port;
        if (uri != null) {
            url = url + uri;
        }
        Request.Builder builder = new Request.Builder().get().url(url);
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            boolean pass = response.code() == 200;
            if (pass) {
                logger.info("register pre check, check http success, host = {}, port = {}, timeout = {}, uri = {}", host, port, timeout, uri);
            } else {
                logger.warn("register pre check, check http fail, host = {}, port = {}, timeout = {}, uri = {}, code = {}", host, port, timeout, uri, response.code());
            }
            return pass;
        } catch (Exception e) {
            logger.warn("register pre check, check http fail, host = {}, port = {}, timeout = {}, uri = {}, e = {}", host, port, timeout, uri, e.toString());
            return false;
        }
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private int getInt(Map<String, String> config, String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

}

package com.netease.nim.camellia.naming.springboot;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;
import com.netease.nim.camellia.naming.nacos.CamelliaNacosNamingService;
import com.netease.nim.camellia.naming.zk.CamelliaZkNamingService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.Properties;


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

    @Bean
    public ICamelliaNamingService camelliaNamingService(CamelliaNamingProperties properties) {
        try {
            String serviceName = properties.getServiceName();
            if (serviceName == null || serviceName.trim().isEmpty()) {
                properties.setServiceName(applicationName);
            }
            if (properties.getPort() <= 0) {
                properties.setPort(port);
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
            return service;
        } catch (CamelliaNamingException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaNamingException(e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register(CamelliaNamingProperties properties) {
        if (properties.isRegisterEnable()) {
            ICamelliaNamingService service = camelliaNamingService(properties);
            service.register();
        }
    }

    @EventListener({ContextStoppedEvent.class, ContextClosedEvent.class})
    public void close(CamelliaNamingProperties properties) {
        if (properties.isRegisterEnable()) {
            ICamelliaNamingService service = camelliaNamingService(properties);
            service.deregister();
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

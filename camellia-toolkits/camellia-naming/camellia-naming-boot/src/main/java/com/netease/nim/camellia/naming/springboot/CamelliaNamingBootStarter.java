package com.netease.nim.camellia.naming.springboot;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;
import com.netease.nim.camellia.naming.nacos.CamelliaNacosNamingService;
import com.netease.nim.camellia.naming.zk.CamelliaZkNamingService;
import com.netease.nim.camellia.tools.utils.InetUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;


/**
 *
 * Created by caojiajun on 2020/3/31.
 */
public class CamelliaNamingBootStarter {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNamingBootStarter.class);

    public static CamelliaNamingBoot init(CamelliaNamingBootConf bootConf) {
        return init(bootConf, null, -1);
    }

    public static CamelliaNamingBoot init(CamelliaNamingBootConf bootConf, String defaultServiceName, int defaultPort) {
        try {
            String serviceName = bootConf.getServiceName();
            if (serviceName == null || serviceName.trim().isEmpty()) {
                serviceName = System.getProperty("camellia.naming.service.name");
                if (serviceName == null || serviceName.trim().isEmpty()) {
                    bootConf.setServiceName(defaultServiceName);
                } else {
                    bootConf.setServiceName(serviceName);
                }
            }
            if (bootConf.getPort() <= 0) {
                String portStr = System.getProperty("camellia.naming.port");
                if (portStr != null) {
                    int port = Integer.parseInt(portStr);
                    if (port > 0) {
                        bootConf.setPort(port);
                    } else {
                        bootConf.setPort(defaultPort);
                    }
                } else {
                    bootConf.setPort(defaultPort);
                }
            }
            if (bootConf.getHost() == null) {
                Map<String, String> config = bootConf.getConfig();
                String ignoredInterfaces = config.get("register.ignored.interfaces");
                String preferredNetworks = config.get("register.preferred.interfaces");
                InetAddress address = InetUtils.findFirstNonLoopbackAddress(ignoredInterfaces, preferredNetworks);
                bootConf.setHost(address.getHostAddress());
            }
            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setHost(bootConf.getHost());
            instanceInfo.setPort(bootConf.getPort());
            ICamelliaNamingService service;
            CamelliaNamingBootConf.Type type = bootConf.getType();
            if (type == CamelliaNamingBootConf.Type.ZK) {
                String zkUrl = bootConf.getConfig().get("zk.url");
                if (zkUrl == null) {
                    throw new CamelliaNamingException("'zk.url' is empty");
                }
                String zkBasePath = bootConf.getConfig().get("zk.base.path");
                if (zkBasePath == null) {
                    throw new CamelliaNamingException("'zk.base.path' is empty");
                }
                int sessionTimeoutMs = getInt(bootConf.getConfig(), "zk.session.timeout.ms", 60 * 1000);
                int connectTimeoutMs = getInt(bootConf.getConfig(), "zk.connect.timeout.ms", 15 * 1000);
                int baseSleepTimeoutMs = getInt(bootConf.getConfig(), "zk.base.sleep.timeout.ms", 1000);
                int maxRetry = getInt(bootConf.getConfig(), "zk.max.retry", 3);
                CuratorFramework client = CuratorFrameworkFactory.builder()
                        .connectString(zkUrl)
                        .sessionTimeoutMs(sessionTimeoutMs)
                        .connectionTimeoutMs(connectTimeoutMs)
                        .retryPolicy(new ExponentialBackoffRetry(baseSleepTimeoutMs, maxRetry))
                        .build();
                client.start();
                service = new CamelliaZkNamingService(client, zkBasePath, bootConf.getServiceName(), instanceInfo);
            } else if (type == CamelliaNamingBootConf.Type.NACOS) {
                Properties nacosProps = new Properties();
                String prefix = "nacos.";
                for (Map.Entry<String, String> entry : bootConf.getConfig().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key.startsWith(prefix)) {
                        key = key.substring(prefix.length());
                        nacosProps.put(key, value);
                    }
                }
                String loggingPath = System.getProperty("JM.LOG.PATH");
                if (loggingPath == null) {
                    loggingPath = bootConf.getConfig().get("nacos.JM.LOG.PATH");
                    if (loggingPath != null) {
                        System.setProperty("JM.LOG.PATH", loggingPath);
                        logger.info("setting `JM.LOG.PATH={}` by config", loggingPath);
                    }
                } else {
                    logger.info("setting `JM.LOG.PATH={}` by System.getProperty", loggingPath);
                }
                logger.info("nacos props = {}", nacosProps);
                NamingService namingService = NacosFactory.createNamingService(nacosProps);
                service = new CamelliaNacosNamingService(namingService, bootConf.getServiceName(), instanceInfo);
            } else {
                throw new CamelliaNamingException("illegal type");
            }
            logger.info("camellia naming boot init success, type = {}", type);
            return new CamelliaNamingBoot(bootConf, service);
        } catch (CamelliaNamingException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaNamingException(e);
        }
    }

    private static int getInt(Map<String, String> config, String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }
}

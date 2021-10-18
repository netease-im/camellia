package com.netease.nim.camellia.redis.proxy.nacos.springboot;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.springboot.CamelliaRedisProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 *
 * Created by caojiajun on 2021/10/18
 */
@Configuration
@AutoConfigureBefore(CamelliaRedisProxyConfiguration.class)
@EnableConfigurationProperties({CamelliaRedisProxyNacosProperties.class})
public class CamelliaRedisProxyNacosConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisProxyNacosConfiguration.class);

    @Bean
    public CamelliaRedisProxyNacosService nacosConfigService(CamelliaRedisProxyNacosProperties properties) {
        try {
            CamelliaRedisProxyNacosService service = new CamelliaRedisProxyNacosService();
            if (properties.isEnable()) {
                Properties nacosProps = new Properties();
                if (properties.getServerAddr() != null) {
                    nacosProps.put("serverAddr", properties.getServerAddr());
                }
                Map<String, String> nacosConf = properties.getNacosConf();
                if (nacosConf != null) {
                    for (Map.Entry<String, String> entry : nacosConf.entrySet()) {
                        nacosProps.put(entry.getKey(), entry.getValue());
                    }
                }
                ConfigService configService = NacosFactory.createConfigService(nacosProps);
                service.setConfigService(configService);
                for (CamelliaRedisProxyNacosProperties.ConfFile confFile : properties.getConfFileList()) {
                    String content = configService.getConfig(confFile.getDataId(), confFile.getGroup(), properties.getTimeoutMs());
                    updateConf(content, confFile.getFileName());
                    configService.addListener(confFile.getDataId(), confFile.getGroup(), new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return null;
                        }
                        @Override
                        public void receiveConfigInfo(String content) {
                            updateConf(content, confFile.getFileName());
                        }
                    });
                }
            }
            return service;
        } catch (NacosException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void updateConf(String content, String fileName) {
        try {
            URL url = ProxyDynamicConf.class.getClassLoader().getResource(fileName);
            File file;
            if (url == null) {
                URL resource = ProxyDynamicConf.class.getClassLoader().getResource("");
                if (resource == null) {
                    throw new IllegalArgumentException();
                }
                file = new File(resource.getPath() + "/" + fileName);
            } else {
                file = new File(url.getPath());
            }
            try (BufferedWriter out = new BufferedWriter(new FileWriter(file, false))) {
                out.write(content);
                logger.info("{} update from nacos, content:\r\n{}", fileName, content);
                ProxyDynamicConf.reload();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}

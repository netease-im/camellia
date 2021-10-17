package com.netease.nim.camellia.redis.proxy.samples;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * Created by caojiajun on 2021/10/17
 */
public class NacosConfStarter {

    private static final Logger logger = LoggerFactory.getLogger(NacosConfStarter.class);

    private static final String fileName = "camellia-redis-proxy.properties";

    public static void start(String serverAddr) {
        try {
            ConfigService configService = NacosFactory.createConfigService(serverAddr);
            String dataId = "camellia-redis-proxy.properties";
            String group = "camellia";
            String config = configService.getConfig(dataId, group, 10000);
            updateConf(config);
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(),
                            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024), new CamelliaThreadFactory("nacos", true));
                }

                @Override
                public void receiveConfigInfo(String content) {
                    updateConf(content);
                }
            });
        } catch (NacosException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void updateConf(String content) {
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

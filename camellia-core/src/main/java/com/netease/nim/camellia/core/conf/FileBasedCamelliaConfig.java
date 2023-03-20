package com.netease.nim.camellia.core.conf;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/11/16
 */
public class FileBasedCamelliaConfig extends CamelliaConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedCamelliaConfig.class);

    private static final ScheduledExecutorService scheduledExecutor = Executors
            .newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-config"));

    private static final AtomicBoolean init = new AtomicBoolean();

    private final String fileName;

    public FileBasedCamelliaConfig(String fileName) {
        this.fileName = fileName;
        init();
    }

    private void init() {
        if (init.compareAndSet(false, true)) {
            reload();
            scheduledExecutor.scheduleAtFixedRate(this::reload, 60, 60, TimeUnit.SECONDS);
        }
    }

    private void reload() {
        URL url = FileBasedCamelliaConfig.class.getClassLoader().getResource(fileName);
        if (url == null) return;
        try {
            Properties props = new Properties();
            try {
                props.load(new FileInputStream(url.getPath()));
            } catch (IOException e) {
                props.load(FileBasedCamelliaConfig.class.getClassLoader().getResourceAsStream(fileName));
            }
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            if (map.equals(this.getConf())) {
                if (logger.isDebugEnabled()) {
                    logger.debug("camellia config skip reload for conf not modify, fileName = {}", fileName);
                }
            } else {
                this.setConf(map);
                logger.info("camellia config reload success, fileName = {}", fileName);
            }
        } catch (Exception e) {
            logger.error("camellia config reload error, fileName = {}", fileName);
        }
    }

}

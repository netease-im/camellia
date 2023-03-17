package com.netease.nim.camellia.core.conf;

import com.netease.nim.camellia.core.api.CamelliaApiCode;
import com.netease.nim.camellia.core.api.CamelliaConfigApi;
import com.netease.nim.camellia.core.api.CamelliaConfigApiUtil;
import com.netease.nim.camellia.core.api.CamelliaConfigResponse;
import com.netease.nim.camellia.tools.utils.SysUtils;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2023/3/17
 */
public class DynamicCamelliaConfig extends AbstractCamelliaConfig {

    private static final Logger logger = LoggerFactory.getLogger(DynamicCamelliaConfig.class);

    private static final ScheduledExecutorService defaultScheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new DefaultThreadFactory("camellia-config-scheduler"));

    private final String url;
    private final String namespace;
    private String md5;
    private final CamelliaConfigApi api;

    private final AtomicBoolean lock = new AtomicBoolean(false);

    public DynamicCamelliaConfig(String url, String namespace) {
        this(url, namespace, 5, defaultScheduler);
    }

    public DynamicCamelliaConfig(String url, String namespace, int intervalSeconds) {
        this(url, namespace, intervalSeconds, defaultScheduler);
    }

    public DynamicCamelliaConfig(String url, String namespace, int intervalSeconds, ScheduledExecutorService scheduler) {
        this.url = url;
        this.namespace = namespace;
        this.api = CamelliaConfigApiUtil.init(url);
        boolean success = reload();
        if (!success) {
            //说明没有初始化成功
            throw new IllegalStateException("camellia config init error, url = " + url + ", namespace = " + namespace);
        }
        scheduler.scheduleAtFixedRate(this::reload, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private boolean reload() {
        if (lock.compareAndSet(false, true)) {
            try {
                CamelliaConfigResponse response = api.getConfig(namespace, md5);
                int code = response.getCode();
                if (code == CamelliaApiCode.NOT_MODIFY.getCode()) {
                    return true;
                }
                if (code == CamelliaApiCode.SUCCESS.getCode()) {
                    Map<String, String> conf = response.getConf();
                    String md5 = response.getMd5();
                    setConf(conf);
                    logger.info("camellia config update, url = {}, namespace = {}, md5 = {}, conf = {}", url, namespace, md5, conf);
                    this.md5 = md5;
                    return true;
                } else {
                    logger.error("camellia config reload wrong, url = {}, code = {}", url, response.getCode());
                    return false;
                }
            } catch (Exception e) {
                logger.error("camellia config reload error, url = {}, namespace = {}", namespace, url, e);
                return false;
            } finally {
                lock.compareAndSet(true, false);
            }
        } else {
            return false;
        }
    }

}

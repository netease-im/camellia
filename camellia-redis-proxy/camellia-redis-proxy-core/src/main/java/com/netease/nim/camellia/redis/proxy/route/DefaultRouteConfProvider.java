package com.netease.nim.camellia.redis.proxy.route;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.redis.proxy.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.FileUtils;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2026/2/11
 */
public class DefaultRouteConfProvider extends RouteConfProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRouteConfProvider.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("camellia-default-route"));

    private final String password;

    private String routeConf;

    public DefaultRouteConfProvider() {
        password = ServerConf.password();
        reload();
        int checkIntervalMillis = ProxyDynamicConf.getInt("route.conf.reload.interval.millis", 3000);
        scheduler.scheduleAtFixedRate(this::reload, checkIntervalMillis, checkIntervalMillis, TimeUnit.MILLISECONDS);
        ProxyDynamicConf.registerCallback(this::reload);
    }

    private void reload() {
        try {
            String routeConf = ProxyDynamicConf.getString("route.conf", null);
            if (routeConf == null) {
                String file = ProxyDynamicConf.getString("route.conf.file", null);
                FileUtils.FileInfo fileInfo = FileUtils.readDynamic(file);
                if (fileInfo == null) {
                    throw new IllegalArgumentException("missing 'route.conf' and 'route.conf.file'");
                }
                routeConf = fileInfo.getFileContent();
            }
            if (Objects.equals(routeConf, this.routeConf)) {
                return;
            }
            ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(routeConf);
            checkResourceTable(resourceTable);
            this.routeConf = ReadableResourceTableUtil.readableResourceTable(resourceTable);
            logger.info("route conf updated, conf = {}", routeConf);
            invokeUpdateResourceTable(this.routeConf);
        } catch (Exception e) {
            logger.error("reload route conf error", e);
        }
    }

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = new ClientIdentity();
        if (userName != null && !userName.equals("default")) {
            clientIdentity.setPass(false);
            return clientIdentity;
        }
        clientIdentity.setPass(!StringUtil.isNullOrEmpty(this.password) && this.password.equals(password));
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return password != null;
    }

    @Override
    public String getRouteConfig() {
        return routeConf;
    }

    @Override
    public String getRouteConfig(long bid, String bgroup) {
        return null;
    }

    @Override
    public boolean isMultiTenantsSupport() {
        return false;
    }
}

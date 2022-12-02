package com.netease.nim.camellia.redis.proxy.console;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.cluster.ClusterModeStatus;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.info.UpstreamInfoUtils;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.*;
import com.netease.nim.camellia.redis.proxy.monitor.model.Stats;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.netty.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class ConsoleServiceAdaptor implements ConsoleService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleServiceAdaptor.class);

    private int serverPort;
    public ConsoleServiceAdaptor(int serverPort) {
        this.serverPort = serverPort;
    }

    public ConsoleServiceAdaptor() {
    }

    @Override
    public ConsoleResult status() {
        ServerStatus.Status status = ServerStatus.getStatus();
        if (logger.isDebugEnabled()) {
            logger.debug("status = {}", status.name());
        }
        if (status == ServerStatus.Status.ONLINE) {
            return ConsoleResult.success(status.name());
        } else {
            return ConsoleResult.error(status.name());
        }
    }

    @Override
    public ConsoleResult online() {
        logger.info("online success");
        ServerStatus.setStatus(ServerStatus.Status.ONLINE);
        if (ClusterModeStatus.getStatus() == ClusterModeStatus.Status.OFFLINE) {
            ClusterModeStatus.setStatus(ClusterModeStatus.Status.ONLINE);
        }
        return ConsoleResult.success();
    }

    @Override
    public ConsoleResult offline() {
        ServerStatus.setStatus(ServerStatus.Status.OFFLINE);
        ClusterModeStatus.setStatus(ClusterModeStatus.Status.OFFLINE);
        if (ServerStatus.isIdle()) {
            logger.info("offline success");
            return ConsoleResult.success("is idle");
        } else {
            logger.info("try offline, but not idle");
            return ConsoleResult.error("not idle");
        }
    }

    @Override
    public ConsoleResult check() {
        int port = serverPort;
        if (port <= 0) {
            port = GlobalRedisProxyEnv.port;
        }
        if (port <= 0) {
            return ConsoleResult.success();
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
            if (logger.isDebugEnabled()) {
                logger.debug("check serverPort = " + port + " success");
            }
            return ConsoleResult.success("check serverPort = " + port + " success");
        } catch (IOException e) {
            logger.error("check serverPort = " + port + " fail");
            return ConsoleResult.error("check serverPort = " + port + " error");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("close error", e);
            }
        }
    }

    @Override
    public ConsoleResult monitor() {
        Stats stats = ProxyMonitorCollector.getStats();
        return ConsoleResult.success(StatsJsonConverter.converter(stats));
    }

    @Override
    public ConsoleResult reload() {
        ProxyDynamicConf.reload();
        logger.info("proxy dynamic conf reload success");
        return ConsoleResult.success();
    }

    @Override
    public ConsoleResult info(Map<String, List<String>> params) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                map.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        String string = ProxyInfoUtils.generateProxyInfo(map);
        return ConsoleResult.success(string);
    }

    @Override
    public ConsoleResult custom(Map<String, List<String>> params) {
        if (logger.isDebugEnabled()) {
            logger.debug("custom, params = {}", params);
        }
        return ConsoleResult.success();
    }

    @Override
    public ConsoleResult detect(Map<String, List<String>> params) {
        if (logger.isDebugEnabled()) {
            logger.debug("detect, params = {}", params);
        }
        String url = null;
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("url")) {
                url = entry.getValue().get(0);
            }
        }
        if (url != null) {
            try {
                JSONObject jsonObject = UpstreamInfoUtils.monitorJson(url);
                if (jsonObject != null) {
                    return ConsoleResult.success(jsonObject.toJSONString());
                } else {
                    return ConsoleResult.error("param wrong");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return ConsoleResult.error("internal error");
            }
        }
        return ConsoleResult.error("param wrong");
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}

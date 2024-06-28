package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.alibaba.fastjson.JSONArray;
import com.netease.nim.camellia.redis.proxy.command.ProxyCurrentNodeInfo;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.Channel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/10/19
 */
public class DefaultUpstreamAddrConverter implements UpstreamAddrConverter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultUpstreamAddrConverter.class);

    private static final String CURRENT_HOST = "@CurrentHost@";

    private List<Config> configList = new ArrayList<>();
    private String currentHost;

    public DefaultUpstreamAddrConverter() {
        reloadConfig();
        ProxyDynamicConf.registerCallback(this::reloadConfig);
    }

    @Override
    public UpstreamAddrConverterResult convert(UpstreamAddrConverterContext context) {
        try {
            boolean enable = ProxyDynamicConf.getBoolean("upstream.addr.converter.enable", false);
            if (!enable) {
                return null;
            }
            if (configList == null || configList.isEmpty()) {
                return null;
            }
            String host = context.getHost();
            String udsPath = context.getUdsPath();
            for (Config config : configList) {
                if (host != null && config.getOriginalHost() != null) {
                    if (config.getOriginalPort() <= 0) {
                        if (config.getOriginalHost().equals(CURRENT_HOST)) {
                            if (host.equals(currentHost)) {
                                return result(context, config);
                            }
                        } else {
                            if (host.equals(config.getOriginalHost())) {
                                return result(context, config);
                            }
                        }
                    } else {
                        if (config.getOriginalHost().equals(CURRENT_HOST) && config.getOriginalPort() == context.getPort()) {
                            if (host.equals(currentHost)) {
                                return result(context, config);
                            }
                        } else {
                            if (host.equals(config.getOriginalHost()) && config.getOriginalPort() == context.getPort()) {
                                return result(context, config);
                            }
                        }
                    }
                }
                if (udsPath != null && config.getOriginalUdsPath() != null && udsPath.equals(config.getOriginalUdsPath())) {
                    return result(context, config);
                }
            }
            return null;
        } catch (Exception e) {
            ErrorLogCollector.collect(DefaultUpstreamAddrConverter.class, "DefaultUpstreamAddrConverter convert error", e);
            return null;
        }
    }

    private UpstreamAddrConverterResult result(UpstreamAddrConverterContext context, Config config) {
        if (config.getTargetHost() != null) {
            if (SocketChannel.class.isAssignableFrom(context.getChannelClass())) {
                return new UpstreamAddrConverterResult(config.getTargetHost(), config.getTargetPort(), null, context.getChannelClass(), ChannelType.tcp);
            }
            Class<? extends Channel> socketChannel = null;
            if (context.getEventLoop().parent() instanceof EpollEventLoopGroup) {
                socketChannel = EpollSocketChannel.class;
            } else if (context.getEventLoop().parent() instanceof KQueueEventLoopGroup) {
                socketChannel = KQueueSocketChannel.class;
            } else if (context.getEventLoop().parent() instanceof IOUringEventLoopGroup) {
                socketChannel = IOUringSocketChannel.class;
            } else if (context.getEventLoop().parent() instanceof NioEventLoopGroup) {
                socketChannel = NioSocketChannel.class;
            }
            if (socketChannel != null) {
                return new UpstreamAddrConverterResult(config.getTargetHost(), config.getTargetPort(), null, socketChannel, ChannelType.tcp);
            }
        }
        if (config.getTargetUdsPath() != null) {
            if (context.getEventLoop().parent() instanceof EpollEventLoopGroup) {
                return new UpstreamAddrConverterResult(null, -1, config.getTargetUdsPath(), EpollDomainSocketChannel.class, ChannelType.uds);
            } else if (context.getEventLoop().parent() instanceof KQueueEventLoopGroup) {
                return new UpstreamAddrConverterResult(null, -1, config.getTargetUdsPath(), KQueueDomainSocketChannel.class, ChannelType.uds);
            }
        }
        return null;
    }

    private void reloadConfig() {
        try {
            String string = ProxyDynamicConf.getString("upstream.addr.converter.config", null);
            if (string != null) {
                configList = JSONArray.parseArray(string, Config.class);
            } else {
                configList = new ArrayList<>();
            }
            this.currentHost = ProxyDynamicConf.getString("current.proxy.host", null);
            if (this.currentHost == null) {
                this.currentHost = ProxyCurrentNodeInfo.current().getHost();
            }
        } catch (Exception e) {
            logger.error("reload upstream addr converter config error", e);
        }
    }

    private static class Config {
        private String originalHost;
        private int originalPort;
        private String originalUdsPath;
        private String targetHost;
        private int targetPort;
        private String targetUdsPath;

        public String getOriginalHost() {
            return originalHost;
        }

        public void setOriginalHost(String originalHost) {
            this.originalHost = originalHost;
        }

        public int getOriginalPort() {
            return originalPort;
        }

        public void setOriginalPort(int originalPort) {
            this.originalPort = originalPort;
        }

        public String getOriginalUdsPath() {
            return originalUdsPath;
        }

        public void setOriginalUdsPath(String originalUdsPath) {
            this.originalUdsPath = originalUdsPath;
        }

        public String getTargetHost() {
            return targetHost;
        }

        public void setTargetHost(String targetHost) {
            this.targetHost = targetHost;
        }

        public int getTargetPort() {
            return targetPort;
        }

        public void setTargetPort(int targetPort) {
            this.targetPort = targetPort;
        }

        public String getTargetUdsPath() {
            return targetUdsPath;
        }

        public void setTargetUdsPath(String targetUdsPath) {
            this.targetUdsPath = targetUdsPath;
        }
    }
}

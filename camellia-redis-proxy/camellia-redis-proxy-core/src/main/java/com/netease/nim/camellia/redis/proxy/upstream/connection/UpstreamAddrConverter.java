package com.netease.nim.camellia.redis.proxy.upstream.connection;

import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;

/**
 * Created by caojiajun on 2023/10/19
 */
public interface UpstreamAddrConverter {

    default UpstreamAddrConverterResult convert(UpstreamAddrConverterContext context) {
        return null;
    }

    public static class UpstreamAddrConverterResult {
        private final String host;
        private final String udsPath;
        private final Class<? extends Channel> channelClass;
        private final ChannelType channelType;

        public UpstreamAddrConverterResult(String host, String udsPath, Class<? extends Channel> socketChannel, ChannelType channelType) {
            this.host = host;
            this.udsPath = udsPath;
            this.channelClass = socketChannel;
            this.channelType = channelType;
        }

        public String getHost() {
            return host;
        }

        public String getUdsPath() {
            return udsPath;
        }

        public Class<? extends Channel> getChannelClass() {
            return channelClass;
        }

        public ChannelType getChannelType() {
            return channelType;
        }
    }

    public static class UpstreamAddrConverterContext {
        private final String host;
        private final String udsPath;
        private final EventLoop eventLoop;
        private final Class<? extends Channel> channelClass;

        public UpstreamAddrConverterContext(String host, String udsPath, EventLoop eventLoop, Class<? extends Channel> channelClass) {
            this.host = host;
            this.udsPath = udsPath;
            this.eventLoop = eventLoop;
            this.channelClass = channelClass;
        }

        public String getHost() {
            return host;
        }

        public String getUdsPath() {
            return udsPath;
        }

        public EventLoop getEventLoop() {
            return eventLoop;
        }

        public Class<? extends Channel> getChannelClass() {
            return channelClass;
        }
    }
}

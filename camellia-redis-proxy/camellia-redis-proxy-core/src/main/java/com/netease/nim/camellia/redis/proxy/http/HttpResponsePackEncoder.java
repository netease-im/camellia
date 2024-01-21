package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.monitor.CommandFailMonitor;
import com.netease.nim.camellia.redis.proxy.monitor.ProxyMonitorCollector;
import com.netease.nim.camellia.redis.proxy.netty.ReplyEncoder;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/1/21
 */
public class HttpResponsePackEncoder extends MessageToMessageEncoder<HttpResponsePack> {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponsePackEncoder.class);

    private long id = 0;
    private final Map<Long, HttpResponsePack> packMap = new HashMap<>();

    public HttpResponsePackEncoder() {
        super();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpResponsePack msg, List<Object> out) throws Exception {
        if (ctx.channel().isActive()) {
            //avoid out of order
            long id = msg.getId();
            if (id == Long.MAX_VALUE) {
                //if 10w qps in one connect, after 2924712 year, connect will be force closed
                logger.error("http response pack id exceed to Long.MAX_VALUE, connect close");
                ctx.close();
                return;
            }
            if (this.id == id - 1) {
                this.id = id;
                out.add(msg.getHttpResponse());
                while (!packMap.isEmpty()) {
                    HttpResponsePack pack = packMap.remove(this.id + 1);
                    if (pack != null) {
                        this.id = pack.getId();
                        out.add(pack.getHttpResponse());
                    } else {
                        break;
                    }
                }
            } else {
                packMap.put(id, msg);
            }
        } else {
            if (ProxyMonitorCollector.isMonitorEnable()) {
                CommandFailMonitor.incr("HttpChannelNotActive");
            }
            ErrorLogCollector.collect(ReplyEncoder.class, "http channel not active, remote.ip=" + ctx.channel().remoteAddress());
        }
    }
}

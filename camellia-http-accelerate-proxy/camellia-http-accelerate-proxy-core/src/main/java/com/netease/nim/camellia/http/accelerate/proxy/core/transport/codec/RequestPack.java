package com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.StrStrMap;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LogBean;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import com.netease.nim.camellia.tools.compress.CamelliaCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by caojiajun on 2023/7/7
 */
public class RequestPack extends ProxyPackBody {

    private static final Logger logger = LoggerFactory.getLogger(RequestPack.class);
    private static final CamelliaCompressor compressor = new CamelliaCompressor(DynamicConf.getInt("request.content.compress.threshold", 1024));

    private ProxyRequest proxyRequest;

    public RequestPack(ProxyRequest request) {
        this.proxyRequest = request;
    }

    public RequestPack() {
    }

    public ProxyRequest getProxyRequest() {
        return proxyRequest;
    }

    @Override
    public void marshal(Pack pack) {

        FullHttpRequest request = proxyRequest.getRequest();
        LogBean logBean = proxyRequest.getLogBean();
        Props props = LogBeanTag.logBean(logBean);
        pack.putMarshallable(props);

        HttpVersion httpVersion = request.protocolVersion();
        pack.putVarstr(httpVersion.text());

        pack.putVarstr(request.method().name());
        String uri = request.uri();
        pack.putVarstr(uri);
        HttpHeaders headers = request.headers();
        StrStrMap map1 = new StrStrMap();
        for (Map.Entry<String, String> header : headers) {
            map1.put(header.getKey(), header.getValue());
        }
        pack.putMarshallable(map1);
        HttpHeaders trailingHeaders = request.trailingHeaders();
        StrStrMap map2 = new StrStrMap();
        for (Map.Entry<String, String> header : trailingHeaders) {
            map2.put(header.getKey(), header.getValue());
        }
        pack.putMarshallable(map2);
        ByteBuf content = request.content();
        int readableBytes = content.readableBytes();
        if (readableBytes > 0) {
            if (DynamicConf.getBoolean("request.content.compress.enable", true)) {
                byte[] originalData = new byte[readableBytes];
                content.readBytes(originalData);
                byte[] compressedData = compressor.compress(originalData);
                pack.putInt(compressedData.length);
                pack.putBuffer(Unpooled.wrappedBuffer(compressedData));
                if (originalData.length != compressedData.length) {
                    logger.info("request content compressed, original.len = {}, compressed.len = {}, host = {}, path = {}, traceId = {}",
                            originalData.length, compressedData.length, logBean.getHost(), logBean.getPath(), logBean.getTraceId());
                }
            } else {
                pack.putInt(readableBytes);
                pack.putBuffer(content);
            }
        } else {
            pack.putInt(0);
        }
    }

    @Override
    public void unmarshal(Unpack unpack) {

        Props props = new Props();
        unpack.popMarshallable(props);
        LogBean logBean = LogBeanTag.parseProps(props);

        HttpVersion httpVersion = HttpVersion.valueOf(unpack.popVarstr());

        HttpMethod httpMethod = HttpMethod.valueOf(unpack.popVarstr());
        String uri = unpack.popVarstr();
        StrStrMap map1 = new StrStrMap();
        unpack.popMarshallable(map1);
        HttpHeaders headers = new DefaultHttpHeaders();
        for (Map.Entry<String, String> entry : map1.m_map.entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }

        StrStrMap map2 = new StrStrMap();
        unpack.popMarshallable(map2);
        HttpHeaders trailingHeaders = new DefaultHttpHeaders();
        for (Map.Entry<String, String> entry : map2.m_map.entrySet()) {
            trailingHeaders.set(entry.getKey(), entry.getValue());
        }
        FullHttpRequest request;
        int size = unpack.popInt();
        if (size > 0) {
            byte[] raw = unpack.popFetch(size);
            byte[] decompressed = compressor.decompress(raw);
            ByteBuf buffer = Unpooled.wrappedBuffer(decompressed);
            if (raw.length != decompressed.length) {
                logger.info("request content decompressed, original.len = {}, decompressed.len = {}, host = {}, path = {}, traceId = {}",
                        raw.length, decompressed.length, logBean.getHost(), logBean.getPath(), logBean.getTraceId());
            }
            request = new DefaultFullHttpRequest(httpVersion, httpMethod, uri, buffer, headers, trailingHeaders);
        } else {
            request = new DefaultFullHttpRequest(httpVersion, httpMethod, uri, Unpooled.buffer(0), headers, trailingHeaders);
        }
        this.proxyRequest = new ProxyRequest(request, logBean);
    }
}

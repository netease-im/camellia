package com.netease.nim.camellia.http.accelerate.proxy.core.transport.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.StrStrMap;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LogBean;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
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
public class RequestAckPack extends ProxyPackBody {

    private static final Logger logger = LoggerFactory.getLogger(RequestAckPack.class);
    private static final CamelliaCompressor compressor = new CamelliaCompressor(DynamicConf.getInt("response.content.compress.threshold", 1024));

    private ProxyResponse proxyResponse;

    public RequestAckPack(ProxyResponse response) {
        this.proxyResponse = response;
    }

    public RequestAckPack() {
    }

    public ProxyResponse getProxyResponse() {
        return proxyResponse;
    }

    @Override
    public void marshal(Pack pack) {

        FullHttpResponse response = proxyResponse.getResponse();
        LogBean logBean = proxyResponse.getLogBean();
        Props props = LogBeanTag.logBean(logBean);
        pack.putMarshallable(props);

        HttpVersion httpVersion = response.protocolVersion();
        pack.putVarstr(httpVersion.text());

        HttpResponseStatus status = response.status();
        pack.putInt(status.code());
        HttpHeaders headers = response.headers();
        StrStrMap map1 = new StrStrMap();
        for (Map.Entry<String, String> header : headers) {
            map1.put(header.getKey(), header.getValue());
        }
        pack.putMarshallable(map1);

        HttpHeaders trailingHeaders = response.trailingHeaders();
        StrStrMap map2 = new StrStrMap();
        for (Map.Entry<String, String> header : trailingHeaders) {
            map2.put(header.getKey(), header.getValue());
        }
        pack.putMarshallable(map2);

        ByteBuf content = response.content();
        int readableBytes = content.readableBytes();
        if (readableBytes > 0) {
            if (DynamicConf.getBoolean("response.content.compress.enable", true)) {
                byte[] originalData = new byte[readableBytes];
                content.readBytes(originalData);
                byte[] compressedData = compressor.compress(originalData);
                pack.putInt(compressedData.length);
                pack.putBuffer(Unpooled.wrappedBuffer(compressedData));
                if (originalData.length != compressedData.length) {
                    logger.info("response content compressed, original.len = {}, compressed.len = {}, host = {}, path = {}, traceId = {}",
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

        HttpResponseStatus httpResponseStatus = HttpResponseStatus.valueOf(unpack.popInt());

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

        FullHttpResponse response;
        int size = unpack.popInt();
        if (size > 0) {
            byte[] raw = unpack.popFetch(size);
            byte[] decompressed = compressor.decompress(raw);
            ByteBuf buffer = Unpooled.wrappedBuffer(decompressed);
            if (raw.length != decompressed.length) {
                logger.info("response content decompressed, original.len = {}, decompressed.len = {}, host = {}, path = {}, traceId = {}",
                        raw.length, decompressed.length, logBean.getHost(), logBean.getPath(), logBean.getTraceId());
            }
            response = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, buffer, headers, trailingHeaders);
        } else {
            response = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, Unpooled.buffer(0), headers, trailingHeaders);
        }
        this.proxyResponse = new ProxyResponse(response, logBean);
    }
}

package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.StrStrMap;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LogBean;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.util.Map;

/**
 * Created by caojiajun on 2023/7/7
 */
public class RequestAckPack extends TcpPackBody {

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
        if (content.readableBytes() > 0) {
            pack.putInt(content.readableBytes());
            pack.putBuffer(content);
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
        int i = unpack.popInt();
        if (i > 0) {
            ByteBuf buffer = unpack.getBuffer();
            response = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, buffer, headers, trailingHeaders);
        } else {
            response = new DefaultFullHttpResponse(httpVersion, httpResponseStatus, Unpooled.buffer(0), headers, trailingHeaders);
        }
        this.proxyResponse = new ProxyResponse(response, logBean);
    }
}

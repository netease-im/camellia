package com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Props;
import com.netease.nim.camellia.codec.StrStrMap;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.LogBean;
import com.netease.nim.camellia.http.accelerate.proxy.core.context.ProxyRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import java.util.Map;

/**
 * Created by caojiajun on 2023/7/7
 */
public class RequestPack extends TcpPackBody {

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
            ByteBuf buffer = Unpooled.wrappedBuffer(unpack.popFetch(size));
            request = new DefaultFullHttpRequest(httpVersion, httpMethod, uri, buffer, headers, trailingHeaders);
        } else {
            request = new DefaultFullHttpRequest(httpVersion, httpMethod, uri, Unpooled.buffer(0), headers, trailingHeaders);
        }
        logBean.setHost(headers.get("Host"));
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        logBean.setPath(queryStringDecoder.path());
        this.proxyRequest = new ProxyRequest(request, logBean);
    }
}

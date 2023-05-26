package com.netease.nim.camellia.hot.key.common.netty.pack;

import com.netease.nim.camellia.hot.key.common.netty.codec.Pack;
import com.netease.nim.camellia.hot.key.common.netty.codec.Props;
import com.netease.nim.camellia.hot.key.common.netty.codec.Unpack;

/**
 * 获取配置的请求包
 * Created by caojiajun on 2023/5/8
 */
public class GetConfigPack extends HotKeyPackBody {

    private static enum Tag {
        namespace(1),
        source(2),
        ;

        private final int value;

        Tag(int value) {
            this.value = value;
        }
    }

    private String namespace;
    private String source;

    public GetConfigPack() {
    }

    public GetConfigPack(String namespace, String source) {
        this.namespace = namespace;
        this.source = source;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSource() {
        return source;
    }

    @Override
    public void marshal(Pack pack) {
        Props props = new Props();
        props.put(Tag.namespace.value, namespace);
        if (source != null) {
            props.put(Tag.source.value, source);
        }
        pack.putMarshallable(props);
    }

    @Override
    public void unmarshal(Unpack unpack) {
        Props property = new Props();
        unpack.popMarshallable(property);
        namespace = property.get(Tag.namespace.value);
        source = property.get(Tag.source.value);
    }
}

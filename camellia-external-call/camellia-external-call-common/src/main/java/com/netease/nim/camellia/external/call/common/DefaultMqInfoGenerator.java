package com.netease.nim.camellia.external.call.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2023/2/28
 */
public class DefaultMqInfoGenerator implements MqInfoGenerator {

    private final String namespace;
    private final String server;

    public DefaultMqInfoGenerator(String namespace, String server) {
        this.namespace = namespace;
        this.server = server;
    }

    @Override
    public List<MqInfo> fast() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_fast_1"));
        list.add(new MqInfo(server, namespace + "_fast_2"));
        list.add(new MqInfo(server, namespace + "_fast_3"));
        return list;
    }

    @Override
    public List<MqInfo> fastError() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_fast_error"));
        return list;
    }

    @Override
    public List<MqInfo> slow() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_slow"));
        return list;
    }

    @Override
    public List<MqInfo> slowError() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_slow_error"));
        return list;
    }

    @Override
    public List<MqInfo> retry() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_retry"));
        return list;
    }

    @Override
    public List<MqInfo> retryHighPriority() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_retry_high_priority"));
        return list;
    }

    @Override
    public List<MqInfo> heavyTraffic() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_heavy_traffic"));
        return list;
    }

    @Override
    public List<MqInfo> isolation() {
        List<MqInfo> list = new ArrayList<>();
        list.add(new MqInfo(server, namespace + "_isolation"));
        return list;
    }
}

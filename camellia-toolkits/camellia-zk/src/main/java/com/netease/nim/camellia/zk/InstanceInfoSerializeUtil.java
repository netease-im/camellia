package com.netease.nim.camellia.zk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.discovery.ServerNode;

import java.nio.charset.StandardCharsets;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class InstanceInfoSerializeUtil {

    public static byte[] serialize(ServerNode serverNode) {
        JSONObject instanceInfo = new JSONObject();
        instanceInfo.put("instance", serverNode);
        instanceInfo.put("registerTime", System.currentTimeMillis());
        String string = JSONObject.toJSONString(instanceInfo);
        JSONObject jsonObject = JSONObject.parseObject(string);
        JSONObject instance = jsonObject.getJSONObject("instance");
        jsonObject.put("proxy", JSONObject.parseObject(instance.toJSONString()));//为了兼容低版本的camellia-redis-proxy-discovery-zk
        return jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8);
    }

    public static ServerNode deserialize(byte[] data) {
        if (data == null || data.length == 0) return null;
        JSONObject json = JSONObject.parseObject(new String(data, StandardCharsets.UTF_8));
        ServerNode serverNode = json.getObject("instance", ServerNode.class);
        if (serverNode == null) {
            serverNode = json.getObject("proxy", ServerNode.class);//为了兼容低版本的camellia-redis-proxy-discovery-zk
        }
        return serverNode;
    }
}

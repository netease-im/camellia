package com.netease.nim.camellia.core.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShadingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.model.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/25.
 */
public class Cache {

    private Map<String, String> map = new HashMap<>();
    private Resource resource;

    public Cache(Resource resource) {
        this.resource = resource;
    }

    @ReadOp
    public String get(@ShadingParam String key) {
        System.out.println("get, resource = " + resource.getUrl() + ", key = " + key);
        return map.get(key);
    }

    @WriteOp
    public int set(@ShadingParam String key, String value) {
        System.out.println("set, resource = " + resource.getUrl() + ", key = " + key);
        map.put(key, value);
        return 1;
    }

    @WriteOp
    public int delete(@ShadingParam String key) {
        System.out.println("delete, resource = " + resource.getUrl() + ", key = " + key);
        map.remove(key);
        return 1;
    }

    @ReadOp
    public Map<String, String> getBulk(@ShadingParam(type = ShadingParam.Type.Collection) String... keys) {
        System.out.println("getBulk, resource = " + resource.getUrl() + ", keys = " + keys);
        Map<String, String> ret = new HashMap<>();
        for (String key : keys) {
            String value = map.get(key);
            if (value != null) {
                ret.put(key, value);
            }
        }
        return ret;
    }

    @WriteOp
    public int setBulk(@ShadingParam(type = ShadingParam.Type.Collection) Map<String, String> kvs) {
        System.out.println("setBulk, resource = " + resource.getUrl() + ", keys = " + JSONObject.toJSONString(kvs.keySet()));
        for (Map.Entry<String, String> entry : kvs.entrySet()) {
            this.map.put(entry.getKey(), entry.getValue());
        }
        return kvs.size();
    }
}

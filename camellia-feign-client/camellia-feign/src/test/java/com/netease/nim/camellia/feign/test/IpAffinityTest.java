package com.netease.nim.camellia.feign.test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.discovery.IpAffinityConfig;
import com.netease.nim.camellia.core.discovery.IpAffinityConfigUtils;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by caojiajun on 2025/3/25
 */
public class IpAffinityTest {

    @Test
    public void test1() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", IpAffinityConfig.Type.affinity.name());
        JSONArray jsonArray = new JSONArray();
        {
            JSONObject json = new JSONObject();
            json.put("source", "10.12.0.0/16");
            json.put("target", "10.12.0.0/16");
            jsonArray.add(json);
        }
        {
            JSONObject json = new JSONObject();
            json.put("source", "10.13.0.0/16");
            json.put("target", "10.13.0.0/16");
            jsonArray.add(json);
        }
        jsonObject.put("config", jsonArray);

        IpAffinityConfig config = IpAffinityConfigUtils.parse(jsonObject.toJSONString());
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.12.1.2", "10.13.2.3");
            Assert.assertFalse(match);
        }
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.12.10.2", "10.12.2.3");
            Assert.assertTrue(match);
        }
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.13.19.2", "10.12.21.23");
            Assert.assertFalse(match);
        }
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.14.71.2", "10.14.22.53");
            Assert.assertFalse(match);
        }
    }

    @Test
    public void test2() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", IpAffinityConfig.Type.anti_affinity.name());
        JSONArray jsonArray = new JSONArray();
        {
            JSONObject json = new JSONObject();
            json.put("source", "10.12.0.0/16");
            json.put("target", "10.12.0.0/16");
            jsonArray.add(json);
        }
        {
            JSONObject json = new JSONObject();
            json.put("source", "10.13.0.0/16");
            json.put("target", "10.13.0.0/16");
            jsonArray.add(json);
        }
        jsonObject.put("config", jsonArray);

        IpAffinityConfig config = IpAffinityConfigUtils.parse(jsonObject.toJSONString());
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.12.1.2", "10.13.2.3");
            Assert.assertTrue(match);
        }
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.12.10.2", "10.12.2.3");
            Assert.assertFalse(match);
        }
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.13.19.2", "10.12.21.23");
            Assert.assertTrue(match);
        }
        {
            boolean match = IpAffinityConfigUtils.match(config, "10.14.71.2", "10.14.22.53");
            Assert.assertTrue(match);
        }
    }

}

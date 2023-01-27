package com.netease.nim.camellia.tools.samples;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.tools.cache.CamelliaLocalCache;

/**
 * Created by caojiajun on 2023/1/27
 */
public class LocalCacheSamples {
    public static void main(String[] args) {
        CamelliaLocalCache cache = new CamelliaLocalCache();
        //put
        cache.put("tag", "key1", new User("jim"), 10);//expireSeconds < 0 means no ttl
        //get
        User user = cache.get("tag", "key1", User.class);
        System.out.println(JSONObject.toJSONString(user));
        //put null
        cache.put("tag", "key2", null, 10);
        //get null
        CamelliaLocalCache.ValueWrapper valueWrapper = cache.get("tag", "key2");
        User u = (User)valueWrapper.get();
        System.out.println(u);
        //get missing
        CamelliaLocalCache.ValueWrapper valueWrapper2 = cache.get("tag", "key3");
        System.out.println(valueWrapper2 == null);
        //del
        cache.evict("tag", "key3");
        //clear all
        cache.clear();
    }

    private static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

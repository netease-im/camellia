package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import org.springframework.stereotype.Component;

/**
 *
 * Created by caojiajun on 2019/12/10.
 */
@Component
public class RedisChecker implements IResourceChecker {

    @Override
    public boolean check(String url) {
        try {
            Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
            return resource != null && resource.getUrl().equals(url);
        } catch (Exception e) {
            return false;
        }
    }
}

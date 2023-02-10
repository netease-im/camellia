package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisType;
import com.netease.nim.camellia.redis.resource.RedisClientResourceUtil;
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
            RedisClientResourceUtil.parseResourceByUrl(new Resource(url));
            return true;
        } catch (Exception e) {
            return url != null && url.startsWith(RedisType.CamelliaRedisProxy.getPrefix()) && url.contains("@");
        }
    }
}

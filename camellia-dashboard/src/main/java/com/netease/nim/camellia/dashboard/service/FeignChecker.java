package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.feign.resource.FeignResourceUtils;

/**
 * Created by caojiajun on 2022/3/8
 */
public class FeignChecker implements IResourceChecker {
    @Override
    public boolean check(String url) {
        try {
            FeignResourceUtils.parseResourceByUrl(new Resource(url));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

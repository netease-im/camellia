package com.netease.nim.camellia.dashboard.service;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.springframework.stereotype.Component;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
@Component
public class HBaseChecker implements IResourceChecker {

    @Override
    public boolean check(String url) {
        try {
            HBaseResourceUtil.parseResourceByUrl(new Resource(url));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

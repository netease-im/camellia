package com.netease.nim.camellia.cache.samples.controller;

import com.netease.nim.camellia.cache.core.CamelliaCachePrefixGetter;
import org.springframework.stereotype.Service;

/**
 * Created by caojiajun on 2022/12/20
 */
@Service
public class CustomCachePrefixGetter implements CamelliaCachePrefixGetter {
    @Override
    public String get() {
        return "v1";
    }
}

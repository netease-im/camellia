package com.netease.nim.camellia.dashboard.service;

/**
 * Created by caojiajun on 2022/3/8
 */
public class FeignChecker implements IResourceChecker {
    @Override
    public boolean check(String url) {
        return false;
    }
}

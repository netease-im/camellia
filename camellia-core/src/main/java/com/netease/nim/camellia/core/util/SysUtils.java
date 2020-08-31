package com.netease.nim.camellia.core.util;

/**
 *
 * Created by caojiajun on 2020/1/19.
 */
public class SysUtils {

    public static int getCpuNum() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static int getCpuHalfNum() {
        int cpuNum = getCpuNum();
        if (cpuNum <= 1) return 1;
        return cpuNum / 2;
    }
}

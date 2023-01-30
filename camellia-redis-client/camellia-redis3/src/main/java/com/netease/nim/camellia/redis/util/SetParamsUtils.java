package com.netease.nim.camellia.redis.util;

import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Created by caojiajun on 2023/1/30
 */
public class SetParamsUtils {

    public static SetParams setParams(byte[] nxxx, byte[] expx, long time) {
        String nxxxStr = null;
        if (nxxx != null) {
            nxxxStr = SafeEncoder.encode(nxxx);
        }
        String expxStr = null;
        if (expx != null) {
            expxStr = SafeEncoder.encode(expx);
        }
        return setParams(nxxxStr, expxStr, time);
    }
    public static SetParams setParams(String nxxx, String expx, long time) {
        SetParams setParams = new SetParams();
        if (nxxx != null) {
            if (nxxx.equalsIgnoreCase("nx")) {
                setParams.nx();
            } else if (nxxx.equalsIgnoreCase("xx")) {
                setParams.xx();
            }
        }
        if (expx != null) {
            if (expx.equalsIgnoreCase("ex")) {
                setParams.ex(time);
            } else if (expx.equalsIgnoreCase("px")) {
                setParams.px(time);
            }
        }
        return setParams;
    }
}

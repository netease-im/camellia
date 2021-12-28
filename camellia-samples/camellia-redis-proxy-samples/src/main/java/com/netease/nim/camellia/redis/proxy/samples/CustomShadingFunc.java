package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.core.client.env.AbstractSimpleShadingFunc;

/**
 *
 * Created by caojiajun on 2020/9/27
 */
public class CustomShadingFunc extends AbstractSimpleShadingFunc {

    @Override
    public int shadingCode(byte[] key) {
        if (key == null) return 0;
        if (key.length == 0) return 0;
        int h = 0;
        for (byte d : key) {
            h = 31 * h + d;
        }
        return (h < 0) ? -h : h;
    }
}

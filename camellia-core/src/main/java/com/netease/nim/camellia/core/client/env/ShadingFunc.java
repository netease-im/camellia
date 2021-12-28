package com.netease.nim.camellia.core.client.env;

/**
 *
 * Created by caojiajun on 2019/11/8.
 */
public interface ShadingFunc {

    /**
     * should return positive int
     */
    int shadingCode(byte[]... data);

}

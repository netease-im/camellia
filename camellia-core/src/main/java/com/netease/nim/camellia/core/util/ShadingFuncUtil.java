package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ShadingFunc;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class ShadingFuncUtil {

    public static ShadingFunc forName(String className) {
        try {
            Class<?> aClass = Class.forName(className);
            Object shadingFuncObj = aClass.newInstance();
            if (shadingFuncObj instanceof ShadingFunc) {
                return (ShadingFunc) shadingFuncObj;
            } else {
                throw new IllegalArgumentException("shadingFunc not instance of " + ShadingFunc.class.getName());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}

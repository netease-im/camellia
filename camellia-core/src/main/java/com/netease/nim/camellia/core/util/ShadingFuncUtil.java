package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ShadingFunc;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class ShadingFuncUtil {

    public static ShadingFunc forName(String className) {
        try {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
            Object shadingFuncObj = clazz.newInstance();
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

package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.env.ShardingFunc;

/**
 *
 * Created by caojiajun on 2019/12/13.
 */
public class ShardingFuncUtil {

    public static ShardingFunc forName(String className) {
        try {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
            Object shardingFuncObj = clazz.newInstance();
            if (shardingFuncObj instanceof ShardingFunc) {
                return (ShardingFunc) shardingFuncObj;
            } else {
                throw new IllegalArgumentException("shardingFunc not instance of " + ShardingFunc.class.getName());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}

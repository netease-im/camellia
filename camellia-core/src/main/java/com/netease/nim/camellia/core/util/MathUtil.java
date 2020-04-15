package com.netease.nim.camellia.core.util;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public class MathUtil {

    //是否是2的N次幂
    public static boolean is2Power(int bucketSize) {
        return (bucketSize > 0) && ((bucketSize & (bucketSize - 1)) == 0);
    }

    //取余
    public static int mod(boolean is2Power, int original, int divisor) {
        if (is2Power) {
            //如果是2的N次幂，则取余可以使用位运算加快
            return original & (divisor - 1);
        } else {
            return original % divisor;
        }
    }
}

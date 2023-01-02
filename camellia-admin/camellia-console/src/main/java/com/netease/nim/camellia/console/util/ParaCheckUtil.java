package com.netease.nim.camellia.console.util;

import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.exception.AppException;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class ParaCheckUtil {
    public static void checkParam(Object param, String paramName) {
        if(param == null) {
            throw new AppException(AppCode.PARAM_WRONG, paramName + " is empty");
        }
    }
}

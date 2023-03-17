package com.netease.nim.camellia.config.util;

import com.netease.nim.camellia.config.conf.LogBean;
import com.netease.nim.camellia.config.exception.AppException;
import org.springframework.http.HttpStatus;

/**
 * Created by caojiajun on 2023/3/16
 */
public class ParamCheckUtils {

    public static void checkValidFlag(Integer validFlag) {
        if (validFlag != null) {
            if (validFlag != 0 && validFlag != 1) {
                LogBean.get().addProps("validFlag.illegal", true);
                throw new AppException(HttpStatus.BAD_REQUEST.value(), "validFlag illegal");
            }
        }
    }

    public static void checkParam(String param, String paramName, int maxLen) {
        if (param == null) {
            LogBean.get().addProps(paramName + ".is.null", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), paramName + " is null");
        }
        if (param.length() > maxLen) {
            LogBean.get().addProps("namespace.len.exceed", true);
            throw new AppException(HttpStatus.BAD_REQUEST.value(), paramName + " len exceed");
        }
    }
}

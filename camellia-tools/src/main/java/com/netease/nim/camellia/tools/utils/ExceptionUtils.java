package com.netease.nim.camellia.tools.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

/**
 * Created by caojiajun on 2022/3/9
 */
public class ExceptionUtils {

    public static Throwable onError(Throwable e) {
        while (true) {
            if (e instanceof ExecutionException) {
                if (e.getCause() == null) {
                    break;
                } else {
                    e = e.getCause();
                }
            } else if (e instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) e).getTargetException();
                if (targetException == null) {
                    break;
                } else {
                    e = targetException;
                }
            } else if (e instanceof UndeclaredThrowableException) {
                if (e.getCause() == null) {
                    break;
                } else {
                    e = e.getCause();
                }
            }
            else {
                break;
            }
        }
        return e;
    }
}

package com.netease.nim.camellia.http.console;

import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2023/6/30
 */
public class ConsoleUtils {

    public static String getParam(Map<String, List<String>> params, String paramName) {
        if (params == null) {
            return null;
        }
        List<String> namespaceParams = params.get(paramName);
        if (namespaceParams == null || namespaceParams.isEmpty()) {
            return null;
        }
        return namespaceParams.get(0);
    }
}

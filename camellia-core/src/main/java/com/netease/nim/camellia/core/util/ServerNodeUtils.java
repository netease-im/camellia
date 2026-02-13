package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.discovery.ServerNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2026/2/13
 */
public class ServerNodeUtils {

    public static List<ServerNode> parse(String config) {
        List<ServerNode> originalList = new ArrayList<>();
        String[] split = config.split(",");
        for (String str : split) {
            String[] split1 = str.split(":");
            originalList.add(new ServerNode(split1[0], Integer.parseInt(split1[1])));
        }
        return originalList;
    }

}

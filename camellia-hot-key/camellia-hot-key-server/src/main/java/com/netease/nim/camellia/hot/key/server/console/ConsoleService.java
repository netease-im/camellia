package com.netease.nim.camellia.hot.key.server.console;

import java.util.List;
import java.util.Map;

public interface ConsoleService {

    ConsoleResult status();

    ConsoleResult online();

    ConsoleResult offline();

    ConsoleResult check();

    ConsoleResult monitor();

    ConsoleResult topN(String namespace, Integer backtrack);

    ConsoleResult prometheus();

    ConsoleResult reload();

    ConsoleResult custom(Map<String, List<String>> params);

}

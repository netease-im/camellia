package com.netease.nim.camellia.redis.proxy.console;

import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
public interface ConsoleService {

    ConsoleResult status();

    ConsoleResult online();

    ConsoleResult offline();

    ConsoleResult check();

    ConsoleResult monitor();

    ConsoleResult reload();

    ConsoleResult info(Map<String, List<String>> params);

    ConsoleResult custom(Map<String, List<String>> params);

    ConsoleResult detect(Map<String, List<String>> params);
}

package com.netease.nim.camellia.hot.key.server.console;

import com.netease.nim.camellia.http.console.ConsoleApi;
import com.netease.nim.camellia.http.console.ConsoleResult;
import com.netease.nim.camellia.http.console.IConsoleService;

import java.util.List;
import java.util.Map;

public interface ConsoleService extends IConsoleService {

    @ConsoleApi(uri = "/status")
    ConsoleResult status();

    @ConsoleApi(uri = "/online")
    ConsoleResult online();

    @ConsoleApi(uri = "/offline")
    ConsoleResult offline();

    @ConsoleApi(uri = "/check")
    ConsoleResult check();

    @ConsoleApi(uri = "/monitor")
    ConsoleResult monitor();

    @ConsoleApi(uri = "/topN")
    ConsoleResult topN(Map<String, List<String>> params);

    @ConsoleApi(uri = "/prometheus")
    ConsoleResult prometheus();

    @ConsoleApi(uri = "/reload")
    ConsoleResult reload();

    @ConsoleApi(uri = "/metrics")
    ConsoleResult metrics();

    @ConsoleApi(uri = "/custom")
    ConsoleResult custom(Map<String, List<String>> params);

}

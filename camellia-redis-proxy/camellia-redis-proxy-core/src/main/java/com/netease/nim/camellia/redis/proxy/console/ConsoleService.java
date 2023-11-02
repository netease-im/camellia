package com.netease.nim.camellia.redis.proxy.console;

import com.netease.nim.camellia.http.console.ConsoleApi;
import com.netease.nim.camellia.http.console.ConsoleResult;
import com.netease.nim.camellia.http.console.IConsoleService;

import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2019/11/28.
 */
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

    @ConsoleApi(uri = "/prometheus")
    ConsoleResult prometheus();

    @ConsoleApi(uri = "/reload")
    ConsoleResult reload();

    @ConsoleApi(uri = "/info")
    ConsoleResult info(Map<String, List<String>> params);

    @ConsoleApi(uri = "/custom")
    ConsoleResult custom(Map<String, List<String>> params);

    @ConsoleApi(uri = "/detect")
    ConsoleResult detect(Map<String, List<String>> params);

    @ConsoleApi(uri = "/shutdownUpstreamClient")
    ConsoleResult shutdownUpstreamClient(Map<String, List<String>> params);
}

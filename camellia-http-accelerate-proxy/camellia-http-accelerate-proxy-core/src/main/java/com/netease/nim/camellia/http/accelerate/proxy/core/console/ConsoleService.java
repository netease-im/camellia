package com.netease.nim.camellia.http.accelerate.proxy.core.console;

import com.netease.nim.camellia.http.console.ConsoleApi;
import com.netease.nim.camellia.http.console.ConsoleResult;
import com.netease.nim.camellia.http.console.IConsoleService;


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

    @ConsoleApi(uri = "/reload")
    ConsoleResult reload();
}

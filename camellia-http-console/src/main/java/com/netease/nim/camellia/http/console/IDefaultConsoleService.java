package com.netease.nim.camellia.http.console;

/**
 * Created by caojiajun on 2023/6/30
 */
public interface IDefaultConsoleService extends IConsoleService {

    @ConsoleApi(uri = "/status")
    ConsoleResult status();
}

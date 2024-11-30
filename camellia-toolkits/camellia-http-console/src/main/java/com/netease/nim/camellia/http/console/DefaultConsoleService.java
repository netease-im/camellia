package com.netease.nim.camellia.http.console;

/**
 * Created by caojiajun on 2023/6/30
 */
public class DefaultConsoleService implements IDefaultConsoleService {
    @Override
    public ConsoleResult status() {
        return ConsoleResult.success();
    }
}

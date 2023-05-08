package com.netease.nim.camellia.hot.key.common.netty.pack;

/**
 * Created by caojiajun on 2023/5/8
 */
public enum HotKeyCommand {

    HEARTBEAT((byte) 1), //client -> server: heartbeat
    GET_CONFIG((byte) 2), //client -> server: get config
    PUSH((byte) 3), //client -> server: push key collect counter
    NOTIFY_HOTKEY((byte) 4), //server -> client: notify hot-key(discovery/delete/update) to client
    NOTIFY_CONFIG((byte) 5),//server -> client: notify new config to client
    ;

    private final byte cmd;

    HotKeyCommand(byte cmd) {
        this.cmd = cmd;
    }

    public byte getCmd() {
        return cmd;
    }

    public static HotKeyCommand getByValue(byte cmd) {
        for (HotKeyCommand command : HotKeyCommand.values()) {
            if (command.getCmd() == cmd) {
                return command;
            }
        }
        return null;
    }
}

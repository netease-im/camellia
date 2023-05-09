package com.netease.nim.camellia.hot.key.common.netty;


import com.netease.nim.camellia.hot.key.common.netty.pack.*;

/**
 * Created by caojiajun on 2023/5/9
 */
public interface HotKeyPackBizHandler {

    default NotifyHotKeyRepPack onNotifyHotKeyPack(NotifyHotKeyPack pack) {
        return NotifyHotKeyRepPack.INSTANCE;
    }

    default NotifyHotKeyConfigRepPack onNotifyHotKeyConfigPack(NotifyHotKeyConfigPack pack) {
        return NotifyHotKeyConfigRepPack.INSTANCE;
    }

    default PushRepPack onPushPack(PushPack pack) {
        return PushRepPack.INSTANCE;
    }

    default HeartbeatRepPack onHeartbeatPack(HeartbeatPack pack) {
        return HeartbeatRepPack.INSTANCE;
    }

    default GetConfigRepPack onGetConfigPack(GetConfigPack pack) {
        return null;
    }
}

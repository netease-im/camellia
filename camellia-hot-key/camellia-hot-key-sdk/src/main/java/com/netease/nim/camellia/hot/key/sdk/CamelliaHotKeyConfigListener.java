package com.netease.nim.camellia.hot.key.sdk;

import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;

/**
 * Created by caojiajun on 2023/5/9
 */
public interface CamelliaHotKeyConfigListener {

    /**
     * hot key config变更通知
     * @param hotKeyConfig 新的HotKeyConfig
     */
    void onHotKeyConfigChange(HotKeyConfig hotKeyConfig);

}

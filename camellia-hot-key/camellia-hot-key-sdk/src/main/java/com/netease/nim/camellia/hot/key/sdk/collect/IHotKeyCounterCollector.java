package com.netease.nim.camellia.hot.key.sdk.collect;

import com.netease.nim.camellia.hot.key.common.model.KeyAction;
import com.netease.nim.camellia.hot.key.common.model.KeyCounter;

import java.util.List;

/**
 * Created by caojiajun on 2023/7/3
 */
public interface IHotKeyCounterCollector {

    void push(String namespace, String key, KeyAction keyAction, long count);

    List<KeyCounter> collect();
}

package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.slot;

import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/6
 */
public interface IKeyManifest {

    /**
     * init and load
     * 初始化
     * @throws IOException exception
     */
    void load() throws IOException;

    /**
     * get slot info
     * 获取slot-info
     * @param slot slot
     * @return slot info
     * @throws IOException exception
     */
    SlotInfo get(short slot) throws IOException;

    /**
     * init slot info
     * 初始化slot info
     * @param slot slot
     * @return slot info
     * @throws IOException exception
     */
    SlotInfo init(short slot) throws IOException;

    /**
     * expand slot info, capacity will expand to double size
     * 扩容slot-info，容量会在当前基础上加倍
     * @param slot slot
     * @return slot info
     * @throws IOException exception
     */
    SlotInfo expand(short slot) throws IOException;
}

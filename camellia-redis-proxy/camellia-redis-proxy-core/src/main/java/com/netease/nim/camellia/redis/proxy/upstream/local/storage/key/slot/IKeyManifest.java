package com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.slot;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2025/1/6
 */
public interface IKeyManifest {

    /**
     * get dir
     * @return dir
     */
    String dir();

    /**
     * get fileId list
     * @return list
     */
    Set<Long> getFileIds();

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
     */
    SlotInfo get(short slot);

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
     * @param slotInfo slot info
     * @return slot info
     * @throws IOException exception
     */
    SlotInfo expand(short slot, SlotInfo slotInfo) throws IOException;

    /**
     * commit new slot info
     * @param slot slot
     * @param newSlotInfo new slot info
     * @param rollBackSlotInfos new slot info
     * @throws IOException exception
     */
    void commit(short slot, SlotInfo newSlotInfo, Set<SlotInfo> rollBackSlotInfos) throws IOException;
}

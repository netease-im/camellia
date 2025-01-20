package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import java.io.IOException;
import java.util.Map;

/**
 * Created by caojiajun on 2025/1/17
 */
public interface IWalManifest {

    /**
     * 初始化加载
     */
    void load() throws IOException;

    /**
     * 这个slot的wal应该写在哪个文件上
     * @param slot slot
     * @return 文件id
     */
    long fileId(short slot) throws IOException;

    /**
     * wal文件已经写到哪里了
     * @param fileId 文件id
     * @return 下一个可以写数据的offset
     */
    long getFileWriteNextOffset(long fileId);

    /**
     * 写完日志后，更新wal文件已经写到哪里了
     * @param fileId 文件id
     * @param nextOffset 下一个可以写数据的offset
     */
    void updateFileWriteNextOffset(long fileId, long nextOffset);

    /**
     * 查询slot最新一条日志写入的offset
     * @param slot slot
     * @return offset
     */
    SlotWalOffset getSlotWalOffsetEnd(short slot);

    /**
     * 更新slot最新一条日志写入的offset位置
     * @param slot slot
     * @param offset offset
     */
    void updateSlotWalOffsetEnd(short slot, SlotWalOffset offset);

    /**
     * 更新slot最早一条日志的offset，小于等于这个offset的日志都无效了
     * @param slot slot
     * @param offset offset
     */
    void updateSlotWalOffsetStart(short slot, SlotWalOffset offset);

    /**
     * 查询所有slot的最早有效记录的offset，小于等于这个offset的日志都无效了
     * @return 记录
     */
    Map<Short, SlotWalOffset> getSlotWalOffsetStartMap();
}

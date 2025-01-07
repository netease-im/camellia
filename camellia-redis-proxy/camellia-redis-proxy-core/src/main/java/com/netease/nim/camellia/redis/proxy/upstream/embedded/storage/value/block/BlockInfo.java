package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block;



/**
 * Created by caojiajun on 2025/1/6
 */
public record BlockInfo(BlockType blockType, BlockLocation blockLocation, byte[] data) {
}

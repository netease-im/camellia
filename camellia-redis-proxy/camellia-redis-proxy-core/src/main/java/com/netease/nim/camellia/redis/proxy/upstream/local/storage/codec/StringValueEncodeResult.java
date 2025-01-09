package com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec;

import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockInfo;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.ValueLocation;

import java.util.List;

/**
 * Created by caojiajun on 2025/1/8
 */
public record StringValueEncodeResult(List<BlockInfo> blockInfos, List<ValueLocation> oldLocations) {

}

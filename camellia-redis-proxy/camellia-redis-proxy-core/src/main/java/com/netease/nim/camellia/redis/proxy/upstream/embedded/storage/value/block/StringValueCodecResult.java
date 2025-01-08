package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.value.block;

import java.util.List;

/**
 * Created by caojiajun on 2025/1/8
 */
public record StringValueCodecResult(List<BlockInfo> blockInfos, List<ValueLocation> oldLocations) {

}

package com.netease.nim.camellia.redis.pipeline;

import redis.clients.jedis.*;

import java.util.List;


public interface ICamelliaRedisPipeline0 extends ICamelliaRedisPipeline {

    Response<List<byte[]>> mget0(byte[] shardingKey, byte[]... keys);

    Response<List<String>> mget0(String shardingKey, String... keys);
}

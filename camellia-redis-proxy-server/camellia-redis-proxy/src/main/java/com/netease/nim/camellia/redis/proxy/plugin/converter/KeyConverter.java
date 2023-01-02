package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;

/**
 * Created by caojiajun on 2021/8/19
 */
public interface KeyConverter {

    /**
     * key转换
     * @param commandContext 上下文
     * @param redisCommand 所属命令
     * @param originalKey 原始key
     * @return 转换后的key
     */
    byte[] convert(CommandContext commandContext, RedisCommand redisCommand, byte[] originalKey);

    /**
     * key反向转换
     * @param commandContext 上下文
     * @param redisCommand 所属命令
     * @param convertedKey 转换后的key
     * @return 原始key
     */
    byte[] reverseConvert(CommandContext commandContext, RedisCommand redisCommand, byte[] convertedKey);
}
